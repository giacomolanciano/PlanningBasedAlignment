package org.processmining.planningbasedalignment.plugins.partialorder;

import java.util.ArrayList;
import java.util.Collection;

import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.util.ui.widgets.traceview.ProMTraceView.Event;
import org.processmining.framework.util.ui.widgets.traceview.ProMTraceView.Trace;
import org.processmining.planningbasedalignment.help.HelpMessages;
import org.processmining.plugins.DataConformance.visualization.comparison.AlignmentComparisonPlugin.AlignmentComparisonResult;
import org.processmining.plugins.DataConformance.visualization.comparison.AlignmentComparisonPlugin.AlignmentEntry;

@Plugin(
	name = "Partial Order Aware Alignment Checker",
	parameterLabels = { "Alignment Comparison" }, 
	returnLabels = { "Filtered Alignment Comparison" },
	returnTypes = { AlignmentComparisonResult.class },
	userAccessible = true
)
public class PartialOrderAwareAlignmentCheckerPlugin {

	private static final String AFFILIATION = "Sapienza University of Rome";
	private static final String AUTHOR = "Giacomo Lanciano";
	private static final String EMAIL = "lanciano.1487019@studenti.uniroma1.it";
	
	/**
	 * The plug-in variant that ...
	 * 
	 * @param context The context to run in.
	 * @param log The event log to replay.
	 */
	@UITopiaVariant(
		affiliation = AFFILIATION, author = AUTHOR, email = EMAIL, pack = HelpMessages.PLANNING_BASED_ALIGNMENT_PACKAGE,
		uiLabel = UITopiaVariant.USEVARIANT)
	@PluginVariant(requiredParameterLabels = { 0 })
	public AlignmentComparisonResult runCheck(PluginContext context, AlignmentComparisonResult alignmentComparison) {
		
		Collection<AlignmentEntry> alignmentComparisonEntries = alignmentComparison.getTraceEntries();
		Collection<AlignmentEntry> filteredEntries = new ArrayList<>();
		
		for (AlignmentEntry entry : alignmentComparisonEntries) {
			Trace<? extends Event> combinedTrace = entry.getCombinedTrace();
			if (!checkFitnessConstraint(combinedTrace))
				filteredEntries.add(entry);
		}
		
		return new AlignmentComparisonResult() {
			public Collection<AlignmentEntry> getTraceEntries() {
				return filteredEntries;
			}
		};
	}
	
	/**
	 * Check whether the fitness of the trace aligned under partial order assumption is greater or equal to the ...
	 * aligned under total order assumption.
	 * 
	 * @param combinedTrace
	 * @return true if the fitness of the trace aligned under partial order assumption is greater or equal ...
	 */
	private boolean checkFitnessConstraint(Trace<? extends Event> combinedTrace) {		
		String stringFitnessA = (combinedTrace.getName().split("Fitness A:"))[1].trim().replace("%", "");
		String stringFitnessB = (combinedTrace.getInfo().split("Fitness B:"))[1].trim().replace("%", "");
		int fitnessA = Integer.parseInt(stringFitnessA);
		int fitnessB = Integer.parseInt(stringFitnessB);
		
		// assume trace A to have been aligned under partially ordered assumption
		return fitnessA >= fitnessB;
	}
	
}
