package org.processmining.planningbasedalignment.models;

import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.plugins.DataConformance.ResultReplay;
import org.processmining.plugins.DataConformance.DataAlignment.DataAlignmentState;
import org.processmining.plugins.DataConformance.framework.ReplayState;
import org.processmining.plugins.DataConformance.framework.VariableMatchCosts;

/**
 * A class to represent the result of a Planning-based Alignment. It extends {@link ResultReplay} in order to be
 * compatible with the visualizations defined in DataAwareReplayer package.
 * 
 * @author Giacomo Lanciano
 *
 */
public class PlanningBasedReplayResult extends ResultReplay {

	/**
	 * The Petri net that has been used to replay the event log.
	 */
	private PetrinetGraph petrinet;
	
	/**
	 * The statistics about the traces alignments time.
	 */
	private SummaryStatistics alignmentTimeSummary;
	
	/**
	 * The statistics about the traces alignments expanded search states.
	 */
	private SummaryStatistics expandedStatesSummary;
	
	/**
	 * The statistics about the traces alignments generated search states.
	 */
	private SummaryStatistics generatedStatesSummary;
	
	public PlanningBasedReplayResult(
			Collection<? extends DataAlignmentState> alignments, XEventClassifier classifier, XLog log,
			PetrinetGraph petrinet) {
		
		super(alignments, null, VariableMatchCosts.NOCOST, new HashMap<String, String>(), log, classifier);
		this.petrinet = petrinet;
		this.alignmentTimeSummary = null;
		this.expandedStatesSummary = null;
		this.generatedStatesSummary = null;
	}
	
	public PlanningBasedReplayResult(
			Collection<? extends DataAlignmentState> alignments, XEventClassifier classifier, XLog log,
			PetrinetGraph petrinet, SummaryStatistics alignmentTimeSummary, SummaryStatistics expandedStatesSummary,
			SummaryStatistics generatedStatesSummary) {
		
		super(alignments, null, VariableMatchCosts.NOCOST, new HashMap<String, String>(), log, classifier);
		this.petrinet = petrinet;
		this.alignmentTimeSummary = alignmentTimeSummary;
		this.expandedStatesSummary = expandedStatesSummary;
		this.generatedStatesSummary = generatedStatesSummary;
	}
	
	/**
	 * Extract and return the control flow fitness of the aligned trace.
	 */
	protected float computeFitness(ReplayState state) {
		DataAlignmentState dataAlignmentState = (DataAlignmentState) state;
		return dataAlignmentState.getControlFlowFitness();
	}

	/* GETTERS & SETTERS */
	
	public PetrinetGraph getPetrinet() {
		return petrinet;
	}

	public SummaryStatistics getAlignmentTimeSummary() {
		return alignmentTimeSummary;
	}

	public void setAlignmentTimeSummary(SummaryStatistics alignmentTimeSummary) {
		this.alignmentTimeSummary = alignmentTimeSummary;
	}
	
	public SummaryStatistics getExpandedStatesSummary() {
		return expandedStatesSummary;
	}

	public void setExpandedStatesSummary(SummaryStatistics expandedStatesSummary) {
		this.expandedStatesSummary = expandedStatesSummary;
	}
	
	public SummaryStatistics getGeneratedStatesSummary() {
		return generatedStatesSummary;
	}

	public void setGeneratedStatesSummary(SummaryStatistics generatedStatesSummary) {
		this.generatedStatesSummary = generatedStatesSummary;
	}

}
