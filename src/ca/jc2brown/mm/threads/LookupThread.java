package ca.jc2brown.mm.threads;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ca.jc2brown.mm.utils.Metadata;
import ca.jc2brown.mm.services.AlarmService;
import ca.jc2brown.mm.services.FileService;
import ca.jc2brown.mm.utils.Filter;
import ca.jc2brown.mm.utils.GroupedProperties;
import ca.jc2brown.mm.utils.MMFile;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


/**
 * LookupService takes a title from the pending queue, modifies it to more likely generate a hit,
 * and makes an IMDB API call to attempt to obtain data for that title.
 * 
 */
public class LookupThread extends AlarmThread implements Runnable {
	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger( LookupThread.class.getName() );
	
	private static final String badchars = "[\\[\\]\\(\\)\\-\\+\\%\\$\\#\\@\\!\\?\\'\\\"\\,\\.]";
	private static final String QUERY_TEMPLATE = "http://www.imdbapi.com/?t={title}&plot=full";	
	
	private Gson gson;
	private List<String> badwords;
	private FileService fileService;
	

	
	public LookupThread(String cur, String next, GroupedProperties conf, FileService fileService, AlarmService alarmService) {
		super(cur, next, alarmService);
		this.gson = new Gson();
		this.fileService = fileService;
		badwords = conf.getValueList("badwords");
		badwords.addAll(conf.getValueList("videotypes"));
		log.debug("LookupThread initialized");
	}

	private String formatTitle(String title) throws UnsupportedEncodingException {

		int cutoff = Integer.MAX_VALUE;
		title = title.toLowerCase();
		
		String part = Metadata.getMultipart(title);
		if ( part != null ) {
			part = part.toLowerCase();
			cutoff = title.indexOf(part);
		}
		
		for (String badword : badwords) {
			badword = badword.toLowerCase();
			int index = title.indexOf(badword);
			if ( index >= 1 && index < cutoff ) {
				cutoff = index;
			}
		}
		
		if ( cutoff < Integer.MAX_VALUE ) 
			title = title.substring(0, cutoff);
		
		
		title = title.replaceAll(badchars, " ");
		title = title.replaceAll("  ", " ");
		//title = title.replaceAll("\\s((19[0-9][0-9])|(20[01][0-9])).*", ""); // remove year
		return title.trim();
	}
	
	private Metadata queryTitle(MMFile file) {
		Metadata metadata = file.getMetadata();
		MMFile sourceFile = metadata.getVideoFile();
		if ( sourceFile == null )
			return null;
		String title = sourceFile.getName(); // sourceFile.getName();
		//title = title.replaceAll(Metadata.multiwords.get(0) + "\\s[1-9]", ""); // Remove part
		
		
		

		
		
		String json = "{ 'Response' : 'Parse Error' } ";
		try {
			title = formatTitle(title);
			String year = Metadata.extract(title, "\\s((19[0-9][0-9])|(20[01][0-9]))");
			//title = title.replaceAll("\\s((19[0-9][0-9])|(20[01][0-9])).*", "");

			
			log.debug("title=" + title);
			title = URLEncoder.encode(title, "ascii");	
			
			String query = QUERY_TEMPLATE;
			query = query.replace("{title}", title);
			if ( year != null ) 
				query += "&year=" + year.substring(1);
			
			log.debug("querying " + title);
			
			URL url = new URL(query);
			URLConnection conn = url.openConnection();
	        BufferedReader in = new BufferedReader( new InputStreamReader( conn.getInputStream()));
	        json = in.readLine();
	        in.close();
	       
		} catch (Exception e) {
			e.printStackTrace();
			log.debug("Lookup failed for " + file);
		}
		
		Type mapType = new TypeToken<Map<String,String>>(){}.getType();
		Map<String,String> map = gson.fromJson(json, mapType);
		metadata.putAll(map);
		log.debug("found " + metadata);
		return metadata;
	}

	
	public void run() {
		while (isAlive()) {
			
			MMFile metaToLookup = fileService.get(thisStage).pickOne("meta"); 
			log.debug("Looking up metadata file " + metaToLookup);
			
			if ( metaToLookup == null ) {
				waitFor(thisStage);
				continue;			
			}
						
			// Attempt to retrieve data for a filename
			Metadata metadata = queryTitle(metaToLookup);
			
			
			if ( metadata == null ) {
				metaToLookup.deleteFile();
				continue;
			}
			fileFound(metadata.get("origin"));
			
			MMFile sourceVideo = metadata.getVideoFile();
			
			// If lookup fails to generate a hit, move the video and delete the metafile
			if ( ! metadata.isValid() ) {
				sourceVideo.moveToDir(fileService.get("lookupfail"));
				fileFailed(sourceVideo);
				metaToLookup.deleteFile();
				continue;
			}
			
			
			// Otherwise, update the filename
			
			String oldtitle = sourceVideo.getBasename();
			String newtitle = metadata.get("Title");
			
			sourceVideo = sourceVideo.moveToDir(fileService.get(nextStage));
			sourceVideo = sourceVideo.renameFile(newtitle + sourceVideo.getFileType()); //title + sourceVideo.getFileType()
			log.info(oldtitle + " -> " + newtitle);
			
			metadata.setVideoFile(sourceVideo);

			// Write the newly-retrieved data
			metadata.write();
			
			// Rename new metafile to something the EncoderService will find
			metaToLookup.moveToDir(fileService.get(nextStage));
			metaToLookup.renameFile(metadata.get("Title") + Filter.getFiletypeFor("meta"));
			
			// Notify anyone waiting for new good metadata
			notify(nextStage);
		
		}		
	}
}
