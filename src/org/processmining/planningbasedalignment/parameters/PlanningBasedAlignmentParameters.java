package org.processmining.planningbasedalignment.parameters;

import java.util.Arrays;
import java.util.Map;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.basicutils.parameters.impl.PluginParametersImpl;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.planningbasedalignment.utils.PlannerSearchStrategy;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;

/**
 * The parameters needed by the plug-in for Planning-based Alignment.
 * 
 * @author Giacomo Lanciano
 *
 */
public class PlanningBasedAlignmentParameters extends PluginParametersImpl {

	/**
	 * The initial marking of the Petri net.
	 */
	private Marking initialMarking;
	
	/**
	 * The final marking of the Petri net.
	 */
	private Marking finalMarking;
	
	/**
	 * The search strategy (e.g. optimal).
	 */
	private PlannerSearchStrategy plannerSearchStrategy;
	
	/**
	 * The mappings between Petri net transition and event classes.
	 */
	private TransEvClassMapping transitionsEventsMapping;
	
	/**
	 * The costs associated to each event class for moves in log.
	 */
	private Map<XEventClass, Integer> movesOnLogCosts;
	
	/**
	 * The costs associated to each Petri net transition for moves in model.
	 */
	private Map<Transition, Integer> movesOnModelCosts;
	
	/**
	 * The costs associated to each synchronous move (indexed according to Petri net transitions).
	 */
	private Map<Transition, Integer> synchronousMovesCosts;
	
	/**
	 * The interval of traces in the log to be aligned.
	 */
	private int[] tracesInterval;
	
	/**
	 * The length boundaries to by matched by the traces in the log to be aligned.
	 */
	private int[] tracesLengthBounds;
	
	/**
	 * The flag stating whether events with same timestamp have to be treated as partially ordered.
	 */
	private boolean partiallyOrderedEvents;
	
	public PlanningBasedAlignmentParameters() {
		super();
		setInitialMarking(null);
		setFinalMarking(null);
		setPlannerSearchStrategy(null);
		setTransitionsEventsMapping(null);
		setMovesOnLogCosts(null);
		setMovesOnModelCosts(null);
		setSynchronousMovesCosts(null);
		setTracesInterval(null);
		setTracesLengthBounds(null);
		setPartiallyOrderedEvents(false);
	}

	public PlanningBasedAlignmentParameters(PlanningBasedAlignmentParameters parameters) {
		super(parameters);
		setInitialMarking(parameters.getInitialMarking());
		setFinalMarking(parameters.getFinalMarking());
		setPlannerSearchStrategy(parameters.getPlannerSearchStrategy());
		setTransitionsEventsMapping(parameters.getTransitionsEventsMapping());
		setMovesOnLogCosts(parameters.getMovesOnLogCosts());
		setMovesOnModelCosts(parameters.getMovesOnModelCosts());
		setSynchronousMovesCosts(parameters.getSynchronousMovesCosts());
		setTracesInterval(parameters.getTracesInterval());
		setTracesLengthBounds(parameters.getTracesLengthBounds());
		setPartiallyOrderedEvents(parameters.isPartiallyOrderedEvents());
	}

	/* GETTERS & SETTERS */

	public PlannerSearchStrategy getPlannerSearchStrategy() {
		return plannerSearchStrategy;
	}

	public void setPlannerSearchStrategy(PlannerSearchStrategy plannerSearchStrategy) {
		this.plannerSearchStrategy = plannerSearchStrategy;
	}

	public TransEvClassMapping getTransitionsEventsMapping() {
		return transitionsEventsMapping;
	}

	public void setTransitionsEventsMapping(TransEvClassMapping transitionsEventsMapping) {
		this.transitionsEventsMapping = transitionsEventsMapping;
	}

	public Marking getInitialMarking() {
		return initialMarking;
	}

	public void setInitialMarking(Marking initialMarking) {
		this.initialMarking = initialMarking;
	}
	
	public Marking getFinalMarking() {
		return finalMarking;
	}

	public void setFinalMarking(Marking finalMarking) {
		this.finalMarking = finalMarking;
	}

	public Map<XEventClass, Integer> getMovesOnLogCosts() {
		return movesOnLogCosts;
	}

	public void setMovesOnLogCosts(Map<XEventClass, Integer> movesOnLogCosts) {
		this.movesOnLogCosts = movesOnLogCosts;
	}

	public Map<Transition, Integer> getMovesOnModelCosts() {
		return movesOnModelCosts;
	}

	public void setMovesOnModelCosts(Map<Transition, Integer> movesOnModelCosts) {
		this.movesOnModelCosts = movesOnModelCosts;
	}

	public Map<Transition, Integer> getSynchronousMovesCosts() {
		return synchronousMovesCosts;
	}

	public void setSynchronousMovesCosts(Map<Transition, Integer> synchronousMovesCosts) {
		this.synchronousMovesCosts = synchronousMovesCosts;
	}

	public int[] getTracesInterval() {
		return tracesInterval;
	}

	public void setTracesInterval(int[] tracesInterval) {
		this.tracesInterval = tracesInterval;
	}

	public int[] getTracesLengthBounds() {
		return tracesLengthBounds;
	}

	public void setTracesLengthBounds(int[] tracesLengthBounds) {
		this.tracesLengthBounds = tracesLengthBounds;
	}

	public boolean isPartiallyOrderedEvents() {
		return partiallyOrderedEvents;
	}

	public void setPartiallyOrderedEvents(boolean partiallyOrderedEvents) {
		this.partiallyOrderedEvents = partiallyOrderedEvents;
	}

	@Override
	public String toString() {
		return "PlanningBasedAlignmentParameters [initialMarking=" + initialMarking + ", finalMarking=" + finalMarking
				+ ", plannerSearchStrategy=" + plannerSearchStrategy + ", transitionsEventsMapping="
				+ transitionsEventsMapping + ", movesOnLogCosts=" + movesOnLogCosts + ", movesOnModelCosts="
				+ movesOnModelCosts + ", synchronousMovesCosts=" + synchronousMovesCosts + ", tracesInterval="
				+ Arrays.toString(tracesInterval) + ", tracesLengthBounds=" + Arrays.toString(tracesLengthBounds)
				+ ", partiallyOrderedEvents=" + partiallyOrderedEvents + "]";
	}

}
