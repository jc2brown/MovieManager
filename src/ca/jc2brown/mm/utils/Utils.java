package ca.jc2brown.mm.utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import ca.jc2brown.mm.MediaManager;

public class Utils {
	private static Logger log = Logger.getLogger( Utils.class.getName() );

	// Tokenizes delim-sepereated values into a list of strings
	public static List<String> tokenize(String line, String delim) {
		List<String> list = new ArrayList<String>();
		if ( line != null ) {
			StringTokenizer st = new StringTokenizer(line, delim);
			while ( st.hasMoreTokens() ) {
				list.add(st.nextToken());			
			}
		}
		return list;
	}
	
	// Returns a Properties object from the given filename if possible, and terminates the program otherwise
	public static GroupedProperties getProperties(String path) {
		GroupedProperties props = new GroupedProperties();
		try {
			props.load(new FileReader(path));
		} catch (FileNotFoundException e) {
			log.fatal("Cannot find properties file " + path);
			MediaManager.quit();
		} catch (IOException e) {
			log.fatal("Error reading properties file " + path);
			MediaManager.quit();
		}
		log.debug("Loaded properties file " + path);
		return props;
	}
	
	
	
	
}
