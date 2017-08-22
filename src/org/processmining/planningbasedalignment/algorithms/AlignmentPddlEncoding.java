package org.processmining.planningbasedalignment.algorithms;

import java.io.File;
import java.io.IOException;

import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeMapImpl;
import org.deckfour.xes.model.impl.XTraceImpl;
import org.processmining.datapetrinets.DataPetriNet;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.planningbasedalignment.parameters.PlanningBasedAlignmentParameters;
import org.processmining.planningbasedalignment.pddl.AbstractPddlEncoder;
import org.processmining.planningbasedalignment.pddl.StandardPddlEncoder;
import org.processmining.planningbasedalignment.utils.FilesWritingProgressChecker;
import org.processmining.planningbasedalignment.utils.OSUtils;

/**
 * The implementation of the algorithm for generating the PDDL encoding of an alignment problem instance.
 * 
 * @author Giacomo Lanciano
 *
 */
public class AlignmentPddlEncoding {

	protected static final String PDDL_EXT = ".pddl";
	protected static final String PDDL_FILES_DIR_PREFIX = "pddl_files_";
	protected static final String PDDL_DOMAIN_FILE_PREFIX = "domain";
	protected static final String PDDL_PROBLEM_FILE_PREFIX = "problem";
	protected static final int EMPTY_TRACE_ID = 0;
	protected static final int PDDL_FILES_PER_TRACE = 2;
	
	/**
	 * The object used to generate the PDDL encodings.
	 */
	protected AbstractPddlEncoder pddlEncoder;
	
	/**
	 * The input directory for the planner.
	 */
	protected File parentDir;
	
	/**
	 * The input directory for the planner.
	 */
	protected File pddlFilesDir;
	
	/**
	 * The timestamp of the execution start.
	 */
	protected long startTime;
	
	/**
	 * The separated thread that check PDDL encoding progress.
	 */
	protected Thread pddlEncodingProgressChecker;
	
	/**
	 * Produces the PDDL input files (representing the instances of the alignment problem) to be fed to the planner.
	 * 
	 * @param log
	 * @param petrinet
	 * @param parameters
	 * @throws IOException 
	 */
	protected void buildPlannerInput(
			File parentDir, PluginContext context, XLog log, DataPetriNet petrinet,
			PlanningBasedAlignmentParameters parameters)
					throws IOException {
		
		startTime = System.currentTimeMillis();
		
		if (parentDir != null)
			this.parentDir = parentDir;
		else {
			// TODO define an appropriate default location for temp files
			// default is currently set to program working directory (i.e. ".")
			// this.parentDir = ? ;
		}
		
		// cleanup folder
		pddlFilesDir = new File(this.parentDir, PDDL_FILES_DIR_PREFIX + startTime);
		OSUtils.cleanDirectory(pddlFilesDir);
		
		context.log("Creating PDDL encodings for trace alignment problem instances...");
		
		int[] traceInterval = parameters.getTracesInterval();
		int traceIdToCheckFrom = traceInterval[0];
		int traceIdToCheckTo = traceInterval[1];

		// set traces length bounds
		int[] traceLengthBounds = parameters.getTracesLengthBounds();
		int minTracesLength = traceLengthBounds[0];
		int maxTracesLength = traceLengthBounds[1];

		pddlEncoder = new StandardPddlEncoder(petrinet, parameters); //TODO change implementation according to params

		// add empty trace to the collection of trace to be aligned to compute fitness
		XTrace emptyTrace = new XTraceImpl(new XAttributeMapImpl());
		writePddlEncodings(emptyTrace, EMPTY_TRACE_ID);
		
		// start progress checker
		int totalPddlFilesNum =  (traceIdToCheckTo - traceIdToCheckFrom + 1) * PDDL_FILES_PER_TRACE;
		pddlEncodingProgressChecker = new FilesWritingProgressChecker(
				context, pddlFilesDir, totalPddlFilesNum, " PDDL files written so far.", 1000);
		pddlEncodingProgressChecker.start();
		
		// consider only the traces in the chosen interval
		for(int traceId = traceIdToCheckFrom-1; traceId < traceIdToCheckTo; traceId++) {

			XTrace trace = log.get(traceId);
			int traceLength = trace.size();						

			// check whether the trace matches the length bounds
			if(traceLength >= minTracesLength && traceLength <= maxTracesLength)  {

				// create PDDL encodings (domain & problem) for current trace
				writePddlEncodings(trace, traceId+1);
			}
		}
		
		pddlEncodingProgressChecker.interrupt();
	}
	
	/**
	 * Shut down all active computations.
	 */
	protected void killSubprocesses() {
		if (pddlEncodingProgressChecker != null)
			pddlEncodingProgressChecker.interrupt();
	}
	
	/**
	 * Write the PDDL files related to the given trace.
	 * 
	 * @param trace The trace.
	 * @param traceId The trace id.
	 * @throws IOException 
	 */
	private void writePddlEncodings(XTrace trace, int traceId) throws IOException {
		String pddlFileSuffix = traceId + PDDL_EXT;
		String sbDomainFileName = new File(pddlFilesDir, PDDL_DOMAIN_FILE_PREFIX + pddlFileSuffix).getCanonicalPath();
		String sbProblemFileName = new File(pddlFilesDir, PDDL_PROBLEM_FILE_PREFIX + pddlFileSuffix).getCanonicalPath();

		OSUtils.writeTextualFile(sbDomainFileName, pddlEncoder.createPropositionalDomain(trace));
		OSUtils.writeTextualFile(sbProblemFileName, pddlEncoder.createPropositionalProblem(trace));
	}

}
