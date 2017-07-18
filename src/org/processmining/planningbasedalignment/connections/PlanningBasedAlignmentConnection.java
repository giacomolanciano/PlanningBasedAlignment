package org.processmining.planningbasedalignment.connections;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.connections.impl.AbstractConnection;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.planningbasedalignment.parameters.PlanningBasedAlignmentParameters;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;

public class PlanningBasedAlignmentConnection extends AbstractConnection {

	/**
	 * Label for event log.
	 */
	public final static String LOG = "Event Log";
	
	/**
	 * Label for Petri net.
	 */
	public final static String PETRINET = "Petri Net";
	
	/**
	 * Label for Petri net replay result.
	 */
	public final static String PN_REPLAY_RESULT = "Petri Net Replay Result";

	/**
	 * Private copy of parameters.
	 */
	private PlanningBasedAlignmentParameters parameters;

	/**
	 * Create a connection.
	 * @param input1 First input.
	 * @param input2 Second input.
	 * @param output Output.
	 * @param parameters Parameters.
	 */
	public PlanningBasedAlignmentConnection(XLog input1, Petrinet input2, PNRepResult output, PlanningBasedAlignmentParameters parameters) {
		super("Planning-based Alignment connection");
		put(LOG, input1);
		put(PETRINET, input2);
		put(PN_REPLAY_RESULT, output);
		this.parameters = new PlanningBasedAlignmentParameters(parameters);
	}

	/**
	 * 
	 * @return The parameters stored in the connection.
	 */
	public PlanningBasedAlignmentParameters getParameters() {
		return parameters;
	}
}
