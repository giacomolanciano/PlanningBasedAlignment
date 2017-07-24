package org.processmining.planningbasedalignment.plugins;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.datapetrinets.DataPetriNet;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginCategory;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.planningbasedalignment.algorithms.PlanningBasedAlignment;
import org.processmining.planningbasedalignment.connections.PlanningBasedAlignmentConnection;
import org.processmining.planningbasedalignment.dialogs.ConfigurationUI;
import org.processmining.planningbasedalignment.help.HelpMessages;
import org.processmining.planningbasedalignment.parameters.PlanningBasedAlignmentParameters;
import org.processmining.plugins.DataConformance.DataAlignment.PetriNet.ResultReplayPetriNetWithData;

/**
 * The ProM plug-in for Planning-based Alignment of an event log and a Petri net.
 * 
 * @author Giacomo Lanciano
 *
 */
@Plugin(
	name = "Planning-based Event Log & Petri Net alignment",
	parameterLabels = { "Event Log", "Petri Net", "Name of your parameters" }, 
	returnLabels = { "Petri Net Replay Result" },
	returnTypes = { ResultReplayPetriNetWithData.class },
	userAccessible = true,
	categories = PluginCategory.ConformanceChecking,
	keywords = {"conformance", "alignment", "planning"},
	help = HelpMessages.PLANNING_BASED_ALIGNMENT_HELP
)
public class PlanningBasedAlignmentPlugin extends PlanningBasedAlignment {
	
	private static final String AFFILIATION = "Sapienza University of Rome";
	private static final String AUTHOR = "Giacomo Lanciano";
	private static final String EMAIL = "lanciano.1487019@studenti.uniroma1.it";
	
	/**
	 * The plug-in variant that runs in a UI context and uses a dialog to get the parameters.
	 * 
	 * @param context The context to run in.
	 * @param log The event log to replay.
	 * @param petrinet The Petri net on which the log has to be replayed.
	 * @return The result of the replay of the event log on the Petri net.
	 * @throws ConnectionCannotBeObtained
	 */
	@UITopiaVariant(affiliation = AFFILIATION, author = AUTHOR, email = EMAIL)
	@PluginVariant(variantLabel = "Planning-based Event Log & Petri Net alignment", requiredParameterLabels = { 0, 1 })
	public ResultReplayPetriNetWithData runUI(UIPluginContext context, XLog log, DataPetriNet petrinet)
			throws ConnectionCannotBeObtained {
		
		// convert input to DataPetriNet object for visualization purposes
//		DataPetriNet petrinet = DataPetriNet.Factory.fromPetrinet(petrinetTemp);
		
	    ConfigurationUI configurationUI = new ConfigurationUI();
		PlanningBasedAlignmentParameters parameters = configurationUI.getPlanningBasedAlignmentParameters(
				context, log, petrinet);
		
		if (parameters == null) {
			context.getFutureResult(0).cancel(true);
			return null;
		}
		
		context.log("replay is performed. All parameters are set.");

		// start algorithm
		ResultReplayPetriNetWithData res = replayLog(context, log, petrinet, parameters);

		context.getFutureResult(0).setLabel(
				"Replay result - log " + XConceptExtension.instance().extractName(log) + " on " + petrinet.getLabel()
				+ " using Automated Planning");
		return res;

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
	private ResultReplayPetriNetWithData replayLog(UIPluginContext context, XLog log, DataPetriNet petrinet,
			PlanningBasedAlignmentParameters parameters) {
			
		ResultReplayPetriNetWithData replayRes =  apply(context, log, petrinet, parameters);
		
		// add connection if result is found
		if (replayRes != null) {			
			context.getConnectionManager().addConnection(
					new PlanningBasedAlignmentConnection(log, petrinet, parameters, replayRes));
		}
		
		return replayRes;	
	}
	
}
