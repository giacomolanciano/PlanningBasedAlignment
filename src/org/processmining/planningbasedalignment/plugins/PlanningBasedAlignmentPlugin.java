package org.processmining.planningbasedalignment.plugins;

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
import org.processmining.datapetrinets.DataPetriNet;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginCategory;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.util.ui.widgets.helper.UserCancelledException;
import org.processmining.planningbasedalignment.algorithms.PlanningBasedAlignment;
import org.processmining.planningbasedalignment.connections.PlanningBasedAlignmentConnection;
import org.processmining.planningbasedalignment.help.HelpMessages;
import org.processmining.planningbasedalignment.parameters.PlanningBasedAlignmentParameters;
import org.processmining.planningbasedalignment.ui.ConfigurationUI;
import org.processmining.planningbasedalignment.utils.ResourcesUnpacker;
import org.processmining.plugins.DataConformance.DataAlignment.PetriNet.ResultReplayPetriNetWithData;

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
	returnTypes = { ResultReplayPetriNetWithData.class },
	userAccessible = true,
	categories = PluginCategory.ConformanceChecking,
	keywords = {"conformance", "alignment", "planning", "PDDL"},
	help = HelpMessages.PLANNING_BASED_ALIGNMENT_HELP
)
public class PlanningBasedAlignmentPlugin extends PlanningBasedAlignment {
	
	private static final String AFFILIATION = "Sapienza University of Rome";
	private static final String AUTHOR = "Giacomo Lanciano";
	private static final String EMAIL = "lanciano.1487019@studenti.uniroma1.it";
	private static final int PYTHON_2_MIN_VERSION = 7;
	
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
	@UITopiaVariant(affiliation = AFFILIATION, author = AUTHOR, email = EMAIL)
	@PluginVariant(variantLabel = "Planning-based Alignment of Event Logs and Petri Nets",
	requiredParameterLabels = { 0, 1 })
	public ResultReplayPetriNetWithData runUI(UIPluginContext context, XLog log, DataPetriNet petrinet) {
		
		if (!isPython27Installed()) {			
			JOptionPane.showMessageDialog(
					new JPanel(),
					"The plug-in is not able to find and call Python 2.7+ on your machine. Please, install it "
					+ "and make sure it is visible in the PATH.\n"
					+ "If you are using another version of Python, you just need to create a virtualenv with the "
					+ "latest version of Python 2 installed\n"
					+ "and start ProM from there.",
					"Python not found", JOptionPane.ERROR_MESSAGE);
			abortExecution(context);
			return null;
		}
		
		if (!checkPlannerSources()) {			
			resourcesUnpacker = new ResourcesUnpacker(context);
			resourcesUnpacker.start();
		}
		
	    ConfigurationUI configurationUI = new ConfigurationUI();
		PlanningBasedAlignmentParameters parameters = configurationUI.getPlanningBasedAlignmentParameters(
				context, log, petrinet);
		
		if (parameters == null) {
			abortExecution(context);
			return null;
		}
		
		// start algorithm
		ResultReplayPetriNetWithData result = runAlgorithm(context, log, petrinet, parameters);

		context.getFutureResult(0).setLabel(
				"Replay result - log " + XConceptExtension.instance().extractName(log) + " on " + petrinet.getLabel()
				+ " using Automated Planning");
		return result;

	}

	/**
	 * Invokes the algorithm to compute replay result.
	 * 
	 * @param context The context to run in.
	 * @param algorithm 
	 * @param log The event log to replay.
	 * @param petrinet The Petri net on which the log has to be replayed.
	 * @param parameters The parameters to be used by the alignment algorithm.
	 * @return The result of the replay of the event log on the Petri net.
	 */
	private ResultReplayPetriNetWithData runAlgorithm(
			UIPluginContext context, XLog log, DataPetriNet petrinet, PlanningBasedAlignmentParameters parameters) {
			
		ResultReplayPetriNetWithData replayRes = align(context, log, petrinet, parameters);
		
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
	 * Check whether Python 2.7+ is installed and callable from command line.
	 * 
	 * @return true if Python 2.7+ is installed and callable from command line.
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	private boolean isPython27Installed() {
		
		Pattern pythonVersionRegexPattern = Pattern.compile("Python 2\\.\\d+");
		
		String[] commandArgs = new String[]{"python", "-V"};		
		ProcessBuilder processBuilder = new ProcessBuilder(commandArgs);
		Process pythonVersionCheckerProcess = null; 
		
		try {
			pythonVersionCheckerProcess = processBuilder.start();
			
			// python version number is outputed on std error
			InputStream output = pythonVersionCheckerProcess.getErrorStream();
			InputStreamReader inputStreamReader = new InputStreamReader(output);
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {	
				Matcher pythonVersionMatcher = pythonVersionRegexPattern.matcher(line);
				if (pythonVersionMatcher.find()) {
					String pythonVersion = pythonVersionMatcher.group();
										
					String[] pythonVersionTokens = pythonVersion.split("\\.");
					int minorVersion = Integer.parseInt(pythonVersionTokens[1]);
					if (minorVersion >= PYTHON_2_MIN_VERSION)
						return true;
				}
			}
			
			// wait for the process to return
			pythonVersionCheckerProcess.waitFor();
			
		} catch (IOException | InterruptedException e) {
			System.out.println(e);
		}
		
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
