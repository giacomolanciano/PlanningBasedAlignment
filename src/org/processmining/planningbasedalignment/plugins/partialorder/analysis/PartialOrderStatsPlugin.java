package org.processmining.planningbasedalignment.plugins.partialorder.analysis;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Map.Entry;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.contexts.uitopia.packagemanager.PMPackage;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.util.HTMLToString;
import org.processmining.planningbasedalignment.plugins.planningbasedalignment.pddl.PartialOrderAwarePddlEncoder;
import org.processmining.planningbasedalignment.utils.HelpMessages;

/**
 * The ProM plug-in for analyzing a partially ordered event log.
 * 
 * @author Giacomo Lanciano
 *
 */
@Plugin(
	name = "Partial Order Statistics",
	parameterLabels = { "Partially Ordered Event Log" }, 
	returnLabels = { "Statistics" },
	returnTypes = { HTMLToString.class },
	userAccessible = true
)
public class PartialOrderStatsPlugin implements HTMLToString {

	/**
	 * The summary containing the relevant statistics about isochronous groups size.
	 */
	protected SummaryStatistics stats;
	
	/**
	 * The plug-in variant that analyze the given partially ordered event log.
	 * 
	 * @param context The context to run in.
	 * @param log The event log to analyze.
	 * @return The text report of the analysis.
	 */
	@UITopiaVariant(
		affiliation = HelpMessages.AFFILIATION, author = HelpMessages.AUTHOR, email = HelpMessages.EMAIL,
		pack = HelpMessages.PLANNING_BASED_ALIGNMENT_PACKAGE)
	@PluginVariant(requiredParameterLabels = { 0 })
	public HTMLToString analyzeLog(PluginContext context, XLog log) {
		
		// init progress bar
		Progress progress = context.getProgress();
		progress.setIndeterminate(false);
		progress.setMaximum(log.size());
		progress.setMinimum(0);
		
		// init stats
		this.stats = new SummaryStatistics();
		PartialOrderAwarePddlEncoder encoder = new PartialOrderAwarePddlEncoder();
		
		for (XTrace trace : log) {
			encoder.buildIsochronousGroups(trace);
			
			// iterate over the isochronous groups of the trace
			for (Entry<Integer, ArrayList<XEvent>> entry : encoder.getGroupIdToEventsMapping().entrySet()) {
				// add isochronous group size to stats
				this.stats.addValue(entry.getValue().size());
			}
			
			// update progress bar
			progress.inc();
		}
		
		return this;
	}

	@Override
	public String toHTMLString(boolean includeHTMLTags) {
		StringBuffer buffer = new StringBuffer();
		NumberFormat realFormat = NumberFormat.getNumberInstance();
		realFormat.setMaximumFractionDigits(2);
		
		if (includeHTMLTags) {
			buffer.append("<html>");
		}
		
		buffer.append("<h2>Partial Order Statistics</h2>");
		buffer.append("<table>");
		buffer.append("<tr><th>Stat</th><th>Value</th></tr>");
		buffer.append("<tr><td>Average</td><td>" + realFormat.format(this.stats.getMean()) + "</td></tr>");
		buffer.append("<tr><td>Maximum</td><td>" + realFormat.format(this.stats.getMax()) + "</td></tr>");
		buffer.append("<tr><td>Minimum</td><td>" + realFormat.format(this.stats.getMin()) + "</td></tr>");
		buffer.append("<tr><td>Std dev</td><td>" + realFormat.format(this.stats.getStandardDeviation()) + "</td></tr>");
		buffer.append("</table>");
		
		if (includeHTMLTags) {
			buffer.append("</html>");
		}
		
		return buffer.toString();
	}

}
