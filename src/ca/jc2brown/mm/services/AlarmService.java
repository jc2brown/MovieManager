package ca.jc2brown.mm.services;

import java.util.HashMap;

import org.apache.log4j.Logger;

import ca.jc2brown.mm.MediaManager;
import ca.jc2brown.mm.utils.Alarm;

public class AlarmService extends HashMap<String, Alarm> {
	private static final long serialVersionUID = 1L;
	private static Logger log = Logger.getLogger( AlarmService.class.getName() );
	
	private int numWaiting;
	private Thread waitThread;
	private boolean dying;
	
	public AlarmService() {
		super();
		numWaiting = 0;
		waitThread = null;
		dying = false;
		log.debug("Instantiated alarms");	
	}
	
	
	
	public synchronized Alarm getToWait(String key) {
		Alarm alarm = get(key);
		numWaiting += alarm.setWaiting(true);
		if ( numWaiting == size() && ! dying ) {
			waitThread = new Thread(new WaitForDeath());
			waitThread.start();
			dying = true;
		}
		return alarm;
	}
	
	public synchronized Alarm getToNotify(String key) {
		Alarm alarm = get(key);
		if ( waitThread != null ) {
			waitThread.interrupt();
		}
		numWaiting -= alarm.setWaiting(false);
		return alarm;
	}

	
	class WaitForDeath implements Runnable {
		public void run() {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				log.debug("Interrupted");
				return;
			}
			log.info("All done.");
			MediaManager.quit();
		}
		
	}
}
