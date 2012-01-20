package ca.jc2brown.mm.threads;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import ca.jc2brown.mm.MediaManager;
import ca.jc2brown.mm.utils.Filter;
import ca.jc2brown.mm.utils.GroupedProperties;
import ca.jc2brown.mm.utils.Metadata;
import ca.jc2brown.mm.utils.StreamGobbler;
import ca.jc2brown.mm.services.AlarmService;
import ca.jc2brown.mm.services.FileService;
import ca.jc2brown.mm.utils.MMFile;

public class LinkerThread extends AlarmThread implements Runnable {
	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger( EncoderThread.class.getName() );
	
	private FileService fileService;
	private boolean skip;
	Object newLinkAlarm;
	Object spewDoneAlarm;
    OutputStreamWriter isr;
    BufferedWriter br;
	
	public static Map<String, MMFile> linkDirs;
	
	
	public LinkerThread(String cur, String next, GroupedProperties conf, FileService fileService, AlarmService alarmService) {
		super(cur, next, alarmService);
		List<String> links = conf.getValueList("links");
		skip = Boolean.parseBoolean(conf.getProperty("linker.skip"));
		linkDirs = new HashMap<String, MMFile>(links.size());
		MMFile linkRoot = fileService.get("links");
		for (String link : links) {
			MMFile linkDir = new MMFile(linkRoot, link);
			linkDir.mkdirs();
			linkDirs.put(link, linkDir);
		}
		this.fileService = fileService;
		newLinkAlarm = new Object();	
		spewDoneAlarm = new Object();	
		log.debug("LinkerThread initialized");
	}
	
	
	private MMFile makeLink(MMFile source, MMFile parent, String linkName) {
		String sep = System.getProperty("line.separator");
		String cmd_cd = 	"cd /d" + parent.getPath() + sep;
		String cmd_mklink = "mklink \"" + linkName + "\" \"" + source + "\"" + sep;
    	
		try {
			br.write(cmd_cd + cmd_mklink);
			br.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
        return null;
    }
	
	
	
	public void run() {

		Process cli = null;
    	try {
			cli = Runtime.getRuntime().exec("cmd", null, fileService.get(thisStage));
		} catch (IOException e) {
			log.error("Could not establish command line interface");
			MediaManager.quit();
		}

		StreamGobbler errorGobbler = new StreamGobbler(cli.getErrorStream());
		StreamGobbler outputGobbler = new StreamGobbler(cli.getInputStream());
		errorGobbler.start();
		outputGobbler.start();
		
        isr = new OutputStreamWriter(cli.getOutputStream());
        br = new BufferedWriter(isr);
		
		while (isAlive()) {
			
			// Pick a metafile to process
			MMFile metaToLink = fileService.get(thisStage).pickOne("meta");
			log.debug("Linking metadata file " + metaToLink);
			
			// Wait until one exists if none are found
			if ( metaToLink == null ) {
				waitFor(thisStage);
				continue;
			}

			// Otherwise, find the video file associated with this metafile
			Metadata metadata = metaToLink.getMetadata();
			MMFile videoToLink = metadata.getVideoFile();
			if ( videoToLink == null ) {
				log.debug("Deleting bad metadata");
				metaToLink.deleteFile();
				continue;
			}
			fileFound(metadata.get("origin"));
			
			
			if ( skip ) {
				metaToLink.renameFile(metadata.getFileTitle() + Filter.getFiletypeFor("meta"));
				MMFile dupDir = fileService.get("duplicate");
				metaToLink = metaToLink.moveToDir(fileService.get("metaout"), dupDir);
				if ( metaToLink != null && ! metaToLink.getParentFile().equals(dupDir) ) {
					fileSucceeded(videoToLink);
				}
				continue;
			}

			
			boolean abort = false;
			List<MMFile> links = new ArrayList<MMFile>();
			MMFile linkFile = null;
			for ( Entry<String, MMFile> entry : linkDirs.entrySet() ) {
				if ( abort ) break;
				String link = entry.getKey();
				MMFile linkDir = entry.getValue(); 
				
				List<String> subLinks = metadata.getValueList(link);
				
				for (String subLink : subLinks) {
					MMFile subLinkDir = new MMFile(linkDir, subLink);
					
					if ( subLinkDir != null ) {
						subLinkDir.mkdirs();
						
						linkFile = makeLink(videoToLink, subLinkDir, videoToLink.getName());
					/*if ( linkFile == null || linkFile.equals(fileService.get(thisStage)) ) {
						abort = true;
						break;
					}*/
						links.add(linkFile);
					}
				}
			}
			if ( abort ) {
				if ( linkFile == null ) {
					metaToLink.deleteFile();
					videoToLink.moveToDir(fileService.get("linkerfail"));
					fileFailed(videoToLink);
				}
				if ( linkFile.equals(fileService.get(thisStage)) ) {
					//spewer.alive = false;
				}
				for ( MMFile link : links ) {
					link.deleteFile();
				}
				continue;
			}
			
			
			
			
			// Rename metafile to something the metafile filter will find
			//videoToLink = videoToLink.moveToDir(fileService.get(nextStage));
			metadata.writeVideoFile(videoToLink);
			metaToLink.renameFile(metadata.getFileTitle() + Filter.getFiletypeFor("meta"));
			MMFile dupDir = fileService.get("duplicate");
			metaToLink = metaToLink.moveToDir(fileService.get("metaout"), dupDir);
			if ( metaToLink != null ) {
				fileSucceeded(videoToLink);
			}
		}
		
		
		try {
			br.write("exit" + System.getProperty("line.separator"));
			br.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
}
