package ca.jc2brown.mm.threads;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import ca.jc2brown.mm.services.AlarmService;
import ca.jc2brown.mm.utils.Alarm;
import ca.jc2brown.mm.utils.MMFile;
import ca.jc2brown.mm.utils.PathComparator;
import ca.jc2brown.mm.utils.StreamGobbler;

public abstract class AlarmThread extends HashMap<String, Set<String>> implements Runnable {
	private static final long serialVersionUID = 1L;
	static Logger log = Logger.getLogger( AlarmThread.class.getName() );

	public static final int ABORTCODE = -67;
	
	private AlarmService alarmService;
	protected String thisStage;
	protected String nextStage;
	protected boolean alive;
	
	public AlarmThread(String thisStage, String nextStage, AlarmService alarmService) {
		super(3);
		this.alarmService = alarmService;
		this.thisStage = thisStage;
		this.nextStage = nextStage;
		alive = true;
		put("found", new TreeSet<String>(new PathComparator()));
		put("succeeded", new TreeSet<String>(new PathComparator()));
		put("failed", new TreeSet<String>(new PathComparator()));
		put("duplicate", new TreeSet<String>(new PathComparator()));
		log.debug(this.thisStage + " => " + this.nextStage);
	}
	
	public boolean isAlive() {
		return alive;
	}
	
	public Alarm getAlarm(String key) {
		Alarm alarm = alarmService.getToWait(key);
		return alarm;
	}
	
	public int waitFor(Process p)  {
		StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream());
		StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream());
		errorGobbler.start();
		outputGobbler.start();
		try {
			return p.waitFor();
		} catch (InterruptedException e) {
			p.destroy();
			return ABORTCODE;
		}
	}
	
	public void waitFor(String key) {
		Alarm alarm = getAlarm(key);
		log.debug(key + " is going to sleep");
		try { 
			synchronized (alarm) {
				alarm.wait();
			}
		}
		catch (InterruptedException e) { 
			alive = false;
			log.debug(thisStage + " interrupted while waiting for " + alarm.getName());
		}
		if ( alive ) {
			log.debug(key + " woke up");
		}
	}
	
	public void doneWaitingFor(String key) {
		//log.debug("Woke for " + key);
	}
	
	public void notify(String key) {
		Object alarm = alarmService.getToNotify(key);
		synchronized (alarm) {
			alarm.notify();
		}
	}
	
	
	protected void fileFound(MMFile file) {
		fileFound(file.getPath());
	}
	protected void fileFound(String path) {
		reportFile("found", path);
	}
	
	protected void fileFailed(MMFile file) {
		reportFile("failed", file);
	}
	
	protected void fileSucceeded(MMFile file) {
		if ( file.isDuplicate() ) {
			reportFile("duplicate", file);
		}
		reportFile("succeeded", file);
	}
	
	private void reportFile(String key, MMFile file) {
		if ( file == null )  return;
		reportFile(key, file.getPath());
	}
	private void reportFile(String key, String path) {
		if ( path == null )  return;
		Set<String> files = get(key);
		files.add(path);
	}

	public void setAlive(boolean alive) {
		this.alive = alive;
		
	}

	
	
}