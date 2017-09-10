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

public class PlanningBasedReplayResult extends ResultReplay {

	private PetrinetGraph petrinet;
	private SummaryStatistics alignmentTimeSummary;
	private SummaryStatistics expandedStatesSummary;
	private SummaryStatistics generatedStatesSummary;
	
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
	
	protected float computeFitness(ReplayState state) {
		DataAlignmentState dataAlignmentState = (DataAlignmentState) state;
		return dataAlignmentState.getControlFlowFitness();
	}

	public PetrinetGraph getPetrinet() {
		return petrinet;
	}

	public SummaryStatistics getAlignmentTimeSummary() {
		return alignmentTimeSummary;
	}

	public SummaryStatistics getExpandedStatesSummary() {
		return expandedStatesSummary;
	}

	public SummaryStatistics getGeneratedStatesSummary() {
		return generatedStatesSummary;
	}

}
