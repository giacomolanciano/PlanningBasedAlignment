package org.processmining.planningbasedalignment.plugins;

import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.datapetrinets.DataPetriNet;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginCategory;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.planningbasedalignment.algorithms.AlignmentPddlEncoding;
import org.processmining.planningbasedalignment.dialogs.ConfigurationUI;
import org.processmining.planningbasedalignment.dialogs.DestinationDirectoryChooser;
import org.processmining.planningbasedalignment.parameters.PlanningBasedAlignmentParameters;

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
	keywords = {"conformance", "alignment", "planning", "PDDL"}
)
public class AlignmentPddlEncodingPlugin extends AlignmentPddlEncoding {

	private static final String AFFILIATION = "Sapienza University of Rome";
	private static final String AUTHOR = "Giacomo Lanciano";
	private static final String EMAIL = "lanciano.1487019@studenti.uniroma1.it";
	
	/**
	 *  The plug-in variant that runs in a UI context and prompt the user to get the parameters.
	 *  
	 * @param context The context to run in.
	 * @param log The event log to replay.
	 * @param petrinet The Petri net on which the log has to be replayed.
	 */
	@UITopiaVariant(affiliation = AFFILIATION, author = AUTHOR, email = EMAIL)
	@PluginVariant(variantLabel = "Generate PDDL Encoding for Planning-based Alignment",
	requiredParameterLabels = { 0, 1 })
	public void runUI(UIPluginContext context, XLog log, DataPetriNet petrinet) {
		
		ConfigurationUI configurationUI = new ConfigurationUI();
		PlanningBasedAlignmentParameters parameters = configurationUI.getPlanningBasedAlignmentParameters(
				context, log, petrinet);
		
		if (parameters == null) {
			abortExecution(context);
			return;
		}
		
		// prompt user for destination directory
		DestinationDirectoryChooser chooser = new DestinationDirectoryChooser();
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
					new JPanel(), "The program failed unexpectedly while writing the files on disk.",
					"Error", JOptionPane.ERROR_MESSAGE);
			abortExecution(context);
			return;
		}		
		
		JOptionPane.showMessageDialog(
			new JPanel(), 
			"The PDDL files have been successfully written on disk. Notice that each file refers to the event\n"
			+ "log trace whose id is indicated in the file name. Besides, \"domain0.pddl\" and \"problem0.pddl\"\n"
			+ "refer to the alignment of the empty trace, whose cost is needed to compute fitness.",
			"Success", JOptionPane.INFORMATION_MESSAGE);
	}
	
	/**
	 * Abort the execution of the plug-in and its subprocesses.
	 * 
	 * @param context The context to run in.
	 */
	private void abortExecution(UIPluginContext context) {
		context.getFutureResult(0).cancel(true);
	}

}
