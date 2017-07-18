package org.processmining.planningbasedalignment.plugins;

import java.io.IOException;
import java.text.NumberFormat;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.connections.petrinets.EvClassLogPetrinetConnection;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.planningbasedalignment.algorithms.PlanningBasedAlignment;
import org.processmining.planningbasedalignment.connections.PlanningBasedAlignmentConnection;
import org.processmining.planningbasedalignment.dialogs.ConfigurationUI;
import org.processmining.planningbasedalignment.help.HelpMessages;
import org.processmining.planningbasedalignment.parameters.PlanningBasedAlignmentParameters;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;

@Plugin(
	name = "Planning-based Event Log & Petri Net alignment",
	parameterLabels = { "Event Log", "Petri Net", "Name of your parameters" }, 
	returnLabels = { "Petri Net Replay Result" },
	returnTypes = { PNRepResult.class },
	userAccessible = true,
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
	 * @param log The first input.
	 * @param petrinet The second input.
	 * @return The result of the replay of the event log on the Petri net.
	 * @throws IOException 
	 */
	@UITopiaVariant(affiliation = AFFILIATION, author = AUTHOR, email = EMAIL)
	@PluginVariant(variantLabel = "Your plug-in name, dialog", requiredParameterLabels = { 0, 1 })
	public PNRepResult runUI(UIPluginContext context, XLog log, Petrinet petrinet)
			throws ConnectionCannotBeObtained {
		
	    ConfigurationUI configurationUI = new ConfigurationUI();
		PlanningBasedAlignmentParameters parameters = configurationUI.getPlanningBasedAlignmentParameters(context, petrinet, log);
		if (parameters == null) {
			context.getFutureResult(0).cancel(true);
			return null;
		}
		
		context.log("replay is performed. All parameters are set.");

		// This connection MUST exists, as it is constructed by the configuration if necessary
		context.getConnectionManager().getFirstConnection(EvClassLogPetrinetConnection.class, context, petrinet, log);

		// start algorithm
		PNRepResult res = replayLog(context, log, petrinet, parameters);

		context.getFutureResult(0).setLabel(
				"Replay result - log " + XConceptExtension.instance().extractName(log) + " on " + petrinet.getLabel()
				+ " using Automated Planning");
		return res;

	}
	
	
	/**
	 * The plug-in variant that allows one to test.
	 * 
	 * @param context The context to run in.
	 * @return The output.
	 * @throws ConnectionCannotBeObtained 
	 */
//	@UITopiaVariant(affiliation = AFFILIATION, author = AUTHOR, email = EMAIL)
//	@PluginVariant(variantLabel = "Your plug-in name, dialog", requiredParameterLabels = { 0, 1 })
//	public PNRepResult testUI(UIPluginContext context, XLog log, Petrinet petrinet) throws ConnectionCannotBeObtained {
//		
//		ConfigurationUI configurationUI = new ConfigurationUI();
//		PlanningBasedAlignmentParameters parameters = configurationUI.getPlanningBasedAlignmentParameters(context, petrinet, log);
//		if (parameters == null) {
//			context.getFutureResult(0).cancel(true);
//			return null;
//		}
//		
//		
//		XLogInfo logInfo = XLogInfoFactory.createLogInfo(log);
//		
//		for (XEventClass c : logInfo.getEventClasses(new XEventNameClassifier()).getClasses())
//			System.out.println(c);
//		
//		for (XTrace t : log) {
//			
//			for (XEvent e : t) {
//				String activityName = XConceptExtension.instance().extractName(e);
//				System.out.println("\t"+activityName);
//			}
//			
//			break;
//		}
//		
//		AbstractPddlEncoder pddlEncoder = new StandardPddlEncoder(petrinet, parameters);
//		int i = 0;
//		int stop = 3;
//		for (XTrace t : log) {
//			
//			if (i == stop)
//				break;
//			
//			StringBuffer domain = pddlEncoder.createPropositionalDomain(t);
//			StringBuffer problem = pddlEncoder.createPropositionalProblem(t);
//			Utilities.writeFile("pddlEnc/domain" + (i+1) + ".pddl", domain);
//			Utilities.writeFile("pddlEnc/problem" + (i+1) + ".pddl", problem);
//			
//			i++;
//		}
//		
//		return null;
//	}
	
	
	/**
	 * 
	 * @param context
	 * @param log
	 * @param net
	 * @param parameters
	 * @return
	 */
	private PNRepResult replayLog(PluginContext context, XLog log, Petrinet net, PlanningBasedAlignmentParameters parameters) {

		PNRepResult replayRes = null;

		// start algorithm measuring the execution time
		long start = System.nanoTime();
		replayRes = apply(context, log, net, parameters);
		long elapsedTime = System.nanoTime() - start;
		
		// log total time
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(2);
		nf.setMaximumFractionDigits(2);
		context.log("Replay is finished in " + nf.format(elapsedTime / 1000000000) + " seconds");

		// add connection
		if (replayRes != null) {			
			context.addConnection(new PlanningBasedAlignmentConnection(log, net, parameters, replayRes));
		}

		return replayRes;	
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
//	private PNRepResult runConnections(PluginContext context, XLog log, Petrinet petrinet, PlanningBasedAlignmentParameters parameters) {
//		if (parameters.isTryConnections()) {
//			// Try to found a connection that matches the inputs and the parameters.
//			Collection<PlanningBasedAlignmentConnection> connections;
//			try {
//				connections = context.getConnectionManager().getConnections(
//						PlanningBasedAlignmentConnection.class, context, log, petrinet);
//				for (PlanningBasedAlignmentConnection connection : connections) {
//					if (connection.getObjectWithRole(PlanningBasedAlignmentConnection.LOG_LABEL)
//							.equals(log) && connection.getObjectWithRole(PlanningBasedAlignmentConnection.PETRINET_LABEL)
//							.equals(petrinet) && connection.getParameters().equals(parameters)) {
//						// Found a match. Return the associated output as result of the algorithm.
//						return connection.getObjectWithRole(PlanningBasedAlignmentConnection.PN_REPLAY_RESULT_LABEL);
//					}
//				}
//			} catch (ConnectionCannotBeObtained e) {
//				// do nothing
//			}
//		}
//		
//		// No connection found. Apply the algorithm to compute a fresh output result.
//		PNRepResult output = apply(context, log, petrinet, parameters);
//		if (parameters.isTryConnections()) {
//			// Store a connection containing the inputs, output, and parameters.
//			context.getConnectionManager().addConnection(
//					new PlanningBasedAlignmentConnection(log, petrinet, parameters, output));
//		}
//		// Return the output.
//		return output;		
//	}

}
