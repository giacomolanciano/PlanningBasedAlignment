package org.processmining.planningbasedalignment.connections;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.connections.impl.AbstractConnection;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.planningbasedalignment.models.PlanningBasedReplayResult;
import org.processmining.planningbasedalignment.parameters.PlanningBasedAlignmentParameters;

/**
 * The connection among Planning-based Alignment plug-in inputs and outputs.
 * 
 * @author Giacomo Lanciano
 *
 */
public class PlanningBasedAlignmentConnection extends AbstractConnection {

	/**
	 * Label for event log.
	 */
	public final static String LOG_LABEL = "Event Log";
	
	/**
	 * Label for Petri net.
	 */
	public final static String PETRINET_LABEL = "Petri Net";
	
	/**
	 * Label for Petri net replay result.
	 */
	public final static String PN_REPLAY_RESULT_LABEL = "Petri Net Replay Result";

	/**
	 * Private copy of parameters.
	 */
	private PlanningBasedAlignmentParameters parameters;

	/**
	 * Create a connection.
	 * 
	 * @param log The event log to replay.
	 * @param petrinet The Petri net on which the log has to be replayed.
	 * @param output The result of the replay of the event log on the Petri net.
	 * @param parameters The parameters to use.
	 */
	public PlanningBasedAlignmentConnection(
			XLog log, Petrinet petrinet, PlanningBasedAlignmentParameters parameters,
			PlanningBasedReplayResult output) {
		
		super("Planning-based Alignment connection");
		put(LOG_LABEL, log);
		put(PETRINET_LABEL, petrinet);
		put(PN_REPLAY_RESULT_LABEL, output);
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
