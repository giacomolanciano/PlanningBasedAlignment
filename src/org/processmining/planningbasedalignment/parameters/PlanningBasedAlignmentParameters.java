package org.processmining.planningbasedalignment.parameters;

import java.util.Map;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.basicutils.parameters.impl.PluginParametersImpl;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.planningbasedalignment.utils.PlannerSearchStrategy;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;

public class PlanningBasedAlignmentParameters extends PluginParametersImpl {

	private Marking initialMarking;
	private Marking finalMarking;
	private PlannerSearchStrategy plannerSearchStrategy;
	private TransEvClassMapping transitionsEventsMapping;
	private Map<XEventClass, Integer> movesOnLogCosts;
	private Map<Transition, Integer> movesOnModelCosts;
	private Map<Transition, Integer> synchronousMovesCosts;
	private int[] tracesInterval;
	private int[] tracesLengthBounds;
	
	public PlanningBasedAlignmentParameters() {
		super();
		setInitialMarking(null);
		setFinalMarking(null);
		setPlannerSearchStrategy(PlannerSearchStrategy.BLIND_A_STAR);
		setTransitionsEventsMapping(null);
		setMovesOnLogCosts(null);
		setMovesOnModelCosts(null);
		setSynchronousMovesCosts(null);
		setTracesInterval(null);
		setTracesLengthBounds(null);
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
	
}
