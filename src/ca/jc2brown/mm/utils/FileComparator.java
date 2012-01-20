package ca.jc2brown.mm.utils;

import java.io.File;
import java.util.Comparator;

 	
/**
 * The FileComparator is used by the dirsToVisit P-Queue.
 * It uses depth (i.e. number of parent directories) as the determinant
 */

public class FileComparator implements Comparator<File> {
	
	public int compare(File f1, File f2) {
		String p1 = f1.getPath();
		String p2 = f2.getPath();
		Integer d1 = depth( p1 );
		Integer d2 = depth( p2 );
		if ( d1 == d2 ) {
			return p2.compareTo(p1);
		}
		return d2.compareTo(d1);
	}
	
	private int depth(String p) {
		int depth = p.split("\\" + File.separator).length;
		return depth;
	}
}