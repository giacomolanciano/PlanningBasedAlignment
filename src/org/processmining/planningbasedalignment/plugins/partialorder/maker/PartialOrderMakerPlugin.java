package org.processmining.planningbasedalignment.plugins.partialorder.maker;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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

/**
 * The ProM plug-in for making an event log partially ordered, according to several different granularities.
 * 
 * @author Giacomo Lanciano
 *
 */
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
	 * @param log The event log to modify.
	 */
	@UITopiaVariant(
		affiliation = AFFILIATION, author = AUTHOR, email = EMAIL, pack = HelpMessages.PLANNING_BASED_ALIGNMENT_PACKAGE,
		uiLabel = UITopiaVariant.USEVARIANT)
	@PluginVariant(variantLabel = "Partial Order Maker (Daily Granularity)", requiredParameterLabels = { 0 })
	public XLog makeDailyPartialOrder(PluginContext context, XLog log) {
		return makePartialOrder(context, log, new DailyPartialOrderMaker());
	}
	
	/**
	 * The plug-in variant that create a copy of the given event log with timestamps rounded down to to the closest 
	 * previous checkpoint of the same day. The checkpoints are set every 8 hours, i.e.: 00:00, 08:00, 16:00.
	 * 
	 * @param context The context to run in.
	 * @param log The event log to modify.
	 */
	@UITopiaVariant(
		affiliation = AFFILIATION, author = AUTHOR, email = EMAIL, pack = HelpMessages.PLANNING_BASED_ALIGNMENT_PACKAGE,
		uiLabel = UITopiaVariant.USEVARIANT)
	@PluginVariant(variantLabel = "Partial Order Maker (8-hours Granularity)", requiredParameterLabels = { 0 })
	public XLog makeEightHoursPartialOrder(PluginContext context, XLog log) {
		return makePartialOrder(context, log, new EightHoursPartialOrderMaker());
	}
	
	/**
	 * Create a copy of the given event log with all timestamps rounded according to the criteria defined by the given
	 * {@link PartialOrderMaker}. 
	 * 
	 * @param context The context to run in.
	 * @param log The event log to modify.
	 * @param partialOrderMaker The criteria according to which the timestamps have to be modified.
	 * @return The new modified event log.
	 */
	private XLog makePartialOrder(PluginContext context, XLog log, PartialOrderMaker partialOrderMaker) {

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
			String caseId = XConceptExtension.instance().extractName(trace);

			// initialize a default timestamp for events that have a null one
			Date defaultDate = new Date(0);
			
			// create new trace with the same attributes (make copy to avoid side-effects)
			XTrace newTrace = factory.createTrace((XAttributeMap) trace.getAttributes().clone());
			
			int eventPos = 1;
			for (XEvent event : trace) {
				// create new event with the same attributes (make copy to avoid side-effects)
				XEvent newEvent = factory.createEvent((XAttributeMap) event.getAttributes().clone());
				Date timestamp = XTimeExtension.instance().extractTimestamp(newEvent);
				
				if (timestamp == null) {
					// the event has a null timestamp, set it as equal to the timestamp of the previous event
					context.log(new RuntimeException("Null timestamp at trace " + caseId + ", event #" + eventPos));
					timestamp = defaultDate;
				}
				
				Date newTimestamp = partialOrderMaker.modifyTimestamp(timestamp);
				XTimeExtension.instance().assignTimestamp(newEvent, newTimestamp);
				
				newTrace.add(newEvent);
				
				// update default date
				defaultDate = (Date) timestamp.clone();
				eventPos++;
			}
			
			newLog.add(newTrace);
			
			// update progress bar
			progress.setValue(progress.getValue() + 1);
		}
		
		// set result label
		DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
		Date date = new Date();		
		String resultLabel = newLogName + " (created @ " + dateFormat.format(date) + ")";		
		context.getFutureResult(0).setLabel(resultLabel);
		
		return newLog;
	}

}
