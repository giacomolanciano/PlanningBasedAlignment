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
import org.processmining.planningbasedalignment.utils.PlannerSearchStrategy;
import org.processmining.planningbasedalignment.utils.StreamGobbler;
import org.processmining.planningbasedalignment.utils.Utilities;
import org.processmining.plugins.DataConformance.DataAlignment.DataAlignmentState;
import org.processmining.plugins.DataConformance.DataAlignment.GenericTrace;
import org.processmining.plugins.DataConformance.DataAlignment.PetriNet.ResultReplayPetriNetWithData;
import org.processmining.plugins.DataConformance.framework.ExecutionStep;
import org.processmining.plugins.DataConformance.framework.ExecutionTrace;
import org.processmining.plugins.DataConformance.framework.VariableMatchCosts;

public class PlanningBasedAlignment {
	
	private static final String WINDOWS = "windows";
	private static final String PYTHON_WIN_DIR = "python27/";
	private static final String PYTHON_WIN_AMD64_DIR = "python27amd64/";
	private static final String FAST_DOWNWARD_DIR = "fast-downward/";
	private static final String PLANS_FOUND_DIR = "plans_found/";
	private static final String PDDL_FILES_DIR = "pddl_files/";
	private static final String PDDL_EXT = ".pddl";
	private static final String PDDL_DOMAIN_FILE_PREFIX = PDDL_FILES_DIR + "domain";
	private static final String PDDL_PROBLEM_FILE_PREFIX = PDDL_FILES_DIR + "problem";
	private static final String COST_ENTRY_PREFIX = "; cost = ";
	private static final String SEARCH_TIME_ENTRY_PREFIX = "; searchtime = ";
	private static final String COMMAND_ARG_PLACEHOLDER = "+";
	private static final String PLANNER_MANAGER_SCRIPT = "planner_manager.py";
	private static final String FAST_DOWNWARD_SCRIPT = "fast-downward.py";
	
	private Process plannerManagerProcess;

	private int traceIdToCheckFrom;
	private int traceIdToCheckTo;
	private int minTracesLength;
	private int maxTracesLength;

	private Pattern decimalNumberRegexPattern = Pattern.compile("\\d+(,\\d{3})*(\\.\\d+)*");
	
	private File plansFoundDir;
	private File pddlFilesDir;
	
	private AbstractPddlEncoder pddlEncoder;

	/**
	 * The method that performs the alignment of an event log and a Petri net using Automated Planning.
	 * 
	 * @param context The context where to run in.
	 * @param log The event log.
	 * @param petrinet The Petri net.
	 * @param parameters The parameters to use.
	 * @return The result of the replay of the event log on the Petri net.
	 */
	public ResultReplayPetriNetWithData apply(PluginContext context, XLog log, DataPetriNet petrinet,
			PlanningBasedAlignmentParameters parameters) {
		
		ResultReplayPetriNetWithData output = null;

		long time = -System.currentTimeMillis();
		parameters.displayMessage("[PlanningBasedAlignment] Start");
		parameters.displayMessage("[PlanningBasedAlignment] First input = " + log.toString());
		parameters.displayMessage("[PlanningBasedAlignment] Second input = " + petrinet.toString());
		parameters.displayMessage("[PlanningBasedAlignment] Parameters = " + parameters.toString());
		
		
		try {

			int[] traceInterval = parameters.getTracesInterval();
			traceIdToCheckFrom = traceInterval[0];
			traceIdToCheckTo = traceInterval[1];
			
//			System.out.println("traceInterval: "+Arrays.toString(traceInterval));
			
			// set traces length bounds
			int[] traceLengthBounds = parameters.getTracesLengthBounds();
			minTracesLength = traceLengthBounds[0];
			maxTracesLength = traceLengthBounds[1];
			
//			System.out.println("traceLengthBounds: "+Arrays.toString(traceLengthBounds));

			// cleanup folders
			plansFoundDir = new File(PLANS_FOUND_DIR);
			pddlFilesDir = new File(PDDL_FILES_DIR);
			Utilities.cleanFolder(plansFoundDir);
			Utilities.cleanFolder(pddlFilesDir);

			/* PLANNER INPUTS BUILDING */
			buildPlannerInput(log, petrinet, parameters);

			/* PLANNER INVOCATION */
			invokePlanner(parameters);
			
			/* PLANNER OUTPUTS PROCESSING */
			output = processPlannerOutput(log, petrinet, parameters);

			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		time += System.currentTimeMillis();
//		parameters.displayMessage("[PlanningBasedAlignment] Output = " + output.toString());
		parameters.displayMessage("[PlanningBasedAlignment] End (took " + time/1000.0 + "  seconds).");

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
	 * Notice that, by default, the domain and problem files are not indicate and should be defined before running 
	 * the command.
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
			if (Utilities.is64bitsOS()) {
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
		if (Utilities.is64bitsOS())
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
		
		//TODO change implementation according to parameters
		pddlEncoder = new StandardPddlEncoder(petrinet, parameters);
		
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
				Utilities.writeFile(sbDomainFileName, sbDomain);
				Utilities.writeFile(sbProblemFileName, sbProblem);

			}
		}
	}
	
	/**
	 * Starts the execution of the planner.
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
		StreamGobbler errorGobbler = new StreamGobbler(plannerManagerProcess.getErrorStream(), "ERROR");
		StreamGobbler outputGobbler = new StreamGobbler(plannerManagerProcess.getInputStream(), "OUTPUT");
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
			PlanningBasedAlignmentParameters parameters) throws IOException {
		
		ResultReplayPetriNetWithData replayResults = null;
		ArrayList<DataAlignmentState> alignments = new ArrayList<DataAlignmentState>();
		
		float cost;
		ExecutionTrace logTrace;
		ExecutionTrace processTrace;
		DataAlignmentState dataAlignmentState;
		for(final File alignmentFile : plansFoundDir.listFiles()) {

			// extract traceId
			Matcher traceIdMatcher = decimalNumberRegexPattern.matcher(alignmentFile.getName());
			traceIdMatcher.find();
			int traceId = Integer.parseInt(traceIdMatcher.group());

			cost = 0;
			logTrace = new GenericTrace(10, "Case " + traceId);
			processTrace = new GenericTrace(10, "Case " + traceId);


			// read trace alignment cost from process output file
			String traceAlignmentCost = new String();
			String traceAlignmentTime = new String();
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
					Matcher matcher = decimalNumberRegexPattern.matcher(outputLine);
					matcher.find();
					traceAlignmentTime = matcher.group();

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
			
			dataAlignmentState = new DataAlignmentState(logTrace, processTrace, cost);
			alignments.add(dataAlignmentState);
		}
		
		VariableMatchCosts variableCost = VariableMatchCosts.NOCOST;			//dummy
		Map<String, String> variableMapping = new HashMap<String, String>();	//dummy
		XEventClassifier eventClassifier = parameters.getTransitionsEventsMapping().getEventClassifier();
		replayResults = new ResultReplayPetriNetWithData(alignments, variableCost, variableMapping, petrinet, log, eventClassifier);
		return replayResults;
	}
	
	public boolean isSynchronousMove(String outputLine) {
		return outputLine.startsWith("(" + AbstractPddlEncoder.SYNCH_MOVE_PREFIX);
	}
	
	public boolean isModelMove(String outputLine) {
		return outputLine.startsWith("(" + AbstractPddlEncoder.MODEL_MOVE_PREFIX);
	}
	
	public boolean isLogMove(String outputLine) {
		return outputLine.startsWith("(" + AbstractPddlEncoder.LOG_MOVE_PREFIX);
	}
	
	public String extractMovePddlId(String outputLine) {
		String[] tokens = outputLine.split("#");
		return tokens[1].replaceAll("\\)", "").trim();
	}
	
}
