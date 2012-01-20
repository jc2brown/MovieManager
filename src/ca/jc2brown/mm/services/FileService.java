package ca.jc2brown.mm.services;

import java.io.File;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import ca.jc2brown.mm.MediaManager;
import ca.jc2brown.mm.utils.FileComparator;
import ca.jc2brown.mm.utils.Filter;
import ca.jc2brown.mm.utils.GroupedProperties;
import ca.jc2brown.mm.utils.MMFile;


/**
 * The FileService provides access to shared directories as well as robust file operations
 */
public class FileService extends TreeMap<String, MMFile> {
	private static final long serialVersionUID = 1L;
	private static Logger log = Logger.getLogger( FileService.class.getName() );

	public FileService(GroupedProperties conf, GroupedProperties dirs) {
		super();
		
		// Abusing a constructor to initialize Filter's static fields
		new Filter(
				conf.getValueList("videotypes"),
				conf.getProperty("metatype"), 
				conf.getProperty("temptype"));
		
		for (String key : dirs.stringPropertyNames()) {
			String path = buildPath(key, dirs);
			log.debug(path);
			MMFile dir = new MMFile(path);
			if ( 	! dir.exists() && 
					dir.getParentFile() != null &&
					! dir.mkdirs() ) {
				MediaManager.bail("Failed to create directory " + dir);
			}
			put(key, dir);
			log.debug("Adding directory " + path);
		}
		MMFile.duplicateDir = get("duplicate");
		log.debug("FileService instantiated");
	}
	
	private String buildPath(String key, Properties dirs) {
		String value = dirs.getProperty(key);
		String[] dirElements = value.split("\\/");
		
		String path = "";
		for (String dirElement : dirElements) {
			if ( dirElement.startsWith(":") ) {
				dirElement = dirElement.substring(1);
				path += buildPath(dirElement, dirs);
			} else {
				path += dirElement;
			}
			if ( ! path.endsWith(File.separator) ) {
				path += File.separator;
			}
		}
		return path;
	}
	
	public void cleanup() {		
		Set<MMFile> dirs = new TreeSet<MMFile>(new FileComparator());
		dirs.addAll(values());
		for ( MMFile file :  dirs) {
			if ( file.isEmpty(true) ) {
				log.debug("Deleting " + file.getPath() + " => " + file.deleteFile() );
			}
		}
	}
	
}


