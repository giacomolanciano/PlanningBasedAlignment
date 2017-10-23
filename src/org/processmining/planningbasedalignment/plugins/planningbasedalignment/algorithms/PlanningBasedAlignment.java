package org.processmining.planningbasedalignment.plugins.planningbasedalignment.algorithms;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.planningbasedalignment.plugins.planningbasedalignment.models.PlannerSearchStrategy;
import org.processmining.planningbasedalignment.plugins.planningbasedalignment.models.PlanningBasedReplayResult;
import org.processmining.planningbasedalignment.plugins.planningbasedalignment.parameters.PlanningBasedAlignmentParameters;
import org.processmining.planningbasedalignment.plugins.planningbasedalignment.pddl.AbstractPddlEncoder;
import org.processmining.planningbasedalignment.utils.FilesWritingProgressChecker;
import org.processmining.planningbasedalignment.utils.OSUtils;
import org.processmining.planningbasedalignment.utils.StreamAsyncReader;
import org.processmining.plugins.DataConformance.DataAlignment.DataAlignmentState;
import org.processmining.plugins.DataConformance.DataAlignment.GenericTrace;
import org.processmining.plugins.DataConformance.framework.ExecutionStep;
import org.processmining.plugins.DataConformance.framework.ExecutionTrace;

/**
 * The implementation of the algorithm for Planning-based Alignment of an event log and a Petri net.
 * 
 * @author Giacomo Lanciano
 *
 */
public class PlanningBasedAlignment extends AlignmentPddlEncoding {
	
	protected static final String PLANNER_MANAGER_SCRIPT = "planner_manager.py";
	protected static final String FAST_DOWNWARD_DIR = "fast-downward/";
	protected static final String FAST_DOWNWARD_SCRIPT = FAST_DOWNWARD_DIR + "fast-downward.py";
	protected static final String PLANS_FOUND_DIR_PREFIX = "plans_found_";
	protected static final String COST_ENTRY_PREFIX = "; cost = ";
	protected static final String SEARCH_TIME_ENTRY_PREFIX = "; searchtime = ";
	protected static final String EXPANDED_STATES_ENTRY_PREFIX = "; expandedstates = ";
	protected static final String GENERATED_STATES_ENTRY_PREFIX = "; generatedstates = ";
	protected static final String COMMAND_ARG_PLACEHOLDER = "+";
	protected static final int INITIAL_EXECUTION_TRACE_CAPACITY = 10;
	protected static final int RESULT_FILES_PER_TRACE = 1;

	/**
	 * The separated process in which the planner is executed.
	 */
	protected Process plannerManagerProcess;
	
	/**
	 * The separated thread that check alignment progress.
	 */
	protected Thread alignmentProgressChecker;
	
	/**
	 * The separated thread that unpack the planner source code.
	 */
	protected Thread resourcesUnpacker;
	
	/**
	 * The output directory for the planner.
	 */
	protected File plansFoundDir;
		
	/**
	 * The method that performs the alignment of an event log and a Petri net using Automated Planning.
	 * 
	 * @param context The context where to run in.
	 * @param log The event log to replay.
	 * @param petrinet The Petri net on which the log has to be replayed.
	 * @param parameters The parameters to use.
	 * @return The result of the replay of the event log on the Petri net.
	 */
	protected PlanningBasedReplayResult align(
			PluginContext context, XLog log, Petrinet petrinet, PlanningBasedAlignmentParameters parameters) {
		  
		PlanningBasedReplayResult output = null;
		
		try {
			buildPlannerInput(null, context, log, petrinet, parameters);
			invokePlanner(context, parameters);			
			output = parsePlannerOutput(log, petrinet, parameters);
			
		} catch (InterruptedException e) {
			killSubprocesses();
		} catch(Exception e){
			e.printStackTrace();
		}
		
		return output;
	}

	/**
	 * Shut down all active computations.
	 */
	protected void killSubprocesses() {
		super.killSubprocesses();
		if (plannerManagerProcess != null)
			plannerManagerProcess.destroy();
		if (alignmentProgressChecker != null)
			alignmentProgressChecker.interrupt();
	}
	
	/**
	 * Starts the execution of the planner for all the produced pairs domain/problem.
	 * 
	 * @param context The context where to run in.
	 * @param parameters The parameters to be used by the encoding algorithm.
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	private void invokePlanner(
			PluginContext context, PlanningBasedAlignmentParameters parameters)
					throws InterruptedException, IOException, URISyntaxException {
		
		if (!pddlFilesDir.exists()) {
			throw new FileNotFoundException("The planner input directory does not exist.");
		}
		
		plansFoundDir = new File(parentDir, PLANS_FOUND_DIR_PREFIX + startTime);
		OSUtils.cleanDirectory(plansFoundDir);
		
		context.log("Invoking planner...");
		
		// prepare command line args for external planner script
		String[] commandArgs = buildFastDownardCommandArgs(context, parameters);
				
		// execute external planner script and wait for results
		ProcessBuilder processBuilder = new ProcessBuilder(commandArgs);
		plannerManagerProcess = processBuilder.start();
		
		// read std out & err in separated thread
		StreamAsyncReader errorGobbler = new StreamAsyncReader(plannerManagerProcess.getErrorStream(), "ERROR");
		StreamAsyncReader outputGobbler = new StreamAsyncReader(plannerManagerProcess.getInputStream(), "OUTPUT");
		errorGobbler.start();
		outputGobbler.start();
		
		// start progress checker (ignoring empty trace related files)
		int totalAlignmentsNum = tracesToAlign.size();
		alignmentProgressChecker = new FilesWritingProgressChecker(
				context, plansFoundDir, totalAlignmentsNum, RESULT_FILES_PER_TRACE, " alignments processed so far.");
		alignmentProgressChecker.start();

		// wait for the process to return to read the generated outputs
		plannerManagerProcess.waitFor();
		alignmentProgressChecker.interrupt();
	}
	
	/**
	 * Build the arguments list needed to launch Fast-Downward planner, tuned according to user selections. 
	 * Notice that the output, the domain and problem files are defined by the planner manager module once that PDDL
	 * encodings are generated.
	 * 
	 * @param context The context where to run in.
	 * @param parameters The parameters to be used by the encoding algorithm.
	 * @return an array of Strings containing the arguments.
	 * @throws IOException 
	 * @throws URISyntaxException 
	 * @throws InterruptedException 
	 */
	private String[] buildFastDownardCommandArgs(PluginContext context, PlanningBasedAlignmentParameters parameters)
			throws IOException, URISyntaxException, InterruptedException {
		
		ArrayList<String> commandComponents = new ArrayList<>();
		
		// Python 2.7 is assumed to be installed as default version on the user machine
		String pythonInterpreter = "python";

		
		/* begin of command args for planner manager */
		commandComponents.add(pythonInterpreter);
		
		if (resourcesUnpacker != null) {
			context.log("Waiting for planner resources to be unpacked.");
			resourcesUnpacker.join();
		}
		
		// the path to the planner manager script
		File plannerManagerScript = new File(PLANNER_MANAGER_SCRIPT);
		commandComponents.add(plannerManagerScript.getCanonicalPath());
		
		// the path of the current working directory
		File workingDir = new File(".");
		commandComponents.add(workingDir.getCanonicalPath());
		
		// the path of the input directory for the planner
		commandComponents.add(pddlFilesDir.getCanonicalPath());
		
		// the path of the output directory for the planner
		commandComponents.add(plansFoundDir.getCanonicalPath());
		

		/* begin of command args for Fast-Downward */
		commandComponents.add(pythonInterpreter);

		// the path to the fast-downward launcher script
		File fdScript = new File(FAST_DOWNWARD_SCRIPT);
		commandComponents.add(fdScript.getCanonicalPath());

		// Fast-Downward is assumed to be built in advance both for 32 and 64 bits OS (both Windows and Unix-like).
		commandComponents.add("--build");
		if (OSUtils.is64bitsOS())
			commandComponents.add("release64");
		else
			commandComponents.add("release32");
		
		commandComponents.add("--plan-file");
		commandComponents.add(COMMAND_ARG_PLACEHOLDER);  // output file
		commandComponents.add(COMMAND_ARG_PLACEHOLDER);  // domain file
		commandComponents.add(COMMAND_ARG_PLACEHOLDER);  // problem file

		// insert heuristic and search strategy according to user selection
		if(parameters.getPlannerSearchStrategy() == PlannerSearchStrategy.BLIND_A_STAR) {
			commandComponents.add("--heuristic");
			commandComponents.add("hcea=cea()");
			commandComponents.add("--search");
			commandComponents.add("astar(blind())");
			
		} else if(parameters.getPlannerSearchStrategy() == PlannerSearchStrategy.LAZY_GREEDY) {
			commandComponents.add("--heuristic");
			commandComponents.add("hhmax=hmax()");
			commandComponents.add("--search");
			commandComponents.add("lazy_greedy([hhmax], preferred=[hhmax])");
		}

		// return the arguments list as an array of strings
		String[] commandArguments = commandComponents.toArray(new String[0]);
		return commandArguments;
	}
	
	/**
	 * Parse planner output to build the alignment results.
	 * 
	 * @param log The event log to replay.
	 * @param petrinet The Petri net on which the log has to be replayed.
	 * @param parameters The parameters to be used by the encoding algorithm.
	 * @return The alignment of the event log and the Petri net.
	 * @throws IOException
	 */
	private PlanningBasedReplayResult parsePlannerOutput(
			XLog log, Petrinet petrinet, PlanningBasedAlignmentParameters parameters) throws IOException {
		
		if (!plansFoundDir.exists()) {
			throw new RuntimeException("The planner output directory does not exist.");
		}
		
		int tracePos;
		String caseId;
		float traceAlignmentCost;
		float emptyTraceAlignmentCost = 0;
		String outputLine;
		BufferedReader processOutputReader;
		Matcher realNumberMatcher;
		Pattern realNumberRegexPattern = Pattern.compile("\\-?\\d+(,\\d{3})*(\\.\\d+)*");
		
		// result
		ExecutionTrace logTrace;
		ExecutionTrace modelTrace;
		DataAlignmentState dataAlignmentState;
		ArrayList<DataAlignmentState> alignments = new ArrayList<DataAlignmentState>();
		PlanningBasedReplayResult result = null;		
		
		// initialize stats summaries
		boolean alignmentTimeReliable = true;
		boolean expandedStatesReliable = true;
		boolean generatedStatesReliable = true;
		SummaryStatistics alignmentTimeSummary = new SummaryStatistics();
		SummaryStatistics expandedStatesSummary = new SummaryStatistics();
		SummaryStatistics generatedStatesSummary = new SummaryStatistics();
		
		// iterate over planner output files
		File[] alignmentFiles = plansFoundDir.listFiles();
		for(final File alignmentFile : alignmentFiles) {

			traceAlignmentCost = 0;
			
			// extract trace position from file name
			realNumberMatcher = realNumberRegexPattern.matcher(alignmentFile.getName());
			realNumberMatcher.find();
			tracePos = Integer.parseInt(realNumberMatcher.group());
			
			// retrieve case id 
			caseId = positionToCaseIdMapping.get(tracePos);
			
			if (caseId == null) {
				throw new RuntimeException("The given position does not match any case id.");
			}
			
			// initialize alignment execution traces
			logTrace = new GenericTrace(INITIAL_EXECUTION_TRACE_CAPACITY, caseId);
			modelTrace = new GenericTrace(INITIAL_EXECUTION_TRACE_CAPACITY, caseId);

			// parse planner output file line by line
			processOutputReader = new BufferedReader(new FileReader(alignmentFile));
			while ((outputLine = processOutputReader.readLine()) != null) {

				// parse real number in output line
				realNumberMatcher = realNumberRegexPattern.matcher(outputLine);
				realNumberMatcher.find();
				
				if(outputLine.startsWith(COST_ENTRY_PREFIX)) {
					// parse alignment cost
					traceAlignmentCost = Float.parseFloat(realNumberMatcher.group());
					
					if (tracePos == EMPTY_TRACE_POS)
						// if empty trace, set the cost to compute fitness
						emptyTraceAlignmentCost = traceAlignmentCost;

				} else if (tracePos != EMPTY_TRACE_POS) {
					
					double parsedValue;
					if(outputLine.startsWith(SEARCH_TIME_ENTRY_PREFIX)) {
						parsedValue = Double.parseDouble(realNumberMatcher.group());
						
						// if the value is negative, then an overflow has occurred. the stat is not reliable anymore
						if (parsedValue < 0)
							alignmentTimeReliable = false;
						
						alignmentTimeSummary.addValue(parsedValue);

					} else if(outputLine.startsWith(EXPANDED_STATES_ENTRY_PREFIX)) {
						parsedValue = Double.parseDouble(realNumberMatcher.group());
						
						if (parsedValue < 0)
							expandedStatesReliable = false;
						
						expandedStatesSummary.addValue(parsedValue);
						
					} else if(outputLine.startsWith(GENERATED_STATES_ENTRY_PREFIX)) {
						parsedValue = Double.parseDouble(realNumberMatcher.group());
						
						if (parsedValue < 0)
							generatedStatesReliable = false;
						
						generatedStatesSummary.addValue(parsedValue);
						
					} else {
						// parse alignment move
						ExecutionStep step = null;
						String stepName = extractMovePddlId(outputLine);
	
						// check move type
						if (isSynchronousMove(outputLine)) {							
							Transition transition = (Transition) pddlEncoder.getPddlIdToPetrinetNodeMapping().get(stepName);
							step = new ExecutionStep(transition.getLabel(), transition);
							logTrace.add(step);
							modelTrace.add(step);
	
						} else if (isModelMove(outputLine)) {
							Transition transition = (Transition) pddlEncoder.getPddlIdToPetrinetNodeMapping().get(stepName);
							step = new ExecutionStep(transition.getLabel(), transition);
							
							if (transition.isInvisible())
								step.setInvisible(true);
							
							logTrace.add(ExecutionStep.bottomStep);
							modelTrace.add(step);
	
						} else if (isLogMove(outputLine)) {
							XEventClass eventClass = pddlEncoder.getPddlIdToEventClassMapping().get(stepName);
							step = new ExecutionStep(eventClass.getId(), eventClass);
							logTrace.add(step);
							modelTrace.add(ExecutionStep.bottomStep);
	
						}
					}
				}
			}
			processOutputReader.close();
			
			if (tracePos != EMPTY_TRACE_POS) {
				XTrace trace = log.get(tracePos-1);
				
				// create alignment object
				dataAlignmentState = new DataAlignmentState(logTrace, modelTrace, traceAlignmentCost);
				float fitness = computeFitness(trace, traceAlignmentCost, emptyTraceAlignmentCost, parameters);
				dataAlignmentState.setControlFlowFitness(fitness);
				
				// add alignment object to collection
				alignments.add(dataAlignmentState);
			}
			
			// delete alignment file from disk
			FileUtils.deleteQuietly(alignmentFile);
		}
		
		// delete alignment files directory from disk
		FileUtils.deleteQuietly(plansFoundDir);
		
		// produce result to be visualized
		XEventClassifier eventClassifier = parameters.getTransitionsEventsMapping().getEventClassifier();
		result = new PlanningBasedReplayResult(alignments, eventClassifier, log, petrinet);
		
		// add alignment time stats to result (if any)
		if (alignmentTimeSummary.getN() > 0) {			
			result.setAlignmentTimeSummary(alignmentTimeSummary);
			
			// print stats
			System.out.println(alignmentTimeSummaryToString(alignmentTimeSummary, alignmentTimeReliable));
		}
		
		// add expanded states stats to result (if any)
		if (expandedStatesSummary.getN() > 0) {			
			result.setExpandedStatesSummary(expandedStatesSummary);
			
			// print stats
			System.out.println(expandedStatesSummaryToString(expandedStatesSummary, expandedStatesReliable));
		}
		
		// add generated states stats to result (if any)
		if (generatedStatesSummary.getN() > 0) {			
			result.setGeneratedStatesSummary(generatedStatesSummary);
			
			// print stats
			System.out.println(generatedStatesSummaryToString(generatedStatesSummary, generatedStatesReliable));
		}
		
		return result;
	}
	
	/**
	 * Check whether the given planner output file line is related to a synchronous move.
	 * 
	 * @param outputLine A String representing the output file line.
	 * @return true if the output file line is related to a synchronous move.
	 */
	private boolean isSynchronousMove(String outputLine) {
		return outputLine.startsWith("(" + AbstractPddlEncoder.SYNCH_MOVE_PREFIX);
	}
	
	/**
	 * Check whether the given planner output file line is related to a model move.
	 * 
	 * @param outputLine A String representing the output file line.
	 * @return true if the output file line is related to a model move.
	 */
	private boolean isModelMove(String outputLine) {
		return outputLine.startsWith("(" + AbstractPddlEncoder.MODEL_MOVE_PREFIX);
	}
	
	/**
	 * Check whether the given planner output file line is related to a log move.
	 * 
	 * @param outputLine A String representing the output file line.
	 * @return true if the output file line is related to a log move.
	 */
	private boolean isLogMove(String outputLine) {
		return outputLine.startsWith("(" + AbstractPddlEncoder.LOG_MOVE_PREFIX);
	}
	
	/**
	 * Extract from the given planner output file line the PDDL identifier of the related move. It could be either the 
	 * name of a Petri net activity (possibly invisible), or the name of an event class.
	 * 
	 * @param outputLine A String representing the output file line.
	 * @return true if the output file line is related to a log move.
	 */
	private String extractMovePddlId(String outputLine) {
		String[] tokens = outputLine.split(AbstractPddlEncoder.SEPARATOR);
		return tokens[1].replaceAll("\\)", "").trim();
	}
	
	/**
	 * Compute the fitness of the given trace.
	 * 
	 * @param trace The trace.
	 * @param alignmentCost The cost of aligning the trace.
	 * @param emptyTraceCost The cost of aligning an empty trace on the same model (used for worst case scenario).
	 * @param parameters The parameters of the plug-in.
	 * @return a float representing the fitness of the trace.
	 */
	private float computeFitness(
			XTrace trace, float alignmentCost, float emptyTraceCost, PlanningBasedAlignmentParameters parameters) {
		
		XEventClassifier eventClassifier = parameters.getTransitionsEventsMapping().getEventClassifier();
		
		// compute the cost of performing a move in log for each event in the trace
		float traceCost = 0;
		for (XEvent event : trace) {
			for(Entry<XEventClass, Integer> entry : parameters.getMovesOnLogCosts().entrySet()) {
				String eventClass = entry.getKey().getId();
				if(eventClass.equalsIgnoreCase(eventClassifier.getClassIdentity(event))) {
					traceCost += entry.getValue();
					break;
				}
			}
		}
		
		float worstCaseCost = traceCost + emptyTraceCost;
		return 1 - (alignmentCost / worstCaseCost);
	}
	
	/**
	 * Adjust the given fitness value for it to be correctly displayed by the visualizer of DataAwareReplayer package
	 * (i.e. not taking into account the data fitness).
	 * 
	 * @param fitness The float representing the fitness value.
	 * @return The adjusted value.
	 */
	@SuppressWarnings("unused")
	@Deprecated
	private float adjustFitness(float fitness) {
		return (2 * fitness) - 1 ;
	}
	
	/**
	 * Provide a standard format for displaying real values.
	 * 
	 * @return A {@link NumberFormat} to format values with.
	 */
	private NumberFormat getRealNumberFormat() {
		NumberFormat realFormat = NumberFormat.getNumberInstance();
		realFormat.setMaximumFractionDigits(2);
		return realFormat;
	}
	
	/**
	 * Provide a textual view of the relevant statistics in the given summary about traces alignments time.
	 * 
	 * @param alignmentTimeSummary The {@link SummaryStatistics} representing the summary about traces alignments time.
	 * @param reliable Whether the statistics are reliable or not (e.g. if an overflow occurred during the computation).
	 */
	private String alignmentTimeSummaryToString(SummaryStatistics alignmentTimeSummary, boolean reliable) {
		StringBuffer result = new StringBuffer();
		NumberFormat realFormat = getRealNumberFormat();
		result.append("\tAlignment Time Reliability: " + reliable);
		result.append('\n');
		result.append("\tAverage (actual) Time: " + realFormat.format(alignmentTimeSummary.getMean()));
		result.append(DEFAULT_TIME_UNIT + '\n');
		result.append("\tMaximum (actual) Time: " + realFormat.format(alignmentTimeSummary.getMax()));
		result.append(DEFAULT_TIME_UNIT + '\n');
		result.append("\tMinimum (actual) Time: " + realFormat.format(alignmentTimeSummary.getMin()));
		result.append(DEFAULT_TIME_UNIT + '\n');
		result.append("\tStandard deviation:    " + realFormat.format(alignmentTimeSummary.getStandardDeviation()));
		result.append(DEFAULT_TIME_UNIT + '\n');
		
		return result.toString();
	}
	
	/**
	 * Provide a textual view of the relevant statistics in the given summary about traces alignments expanded search 
	 * states.
	 * 
	 * @param expandedStatesSummary The {@link SummaryStatistics} representing the summary about traces alignments
	 * expanded search states.
	 * @param reliable Whether the statistics are reliable or not (e.g. if an overflow occurred during the computation).
	 */
	private String expandedStatesSummaryToString(SummaryStatistics expandedStatesSummary, boolean reliable) {
		StringBuffer result = new StringBuffer();
		NumberFormat realFormat = getRealNumberFormat();
		result.append("\tExpanded States Reliability: " + reliable);
		result.append('\n');
		result.append("\tAverage Expanded States: " + realFormat.format(expandedStatesSummary.getMean()));
		result.append('\n');
		result.append("\tMaximum Expanded States: " + realFormat.format(expandedStatesSummary.getMax()));
		result.append('\n');
		result.append("\tMinimum Expanded States: " + realFormat.format(expandedStatesSummary.getMin()));
		result.append('\n');
		result.append("\tStandard deviation:      " + realFormat.format(expandedStatesSummary.getStandardDeviation()));
		result.append('\n');
		
		return result.toString();
	}
	
	/**
	 * Provide a textual view of the relevant statistics in the given summary about traces alignments generated search 
	 * states.
	 * 
	 * @param generatedStatesSummary The {@link SummaryStatistics} representing the summary about traces alignments
	 * generated search states.
	 * @param reliable Whether the statistics are reliable or not (e.g. if an overflow occurred during the computation).
	 */
	private String generatedStatesSummaryToString(SummaryStatistics generatedStatesSummary, boolean reliable) {
		StringBuffer result = new StringBuffer();
		NumberFormat realFormat = getRealNumberFormat();
		result.append("\tGenerated States Reliability: " + reliable);
		result.append('\n');
		result.append("\tAverage Generated States: " + realFormat.format(generatedStatesSummary.getMean()));
		result.append('\n');
		result.append("\tMaximum Generated States: " + realFormat.format(generatedStatesSummary.getMax()));
		result.append('\n');
		result.append("\tMinimum Generated States: " + realFormat.format(generatedStatesSummary.getMin()));
		result.append('\n');
		result.append("\tStandard deviation:       " + realFormat.format(generatedStatesSummary.getStandardDeviation()));
		result.append('\n');
		
		return result.toString();
	}
	
}
