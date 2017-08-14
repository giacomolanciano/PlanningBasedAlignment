package org.processmining.planningbasedalignment.plugins;

import java.util.Iterator;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginCategory;
import org.processmining.framework.plugin.annotations.PluginVariant;

/**
 * The ProM plug-in for removing traces of length 1 from an event log.
 * 
 * @author Giacomo Lanciano
 *
 */
@Plugin(
	name = "Remove length-1 Traces from Event Log",
	parameterLabels = { "Event Log" }, 
	returnLabels = { "Filtered Event Log" },
	returnTypes = { XLog.class },
	userAccessible = true,
	categories = PluginCategory.Filtering
)
public class RemoveLength1TracesPlugin {

	private static final String AFFILIATION = "Sapienza University of Rome";
	private static final String AUTHOR = "Giacomo Lanciano";
	private static final String EMAIL = "lanciano.1487019@studenti.uniroma1.it";
	
	@UITopiaVariant(affiliation = AFFILIATION, author = AUTHOR, email = EMAIL)
	@PluginVariant(variantLabel = "Remove length-1 Traces from Event Log",
	requiredParameterLabels = { 0 })
	public XLog run(UIPluginContext context, XLog log) {

		context.log("Copying source log...");
		XLog result = (XLog) log.clone();
		
		context.log("Removing traces...");
		Iterator<XTrace> it = result.listIterator();
		XTrace trace;
		while (it.hasNext()) {
			trace = it.next();
			if (trace.size() <= 1)
				it.remove();
		}
		
		context.getFutureResult(0).setLabel(
				XConceptExtension.instance().extractName(log) + " (filtered on length-1 traces)");
		return result;

	}
}
