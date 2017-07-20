package org.processmining.planningbasedalignment.algorithms;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.datapetrinets.DataPetriNet;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.planningbasedalignment.parameters.PlanningBasedAlignmentParameters;
import org.processmining.planningbasedalignment.pddl.AbstractPddlEncoder;
import org.processmining.planningbasedalignment.pddl.StandardPddlEncoder;
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
	
	protected static final String WINDOWS = "windows";
	protected static final String PYTHON_WIN_DIR = "python27/";
	protected static final String PYTHON_WIN_AMD64_DIR = "python27amd64/";
	protected static final String FAST_DOWNWARD_DIR = "fast-downward/";
	protected static final String PLANS_FOUND_DIR = "plans_found/";
	protected static final String PDDL_FILES_DIR = "pddl_files/";
	protected static final String PDDL_EXT = ".pddl";
	protected static final String PDDL_DOMAIN_FILE_PREFIX = PDDL_FILES_DIR + "domain";
	protected static final String PDDL_PROBLEM_FILE_PREFIX = PDDL_FILES_DIR + "problem";
	protected static final String COST_ENTRY_PREFIX = "; cost = ";
	protected static final String SEARCH_TIME_ENTRY_PREFIX = "; searchtime = ";
	protected static final String COMMAND_ARG_PLACEHOLDER = "+";
	protected static final String PLANNER_MANAGER_SCRIPT = "planner_manager.py";
	protected static final String FAST_DOWNWARD_SCRIPT = "fast-downward.py";
	protected static final String CASE_PREFIX = "Case ";
	protected static final int INITIAL_EXECUTION_TRACE_CAPACITY = 10;
	
	/**
	 * The object used to generate the PDDL encodings.
	 */
	protected AbstractPddlEncoder pddlEncoder;
	
	/**
	 * The separated process in which the planner is executed.
	 */
	protected Process plannerManagerProcess;

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
			invokePlanner(parameters);
			
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
		plannerManagerProcess.destroy();
	}
	
	/**
	 * Build the arguments list needed to launch Fast-Downward planner, tuned according to user selections. 
	 * Notice that the output, the domain and problem files are defined by the planner manager module once that PDDL
	 * encodings are generated.
	 * 
	 * @return an array of Strings containing the arguments.
	 * @throws IOException 
	 */
	protected String[] buildFastDownardCommandArgs(PlanningBasedAlignmentParameters parameters) throws IOException {
		ArrayList<String> commandComponents = new ArrayList<>();

		// determine which python interpreter must be used
		String pythonInterpreter = "python";
		
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains(WINDOWS)) {
			if (OSUtils.is64bitsOS()) {
				pythonInterpreter = PYTHON_WIN_AMD64_DIR + pythonInterpreter;
			} else {
				pythonInterpreter = PYTHON_WIN_DIR + pythonInterpreter;
			}
		}

		/* begin of command args for planner manager */

		commandComponents.add(pythonInterpreter);

		File plannerManagerScript = new File(PLANNER_MANAGER_SCRIPT);
		commandComponents.add(plannerManagerScript.getCanonicalPath());


		/* begin of command args for Fast-Downward */

		commandComponents.add(pythonInterpreter);

		File fdScript = new File(FAST_DOWNWARD_DIR + FAST_DOWNWARD_SCRIPT);
		commandComponents.add(fdScript.getCanonicalPath());

		// Fast-Downward is assumed to be built in advance both for 32 and 64 bits OS (being them Windows or Unix-like).
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

		String[] commandArguments = commandComponents.toArray(new String[0]);
		return commandArguments;
	}
	
	/**
	 * Produces the PDDL files (representing the instances of the alignment problem) to be fed to the planner.
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

		// consider only the traces in the chosen interval
		for(int traceId = traceIdToCheckFrom-1; traceId < traceIdToCheckTo; traceId++) {

			XTrace trace = log.get(traceId);
			int traceLength = trace.size();						

			// check whether the trace matches the length bounds
			if(traceLength >= minTracesLength && traceLength <= maxTracesLength)  {

				// create PDDL encodings (domain & problem) for current trace
				StringBuffer sbDomain = pddlEncoder.createPropositionalDomain(trace);
				StringBuffer sbProblem = pddlEncoder.createPropositionalProblem(trace);
				String sbDomainFileName = PDDL_DOMAIN_FILE_PREFIX + (traceId+1) + PDDL_EXT;
				String sbProblemFileName = PDDL_PROBLEM_FILE_PREFIX + (traceId+1) + PDDL_EXT;
				OSUtils.writeFile(sbDomainFileName, sbDomain);
				OSUtils.writeFile(sbProblemFileName, sbProblem);

			}
		}
	}
	
	/**
	 * Starts the execution of the planner for all the produced pairs domain/problem.
	 * 
	 * @param parameters
	 * @throws InterruptedException
	 * @throws IOException
	 */
	protected void invokePlanner(PlanningBasedAlignmentParameters parameters) throws InterruptedException, IOException {
		String[] commandArgs = buildFastDownardCommandArgs(parameters);

		// execute external planner script and wait for results
		ProcessBuilder processBuilder = new ProcessBuilder(commandArgs);
		plannerManagerProcess = processBuilder.start();

		//System.out.println(Arrays.toString(commandArgs));

		// read std out & err in separated thread
		StreamAsyncReader errorGobbler = new StreamAsyncReader(plannerManagerProcess.getErrorStream(), "ERROR");
		StreamAsyncReader outputGobbler = new StreamAsyncReader(plannerManagerProcess.getInputStream(), "OUTPUT");
		errorGobbler.start();
		outputGobbler.start();

		// wait for the process to return to read the generated outputs
		plannerManagerProcess.waitFor();
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
		
		float cost;
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

			cost = 0;
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
					cost = Float.parseFloat(traceAlignmentCost);

				} else if(outputLine.startsWith(SEARCH_TIME_ENTRY_PREFIX)) {
					// parse alignment time						
//					Matcher matcher = decimalNumberRegexPattern.matcher(outputLine);
//					matcher.find();
//					traceAlignmentTime = matcher.group();

				} else {						
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
			
			// add trace alignment to collection
			dataAlignmentState = new DataAlignmentState(logTrace, processTrace, cost);
			dataAlignmentState.setControlFlowFitness(0.3F);
			alignments.add(dataAlignmentState);
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
	
}
