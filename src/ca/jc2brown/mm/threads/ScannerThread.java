package ca.jc2brown.mm.threads;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.log4j.Logger;

import ca.jc2brown.mm.utils.Metadata;
import ca.jc2brown.mm.services.AlarmService;
import ca.jc2brown.mm.services.FileService;
import ca.jc2brown.mm.utils.MMFile;

/**
 * The ScannerService scans for videoFiletypes in inputDir and subfileService, 
 * places them in badVideoDir, and extracts metadata to badMetaDir
 * 
 */
public class ScannerThread extends AlarmThread implements Runnable {
	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger( ScannerThread.class.getName() );

	private FileService fileService;
	private Queue<MMFile> dirsToVisit;
	
	public ScannerThread(String cur, String next, FileService fileService, AlarmService alarmService) {
		super(cur, next, alarmService);
		this.fileService = fileService;
		dirsToVisit = new PriorityQueue<MMFile>(1, new FileComparator());
		dirsToVisit.offer(fileService.get(thisStage));
		log.debug("ScannerThread initialized");
	}
	
	public void run() {
		while (isAlive()) {
			
			// Nothing to do? Wake when something is in the input folder
			if ( dirsToVisit.peek() == null ) {
				waitFor(thisStage);
				continue;
			}
			
			
			// Otherwise, grab the deepest directory we can find...
			MMFile dir = dirsToVisit.remove();
			log.debug("Scanning for video files in " + dir.getPath());
			
			// ...and process any videos in it
			List<MMFile> inputVideos = dir.pickAll("video");
			if ( inputVideos == null ) {
				continue;
			}

			for ( MMFile inputVideo : inputVideos ) {
				log.debug("Found video file " + inputVideo);
				
				// Move the source video file
				
				MMFile videoToLookup = inputVideo.moveToDir(fileService.get(nextStage));
				if ( videoToLookup == null ) {
					continue;
				}
	
				fileFound(inputVideo);
				
				// Create a placeholder metadata file here, so it is not found until after writing
				MMFile metaToLookup = fileService.get(thisStage).getTempFile("meta");
				//Process p = ffmpeg.getBadMetadata(videoToLookup, metaToLookup);
				//waitFor(p);
				
				
				Metadata metadata = metaToLookup.getMetadata(inputVideo.getPath());
				metadata.setVideoFile(videoToLookup);
				metadata.put("Title", videoToLookup.getBasename());
				metadata.write();
				
				
				metaToLookup.moveToDir(fileService.get(nextStage));
				
				
				
				// Alert anyone waiting for new bad metadata
				notify(nextStage);
			}
			
			
			// Add any subfileService for subsesquent passes
			List<MMFile> subdirs = dir.pickAll("directories");
			if ( subdirs != null ) {
				dirsToVisit.addAll( subdirs );
			}
		}
	}
	
	/*
	private void appendLocalPath(MMFile metaFile, MMFile videoFile) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(metaFile, true));
			out.append("localpath=" + videoFile.getPath());
			out.close();
		} 
		catch (IOException e) {e.printStackTrace();}
	}
	*/
	
	/**
	 * The FileComparator is used by the dirsToVisit P-Queue.
	 * It uses depth (i.e. number of parent fileService) as the determinant
	 */
	
	class FileComparator implements Comparator<File> {
		String pathSeperator = File.pathSeparator;
		
		public int compare(File f1, File f2) {
			int c1 = countSeperators(f1.getPath());
			int c2 = countSeperators(f2.getPath());
			return c1 - c2;
		}
		
		private int countSeperators(String p) {
			String q = p.replace(pathSeperator, "");
			return p.length() / q.length();
		}
	};
}



