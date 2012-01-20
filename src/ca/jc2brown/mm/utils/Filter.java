package ca.jc2brown.mm.utils;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

public class Filter extends HashMap<String, FileFilter> {
	private static final long serialVersionUID = 1L;
	private static Logger log = Logger.getLogger( Filter.class.getName() );
	
	private static Filter instance = null;
	
	public FileFilter directoryFilter;
	public FileFilter videoFilter;
	public FileFilter metadataFilter;
	public FileFilter tempfileFilter;

	private List<String> videotypes;
	private List<String> metatypes; 
	private List<String> temptypes;
	
	public Filter(List<String> videotypes, String metatype, String temptype) {
		this.videotypes = videotypes;
		this.metatypes = new ArrayList<String>();
		this.metatypes.add(metatype);
		this.temptypes = new ArrayList<String>();
		this.temptypes.add(temptype);
		directoryFilter = new DirectoryFilter();
		videoFilter = new FileTypeFilter(this.videotypes);
		metadataFilter = new FileTypeFilter(this.metatypes);
		tempfileFilter = new FileTypeFilter(this.temptypes);
		put("directories", directoryFilter);
		put("video", videoFilter);
		put("meta", metadataFilter);
		put("temp", tempfileFilter);
		instance = this;
		log.debug("Filters initialized");
	}
	
	/*
	public static List<String> getVideotypes() {
		FileTypeFilter filter = (FileTypeFilter) instance.get("video");
		return filter.filetypes;
	}
	*/
	
	public static FileFilter getFilterFor(String key) {
		return instance.get(key);
	}
	
	public static String getFiletypeFor(String key) {
		FileTypeFilter filter = (FileTypeFilter) instance.get(key);
		return filter.filetypes.get(0);
	}
	
	class DirectoryFilter implements FileFilter {
		public DirectoryFilter() {
		}

		public boolean accept(File path) {
			if ( path.isDirectory() ) {
				return true;
			}
			return false;
		}
	}

	class FileTypeFilter implements FileFilter {
		
		List<String> filetypes;
		
		public FileTypeFilter(List<String> filetypes) {
			this.filetypes = filetypes;
		}
		
		public boolean accept(File path) {
			if ( path.isFile() ) {
				String filename = path.getPath();
				for ( String filetype : filetypes ) {
					if ( filename.endsWith(filetype) ) {
						return true;
					}
				}
			}
			return false;
		}
	}
}

	

