package org.processmining.planningbasedalignment.algorithms;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.planningbasedalignment.parameters.PlanningBasedAlignmentParameters;
import org.processmining.planningbasedalignment.pddl.AbstractPddlEncoder;
import org.processmining.planningbasedalignment.pddl.StandardPddlEncoder;
import org.processmining.planningbasedalignment.utils.PlannerSearchStrategy;
import org.processmining.planningbasedalignment.utils.StreamGobbler;
import org.processmining.planningbasedalignment.utils.Utilities;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;

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
	private static final String PLAN_FILE_PREFIX = PLANS_FOUND_DIR + "alignment_";
	private static final String COST_ENTRY_PREFIX = "; cost = ";
	private static final String SEARCH_TIME_ENTRY_PREFIX = "; searchtime = ";
	private static final String COMMAND_ARG_PLACEHOLDER = "+";
	private static final String PLANNER_MANAGER_SCRIPT = "planner_manager.py";
	private static final String FAST_DOWNWARD_SCRIPT = "fast-downward.py";
	
	private Process plannerManagerProcess;
	private Vector<XTrace> tracesWithFailureVector = new Vector<XTrace>();

	private int traceIdToCheckFrom;
	private int traceIdToCheckTo;
	private int minTracesLength;
	private int maxTracesLength;
	private int alignedTracesAmount = 0;
	private float totalAlignmentCost = 0;
	private float totalAlignmentTime = 0;

	private Pattern decimalNumberRegexPattern = Pattern.compile("\\d+(,\\d{3})*(\\.\\d+)*");
	
	private File plansFoundDir;

	/**
	 * The method that performs the alignment of an event log and a Petri net using Automated Planning.
	 * 
	 * @param context The context where to run in.
	 * @param log The event log.
	 * @param petrinet The Petri net.
	 * @param parameters The parameters to use.
	 * @return The result of the replay of the event log on the Petri net.
	 */
	public PNRepResult apply(PluginContext context, XLog log, Petrinet petrinet, PlanningBasedAlignmentParameters parameters) {
		
		PNRepResult output = null;

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
			File plansFoundDir = new File(PLANS_FOUND_DIR);
			File pddlFilesDir = new File(PDDL_FILES_DIR);
			Utilities.cleanFolder(plansFoundDir);
			Utilities.cleanFolder(pddlFilesDir);

			/* PLANNER INPUTS BUILDING */
			buildPlannerInput(log, petrinet, parameters);

			/* PLANNER INVOCATION */
			invokePlanner(parameters);
			
			/* PLANNER OUTPUTS PROCESSING */
//			processPlannerOutput(log);

			
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
	protected void buildPlannerInput(XLog log, Petrinet petrinet, PlanningBasedAlignmentParameters parameters) {
		
		//TODO change implementation according to parameters
		AbstractPddlEncoder pddlEncoder = new StandardPddlEncoder(petrinet, parameters);
		
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
	protected void processPlannerOutput(XLog log) throws IOException {
		
		// TODO Auto-generated method stub
		
		for(final File alignmentFile : plansFoundDir.listFiles()) {

			// extract traceId
			Matcher traceIdMatcher = decimalNumberRegexPattern.matcher(alignmentFile.getName());
			traceIdMatcher.find();
			int traceId = Integer.parseInt(traceIdMatcher.group());

			XTrace trace = log.get(traceId - 1);

			// check execution results
			BufferedReader processOutputReader = new BufferedReader(new FileReader(alignmentFile));
			String outputLine = processOutputReader.readLine(); 
			if (outputLine == null) {
				tracesWithFailureVector.addElement(trace);
				
			} else {		

				// read trace alignment cost from process output file
				String traceAlignmentCost = new String();
				String traceAlignmentTime = new String();
				while (outputLine != null) {

					// parse alignment cost
					if(outputLine.startsWith(COST_ENTRY_PREFIX)) {

						Matcher matcher = decimalNumberRegexPattern.matcher(outputLine);
						matcher.find();
						traceAlignmentCost = matcher.group();

						if(Integer.parseInt(traceAlignmentCost) > 0)
							alignedTracesAmount++;
					}

					// parse alignment time
					if(outputLine.startsWith(SEARCH_TIME_ENTRY_PREFIX)) {

						Matcher matcher = decimalNumberRegexPattern.matcher(outputLine);
						matcher.find();
						traceAlignmentTime = matcher.group();
					}

					outputLine = processOutputReader.readLine();
				}									

				// update total counters
				totalAlignmentCost += Float.parseFloat(traceAlignmentCost);
				totalAlignmentTime += Float.parseFloat(traceAlignmentTime);
			}
			processOutputReader.close();

		}

	}
	

}
