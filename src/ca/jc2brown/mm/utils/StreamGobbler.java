package ca.jc2brown.mm.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

public class StreamGobbler extends Thread {
	private static Logger log = Logger.getLogger( StreamGobbler.class.getName() );
	
    InputStream is;

    // reads everything from is until empty. 
    public StreamGobbler(InputStream is) {
        this.is = is;
    }

    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            while ( (line = br.readLine()) != null) {
                log.debug(line);    
            }
            isr.close();
            is.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();  
        }
    }
}
