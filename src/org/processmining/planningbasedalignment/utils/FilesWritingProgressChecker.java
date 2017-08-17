package org.processmining.planningbasedalignment.utils;

import java.io.File;

import org.processmining.framework.plugin.PluginContext;

/**
 * Worker thread for showing the progress of a process that writes a bunch of files in a directory. 
 * 
 * @author Giacomo Lanciano
 *
 */
public class FilesWritingProgressChecker extends Thread {

	private static final int DEFAULT_DELAY_MILLISECS = 5000;
	
	/**
	 * The delay after which the thread show the progress.
	 */
	private int delayMillisecs;

	/**
	 * The context where the plug-in runs.
	 */
	private PluginContext context;
	
	/**
	 * The directory where the files to be counted are saved by the process.
	 */
	private File directory;
	
	/**
	 * The total number of files to be written. 
	 */
	private int totalFilesNum;
	
	/**
	 * The message to be displayed beside the file counter.
	 */
	private String message;
	
	/**
	 * Create a worker thread for showing the progress of a process that writes a bunch of files in a directory every
	 * 5 seconds.
	 * 
	 * @param context The context where the plug-in runs.
	 * @param directory The directory where the files to be counted are saved by the process.
	 * @param totalFilesNum The total number of files to be written.
	 * @param message The message to be displayed beside the file counter.
	 */
	public FilesWritingProgressChecker(
			PluginContext context, File directory, int totalFilesNum, String message) {
		super();
		this.context = context;
		this.directory = directory;
		this.totalFilesNum = totalFilesNum;
		this.message = message;
		this.delayMillisecs = DEFAULT_DELAY_MILLISECS;
	}
	
	/**
	 * Create a worker thread for showing the progress of a process that writes a bunch of files in a directory each
	 * time that the given amount of milliseconds passes.
	 * 
	 * @param context The context where the plug-in runs.
	 * @param directory The directory where the files to be counted are saved by the process.
	 * @param totalFilesNum The total number of files to be written.
	 * @param message The message to be displayed beside the file counter.
	 * @param delayMillisecs The delay after which the thread show the progress.
	 */
	public FilesWritingProgressChecker(
			PluginContext context, File directory, int totalFilesNum, String message, int delayMillisecs) {
		super();
		this.context = context;
		this.directory = directory;
		this.totalFilesNum = totalFilesNum;
		this.message = message;
		this.delayMillisecs = delayMillisecs;
	}

	@Override
	public void run() {
		try {
			while(true) {
				int filesNum = directory.listFiles().length;
				context.log(filesNum + "/" + totalFilesNum + " " + message);				
				Thread.sleep(delayMillisecs);
			}
		} catch (InterruptedException e) {
			System.out.println("Stop checking progress.");
		}
	}
	
}
