package org.processmining.planningbasedalignment.plugins.partialorder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.planningbasedalignment.help.HelpMessages;

@Plugin(
	name = "Partial Order Maker",
	parameterLabels = { "Event Log" }, 
	returnLabels = { "Partially Ordered Log" },
	returnTypes = { XLog.class },
	userAccessible = true
)
public class PartialOrderMakerPlugin {

	private static final String AFFILIATION = "Sapienza University of Rome";
	private static final String AUTHOR = "Giacomo Lanciano";
	private static final String EMAIL = "lanciano.1487019@studenti.uniroma1.it";
	
	/**
	 * The plug-in variant that create a copy of the given event log with all timestamps rounded down to the midnight of
	 * the same day.
	 * 
	 * @param context The context to run in.
	 * @param log The event log to replay.
	 */
	@UITopiaVariant(
		affiliation = AFFILIATION, author = AUTHOR, email = EMAIL, pack = HelpMessages.PLANNING_BASED_ALIGNMENT_PACKAGE,
		uiLabel = UITopiaVariant.USEVARIANT)
	@PluginVariant(variantLabel = "Partial Order Maker (Daily)", requiredParameterLabels = { 0 })
	public XLog makeDailyPartialOrder(PluginContext context, XLog log) {

		// init progress bar
		Progress progress = context.getProgress();
		progress.setIndeterminate(false);
		progress.setMaximum(log.size());
		progress.setMinimum(0);
		
		// create new log with the same attributes (make copy to avoid side-effects)
		XFactory factory = XFactoryRegistry.instance().currentDefault();
		XLog newLog = factory.createLog((XAttributeMap) log.getAttributes().clone());
		String newLogName = "Partially Ordered " + XConceptExtension.instance().extractName(log);
		XConceptExtension.instance().assignName(newLog, newLogName);
		
		for (XTrace trace : log) {
			// create new trace with the same attributes (make copy to avoid side-effects)
			XTrace newTrace = factory.createTrace((XAttributeMap) trace.getAttributes().clone());
			
			for (XEvent event : trace) {
				// create new event with the same attributes (make copy to avoid side-effects)
				XEvent newEvent = factory.createEvent((XAttributeMap) event.getAttributes().clone());
				Date date = XTimeExtension.instance().extractTimestamp(newEvent);
				
				// round down timestamp to midnight of the same day
				Date newDate = DateUtils.truncate(date, Calendar.DATE);
				XTimeExtension.instance().assignTimestamp(newEvent, newDate);
				
				newTrace.add(newEvent);
			}
			
			newLog.add(newTrace);
			
			// update progress bar
			progress.setValue(progress.getValue() + 1);
		}
		
		// set result label
		DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
		Date date = new Date();		
		String resultLabel = newLogName + " (created @ "+dateFormat.format(date)+")";		
		context.getFutureResult(0).setLabel(resultLabel);
		
		return newLog;
	}

}
