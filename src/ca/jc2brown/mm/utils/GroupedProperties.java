package ca.jc2brown.mm.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

public class GroupedProperties extends Properties {
	private static final long serialVersionUID = 1L;
	private static Logger log = Logger.getLogger( GroupedProperties.class.getName() );

	public List<String> getValueList(String property) {
		List<String> list = new ArrayList<String>();
		String line = getProperty(property);
		if ( line == null ) {
			log.warn(property + " not found in properties file");
			return list;
		}
		for (String s : line.split(" ")) {
			list.add(s);
		}
		return list;
	}
}
