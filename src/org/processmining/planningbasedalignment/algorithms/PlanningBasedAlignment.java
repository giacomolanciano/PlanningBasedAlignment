package org.processmining.planningbasedalignment.algorithms;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeMapImpl;
import org.deckfour.xes.model.impl.XTraceImpl;
import org.processmining.datapetrinets.DataPetriNet;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.planningbasedalignment.parameters.PlanningBasedAlignmentParameters;
import org.processmining.planningbasedalignment.pddl.AbstractPddlEncoder;
import org.processmining.planningbasedalignment.pddl.StandardPddlEncoder;
import org.processmining.planningbasedalignment.utils.AlignmentProgressChecker;
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
public class PlanningBasedAlignment {
	
	protected static final String PLANS_FOUND_DIR = "plans_found/";
	protected static final String PDDL_FILES_DIR = "pddl_files/";
	protected static final String PDDL_EXT = ".pddl";
	protected static final String PDDL_DOMAIN_FILE_PREFIX = PDDL_FILES_DIR + "domain";
	protected static final String PDDL_PROBLEM_FILE_PREFIX = PDDL_FILES_DIR + "problem";
	protected static final String COST_ENTRY_PREFIX = "; cost = ";
	protected static final String SEARCH_TIME_ENTRY_PREFIX = "; searchtime = ";
	protected static final String COMMAND_ARG_PLACEHOLDER = "+";
	protected static final String PLANNER_MANAGER_SCRIPT = "planner_manager.py";
	protected static final String FAST_DOWNWARD_DIR = "fast-downward/";
	protected static final String FAST_DOWNWARD_SCRIPT = FAST_DOWNWARD_DIR + "fast-downward.py";
	protected static final String CASE_PREFIX = "Case ";
	protected static final int INITIAL_EXECUTION_TRACE_CAPACITY = 10;
	protected static final int EMPTY_TRACE_ID = 0;
	
	/**
	 * The object used to generate the PDDL encodings.
	 */
	protected AbstractPddlEncoder pddlEncoder;
	
	/**
	 * The separated process in which the planner is executed.
	 */
	protected Process plannerManagerProcess;
	
	/**
	 * The separated thread that check alignment progress.
	 */
	protected AlignmentProgressChecker progressChecker;
	
	protected Thread unpackerThread;

	/**
	 * The method that performs the alignment of an event log and a Petri net using Automated Planning.
	 * 
	 * @param context The context where to run in.
	 * @param log The event log.
	 * @param petrinet The Petri net.
	 * @param parameters The parameters to use.
	 * @return The result of the replay of the event log on the Petri net.
	 */
	protected ResultReplayPetriNetWithData apply(PluginContext context, XLog log, DataPetriNet petrinet,
			PlanningBasedAlignmentParameters parameters) {
		  
		ResultReplayPetriNetWithData output = null;
		
		File pddlFilesDir;	// The input directory for the planner.
		File plansFoundDir; // The output directory for the planner.
		
		try {
			// cleanup folders
			plansFoundDir = new File(PLANS_FOUND_DIR);
			pddlFilesDir = new File(PDDL_FILES_DIR);
			OSUtils.cleanFolder(plansFoundDir);
			OSUtils.cleanFolder(pddlFilesDir);

			/* PLANNER INPUTS BUILDING */
			buildPlannerInput(log, petrinet, parameters);

			/* PLANNER INVOCATION */
			invokePlanner(context, parameters, plansFoundDir);
			
			/* PLANNER OUTPUTS PROCESSING */
			output = processPlannerOutput(log, petrinet, parameters, plansFoundDir);
			
		} catch (InterruptedException e) {
			killSubprocesses();
		} catch(Exception e){
			e.printStackTrace();
		}
		
		return output;
	}

	/**
	 * Shut down all active computations.
	 * 
	 */
	protected void killSubprocesses() {
		if (plannerManagerProcess != null)
			plannerManagerProcess.destroy();
		if (progressChecker != null)
			progressChecker.interrupt();
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
	protected String[] buildFastDownardCommandArgs(PluginContext context, PlanningBasedAlignmentParameters parameters)
			throws IOException, URISyntaxException, InterruptedException {
		
		ArrayList<String> commandComponents = new ArrayList<>();
		
		// Python 2.7 is assumed to be installed as default version on the user machine
		String pythonInterpreter = "python";
		
		// since this class is never directly instantiated, access the superclass to get the correct package
//		String packageName = this.getClass().getSuperclass().getPackage().getName();
//		packageName = packageName.replaceAll("\\.", "/") + "/";
		
		
		/* begin of command args for planner manager */
		commandComponents.add(pythonInterpreter);
		
		if (unpackerThread != null)
			context.log("Waiting for planner resources to be unpacked.");
			unpackerThread.join();
		
		// the path to the planner manager script
		File plannerManagerScript = new File(PLANNER_MANAGER_SCRIPT);
		commandComponents.add(plannerManagerScript.getCanonicalPath());
		
		// the path of the current working directory, where planner inputs and outputs are saved
		File workingDir = new File(".");
		commandComponents.add(workingDir.getCanonicalPath());
		

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
	 * Produces the PDDL input files (representing the instances of the alignment problem) to be fed to the planner.
	 * 
	 * @param log
	 * @param petrinet
	 * @param parameters
	 */
	protected void buildPlannerInput(XLog log, DataPetriNet petrinet, PlanningBasedAlignmentParameters parameters) {
		
		int[] traceInterval = parameters.getTracesInterval();
		int traceIdToCheckFrom = traceInterval[0];
		int traceIdToCheckTo = traceInterval[1];

		// set traces length bounds
		int[] traceLengthBounds = parameters.getTracesLengthBounds();
		int minTracesLength = traceLengthBounds[0];
		int maxTracesLength = traceLengthBounds[1];

		pddlEncoder = new StandardPddlEncoder(petrinet, parameters);	//TODO change implementation according to parameters

		// add empty trace to the collection of trace to be aligned to compute fitness
		XTrace emptyTrace = new XTraceImpl(new XAttributeMapImpl());
		writePddlFiles(emptyTrace, EMPTY_TRACE_ID);
		
		// consider only the traces in the chosen interval
		for(int traceId = traceIdToCheckFrom-1; traceId < traceIdToCheckTo; traceId++) {

			XTrace trace = log.get(traceId);
			int traceLength = trace.size();						

			// check whether the trace matches the length bounds
			if(traceLength >= minTracesLength && traceLength <= maxTracesLength)  {

				// create PDDL encodings (domain & problem) for current trace
				writePddlFiles(trace, traceId+1);
			}
		}
	}
	
	/**
	 * Write the PDDL files related to the given trace.
	 * 
	 * @param trace The trace.
	 * @param traceId The trace id.
	 */
	protected void writePddlFiles(XTrace trace, int traceId) {
		StringBuffer sbDomain = pddlEncoder.createPropositionalDomain(trace);
		StringBuffer sbProblem = pddlEncoder.createPropositionalProblem(trace);
		String sbDomainFileName = PDDL_DOMAIN_FILE_PREFIX + traceId + PDDL_EXT;
		String sbProblemFileName = PDDL_PROBLEM_FILE_PREFIX + traceId + PDDL_EXT;
		OSUtils.writeFile(sbDomainFileName, sbDomain);
		OSUtils.writeFile(sbProblemFileName, sbProblem);
	}
	
	/**
	 * Starts the execution of the planner for all the produced pairs domain/problem.
	 * @param context 
	 * 
	 * @param parameters
	 * @param plansFoundDir 
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws URISyntaxException 
	 */
	protected void invokePlanner(PluginContext context, PlanningBasedAlignmentParameters parameters,
			File alignmentsDirectory) throws InterruptedException, IOException, URISyntaxException {
		
		String[] commandArgs = buildFastDownardCommandArgs(context, parameters);
		
		// execute external planner script and wait for results
		ProcessBuilder processBuilder = new ProcessBuilder(commandArgs);
		plannerManagerProcess = processBuilder.start();

		// read std out & err in separated thread
		StreamAsyncReader errorGobbler = new StreamAsyncReader(plannerManagerProcess.getErrorStream(), "ERROR");
		StreamAsyncReader outputGobbler = new StreamAsyncReader(plannerManagerProcess.getInputStream(), "OUTPUT");
		errorGobbler.start();
		outputGobbler.start();
		
		// start thread to show progress to the user
		progressChecker = new AlignmentProgressChecker(context, alignmentsDirectory);
		progressChecker.start();

		// wait for the process to return to read the generated outputs
		plannerManagerProcess.waitFor();
		progressChecker.interrupt();
	}
	
	/**
	 * Process planner outputs to build the alignment results. 
	 * 
	 * @param log
	 * @throws IOException
	 */
	protected ResultReplayPetriNetWithData processPlannerOutput(XLog log, DataPetriNet petrinet,
			PlanningBasedAlignmentParameters parameters, File plansFoundDir) throws IOException {
				
		Pattern decimalNumberRegexPattern = Pattern.compile("\\d+(,\\d{3})*(\\.\\d+)*");
		
		float alignmentCost;
		float emptyTraceCost = 0;
		ExecutionTrace logTrace;
		ExecutionTrace processTrace;
		DataAlignmentState dataAlignmentState;
		ArrayList<DataAlignmentState> alignments = new ArrayList<DataAlignmentState>();
		ResultReplayPetriNetWithData result = null;
		
		// iterate over planner output files
		for(final File alignmentFile : plansFoundDir.listFiles()) {

			// extract traceId
			Matcher traceIdMatcher = decimalNumberRegexPattern.matcher(alignmentFile.getName());
			traceIdMatcher.find();
			int traceId = Integer.parseInt(traceIdMatcher.group());

			alignmentCost = 0;
			logTrace = new GenericTrace(INITIAL_EXECUTION_TRACE_CAPACITY, CASE_PREFIX + traceId);
			processTrace = new GenericTrace(INITIAL_EXECUTION_TRACE_CAPACITY, CASE_PREFIX + traceId);

			// parse planner output file line by line
			String traceAlignmentCost = new String();
//			String traceAlignmentTime = new String();
			BufferedReader processOutputReader = new BufferedReader(new FileReader(alignmentFile));
			String outputLine = processOutputReader.readLine(); 
			while (outputLine != null) {

				if(outputLine.startsWith(COST_ENTRY_PREFIX)) {
					// parse alignment cost
					Matcher matcher = decimalNumberRegexPattern.matcher(outputLine);
					matcher.find();
					traceAlignmentCost = matcher.group();
					alignmentCost = Float.parseFloat(traceAlignmentCost);
					
					if (traceId == EMPTY_TRACE_ID)
						// if empty trace, set the cost to compute fitness
						emptyTraceCost = alignmentCost;

				} else if(outputLine.startsWith(SEARCH_TIME_ENTRY_PREFIX)) {
					// parse alignment time						
//					Matcher matcher = decimalNumberRegexPattern.matcher(outputLine);
//					matcher.find();
//					traceAlignmentTime = matcher.group();

				} else if (traceId != EMPTY_TRACE_ID) {
					// if not empty trace, process the alignment move
					
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
				outputLine = processOutputReader.readLine();
			}
			processOutputReader.close();
			
			if (traceId != EMPTY_TRACE_ID) {
				XTrace trace = log.get(traceId-1);
				
				// create alignment object
				dataAlignmentState = new DataAlignmentState(logTrace, processTrace, alignmentCost);
				float fitness = computeFitness(trace, alignmentCost, emptyTraceCost, parameters);
				
				// since the visualization used by the plug-in takes also into account the data fitness, 
				// control flow fitness has to be adjusted in order to be shown properly.
				fitness = adjustFitness(fitness);
				
				dataAlignmentState.setControlFlowFitness(fitness);
				
				// add alignment object to collection
				alignments.add(dataAlignmentState);
			}
		}
		
		VariableMatchCosts variableCost = VariableMatchCosts.NOCOST;			// dummy
		Map<String, String> variableMapping = new HashMap<String, String>();	// dummy
		XEventClassifier eventClassifier = parameters.getTransitionsEventsMapping().getEventClassifier();
		result = new ResultReplayPetriNetWithData(alignments, variableCost, variableMapping, petrinet, log, eventClassifier);
		return result;
	}
	
	/**
	 * Check whether the given planner output file line is related to a synchronous move.
	 * 
	 * @param outputLine A String representing the output file line.
	 * @return true if the output file line is related to a synchronous move.
	 */
	protected boolean isSynchronousMove(String outputLine) {
		return outputLine.startsWith("(" + AbstractPddlEncoder.SYNCH_MOVE_PREFIX);
	}
	
	/**
	 * Check whether the given planner output file line is related to a model move.
	 * 
	 * @param outputLine A String representing the output file line.
	 * @return true if the output file line is related to a model move.
	 */
	protected boolean isModelMove(String outputLine) {
		return outputLine.startsWith("(" + AbstractPddlEncoder.MODEL_MOVE_PREFIX);
	}
	
	/**
	 * Check whether the given planner output file line is related to a log move.
	 * 
	 * @param outputLine A String representing the output file line.
	 * @return true if the output file line is related to a log move.
	 */
	protected boolean isLogMove(String outputLine) {
		return outputLine.startsWith("(" + AbstractPddlEncoder.LOG_MOVE_PREFIX);
	}
	
	/**
	 * Extract from the given planner output file line the PDDL identifier of the related move. It could be either the 
	 * name of a Petri net activity (possibly invisible), or the name of an event class.
	 * 
	 * @param outputLine A String representing the output file line.
	 * @return true if the output file line is related to a log move.
	 */
	protected String extractMovePddlId(String outputLine) {
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
	protected float computeFitness(XTrace trace, float alignmentCost, float emptyTraceCost,
			PlanningBasedAlignmentParameters parameters) {
		
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
	protected float adjustFitness(float fitness) {
		return (2 * fitness) - 1 ;
	}
	
	protected static boolean checkPlannerSources() {
		File plannerManagerScript = new File(PLANNER_MANAGER_SCRIPT);
		File fdScript = new File(FAST_DOWNWARD_DIR);
		return plannerManagerScript.exists() && fdScript.exists();
	}
	
}
