package org.processmining.planningbasedalignment.plugins.partialorder.maker;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang3.time.DateUtils;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.planningbasedalignment.help.HelpMessages;
import org.processmining.planningbasedalignment.plugins.partialorder.maker.algorithms.DailyPartialOrderMaker;
import org.processmining.planningbasedalignment.plugins.partialorder.maker.algorithms.EightHoursPartialOrderMaker;
import org.processmining.planningbasedalignment.plugins.partialorder.maker.algorithms.TimeWisePartialOrderMaker;
import org.processmining.planningbasedalignment.plugins.partialorder.maker.parameters.GroupsWisePartialOrderMakerParameters;
import org.processmining.planningbasedalignment.plugins.partialorder.maker.ui.GroupsWisePartialOrderMakerConfiguration;

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
	 * @return The partially ordered event log with a daily granularity.
	 */
	@UITopiaVariant(
		affiliation = AFFILIATION, author = AUTHOR, email = EMAIL, pack = HelpMessages.PLANNING_BASED_ALIGNMENT_PACKAGE,
		uiLabel = UITopiaVariant.USEVARIANT)
	@PluginVariant(
		variantLabel = "Make an Event Log partially ordered (Daily Granularity)", requiredParameterLabels = { 0 })
	public XLog makeDailyPartialOrder(PluginContext context, XLog log) {
		return makeTimeWisePartialOrder(context, log, new DailyPartialOrderMaker());
	}
	
	/**
	 * The plug-in variant that create a copy of the given event log with timestamps rounded down to to the closest 
	 * previous checkpoint of the same day. The checkpoints are set every 8 hours, i.e.: 00:00, 08:00, 16:00.
	 * 
	 * @param context The context to run in.
	 * @param log The event log to modify.
	 * @return The partially ordered event log with a 8-hours granularity.
	 */
	@UITopiaVariant(
		affiliation = AFFILIATION, author = AUTHOR, email = EMAIL, pack = HelpMessages.PLANNING_BASED_ALIGNMENT_PACKAGE,
		uiLabel = UITopiaVariant.USEVARIANT)
	@PluginVariant(
		variantLabel = "Make an Event Log partially ordered (8-hours Granularity)", requiredParameterLabels = { 0 })
	public XLog makeEightHoursPartialOrder(PluginContext context, XLog log) {
		return makeTimeWisePartialOrder(context, log, new EightHoursPartialOrderMaker());
	}
	
	/**
	 * The plug-in variant that create a copy of the given event log with timestamps adjusted in order to divide traces
	 * in isochronous groups whose expected size is equal to the value provided by the user. 
	 * 
	 * @param context The context to run in.
	 * @param log The event log to modify.
	 * @return The partially ordered event log.
	 */
	@UITopiaVariant(
		affiliation = AFFILIATION, author = AUTHOR, email = EMAIL, pack = HelpMessages.PLANNING_BASED_ALIGNMENT_PACKAGE,
		uiLabel = UITopiaVariant.USEVARIANT)
	@PluginVariant(
		variantLabel = "Make an Event Log partially ordered setting isochronous groups expected size", 
		requiredParameterLabels = { 0 })
	public XLog makeGroupsWisePartialOrder(UIPluginContext context, XLog log) {
		
		// start configuration GUI to tune parameters
		GroupsWisePartialOrderMakerConfiguration configurationUI = new GroupsWisePartialOrderMakerConfiguration();
		GroupsWisePartialOrderMakerParameters parameters = configurationUI.getParameters(context);

		if (parameters == null) {
			context.getFutureResult(0).cancel(true);
			return null;
		}
		
		int expectedGroupSize = parameters.getExpectedGroupSize(); 
		int intervalRadius = parameters.getIntervalRadius();
		
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
		
		long hour;
		int tracePointer;
		List<Integer> groupsDistribution;
		for (XTrace trace : log) {
			
			// create new trace with the same attributes (make copy to avoid side-effects)
			XTrace newTrace = factory.createTrace((XAttributeMap) trace.getAttributes().clone());
			
			// iterate over the trace to build isochronous groups
			hour = 0;
			tracePointer = 0;
			groupsDistribution = computeGroupsDistribution(trace, expectedGroupSize, intervalRadius);
			for (Integer groupSize : groupsDistribution) {
				
				// create a time stamp associated with the current hour
				Date newTimestamp = new Date(hour);
				
				for (XEvent event : trace.subList(tracePointer, tracePointer + groupSize)) {
					
					// create new event with the same attributes (make copy to avoid side-effects)
					XEvent newEvent = factory.createEvent((XAttributeMap) event.getAttributes().clone());	
					
					// set timestamp to new value
					XTimeExtension.instance().assignTimestamp(newEvent, newTimestamp);
					
					newTrace.add(newEvent);
				}
				
				tracePointer += groupSize;
				hour += DateUtils.MILLIS_PER_HOUR;
			}
			
			newLog.add(newTrace);
			
			// update progress bar
			progress.inc();
		}
		
		// set result label
		DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
		Date date = new Date();		
		String resultLabel = newLogName + " (created @ " + dateFormat.format(date) + ")";		
		context.getFutureResult(0).setLabel(resultLabel);
		
		return newLog;
	}
	
	/**
	 * Create a copy of the given event log with all timestamps rounded according to the time-wise criteria defined by 
	 * the given {@link TimeWisePartialOrderMaker}. 
	 * 
	 * @param context The context to run in.
	 * @param log The event log to modify.
	 * @param partialOrderMaker The criteria according to which the timestamps have to be modified.
	 * @return The new modified event log.
	 */
	private XLog makeTimeWisePartialOrder(
			PluginContext context, XLog log, TimeWisePartialOrderMaker partialOrderMaker) {

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
				
				// set timestamp to new value
				Date newTimestamp = partialOrderMaker.modifyTimestamp(timestamp);
				XTimeExtension.instance().assignTimestamp(newEvent, newTimestamp);
				
				newTrace.add(newEvent);
				
				// update default date
				defaultDate = (Date) timestamp.clone();
				eventPos++;
			}
			
			newLog.add(newTrace);
			
			// update progress bar
			progress.inc();
		}
		
		// set result label
		DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
		Date date = new Date();		
		String resultLabel = newLogName + " (created @ " + dateFormat.format(date) + ")";		
		context.getFutureResult(0).setLabel(resultLabel);
		
		return newLog;
	}
	
	/**
	 * Compute a distribution of groups sizes, uniformly and randomly chosen from an interval of values defined by the
	 * given expected size and the radius of the interval. 
	 * 
	 * @param trace The trace whose groups sizes distribution has to be computed.
	 * @param expectedSize The expected size of the groups.
	 * @param intervalRadius The radius of the interval where to pick values from.
	 * @return A {@link List} containing the groups sizes distribution.
	 */
	private List<Integer> computeGroupsDistribution(XTrace trace, int expectedSize, int intervalRadius) {
		
		int sum = 0;
		int randomSize;
		int traceLength = trace.size();
		int minRandomSize = expectedSize - intervalRadius;
		int maxRandomSize = expectedSize + intervalRadius;
		List<Integer> groupsDistribution = new ArrayList<Integer>();
		
		// generate a list of random integers picked from the defined interval
		while (sum < traceLength) {
			randomSize = ThreadLocalRandom.current().nextInt(minRandomSize, maxRandomSize + 1);
			sum += randomSize;
			groupsDistribution.add(randomSize);
		}
				
		// check groups sizes distribution against actual trace length
		if (sum > traceLength) {
			
			int lastGroupPosition = groupsDistribution.size() - 1;
			
			// get the amount of remainder events to distribute among the other groups
			int remainder = groupsDistribution.get(lastGroupPosition) - (sum - traceLength);
						
			// find a size in the distribution that can be filled with the remainder
			int fillableSizePos = 0;
			boolean fillableSizeFound = false;
			for (Integer groupSize : groupsDistribution.subList(0, lastGroupPosition)) {
				if (groupSize + remainder <= maxRandomSize) {
					fillableSizeFound = true;
					break;
				}
				fillableSizePos++;
			}
			
			if (fillableSizeFound) {
				// if a fillable size has been found, update it and remove the last size from the distribution
				int fillableSize = groupsDistribution.get(fillableSizePos);
				groupsDistribution.set(fillableSizePos, fillableSize + remainder);				
				groupsDistribution.remove(lastGroupPosition);
			} else {
				// otherwise, set the last group to be as big as the remainder
				groupsDistribution.set(lastGroupPosition, remainder);
			}
		}
				
		// check groups distribution integrity
		int finalSum = 0;
		for (Integer i : groupsDistribution) {
			finalSum += i;
		}
		if (finalSum != traceLength)
			throw new RuntimeException(
					"The computed distribution is invalid. The sum does not match the trace length.");
				
		return groupsDistribution;
	}

}
