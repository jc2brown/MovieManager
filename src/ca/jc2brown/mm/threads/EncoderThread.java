package ca.jc2brown.mm.threads;

import org.apache.log4j.Logger;

import ca.jc2brown.mm.utils.GroupedProperties;
import ca.jc2brown.mm.utils.Metadata;
import ca.jc2brown.mm.services.AlarmService;
import ca.jc2brown.mm.services.FFmpegService;
import ca.jc2brown.mm.services.FileService;
import ca.jc2brown.mm.utils.MMFile;

public class EncoderThread extends AlarmThread implements Runnable {
	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger( EncoderThread.class.getName() );
	
	private FFmpegService ffmpeg;
	private FileService fileService;
	
	private boolean deleteSource;
	private boolean skip;
	

	public EncoderThread(String cur, String next, GroupedProperties conf, FFmpegService ffmpeg, FileService fileService, AlarmService alarmService) {
		super(cur, next, alarmService);
		this.ffmpeg = ffmpeg;
		this.fileService = fileService;
		this.deleteSource = Boolean.parseBoolean(conf.getProperty("encoder.deletesource"));
		this.skip = Boolean.parseBoolean(conf.getProperty("encoder.skip"));
		log.debug("EncoderThread initialized");
	}
	
	
	public void run() {
		while (isAlive()) {
			
			// Pick a metafile to process
			MMFile metafile = fileService.get(thisStage).pickOne("meta"); 

			
			log.debug("Encoding metadata file " + metafile);
			
			// Wait until one exists if none are found
			if ( metafile == null ) {
				waitFor(thisStage);
				continue;
			}
			/*
			if ( metafile.equals(lastFile) ) {
				metafile.deleteFile();
				continue;
			}
			lastFile = metafile;
*/
			// Otherwise, find the video file associated with this metafile
			Metadata metadata = metafile.getMetadata();
			MMFile sourceVideo = metadata.getVideoFile();
			if ( sourceVideo == null ) {
				metafile.deleteFile();
				continue;
			}
			fileFound(metadata.get("origin"));
			
			
			MMFile outputVideo = fileService.get(nextStage).getTempFile(sourceVideo.getFileType());
			outputVideo.deleteOnExit();
			
			if ( skip && sourceVideo.exists() ) {
				if ( deleteSource ) {
					outputVideo = sourceVideo.renameFile(outputVideo);
				} else {
					sourceVideo.copyTo(outputVideo);
					sourceVideo.moveToDir(fileService.get("sourceout"));
				}
				
				outputVideo = outputVideo.renameFile(metadata.get("Title") + outputVideo.getFileType(), fileService.get("duplicate"));
				
				if ( outputVideo != null ) {
					metadata.writeVideoFile(outputVideo);
					metafile.moveToDir(fileService.get(nextStage));
					notify(nextStage);
				}
				continue;
			}
			
			// Encode video, blocking until complete			
			Process p = ffmpeg.getGoodVideo(sourceVideo, metafile, outputVideo);
			int exitCode = waitFor(p);
			
			// Handle success/failure
			if ( exitCode != 0 ) {
				if ( exitCode == ABORTCODE ) {
					alive = false;
				} else {
					sourceVideo.moveToDir(fileService.get("encoderfail"));
					fileFailed(sourceVideo);
					metafile.deleteFile();
				}
				outputVideo.deleteFile();
				continue;
				
			} else {
				//outputVideo = outputVideo.renameFile(metadata.getFileName()); 
				outputVideo = outputVideo.renameFile(
						metadata.get("Title") + outputVideo.getFileType(), fileService.get("duplicate"));
				
				if ( deleteSource ) 
					sourceVideo.deleteFile();
				else 
					sourceVideo.moveToDir(fileService.get("sourceout"));
				
				metadata.writeVideoFile(outputVideo);
			}
			
			
			// Rename metafile to something the metafile filter will find
			metafile.renameFile(fileService.get(nextStage).getTempFile("meta"));
			
			// Alert the next process in line
			notify(nextStage);
			
			

			
		}		
	}
}
