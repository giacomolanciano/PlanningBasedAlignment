package org.processmining.planningbasedalignment.plugins;

import java.util.Collection;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.planningbasedalignment.algorithms.PlanningBasedAlignment;
import org.processmining.planningbasedalignment.connections.PlanningBasedAlignmentConnection;
import org.processmining.planningbasedalignment.dialogs.YourDialog;
import org.processmining.planningbasedalignment.help.HelpMessages;
import org.processmining.planningbasedalignment.parameters.PlanningBasedAlignmentParameters;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;

@Plugin(
	name = "Planning-based Event Log & Petri Net alignment",
	parameterLabels = { "Event Log", "Petri Net", "Name of your parameters" }, 
	returnLabels = { "Petri Net Replay Result" }, returnTypes = { PNRepResult.class },
	userAccessible = true,
	help = HelpMessages.PLANNING_BASED_ALIGNMENT_HELP
)

public class PlanningBasedAlignmentPlugin extends PlanningBasedAlignment {

	/**
	 * The plug-in variant that runs in any context and requires a parameters.
	 * 
	 * @param context The context to run in.
	 * @param log The first input.
	 * @param petrinet The second input.
	 * @param parameters The parameters to use.
	 * @return The output.
	 */
	@UITopiaVariant(affiliation = "Sapienza University of Rome", author = "Giacomo Lanciano", email = "lanciano.1487019@studenti.uniroma1.it")
	@PluginVariant(variantLabel = "Your plug-in name, parameters", requiredParameterLabels = { 0, 1, 2 })
	public PNRepResult run(PluginContext context, XLog log, Petrinet petrinet, PlanningBasedAlignmentParameters parameters) {
		// Apply the algorithm depending on whether a connection already exists.
	    return runConnections(context, log, petrinet, parameters);
	}
	
	/**
	 * The plug-in variant that runs in any context and uses the default parameters.
	 * 
	 * @param context The context to run in.
	 * @param log The first input.
	 * @param petrinet The second input.
	 * @return The output.
	 */
	@UITopiaVariant(affiliation = "Sapienza University of Rome", author = "Giacomo Lanciano", email = "lanciano.1487019@studenti.uniroma1.it")
	@PluginVariant(variantLabel = "Your plug-in name, parameters", requiredParameterLabels = { 0, 1 })
	public PNRepResult runDefault(PluginContext context, XLog log, Petrinet petrinet) {
		// Get the default parameters.
	    PlanningBasedAlignmentParameters parameters = new PlanningBasedAlignmentParameters(log, petrinet);
		// Apply the algorithm depending on whether a connection already exists.
	    return runConnections(context, log, petrinet, parameters);
	}
	
	/**
	 * The plug-in variant that runs in a UI context and uses a dialog to get the parameters.
	 * 
	 * @param context The context to run in.
	 * @param log The first input.
	 * @param petrinet The second input.
	 * @return The output.
	 */
	@UITopiaVariant(affiliation = "Sapienza University of Rome", author = "Giacomo Lanciano", email = "lanciano.1487019@studenti.uniroma1.it")
	@PluginVariant(variantLabel = "Your plug-in name, dialog", requiredParameterLabels = { 0, 1 })
	public PNRepResult runUI(UIPluginContext context, XLog log, Petrinet petrinet) {
		// Get the default parameters.
	    PlanningBasedAlignmentParameters parameters = new PlanningBasedAlignmentParameters(log, petrinet);
	    // Get a dialog for this parameters.
	    YourDialog dialog = new YourDialog(context, log, petrinet, parameters);
	    // Show the dialog. User can now change the parameters.
	    InteractionResult result = context.showWizard("Your dialog title", true, true, dialog);
	    // User has close the dialog.
	    if (result == InteractionResult.FINISHED) {
			// Apply the algorithm depending on whether a connection already exists.
	    	return runConnections(context, log, petrinet, parameters);
	    }
	    // Dialog got canceled.
	    return null;
	}	
	
	/**
	 * The plug-in variant that allows one to test the dialog to get the parameters.
	 * 
	 * @param context The context to run in.
	 * @return The output.
	 */
	@UITopiaVariant(affiliation = "Sapienza University of Rome", author = "Giacomo Lanciano", email = "lanciano.1487019@studenti.uniroma1.it")
	@PluginVariant(variantLabel = "Your plug-in name, dialog", requiredParameterLabels = { })
	public PNRepResult testUI(UIPluginContext context) {
		// Create default inputs.
		XLog input1 = null;
		Petrinet input2 = null;
		// Get the default parameters.
	    PlanningBasedAlignmentParameters parameters = new PlanningBasedAlignmentParameters(input1, input2);
	    // Get a dialog for this parameters.
	    YourDialog dialog = new YourDialog(context, input1, input2, parameters);
	    // Show the dialog. User can now change the parameters.
	    InteractionResult result = context.showWizard("Your dialog title", true, true, dialog);
	    // User has close the dialog.
	    if (result == InteractionResult.FINISHED) {
			// Apply the algorithm depending on whether a connection already exists.
	    	return runConnections(context, input1, input2, parameters);
	    }
	    // Dialog got canceled.
	    return null;
	}	
	
	/**
	 * Apply the algorithm depending on whether a connection already exists.
	 * 
	 * @param context The context to run in.
	 * @param log The event log.
	 * @param petrinet The Petri net.
	 * @param parameters The parameters to use.
	 * @return The result of the replay of the event log on the Petri net.
	 */
	private PNRepResult runConnections(PluginContext context, XLog log, Petrinet petrinet, PlanningBasedAlignmentParameters parameters) {
		if (parameters.isTryConnections()) {
			// Try to found a connection that matches the inputs and the parameters.
			Collection<PlanningBasedAlignmentConnection> connections;
			try {
				connections = context.getConnectionManager().getConnections(
						PlanningBasedAlignmentConnection.class, context, log, petrinet);
				for (PlanningBasedAlignmentConnection connection : connections) {
					if (connection.getObjectWithRole(PlanningBasedAlignmentConnection.LOG)
							.equals(log) && connection.getObjectWithRole(PlanningBasedAlignmentConnection.PETRINET)
							.equals(petrinet) && connection.getParameters().equals(parameters)) {
						// Found a match. Return the associated output as result of the algorithm.
						return connection.getObjectWithRole(PlanningBasedAlignmentConnection.PN_REPLAY_RESULT);
					}
				}
			} catch (ConnectionCannotBeObtained e) {
				// do nothing
			}
		}
		// No connection found. Apply the algorithm to compute a fresh output result.
		PNRepResult output = apply(context, log, petrinet, parameters);
		if (parameters.isTryConnections()) {
			// Store a connection containing the inputs, output, and parameters.
			context.getConnectionManager().addConnection(
					new PlanningBasedAlignmentConnection(log, petrinet, output, parameters));
		}
		// Return the output.
		return output;
	}

}
