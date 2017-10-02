package org.processmining.planningbasedalignment.plugins.planningbasedalignment;

import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginCategory;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.planningbasedalignment.plugins.planningbasedalignment.algorithms.AlignmentPddlEncoding;
import org.processmining.planningbasedalignment.plugins.planningbasedalignment.parameters.PlanningBasedAlignmentParameters;
import org.processmining.planningbasedalignment.plugins.planningbasedalignment.ui.PlanningBasedAlignmentConfiguration;
import org.processmining.planningbasedalignment.utils.DirectoryChooser;
import org.processmining.planningbasedalignment.utils.HelpMessages;

/**
 * The ProM plug-in for generating PDDL encodings for Planning-based Alignment.
 * 
 * @author Giacomo Lanciano
 *
 */
@Plugin(
	name = "Generate PDDL Encoding for Planning-based Alignment",
	parameterLabels = { "Event Log", "Petri Net" }, 
	returnLabels = { "PDDL Files" },
	returnTypes = { Void.class },
	userAccessible = true,
	categories = PluginCategory.ConformanceChecking,
	keywords = {"conformance", "alignment", "planning", "PDDL"},
	help = HelpMessages.ALIGNMENT_PDDL_ENCODING_HELP
)
public class AlignmentPddlEncodingPlugin extends AlignmentPddlEncoding {

	/**
	 *  The plug-in variant that runs in a UI context and prompt the user to get the parameters.
	 *  
	 * @param context The context to run in.
	 * @param log The event log to replay.
	 * @param petrinet The Petri net on which the log has to be replayed.
	 */
	@UITopiaVariant(
		affiliation = HelpMessages.AFFILIATION, author = HelpMessages.AUTHOR, email = HelpMessages.EMAIL,
		pack = HelpMessages.PLANNING_BASED_ALIGNMENT_PACKAGE)
	@PluginVariant(requiredParameterLabels = { 0, 1 })
	public void runUI(UIPluginContext context, XLog log, Petrinet petrinet) {
		
		PlanningBasedAlignmentConfiguration configurationUI = new PlanningBasedAlignmentConfiguration();
		PlanningBasedAlignmentParameters parameters = configurationUI.getParameters(
				context, log, petrinet);
		
		if (parameters == null) {
			abortExecution(context);
			return;
		}
		
		// prompt user for destination directory
		DirectoryChooser chooser = new DirectoryChooser();
		File destinationDir = chooser.chooseDirectory();
		
		if (destinationDir == null) {
			abortExecution(context);
			return;
		}
		
		if (!destinationDir.isDirectory()) {
			JOptionPane.showMessageDialog(
					new JPanel(), "The chosen directory is not valid.", "Error", JOptionPane.ERROR_MESSAGE);
			abortExecution(context);
			return;
		}
		
		try {
			buildPlannerInput(destinationDir, context, log, petrinet, parameters);
			
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(
					new JPanel(), "The program failed unexpectedly while writing the PDDL files on disk.",
					"Error", JOptionPane.ERROR_MESSAGE);
			abortExecution(context);
			return;
		}		
		
		context.log("The PDDL files have been successfully written on disk.");
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

}
