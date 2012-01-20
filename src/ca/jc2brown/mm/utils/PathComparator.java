package ca.jc2brown.mm.utils;

import java.io.File;
import java.util.Comparator;



public class PathComparator implements Comparator<String> {
	
	public int compare(String p1, String p2) {
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