package org.processmining.planningbasedalignment.plugins.visualization.alignment;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.planningbasedalignment.models.PlanningBasedReplayResult;
import org.processmining.planningbasedalignment.plugins.visualization.alignment.StrippedDownAlignmentView.Layout;
import org.processmining.plugins.DataConformance.Alignment;
import org.processmining.plugins.DataConformance.ResultReplay;
import org.processmining.plugins.DataConformance.visualization.alignment.ColorTheme;
import org.processmining.plugins.DataConformance.visualization.alignment.XTraceResolver;
import org.processmining.plugins.balancedconformance.export.XAlignmentConverter;
import org.processmining.plugins.balancedconformance.result.AlignmentCollection;
import org.processmining.xesalignmentextension.XAlignmentExtension.XAlignment;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

/**
 * Borrowed from DataAwareReplayer.
 *
 */
@Plugin(
	name = "01 Planning-based Alignment",
	returnLabels = { "Planning-based Alignment" },
	returnTypes = { JComponent.class },
	parameterLabels = { "Matching Instances" }
)
@Visualizer
public class PlanningBasedReplayResultVisualizer {

	private StrippedDownAlignmentView alignmentView;

	@PluginVariant(requiredParameterLabels = { 0 })
	public JComponent visualize(PluginContext context, PlanningBasedReplayResult alignments) {
		XAlignmentConverter converter = new XAlignmentConverter();
		
		ResultReplay result = (ResultReplay) alignments;
		final Map<String, XTrace> traceMap = buildTraceMap(result);
		XTraceResolver traceResolver = new XTraceResolver() {

			public XTrace getOriginalTrace(String name) {
				return traceMap.get(name);
			}

			public boolean hasOriginalTraces() {
				return true;
			}
		};
		converter.setClassifier(result.getClassifier()).setVariableMapping(result.getVariableMapping());
		return doVisualize(context, traceResolver, convertToXAlignment(alignments, traceResolver, converter));
		
	}

	private Iterable<XAlignment> convertToXAlignment(AlignmentCollection alignments,
			final XTraceResolver traceResolver, final XAlignmentConverter converter) {
		return Iterables.transform(alignments.getAlignments(), new Function<Alignment, XAlignment>() {

			public XAlignment apply(Alignment a) {
				return converter.viewAsXAlignment(a, traceResolver.getOriginalTrace(a.getTraceName()));
			}
		});
	}

	public JComponent doVisualize(PluginContext context, final XTraceResolver traceResolver,
			Iterable<XAlignment> alignments) {
		Map<String, Color> activityColorMap = ColorTheme.createColorMap(alignments);
		alignmentView = new StrippedDownAlignmentView(Layout.TWOCOLUMN, context, traceResolver, activityColorMap);
		alignmentView.getListView().addAll(alignments);
		return alignmentView;
	}

	private Map<String, XTrace> buildTraceMap(ResultReplay logReplayResult) {
		HashMap<String, XTrace> traceMap = new HashMap<>();
		for (XTrace trace : logReplayResult.getAlignedLog()) {
			traceMap.put(XConceptExtension.instance().extractName(trace), trace);
		}
		return traceMap;
	}

}
