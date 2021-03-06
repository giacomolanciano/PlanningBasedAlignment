package org.processmining.planningbasedalignment.plugins.planningbasedalignment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginCategory;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.util.ui.widgets.helper.UserCancelledException;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.planningbasedalignment.plugins.planningbasedalignment.algorithms.PlanningBasedAlignment;
import org.processmining.planningbasedalignment.plugins.planningbasedalignment.connections.PlanningBasedAlignmentConnection;
import org.processmining.planningbasedalignment.plugins.planningbasedalignment.models.PlanningBasedReplayResult;
import org.processmining.planningbasedalignment.plugins.planningbasedalignment.parameters.PlanningBasedAlignmentParameters;
import org.processmining.planningbasedalignment.plugins.planningbasedalignment.ui.PlanningBasedAlignmentConfiguration;
import org.processmining.planningbasedalignment.utils.HelpMessages;
import org.processmining.planningbasedalignment.utils.ResourcesUnpacker;

/**
 * The ProM plug-in for Planning-based Alignment of an event log and a Petri net.
 * 
 * @author Giacomo Lanciano
 *
 */
@Plugin(
	name = "Planning-based Alignment of Event Logs and Petri Nets",
	parameterLabels = { "Event Log", "Petri Net", "Name of your parameters" }, 
	returnLabels = { "Petri Net Replay Result" },
	returnTypes = { PlanningBasedReplayResult.class },
	userAccessible = true,
	categories = PluginCategory.ConformanceChecking,
	keywords = {"conformance", "alignment", "planning", "PDDL"},
	help = HelpMessages.PLANNING_BASED_ALIGNMENT_HELP
)
public class PlanningBasedAlignmentPlugin extends PlanningBasedAlignment {

	private static final int PYTHON_2 = 2;
	private static final int PYTHON_3 = 3;
	private static final int PYTHON_2_MIN_VERSION = 7;
	private static final int PYTHON_3_MIN_VERSION = 2;
	
	/**
	 * A flag that tells whether another run of the plug-in is in progress or not, to avoid conflicts due to the fact
	 * that the underlying planner is not designed to handle concurrent execution.
	 */
	private static boolean plannerLock = false;

	/**
	 * The plug-in variant that runs in a UI context and prompt the user to get the parameters.
	 * 
	 * @param context The context to run in.
	 * @param log The event log to replay.
	 * @param petrinet The Petri net on which the log has to be replayed.
	 * @return The result of the replay of the event log on the Petri net.
	 * @throws ConnectionCannotBeObtained
	 * @throws UserCancelledException 
	 */
	@UITopiaVariant(
		affiliation = HelpMessages.AFFILIATION, author = HelpMessages.AUTHOR, email = HelpMessages.EMAIL,
		pack = HelpMessages.PLANNING_BASED_ALIGNMENT_PACKAGE)
	@PluginVariant(requiredParameterLabels = { 0, 1 })
	public PlanningBasedReplayResult runUI(UIPluginContext context, XLog log, Petrinet petrinet) {

		if (plannerLock) {
			JOptionPane.showMessageDialog(
					new JPanel(),
					"It is not allowed to run many instances of the plug-in in parallel, since the underlying planner\n"
					+ "is not designed to handle concurrent executions. Wait for the other run to terminate and start\n"
					+ "the execution again.",
					"Concurrent Execution Not Allowed", JOptionPane.ERROR_MESSAGE);
			abortExecution(context);
			return null;
		}
		
		if (!isPythonInstalled()) {			
			JOptionPane.showMessageDialog(
					new JPanel(),
					"The plug-in is not able to find and call Python 2.7+ or 3.2+ on your machine. Please, install it "
					+ "and make sure it is visible in the PATH.\n"
					+ "If you are using another version of Python, you just need to create a virtualenv with the "
					+ "latest version of Python (2 or 3) installed\n"
					+ "and start ProM from there.\n"
					+ "To check which version of Python you are running, type \"python -V\" on a command line (the"
					+ "expected output is \"Python X.X.X ...\").",
					"Python not found", JOptionPane.ERROR_MESSAGE);
			abortExecution(context);
			return null;
		}
		
		// acquire lock
		plannerLock = true;

		if (!checkPlannerSources()) {			
			resourcesUnpacker = new ResourcesUnpacker(context);
			resourcesUnpacker.start();
		}

		PlanningBasedAlignmentConfiguration configurationUI = new PlanningBasedAlignmentConfiguration();
		PlanningBasedAlignmentParameters parameters = configurationUI.getParameters(
				context, log, petrinet);

		if (parameters == null) {
			abortExecution(context);
			
			// release lock
			plannerLock = false;
			
			return null;
		}

		// start algorithm
		PlanningBasedReplayResult result = runAlgorithm(context, log, petrinet, parameters);

		String resultLabel = "Replay result - log " + XConceptExtension.instance().extractName(log) 
				+ " on " + petrinet.getLabel() + " using Automated Planning";

		if (parameters.isPartiallyOrderedEvents())
			resultLabel += " (Partial Order Assumption)";

		context.getFutureResult(0).setLabel(resultLabel);
		
		// release lock
		plannerLock = false;
		
		return result;

	}

	/**
	 * Invokes the algorithm to compute replay result.
	 * 
	 * @param context The context to run in.
	 * @param log The event log to replay.
	 * @param petrinet The Petri net on which the log has to be replayed.
	 * @param parameters The parameters to be used by the alignment algorithm.
	 * @return The result of the replay of the event log on the Petri net.
	 */
	private PlanningBasedReplayResult runAlgorithm(
			UIPluginContext context, XLog log, Petrinet petrinet, PlanningBasedAlignmentParameters parameters) {

		PlanningBasedReplayResult replayRes = align(context, log, petrinet, parameters);

		// add connection if result is found
		if (replayRes != null) {
			context.getConnectionManager().addConnection(
					new PlanningBasedAlignmentConnection(log, petrinet, parameters, replayRes));
		}

		return replayRes;	
	}

	/**
	 * Abort the execution of the plug-in and its subprocesses.
	 * 
	 * @param context The context to run in.
	 */
	private void abortExecution(UIPluginContext context) {
		context.getFutureResult(0).cancel(true);
		killSubprocesses();
	}

	/**
	 * Check whether Python 2.7+ or 3.2+ is installed and callable from command line.
	 * 
	 * @return true if Python 2.7+ or 3.2+ is installed and callable from command line.
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	private boolean isPythonInstalled() {

		String python = "Python ";
		Pattern pythonVersionRegexPattern = Pattern.compile("\\d+\\.\\d+");
		String[] commandArgs = new String[]{"python", "-V"};
		ProcessBuilder processBuilder = new ProcessBuilder(commandArgs);
		Process pythonVersionCheckerProcess = null; 

		try {
			pythonVersionCheckerProcess = processBuilder.start();

			// Python version number is outputed on std error
			InputStream output = pythonVersionCheckerProcess.getErrorStream();
			InputStreamReader inputStreamReader = new InputStreamReader(output);
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
			String line = null;
			Matcher pythonVersionMatcher = null;
			while ((line = bufferedReader.readLine()) != null) {
								
				// check only the line with Python version
				if (line.startsWith(python)) {	
					pythonVersionMatcher = pythonVersionRegexPattern.matcher(line);
					
					if (pythonVersionMatcher.find()) {
						String pythonVersion = pythonVersionMatcher.group();
						
						String[] pythonVersionTokens = pythonVersion.split("\\.");
						int majorVersion = Integer.parseInt(pythonVersionTokens[0]);
						int minorVersion = Integer.parseInt(pythonVersionTokens[1]);

						if ((majorVersion == PYTHON_2 && minorVersion >= PYTHON_2_MIN_VERSION)
								|| (majorVersion == PYTHON_3 && minorVersion >= PYTHON_3_MIN_VERSION)) {
							System.out.println("Python found.");
							return true;	
						}
					}
					
					// no need to check other lines
					break;
				}
			}

			// wait for the process to return
			pythonVersionCheckerProcess.waitFor();

		} catch (IOException | InterruptedException e) {
			System.out.println(e);
		}

		System.out.println("Python not found.");
		return false;
	}

	/**
	 * Check whether the planner source code has already been unpacked.
	 * 
	 * @return true if the planner source code has already been unpacked.
	 */
	private static boolean checkPlannerSources() {
		File plannerManagerScript = new File(PlanningBasedAlignment.PLANNER_MANAGER_SCRIPT);
		File fdScript = new File(PlanningBasedAlignment.FAST_DOWNWARD_DIR);
		return plannerManagerScript.exists() && fdScript.exists();
	}

}
