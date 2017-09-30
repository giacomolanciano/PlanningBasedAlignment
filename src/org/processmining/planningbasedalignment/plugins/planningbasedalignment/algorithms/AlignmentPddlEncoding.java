package org.processmining.planningbasedalignment.plugins.planningbasedalignment.algorithms;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeMapImpl;
import org.deckfour.xes.model.impl.XTraceImpl;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.planningbasedalignment.plugins.planningbasedalignment.parameters.PlanningBasedAlignmentParameters;
import org.processmining.planningbasedalignment.plugins.planningbasedalignment.pddl.AbstractPddlEncoder;
import org.processmining.planningbasedalignment.plugins.planningbasedalignment.pddl.PartialOrderAwarePddlEncoder;
import org.processmining.planningbasedalignment.plugins.planningbasedalignment.pddl.StandardPddlEncoder;
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
	protected static final String MAPPING_FILE_NAME = "_mapping.txt";
	protected static final int EMPTY_TRACE_POS = 0;
	protected static final int PDDL_FILES_PER_TRACE = 2;
	protected static final int PROGRESS_CHECKER_DELAY = 1000;
	
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
	 * The mapping between the position of a trace in a log (starting from 1) and the relate case id. Notice that key
	 * 0 is reserved for the empty trace.
	 */
	protected Map<Integer, String> positionToCaseIdMapping;
	
	/**
	 * The positions in the log of the traces to align.
	 */
	protected List<Integer> tracesToAlign;
	
	/**
	 * Produces the PDDL input files (representing the instances of the alignment problem) to be fed to the planner.
	 * 
	 * @param log The event log to replay.
	 * @param petrinet The Petri net on which the log has to be replayed.
	 * @param parameters The parameters to be used by the encoding algorithm.
	 * @throws IOException 
	 */
	protected void buildPlannerInput(
			File parentDir, PluginContext context, XLog log, Petrinet petrinet,
			PlanningBasedAlignmentParameters parameters)
					throws IOException {
		
		startTime = System.currentTimeMillis();
		
		if (parentDir != null) {
			this.parentDir = parentDir;
		} else {
			// TODO define an appropriate default location for temp files
			// default is currently set to program working directory (i.e. ".")
			// this.parentDir = ? ;
		}
		
		// cleanup folder
		pddlFilesDir = new File(this.parentDir, PDDL_FILES_DIR_PREFIX + startTime);
		OSUtils.cleanDirectory(pddlFilesDir);
		
		context.log("Creating PDDL encodings for trace alignment problem instances...");

		// select the PDDL encoder implementation according to the assumption on events ordering
		if (parameters.isPartiallyOrderedEvents())
			pddlEncoder = new PartialOrderAwarePddlEncoder(petrinet, parameters);
		else
			pddlEncoder = new StandardPddlEncoder(petrinet, parameters);

		// get the positions in the log of the traces to align
		int[] tracesInterval = parameters.getTracesInterval();
		int[] tracesLengthBounds = parameters.getTracesLengthBounds();
		tracesToAlign = computeTracesToAlign(log, tracesInterval, tracesLengthBounds);
		
		// initialize position to case id mapping
		positionToCaseIdMapping = new HashMap<Integer, String>();
		
		// add empty trace to the collection of trace to be aligned (to compute fitness)
		XTrace emptyTrace = new XTraceImpl(new XAttributeMapImpl());
		writePddlEncoding(emptyTrace, EMPTY_TRACE_POS);
		
		// start progress checker (ignoring empty trace related files)
		int totalPddlFilesNum =  tracesToAlign.size() * PDDL_FILES_PER_TRACE;
		pddlEncodingProgressChecker = new FilesWritingProgressChecker(
				context, pddlFilesDir, totalPddlFilesNum, PDDL_FILES_PER_TRACE, " PDDL files written so far.",
				PROGRESS_CHECKER_DELAY);
		pddlEncodingProgressChecker.start();
		
		// create the PDDL encoding for each trace
		XTrace trace;
		for (Integer tracePos : tracesToAlign) {
			trace = log.get(tracePos);
			writePddlEncoding(trace, tracePos+1);
		}
		
		pddlEncodingProgressChecker.interrupt();
		
		context.log("Dumping mapping between case ids and positions of the traces in the log...");
		writePositionToCaseIdMapping();
	}
	
	/**
	 * Shut down all active computations.
	 */
	protected void killSubprocesses() {
		if (pddlEncodingProgressChecker != null)
			pddlEncodingProgressChecker.interrupt();
	}
	
	/**
	 * Write the PDDL encoding of the alignment problem related to the given trace on disk.
	 * 
	 * @param trace The trace.
	 * @throws IOException 
	 */
	private void writePddlEncoding(XTrace trace, int tracePos) throws IOException {
		updatePositionToCaseIdMapping(trace, tracePos);
		
		// create files for PDDL encoding
		String pddlFileSuffix = tracePos + PDDL_EXT;
		String sbDomainFileName = new File(pddlFilesDir, PDDL_DOMAIN_FILE_PREFIX + pddlFileSuffix).getCanonicalPath();
		String sbProblemFileName = new File(pddlFilesDir, PDDL_PROBLEM_FILE_PREFIX + pddlFileSuffix).getCanonicalPath();

		// write contents on disk
		String[] pddlEncoding = pddlEncoder.getPddlEncoding(trace);
		OSUtils.writeTextualFile(sbDomainFileName, pddlEncoding[0]);
		OSUtils.writeTextualFile(sbProblemFileName, pddlEncoding[1]);
	}
	
	/**
	 * Insert a mapping between the position in the event log and the case id of the given trace.
	 * 
	 * @param trace The trace.
	 * @param tracePos The position of the trace in the event log.
	 */
	private void updatePositionToCaseIdMapping(XTrace trace, int tracePos) {
		String caseId = XConceptExtension.instance().extractName(trace);
		
		if (caseId == null)
			caseId = "";
		
		positionToCaseIdMapping.put(tracePos, caseId);
	}
	
	/**
	 * Scan the given interval of the log to check which traces match the given length constraints.
	 * 
	 * @param log The event log to replay.
	 * @param parameters The parameters to be used by the encoding algorithm.
	 * @return A list of integers containing the positions in the log of the traces to align (ascending order).
	 */
	private List<Integer> computeTracesToAlign(XLog log, int[] tracesInterval, int[] tracesLengthBounds) {
		List<Integer> result = new ArrayList<Integer>();
		
		// get traces interval end-points
		int tracePosToCheckFrom = tracesInterval[0];
		int tracePosToCheckTo = tracesInterval[1];

		// get traces min & max length
		int minTraceLength = tracesLengthBounds[0];
		int maxTraceLength = tracesLengthBounds[1];
		
		// consider only the traces in the given interval
		for(int tracePos = tracePosToCheckFrom - 1; tracePos < tracePosToCheckTo; tracePos++) {
			XTrace trace = log.get(tracePos);
			int traceLength = trace.size();						

			// check whether the trace matches the length constraints
			if(traceLength >= minTraceLength && traceLength <= maxTraceLength) {
				result.add(tracePos);
			}
		}
		
		return result;
	}
	
	/**
	 * Write the mapping between case ids and positions of the traces in the log on disk.
	 * 
	 * @throws IOException
	 */
	private void writePositionToCaseIdMapping() throws IOException {
		String mappingFileName = new File(pddlFilesDir, MAPPING_FILE_NAME).getCanonicalPath();
		OSUtils.writeTextualFile(mappingFileName, positionToCaseIdMappingToString());
	}
	
	/**
	 * Create a textual representation of the mapping between case ids and positions of the traces in the log.
	 * 
	 * @return The String representing the mapping between case ids and positions of the traces in the log.
	 */
	private String positionToCaseIdMappingToString() {
		StringBuffer result = new StringBuffer();
		
		for (Entry<Integer, String> entry : positionToCaseIdMapping.entrySet()) {
			result.append(entry.getKey() + "\t\t\t" + entry.getValue() + "\n");
		}
		
		return result.toString();
	}

}
