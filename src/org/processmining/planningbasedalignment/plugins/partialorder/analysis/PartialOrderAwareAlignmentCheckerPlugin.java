package org.processmining.planningbasedalignment.plugins.partialorder.analysis;

import java.util.ArrayList;
import java.util.Collection;

import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.util.ui.widgets.traceview.ProMTraceView.Event;
import org.processmining.framework.util.ui.widgets.traceview.ProMTraceView.Trace;
import org.processmining.planningbasedalignment.plugins.planningbasedalignment.models.PlanningBasedReplayResult;
import org.processmining.planningbasedalignment.utils.HelpMessages;
import org.processmining.plugins.DataConformance.visualization.comparison.AlignmentComparisonPlugin;
import org.processmining.plugins.DataConformance.visualization.comparison.AlignmentComparisonPlugin.AlignmentComparisonResult;
import org.processmining.plugins.DataConformance.visualization.comparison.AlignmentComparisonPlugin.AlignmentEntry;

/**
 * The ProM plug-in for checking the result of Planning-based Alignment under partial ordering assumptions.
 * 
 * @author Giacomo Lanciano
 *
 */
@Plugin(
	name = "Partial Order Aware Alignment Checker",
	parameterLabels = { "Alignment Comparison", "Partial Order Aware Alignment", "Standard Alignment" }, 
	returnLabels = { "Alignments Violating Fitness Constraint" },
	returnTypes = { AlignmentComparisonResult.class },
	userAccessible = true
)
public class PartialOrderAwareAlignmentCheckerPlugin {
	
	/**
	 * The plug-in variant that retains the alignments violating the fitness constraint (see
	 * {@link #checkFitnessConstraint(Trace)}) from a previously computed alignment results comparison. The first 
	 * alignment result is assumed to be the one that has been computed under Partial Order assumption.
	 * 
	 * @param context The context to run in.
	 * @param alignmentComparison The result of the comparison of the two alignments.
	 * @return The filtered result of the comparison of the two alignments, retaining only the trace alignments
	 * violating the fitness constraint.
	 */
	@UITopiaVariant(
		affiliation = HelpMessages.AFFILIATION, author = HelpMessages.AUTHOR, email = HelpMessages.EMAIL,
		pack = HelpMessages.PLANNING_BASED_ALIGNMENT_PACKAGE, uiLabel = UITopiaVariant.USEVARIANT)
	@PluginVariant(
		variantLabel = "Retain Alignments Violating Fitness Constraint", requiredParameterLabels = { 0 })
	public AlignmentComparisonResult retainViolatingAlignments(
			PluginContext context, AlignmentComparisonResult alignmentComparison) {
		
		final Collection<AlignmentEntry> filteredEntries = new ArrayList<>();
		Collection<AlignmentEntry> alignmentComparisonEntries = alignmentComparison.getTraceEntries();
		
		for (AlignmentEntry entry : alignmentComparisonEntries) {
			Trace<? extends Event> combinedTrace = entry.getCombinedTrace();
			if (!checkFitnessConstraint(combinedTrace))
				filteredEntries.add(entry);
		}
		
		context.getFutureResult(0).setLabel("Alignments Violating Fitness Constraint");
		
		return new AlignmentComparisonResult() {
			public Collection<AlignmentEntry> getTraceEntries() {
				return filteredEntries;
			}
		};
	}
	
	/**
	 * The plug-in variant that compares two alignment results, the first of which is assumed to be the one that has 
	 * been computed under Partial Order assumption, and retains the alignments violating the fitness constraint (see
	 * {@link #checkFitnessConstraint(Trace)}).
	 * 
	 * @param context The context to run in.
	 * @param partialOrderAwareResult The Planning-based alignments under Partial Order assumption.
	 * @param result The standard Planning-based alignments.
	 * @return The filtered result of the comparison of the two alignments, retaining only the trace alignments
	 * violating the fitness constraint.
	 * @throws Exception
	 */
	@UITopiaVariant(
		affiliation = HelpMessages.AFFILIATION, author = HelpMessages.AUTHOR, email = HelpMessages.EMAIL,
		pack = HelpMessages.PLANNING_BASED_ALIGNMENT_PACKAGE, uiLabel = UITopiaVariant.USEVARIANT)
	@PluginVariant(
		variantLabel = "Compare Alignment Results to Check Fitness Constraint", requiredParameterLabels = { 1, 2 })
	public AlignmentComparisonResult compareAlignmentResults(
			PluginContext context, final PlanningBasedReplayResult partialOrderAwareResult,
			final PlanningBasedReplayResult result) throws Exception {
		
		return retainViolatingAlignments(
				context, new AlignmentComparisonPlugin().compareLogs(context, partialOrderAwareResult, result));
	}
	
	/**
	 * Check whether the fitness of the trace aligned under Partial Order assumption is greater or equal to the fitness
	 * of the trace aligned under Total Order (standard) assumption.
	 * 
	 * @param combinedTrace the alignments whose fitness values must be checked.
	 * @return true if the constraint is satisfied.
	 */
	private boolean checkFitnessConstraint(Trace<? extends Event> combinedTrace) {		
		String parsedFitnessA = (combinedTrace.getName().split("Fitness A:"))[1].trim().replace("%", "");
		String parsedFitnessB = (combinedTrace.getInfo().split("Fitness B:"))[1].trim().replace("%", "");
		int fitnessA = Integer.parseInt(parsedFitnessA);
		int fitnessB = Integer.parseInt(parsedFitnessB);
		
		// assume trace A to have been aligned under partially ordered assumption
		return fitnessA >= fitnessB;
	}
	
}
