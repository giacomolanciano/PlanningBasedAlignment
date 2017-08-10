package org.processmining.planningbasedalignment.utils;

import java.io.File;

import org.processmining.framework.plugin.PluginContext;

/**
 * Class for showing the progress of the Planning-based Alignment on a separated thread. 
 * 
 * @author Giacomo Lanciano
 *
 */
public class AlignmentProgressChecker extends Thread {
	
	/**
	 * The delay after which the thread show the progress.
	 */
	private static final int DELAY_MILLISECS = 5000;

	/**
	 * The context where the plug-in runs.
	 */
	private PluginContext context;
	
	/**
	 * The directory where the alignments are saved by the planner.
	 */
	private File alignmentsDirectory;
	
	/**
	 * The total number of alignments to be computed. 
	 */
	private int totalAlignmentsNum;
	
	/**
	 * 
	 * @param context The context where the plug-in runs.
	 * @param alignmentsDirectory The directory where the alignments are saved by the planner.
	 */
	public AlignmentProgressChecker(PluginContext context, File alignmentsDirectory, int totalAlignmentsNum) {
		super();
		this.context = context;
		this.alignmentsDirectory = alignmentsDirectory;
		this.totalAlignmentsNum = totalAlignmentsNum;
	}

	@Override
	public void run() {
		try {
			while(true) {
				int filesNum = alignmentsDirectory.listFiles().length;
				context.log(filesNum + "/" + totalAlignmentsNum + " alignments processed so far.");				
				Thread.sleep(DELAY_MILLISECS);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
	
}