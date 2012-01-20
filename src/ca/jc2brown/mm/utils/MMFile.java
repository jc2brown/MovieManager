package ca.jc2brown.mm.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ca.jc2brown.mm.utils.Metadata;

public class MMFile extends File {
	private static final long serialVersionUID = 1L;
	private static Logger log = Logger.getLogger( MMFile.class.getName() );
	
	public static MMFile duplicateDir;
	
	public MMFile(String path) {
		super(path);
	}
	
	public MMFile(File file) {
		super(( file == null ? "" : file.getPath() ));
	}
	
	public MMFile(File parent, String child) {
		super(parent, child);
	}
	
	public MMFile(String parent, String child) {
		super(parent + File.separator + child);
	}
	
	public String getFileType() {
		String path = getName();
		int start = path.lastIndexOf(".");
		String filetype = path.substring(start);
		return filetype;
	}
	
	public String getBasename() {
		String path = getName();
		int end = path.lastIndexOf(".");
		String basename = path.substring(0, end);
		return basename;
	}

	public MMFile moveToDir(File dir) {
		return moveFile(this, dir, null, null);
	}
	
	public MMFile moveToDir(File dir, File overflow) {
		return moveFile(this, dir, null, overflow);
	}

	public MMFile renameFile(String destName) {
		return moveFile(this, null, destName, null);
	}
	
	public MMFile renameFile(String destName, File overflow) {
		return moveFile(this, null, destName, overflow);
	}
	
	public MMFile renameFile(File file) {
		return moveFile(this, file.getParentFile(), file.getName(), null);
	}
	
	public MMFile renameFile(File file, File overflow) {
		return moveFile(this, file.getParentFile(), file.getName(), overflow);
	}

	public File deleteFile() {
		boolean deleteSuccess = this.delete();
		if ( deleteSuccess ) {
			while ( this.exists() ) {
				log.warn("-X-");
			}
			return null;
		}
		log.debug("deleteFile: could not delete file " + this);
		return this;
	}

	private MMFile moveFile(MMFile src, File destDir, String destName, File overflow) {
		// Build destination path
		if ( src == null ) 				log.debug("moveFile: src is null");
		if ( ! src.isFile() ) 			log.debug("moveFile: src is not a file");
		if ( destDir == null ) 			destDir = new MMFile(src.getParentFile());
		if ( ! destDir.isDirectory() ) 	log.debug("moveFile: destDir is not a directory");
		if ( destName == null ) 		destName = src.getName();
		
		// Get destination file
		MMFile dest = new MMFile(destDir, destName);
		
		// If src and dest point to the same file, return either
		if ( src.getPath().equalsIgnoreCase(dest.getPath()) )
			return src;
		
		// Remove existing destination file, if present
		//dest.delete();
		File parent = destDir;
		String basename = dest.getBasename();
		String filetype = dest.getFileType();

		if ( dest.exists() && overflow != null )
			parent = overflow;
			
		int i = 0;
		while ( dest.exists() ) {
			dest = new MMFile(parent, basename + ( i == 0 ? "" : "(" + i + ")" ) + filetype);
			i += 1;
		}
		/*
		if ( dest.exists() ) {
			//log.debug("moveFile: could not delete obstructing destination file " + dest);
			//return src;
			
		}*/
	
		// Move file if possible; return destination or null
		boolean renameSuccess = src.renameTo(dest);
		if ( renameSuccess ) {
			while ( src.exists() ) {
				log.warn("->-");
			}
		} else {
			dest = null;
		}
		log.debug("moveFile : " + src + " => " + dest);
		log.debug("should be: " + src + " => " + new MMFile(destDir, destName));
		
		return dest;
	}
	
	
	
	
	
	
	// If type is meta or temp, lookup extension
	// Otherwise use what's given
	public MMFile getTempFile(String type) {
		if ( "meta".equals(type) || "temp".equals(type) ) 
			type = Filter.getFiletypeFor(type);		
		File file = null;
		try {					file = File.createTempFile("Media", type, this); } 
		catch (IOException e) {	log.debug("could not create temp file");	}
		return new MMFile(file);
	}
	

	public MMFile pickOne(String type) {
		List<MMFile> files = pickAll(type);
		if ( files == null ) {
			return null;
		}
		return files.get(0);
	}
	
	public List<MMFile> pickAll(String type) {
		File[] files = listFiles( Filter.getFilterFor(type) );
		if ( files == null || files.length == 0 ) {
			return null;
		}
		List<MMFile> list = new ArrayList<MMFile>(files.length);
		for (File file : files) 
			list.add(new MMFile(file));
		return list;
	}
	
	public Metadata getMetadata() {
		return new Metadata(this, null);
	}
	
	public Metadata getMetadata(String path) {
		return new Metadata(this, path);
	}

	
	public List<MMFile> listMMFiles() {
		return listMMFiles(null);
	}
	

	public List<MMFile> listMMFiles(FileFilter filterFor) {
		if ( ! isDirectory() ) 
			return null;
		File[] files = ( filterFor == null ? listFiles() : listFiles(filterFor) );
		List<MMFile> list = new ArrayList<MMFile>(files.length);
		for (File file : files)
			list.add(new MMFile(file));
		return list;
	}
	
	
	public boolean isEmpty(boolean deleteIfEmpty) {
		if ( ! isDirectory() ) 
			return false;
		
		List<MMFile> files = listMMFiles();
		for ( MMFile file : files ) {
			if ( ! file.isEmpty(deleteIfEmpty) ) {
				return false;
			} else {
				file.deleteFile();
			}
		}
		return true;
	}

	public MMFile copyTo(MMFile dest) {
	    int c;

	    log.debug("Copying " + this + " => " + dest + " ...");
	    try {
			BufferedReader in = new BufferedReader( new FileReader(this) );
			BufferedWriter out = new BufferedWriter( new FileWriter(dest) );
		    
			while ((c = in.read()) != -1)
			  out.write(c);
			
		    in.close();
		    out.close();
		} catch (IOException e) {
			log.debug("... failed.");
			return null;
		}
		log.debug("... done.");
		return dest;
	}

	public boolean isDuplicate() {
		String path = getPath();
		String dupPath = duplicateDir.getPath();
		return path.startsWith(dupPath) || path.matches(".+\\([1-9]\\).*");
	}

	
	
	
}