package ca.jc2brown.mm.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import ca.jc2brown.mm.threads.LinkerThread;
import ca.jc2brown.mm.utils.MMFile;

public class Metadata extends HashMap<String, String> {

	private static Logger log = Logger.getLogger( Metadata.class.getName() );
	
	private static String invalidChars = "[\\\\/*?\"<>|]";
	private static String magicString = "abcdefg12345678";
	
	private static final long serialVersionUID = 1L;
	public static List<String> commentKeyList;
	public static List<String> multiwords;
	
	private MMFile file;	
	
	public static String extract(String str, String regex) {
		log.debug("extract( " + str + ", " + regex + " );");
		if ( str == null ) return null;
		if ( str.matches( ".*" + regex + ".*" ) ) {
			String bad = str.replaceFirst(regex, magicString);
			int start = bad.indexOf(magicString);
			if ( start < 0 ) return null;
			int partlength = str.length() + magicString.length() - bad.length();
			int end = start + partlength;
			return str.substring(start, end);
		}
		return null;
	}
	
	public static String getMultipart(String title) {
		for ( String keyword : multiwords ) {
			keyword = keyword.toLowerCase();
			title = title.toLowerCase();
			String regex = keyword + ".?[1-9]";
			String match = extract(title, regex);
			if ( match != null ) 
				return match;
		}
		return null;
	}
	
	public static String getMultipartNum(String title) {
		String match = getMultipart(title);	// Get ... part X ...
		match = extract(match, "[1-9]");		// Get X
		return match;
	}
	
	
	
	protected Metadata(MMFile file, String origin) {
		super();
		this.file = file;
		BufferedReader reader;
		if (this.file == null)
			return;
		
		try {
			reader = new BufferedReader(new FileReader(file));
			String line = null;
			while ( (line = reader.readLine() ) != null ) {
				int delindex = line.indexOf("=");
				if (delindex > 0) {
					String key = line.substring(0, delindex).trim();
					String value = line.substring(delindex).trim();
					if ( LinkerThread.linkDirs.containsKey(key) ) {
						value = legalize(value);
					}
					put(key, value);		
				}
			}
			reader.close();
			if ( origin != null ) {
				put("origin", origin);
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	
	private static String legalize(String s) {
		s = s.replaceAll("\\:", " - ");
		s = s.replaceAll(invalidChars, " ");
		s = s.replaceAll("  ", " ");
		return s;
	}
	

	
	public String put(String key, String value) {
		while ( value.startsWith("=") ) {
			value = value.replace("=", "");
		}
		if ( "title".equalsIgnoreCase(key) ) {
			value = legalize(value);
		}
		return super.put(key, value);
	}

	public boolean isValid() {
		String response = get("Response");
		return ! response.startsWith("Parse Error");
	}
	
	
	public MMFile getVideoFile() {
		String path = get("localpath");
		if ( path == null ) {
			log.debug("localpath not found");
			return null;
		}
		MMFile videoFile = new MMFile(path);
		if ( ! videoFile.exists() ) {
			return null;
		}
		return videoFile;
	}
	
	public String getFileTitle() {
		String part = get("part");
		String title = get("Title");
		String result = title;
		if ( title == null ) 
			return null;
		if ( part != null )
			result = result + " " + multiwords.get(0) + " " + part;
		return result;
	}
	
	public String getQueryTitle() {
		String title = get("Title");
		String result = title;
		if ( title == null ) 
			title = new MMFile(get("localpath")).getBasename();
		title = title.replaceAll("\\([1-9]\\)", "");
		return result;
		
	}
	
	public List<String> getValueList(String key) {
		List<String> list = new ArrayList<String>();
		String line = get(key);
		if ( line == null ) {
			log.warn(key + " not found");
			return list;
		}
		for (String s : line.split(", ")) {
			list.add(s);
		}
		return list;
	}
	
	
	public void write() {
		
	
		BufferedWriter out;
		try {
			out = new BufferedWriter(new FileWriter(file));
		
			log.debug("Writing metadata");
			
			out.write(";FFMETADATA1\n");
			
			/*
			for (String field : values()) {
				String value = get(field);
				if ( value != null ) {
					out.write(field + "=" + value + "\n");
				}
			}*/
			
			if ( get("origin") == null && get("localhost") != null ) {
				put("origin", get("localhost"));
			}
			
			
			
			for (java.util.Map.Entry<String, String> entry : entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				if ( value != null ) {
					out.write(key + "=" + value + "\n");
				}
			}
			
			StringBuffer sb = new StringBuffer();
			for (String key : commentKeyList) {
				String value = get(key);
				if ( value != null ) {
					sb.append(key + ": " + value + "   ");
				}
			}
			out.write("Comment=" + sb.toString() + "\n");
			out.write("Comments=" + sb.toString() + "\n");
			/*
			String part = get("part");
			if ( part != null ) {
				out.write("part=" + part + "\n");
			}*/
			out.close();
			

		} catch (IOException e) {
			e.printStackTrace();
		}
			
	}

	public void setVideoFile(MMFile videoFile) {
		String title = videoFile.getBasename();
		//title = title.replaceAll("\\([1-9]\\)", "");	// remove duplicate
		String part = getMultipartNum(title);
		if ( part != null ){
			put("part", part);
		}
		String path = videoFile.getPath();
		//path = path.replaceAll("\\([1-9]\\)", "");	// remove duplicate
		if ( get("origin") == null ) {
			put("origin", path);
		}
		put("localpath", path);
	}
	
	
	public void writeVideoFile(MMFile videoFile) {
		setVideoFile(videoFile);
		write();
	}
	
}
