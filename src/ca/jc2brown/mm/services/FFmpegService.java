package ca.jc2brown.mm.services;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import ca.jc2brown.mm.MediaManager;

public class FFmpegService {
	private static Logger log = Logger.getLogger( FFmpegService.class.getName() );
	
	private final String CMD_EXTRACT = "-y -i \"{input}\" -f ffmetadata \"{metadata}\"";
	private final String CMD_UPDATE = "-y -i \"{input}\" -i \"{metadata}\" -vcodec copy -acodec copy -map_metadata 0:1 \"{output}\"";


	/**
	 * Returns a File that contains raw metadata from the given video file
	 */
	public Process getBadMetadata(File badVideoFile, File badMetaFile) {
		String badMetaPath = badMetaFile.getPath();
		String badVideoPath = badVideoFile.getPath();
		String cmd = formatExtract(badVideoPath, badMetaPath);
		return runFFmpeg(cmd);
	}
	
	/**
	 * Returns a video file that is the provided video with metadata from the provided file
	 */
	public Process getGoodVideo(File badVideoFile, File goodMetaFile, File goodVideoFile)  {
		String goodVideoPath = goodVideoFile.getPath();
		String badVideoPath = badVideoFile.getPath();
		String goodMetaPath = goodMetaFile.getPath();
		String cmd = formatUpdate(badVideoPath, goodMetaPath, goodVideoPath);
		return runFFmpeg(cmd);
	}
	
	private Process runFFmpeg(String cmd) {
        try {
        	cmd = "ffmpeg.exe " + cmd;
        	log.debug(cmd);        	
            Process p = Runtime.getRuntime().exec(cmd);
            return p;
        }
        catch (IOException e) {
            log.error("An error occured executing the following command: " + cmd);
            MediaManager.quit();
        }
        return null;
    }
	
	
	private String formatExtract(String badVideo, String badMeta) {
		String cmd = CMD_EXTRACT;
		cmd = cmd.replace("{input}", badVideo);
		cmd = cmd.replace("{metadata}", badMeta);
		return cmd;
	}
	
	private String formatUpdate(String badVideo, String goodMeta, String goodVideo) {
		String cmd = CMD_UPDATE;
		cmd = cmd.replace("{input}", badVideo);
		cmd = cmd.replace("{metadata}", goodMeta);
		cmd = cmd.replace("{output}", goodVideo);
		return cmd;
	}
}
