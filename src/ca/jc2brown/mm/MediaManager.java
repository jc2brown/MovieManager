package ca.jc2brown.mm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import ca.jc2brown.mm.services.AlarmService;
import ca.jc2brown.mm.services.FFmpegService;
import ca.jc2brown.mm.services.FileService;
import ca.jc2brown.mm.threads.AlarmThread;
import ca.jc2brown.mm.threads.EncoderThread;
import ca.jc2brown.mm.threads.LinkerThread;
import ca.jc2brown.mm.threads.LookupThread;
import ca.jc2brown.mm.threads.ScannerThread;
import ca.jc2brown.mm.utils.Alarm;
import ca.jc2brown.mm.utils.GroupedProperties;
import ca.jc2brown.mm.utils.Metadata;
import ca.jc2brown.mm.utils.PathComparator;
import ca.jc2brown.mm.utils.Utils;

public class MediaManager {

	private static Logger log = Logger.getLogger( MediaManager.class.getName() );
	
	private static MediaManager main;
	
	private FFmpegService ffmpeg;
	private ScannerThread scannerThread;
	private LookupThread lookupThread;
	private EncoderThread encoderThread;
	private LinkerThread linkerThread;
	private Set<Thread> threads;
	private Map<String, AlarmThread> alarmThreads;
	private FileService fileService;
	private AlarmService alarms;
	private boolean alive = true;
	private List<String> reportkeys;

	
	//TODO: ensure all properties exist before using them
	public MediaManager() {
		
		ffmpeg = new FFmpegService();
		log.debug("Instantiated ffmpeg");

		// Load and read config files
		GroupedProperties conf = Utils.getProperties("config.properties");
		GroupedProperties dirs = Utils.getProperties("directories.properties");
		
		reportkeys = conf.getValueList("report");

		Metadata.commentKeyList = conf.getValueList("encodecomments");
		Metadata.multiwords = conf.getValueList("multiwords");
		
		fileService = new FileService(conf, dirs);
		log.debug("Instantiated FileService");

		threads = new HashSet<Thread>();
		alarmThreads = new HashMap<String, AlarmThread>();
		
		
		alarms = new AlarmService();
		
		List<String> stages = conf.getValueList("stages");
		for ( int i = 0; i < stages.size(); i++ ) {
			String stage = stages.get(i);
			String nextStage = ( i == stages.size() - 1 ? null : stages.get(i + 1));
			if ( "scanner".equals( stage ) ) {
				scannerThread = new ScannerThread(stage, nextStage, fileService, alarms);
				alarmThreads.put(stage, scannerThread);
				alarms.put(stage, new Alarm("video files to scan"));
			} else if ( "lookup".equals( stage ) ) {
				lookupThread = new LookupThread(stage, nextStage, conf, fileService, alarms);
				alarmThreads.put(stage, lookupThread);
				alarms.put(stage, new Alarm("files to lookup"));
			} else if ( "encoder".equals( stage ) ) {
				encoderThread = new EncoderThread(stage, nextStage, conf, ffmpeg, fileService, alarms);
				alarmThreads.put(stage, encoderThread);
				alarms.put(stage, new Alarm("video files to encode"));
			} else if ( "linker".equals( stage ) ) {
				linkerThread = new LinkerThread(stage, nextStage, conf, fileService, alarms);
				alarmThreads.put(stage, linkerThread);
				alarms.put(stage, new Alarm("video files to link"));
			}
		}
	}

	
	public static void bail(String error) {
		log.fatal(error);
		System.exit(-1);
	}
	
	public void run() {
		
		for (AlarmThread alarmThread : alarmThreads.values()) {
			Thread thread = new Thread(alarmThread);
			threads.add(thread);
			thread.start();
		}
		
		log.info("All threads initialized.");
		log.info("This might take a while... ");

		log.info("\nType :q followed by Enter to quit the program. The process can be continued later.");
		BufferedReader in = new BufferedReader( new InputStreamReader (System.in) );
		while ( alive ) {
			String line = null;
			try {
				line = in.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (alive && ":q".equalsIgnoreCase(line.trim()) ) {
				log.info("User quit");
				MediaManager.quit();
			}
		}

		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	private void shutdown() {
		if ( alive ) {
			alive = false;
			Thread shutdown = new Thread(new ShutdownThread());
			shutdown.run();
		}
	}
	
	
	public static void main(String[] args) {
		main = new MediaManager();
		main.run();
	}

	public static void quit() {
		main.shutdown();
	}
	
	
	class ShutdownThread implements Runnable {
		public void run() {
			for (Thread thread : threads) {
				thread.interrupt();
			}
			for (AlarmThread alarmThread : alarmThreads.values()) {
				alarmThread.setAlive(false);
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			fileService.cleanup();
			
			printReport();

			log.info("\nPress Enter to close the program.");
			
		}
		
		
		public void printReport() {
			String[] keys = new String[] { "found", "succeeded", "failed", "duplicate" };
			
			Map<String, Set<String>> fileMap = new HashMap<String, Set<String>>();
			for ( String key : keys ) {
				fileMap.put(key, new TreeSet<String>(new PathComparator()));
			}
			
			for ( AlarmThread alarmThread : alarmThreads.values() ) {
				for ( String key : keys ) {
					Set<String> knownFiles = fileMap.get(key);
					Set<String> newFiles = alarmThread.get(key);
					knownFiles.addAll(newFiles);
				}
			}
			
			int numVideos = fileMap.get("found").size();
			int numSuccessful = fileMap.get("succeeded").size();
			
			log.info("\n*** FINAL REPORT ***");
			log.info("\n" + numVideos + " video files found");
			log.info(numSuccessful + " video files successfully processed");
			if ( numVideos != 0 )
				log.info("Success rate: " + numSuccessful * 100 / numVideos + "%");
			
			
			for ( String key : reportkeys ) {
				Set<String> knownFiles = fileMap.get(key);
				if ( knownFiles.isEmpty() ) {
					continue;
				}
				log.info(key);
				for (String file : knownFiles) {
					log.info("\t" + file);
				}
			}
			

			
		}		
	}
}






	

