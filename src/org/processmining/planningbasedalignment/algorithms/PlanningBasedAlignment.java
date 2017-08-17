package org.processmining.planningbasedalignment.algorithms;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.datapetrinets.DataPetriNet;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.planningbasedalignment.parameters.PlanningBasedAlignmentParameters;
import org.processmining.planningbasedalignment.pddl.AbstractPddlEncoder;
import org.processmining.planningbasedalignment.utils.FilesWritingProgressChecker;
import org.processmining.planningbasedalignment.utils.OSUtils;
import org.processmining.planningbasedalignment.utils.PlannerSearchStrategy;
import org.processmining.planningbasedalignment.utils.StreamAsyncReader;
import org.processmining.plugins.DataConformance.DataAlignment.DataAlignmentState;
import org.processmining.plugins.DataConformance.DataAlignment.GenericTrace;
import org.processmining.plugins.DataConformance.DataAlignment.PetriNet.ResultReplayPetriNetWithData;
import org.processmining.plugins.DataConformance.framework.ExecutionStep;
import org.processmining.plugins.DataConformance.framework.ExecutionTrace;
import org.processmining.plugins.DataConformance.framework.VariableMatchCosts;

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
	protected static final String CASE_PREFIX = "Case ";
	protected static final String DEFAULT_TIME_UNIT = " ms";
	protected static final int INITIAL_EXECUTION_TRACE_CAPACITY = 10;
	
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
	 * @param log The event log.
	 * @param petrinet The Petri net.
	 * @param parameters The parameters to use.
	 * @return The result of the replay of the event log on the Petri net.
	 */
	protected ResultReplayPetriNetWithData align(
			PluginContext context, XLog log, DataPetriNet petrinet, PlanningBasedAlignmentParameters parameters) {
		  
		ResultReplayPetriNetWithData output = null;
		
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
		if (resourcesUnpacker != null)
			resourcesUnpacker.interrupt();
	}
	
	/**
	 * Starts the execution of the planner for all the produced pairs domain/problem.
	 * 
	 * @param context
	 * @param parameters
	 * @param plansFoundDir
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
		
		String[] commandArgs = buildFastDownardCommandArgs(context, parameters);
		
//		System.out.println("\n" + startTime + "\n" + Arrays.toString(commandArgs));
		
		// execute external planner script and wait for results
		ProcessBuilder processBuilder = new ProcessBuilder(commandArgs);
		plannerManagerProcess = processBuilder.start();

		// read std out & err in separated thread
		StreamAsyncReader errorGobbler = new StreamAsyncReader(plannerManagerProcess.getErrorStream(), "ERROR");
		StreamAsyncReader outputGobbler = new StreamAsyncReader(plannerManagerProcess.getInputStream(), "OUTPUT");
		errorGobbler.start();
		outputGobbler.start();
		
		// start thread to show progress to the user
		int[] traceInterval = parameters.getTracesInterval();
		int totalAlignmentsNum = traceInterval[1] - traceInterval[0] + 1;
		alignmentProgressChecker = new FilesWritingProgressChecker(
				context, plansFoundDir, totalAlignmentsNum, " alignments processed so far.");
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
	 * @param log
	 * @param petrinet
	 * @param parameters
	 * @param plansFoundDir
	 * @return
	 * @throws IOException
	 */
	private ResultReplayPetriNetWithData parsePlannerOutput(
			XLog log, DataPetriNet petrinet, PlanningBasedAlignmentParameters parameters) throws IOException {
		
		if (!plansFoundDir.exists()) {
			throw new FileNotFoundException("The planner output directory does not exist.");
		}
		
		Pattern decimalNumberRegexPattern = Pattern.compile("\\d+(,\\d{3})*(\\.\\d+)*");
		ExecutionTrace logTrace;
		ExecutionTrace processTrace;
		DataAlignmentState dataAlignmentState;
		ArrayList<DataAlignmentState> alignments = new ArrayList<DataAlignmentState>();
		ResultReplayPetriNetWithData result = null;		
		float traceAlignmentCost;
		float emptyTraceAlignmentCost = 0;
		
		// initialize stats summaries
		SummaryStatistics alignmentTimeSummary =new SummaryStatistics();
		SummaryStatistics expandedStatesSummary =new SummaryStatistics();
		SummaryStatistics generatedStatesSummary =new SummaryStatistics();
		
		// iterate over planner output files
		File[] alignmentFiles = plansFoundDir.listFiles();
		for(final File alignmentFile : alignmentFiles) {

			// extract traceId
			Matcher traceIdMatcher = decimalNumberRegexPattern.matcher(alignmentFile.getName());
			traceIdMatcher.find();
			int traceId = Integer.parseInt(traceIdMatcher.group());

			traceAlignmentCost = 0;
			logTrace = new GenericTrace(INITIAL_EXECUTION_TRACE_CAPACITY, CASE_PREFIX + traceId);
			processTrace = new GenericTrace(INITIAL_EXECUTION_TRACE_CAPACITY, CASE_PREFIX + traceId);

			// parse planner output file line by line
			Matcher matcher;
			BufferedReader processOutputReader = new BufferedReader(new FileReader(alignmentFile));
			String outputLine = processOutputReader.readLine(); 
			while (outputLine != null) {

				// parse decimal number in output line
				matcher = decimalNumberRegexPattern.matcher(outputLine);
				matcher.find();
				
				if(outputLine.startsWith(COST_ENTRY_PREFIX)) {
					// parse alignment cost
					traceAlignmentCost = Float.parseFloat(matcher.group());
					
					if (traceId == EMPTY_TRACE_ID)
						// if empty trace, set the cost to compute fitness
						emptyTraceAlignmentCost = traceAlignmentCost;

				} else if (traceId != EMPTY_TRACE_ID) {
					
					if(outputLine.startsWith(SEARCH_TIME_ENTRY_PREFIX)) {
						alignmentTimeSummary.addValue(Double.parseDouble(matcher.group()));

					} else if(outputLine.startsWith(EXPANDED_STATES_ENTRY_PREFIX)) {
						expandedStatesSummary.addValue(Double.parseDouble(matcher.group()));
						
					} else if(outputLine.startsWith(GENERATED_STATES_ENTRY_PREFIX)) {
						generatedStatesSummary.addValue(Double.parseDouble(matcher.group()));
						
					} else {
						// parse alignment move
						ExecutionStep step = null;
						String stepName = extractMovePddlId(outputLine);
	
						// check move type
						if (isSynchronousMove(outputLine)) {							
							Transition transition = (Transition) pddlEncoder.getPddlIdToPetrinetNodeMapping().get(stepName);
							step = new ExecutionStep(transition.getLabel(), transition);
							logTrace.add(step);
							processTrace.add(step);
	
						} else if (isModelMove(outputLine)) {
							Transition transition = (Transition) pddlEncoder.getPddlIdToPetrinetNodeMapping().get(stepName);
							step = new ExecutionStep(transition.getLabel(), transition);
							
							if (transition.isInvisible())
								step.setInvisible(true);
							
							logTrace.add(ExecutionStep.bottomStep);
							processTrace.add(step);
	
						} else if (isLogMove(outputLine)) {
							XEventClass eventClass = pddlEncoder.getPddlIdToEventClassMapping().get(stepName);
							step = new ExecutionStep(eventClass.getId(), eventClass);
							logTrace.add(step);
							processTrace.add(ExecutionStep.bottomStep);
	
						}
					}
				}
				outputLine = processOutputReader.readLine();
			}
			processOutputReader.close();
			
			if (traceId != EMPTY_TRACE_ID) {
				XTrace trace = log.get(traceId-1);
				
				// create alignment object
				dataAlignmentState = new DataAlignmentState(logTrace, processTrace, traceAlignmentCost);
				float fitness = computeFitness(trace, traceAlignmentCost, emptyTraceAlignmentCost, parameters);
				
				// since the visualization used by the plug-in takes also into account the data fitness, 
				// control flow fitness has to be adjusted in order to be shown properly.
				fitness = adjustFitness(fitness);
				
				dataAlignmentState.setControlFlowFitness(fitness);
				
				// add alignment object to collection
				alignments.add(dataAlignmentState);
			}
		}
		
		// print stats
		System.out.println("\tAverage (actual) Time: " + alignmentTimeSummary.getMean() + DEFAULT_TIME_UNIT);
		System.out.println("\tMaximum (actual) Time: " + alignmentTimeSummary.getMax() + DEFAULT_TIME_UNIT);
		System.out.println("\tMinimum (actual) Time: " + alignmentTimeSummary.getMin() + DEFAULT_TIME_UNIT);
		System.out.println("\tStandard deviation:    " + alignmentTimeSummary.getStandardDeviation() + DEFAULT_TIME_UNIT);
		System.out.println();
		System.out.println("\tAverage Expanded States: " + expandedStatesSummary.getMean());
		System.out.println("\tMaximum Expanded States: " + expandedStatesSummary.getMax());
		System.out.println("\tMinimum Expanded States: " + expandedStatesSummary.getMin());
		System.out.println("\tStandard deviation:      " + expandedStatesSummary.getStandardDeviation());
		System.out.println();
		System.out.println("\tAverage Generated States: " + generatedStatesSummary.getMean());
		System.out.println("\tMaximum Generated States: " + generatedStatesSummary.getMax());
		System.out.println("\tMinimum Generated States: " + generatedStatesSummary.getMin());
		System.out.println("\tStandard deviation:       " + generatedStatesSummary.getStandardDeviation());
		
		// produce result to be visualized
		VariableMatchCosts variableCost = VariableMatchCosts.NOCOST;			// dummy
		Map<String, String> variableMapping = new HashMap<String, String>();	// dummy
		XEventClassifier eventClassifier = parameters.getTransitionsEventsMapping().getEventClassifier();
		result = new ResultReplayPetriNetWithData(
				alignments, variableCost, variableMapping, petrinet, log, eventClassifier);
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
		String[] tokens = outputLine.split("#");
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
	private float adjustFitness(float fitness) {
		return (2 * fitness) - 1 ;
	}
	
}
