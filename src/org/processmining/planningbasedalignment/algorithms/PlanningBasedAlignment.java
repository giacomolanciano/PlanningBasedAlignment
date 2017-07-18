package org.processmining.planningbasedalignment.algorithms;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.planningbasedalignment.parameters.PlanningBasedAlignmentParameters;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;

public class PlanningBasedAlignment {

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
		/**
		 * Put your algorithm here, which computes an output form the inputs provided the parameters.
		 */
		PNRepResult output = null;
		long time = -System.currentTimeMillis();
		parameters.displayMessage("[PlanningBasedAlignment] Start");
		parameters.displayMessage("[PlanningBasedAlignment] First input = " + log.toString());
		parameters.displayMessage("[PlanningBasedAlignment] Second input = " + petrinet.toString());
		parameters.displayMessage("[PlanningBasedAlignment] Parameters = " + parameters.toString());
		
		
		time += System.currentTimeMillis();
		parameters.displayMessage("[PlanningBasedAlignment] Output = " + output.toString());
		parameters.displayMessage("[PlanningBasedAlignment] End (took " + time/1000.0 + "  seconds).");
	    
		return output;
	}
}
