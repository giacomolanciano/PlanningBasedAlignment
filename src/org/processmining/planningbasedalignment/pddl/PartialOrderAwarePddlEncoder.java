package org.processmining.planningbasedalignment.pddl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections15.MapUtils;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.planningbasedalignment.parameters.PlanningBasedAlignmentParameters;

/**
 * @author Giacomo Lanciano
 *
 */
public class PartialOrderAwarePddlEncoder extends AbstractPddlEncoder {

	/**
	 * The mapping between the events in the trace and their (positional, unique) labels, used to refer to them in the
	 * PDDL encding.
	 */
	Map<XEvent, String> eventToLabelMapping;
	
	/**
	 * The mapping between isochronous groups timestamps (that are the unique timestamps in the trace) and their ids.
	 * 
	 * NOTE:
	 * Since the ids reflect their chronological order, it is easy to access the "previous" group, given the timestamp 
	 * of an event. 
	 */
	Map<String, Integer> timestampToGroupIdMapping;
	
	/**
	 * The mapping between isochronous groups ids and the related events.
	 */
	Map<Integer, ArrayList<XEvent>> groupIdToEventsMapping;
	
	public PartialOrderAwarePddlEncoder(Petrinet petrinet, PlanningBasedAlignmentParameters parameters) {
		super(petrinet, parameters);
	}
	
	@Override
	public String[] getPddlEncoding(XTrace trace) {
		// force isochronous groups to be built before computing the encoding
		buildIsochronousGroups(trace);
		return super.getPddlEncoding(trace);
	}
	
	/**
	 * Initialize the data structures needed for dealing with partially ordered events.
	 * 
	 * @param trace The event log trace whose alignment has to be encoded.
	 */
	protected void buildIsochronousGroups(XTrace trace) {
		
		eventToLabelMapping = new HashMap<XEvent, String>();
		timestampToGroupIdMapping = new HashMap<String, Integer>();
		groupIdToEventsMapping = new HashMap<Integer, ArrayList<XEvent>>();
		
		int eventIndex = 0;
		for (XEvent event : trace) {
			// store the event label
			String eventLabel = "ev" + ++eventIndex;
			eventToLabelMapping.put(event, eventLabel);
			
			// store the (unique) timestamps
			String timestamp = extractSafeEventTimestamp(event);
			timestampToGroupIdMapping.put(timestamp, 0);
		}

		// get sorted (unique) timestamps
		String[] timestamps = timestampToGroupIdMapping.keySet().toArray(new String[] {});
		Arrays.sort(timestamps);
		
		// update timestamp to group id mapping with chronologically sorted ids
		int groupIndex = 0;
		for (String timestamp : timestamps) {
			timestampToGroupIdMapping.put(timestamp, groupIndex++);
		}
		
		// put each event in its isochronous group
		for (XEvent event : trace) {
			String timestamp = extractSafeEventTimestamp(event);
			Integer groupId = timestampToGroupIdMapping.get(timestamp);
			
			if (groupId == null)
				throw new RuntimeException("Unable to find a group associated with timestamp " + timestamp);
			
			ArrayList<XEvent> isochronousGroup = groupIdToEventsMapping.get(groupId);
			if (isochronousGroup != null) {
				isochronousGroup.add(event);
			} else {
				isochronousGroup = new ArrayList<XEvent>();
				isochronousGroup.add(event);
				groupIdToEventsMapping.put(groupId, isochronousGroup);
			}
		}
		
		// print data structure for debug
		MapUtils.debugPrint(System.out, "eventToLabel", eventToLabelMapping);
		MapUtils.debugPrint(System.out, "timestampToGroupId", timestampToGroupIdMapping);
		MapUtils.debugPrint(System.out, "groupIdToEvents", groupIdToEventsMapping);
	}
	
	@Override
	protected String createPropositionalDomain(XTrace trace) {

		int traceLength = trace.size();
		StringBuffer syncMovesBuffer = new StringBuffer();
		StringBuffer movesOnLogBuffer = new StringBuffer();
		StringBuffer pddlDomainBuffer = new StringBuffer();
		Map<XEventClass, Integer> movesOnLogCosts = parameters.getMovesOnLogCosts();

		// define domain and objects types
		pddlDomainBuffer.append("(define (domain Mining)\n");
		pddlDomainBuffer.append("(:requirements :typing :equality)\n");
		pddlDomainBuffer.append("(:types place event)\n\n");

		// define predicates
		pddlDomainBuffer.append("(:predicates\n");
		pddlDomainBuffer.append("(token ?p - place)\n");
		pddlDomainBuffer.append("(aligned ?e - event)\n");
		pddlDomainBuffer.append("(allowed)\n");
		pddlDomainBuffer.append(")\n\n");

		// define total-cost function
		pddlDomainBuffer.append("(:functions\n");
		pddlDomainBuffer.append("(total-cost)\n");
		pddlDomainBuffer.append(")\n\n");
		
		/* Sync Moves */
		if (traceLength > 0) {
			
			for (Transition transition : petrinet.getTransitions()) {

				String transitionName = encode(transition);
				Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> transitionInEdgesCollection = 
						petrinet.getInEdges(transition);
				Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> transitionOutEdgesCollection = 
						petrinet.getOutEdges(transition);

				if (!transition.isInvisible()) {

					String mappedEventClass = encode(parameters.getTransitionsEventsMapping().get(transition));
					for (XEvent event : trace) {

						String eventPddlId = encode(event);
						if (eventPddlId.equalsIgnoreCase(mappedEventClass)) {
							
							String eventLabel = eventToLabelMapping.get(event);
							syncMovesBuffer.append("(:action " + SYNCH_MOVE_PREFIX + SEPARATOR + transitionName
									+ SEPARATOR + eventLabel + "\n");
							
							/* add action pre-conditions */
							syncMovesBuffer.append(":precondition (and");

							// add firing rules constraint
							for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : transitionInEdgesCollection) {
								Place place = (Place) inEdge.getSource();
								syncMovesBuffer.append(" (token " + encode(place) + ")");
							}

							// add "aligned" constraint
							ArrayList<XEvent> previousIsochronousGroup = getPreviousIsochronousGroup(event);
							if (previousIsochronousGroup != null) {
								for (XEvent precEvent : previousIsochronousGroup) {
									syncMovesBuffer.append(" (aligned " + eventToLabelMapping.get(precEvent) + ")");
								}
							}
							syncMovesBuffer.append(")\n");

							/* add action post-conditions */
							syncMovesBuffer.append(":effect (and (allowed)");
							
							// add firing rules effect
							for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : transitionInEdgesCollection) {
								Place place = (Place) inEdge.getSource();
								syncMovesBuffer.append(" (not (token " + encode(place) + "))");
							}
							for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge : transitionOutEdgesCollection) {
								Place place = (Place) outEdge.getTarget();
								syncMovesBuffer.append(" (token " + encode(place) + ")");
							}
							
							// add "aligned" effect
							syncMovesBuffer.append(" (aligned " + eventLabel + ")");							
							syncMovesBuffer.append(")\n");
							syncMovesBuffer.append(")\n\n");
						}
					}
				}
			} 
		}
		
		/* Moves in the Log */
		int eventIndex = 0;
		for(XEvent event : trace) {

			String eventName = encode(event);
			
			int currentEventIndex = ++eventIndex;
			int nextEventIndex = currentEventIndex + 1;
			String currentEventLabel = "ev" + currentEventIndex;

			String nextEventLabel;
			if(currentEventIndex == traceLength)
				nextEventLabel = "evEND";
			else
				nextEventLabel = "ev" + nextEventIndex;

			movesOnLogBuffer.append(
					"(:action " + LOG_MOVE_PREFIX + SEPARATOR + eventName + SEPARATOR 
					+ currentEventLabel + "-" + nextEventLabel + "\n");
			
			/* add action pre-conditions */
			movesOnLogBuffer.append(":precondition (and (allowed)");
			
			// add "aligned" constraint
			ArrayList<XEvent> previousIsochronousGroup = getPreviousIsochronousGroup(event);
			if (previousIsochronousGroup != null) {
				for (XEvent precEvent : previousIsochronousGroup) {
					movesOnLogBuffer.append(" (aligned " + eventToLabelMapping.get(precEvent) + ")");
				}
			}
			movesOnLogBuffer.append(")\n");
			
			/* add action post-conditions */
			movesOnLogBuffer.append(":effect (and (aligned " + currentEventLabel  + ")");
			movesOnLogBuffer.append(" (increase (total-cost) ");
			
			// get the cost of the event class
			for(Entry<XEventClass, Integer> entry : movesOnLogCosts.entrySet()) {
				String eventClass = encode(entry.getKey());
				if(eventClass.equalsIgnoreCase(eventName)) {
					movesOnLogBuffer.append(entry.getValue() + ")\n");
					break;
				}
			}
			
			movesOnLogBuffer.append(")\n");
			movesOnLogBuffer.append(")\n\n");
		}

		pddlDomainBuffer.append(syncMovesBuffer);
		pddlDomainBuffer.append(movesOnLogBuffer);
		pddlDomainBuffer.append(movesOnModelBuffer);
		pddlDomainBuffer.append(")");
		return pddlDomainBuffer.toString();
	}

	@Override
	protected String createPropositionalProblem(XTrace trace) {

		StringBuffer pddlObjectsBuffer = new StringBuffer();	
		StringBuffer pddlInitBuffer = new StringBuffer();
		StringBuffer pddlGoalBuffer = new StringBuffer();
		StringBuffer pddlProblemBuffer = new StringBuffer();
		Collection<Place> places = petrinet.getPlaces();
		
		/* add objects to PDDL problem */
		pddlObjectsBuffer.append("(define (problem Align) (:domain Mining)\n");
		pddlObjectsBuffer.append("(:objects\n");	
		for(Place place : places) {
			pddlObjectsBuffer.append(encode(place) + " - place\n");
		}

		// create an object for each event in the trace
		for(Entry<XEvent, String> entry : eventToLabelMapping.entrySet()) {
			pddlObjectsBuffer.append(entry.getValue() + " - event\n");
		}
		
		pddlObjectsBuffer.append(")\n");
		
		/* add init and goal conditions to PDDL problem */
		pddlInitBuffer.append("(:init\n");		
		pddlInitBuffer.append("(allowed)\n");
		
		pddlGoalBuffer.append("(:goal\n");
		pddlGoalBuffer.append("(and\n");

		Marking initialMarking = parameters.getInitialMarking();
		Marking finalMarking = parameters.getFinalMarking();		
		for(Place place : places) {
			String placeName = encode(place);
			
			if(initialMarking.contains(place)) 
				pddlInitBuffer.append("(token " + placeName + ")\n");

			if(finalMarking.contains(place)) 
				pddlGoalBuffer.append("(token " + placeName + ")\n");
			else 
				pddlGoalBuffer.append("(not (token " + placeName + "))\n");
		}

		pddlInitBuffer.append("(= (total-cost) 0)\n");
		pddlInitBuffer.append(")\n");
		
		// create an object for each event in the trace
		for(Entry<XEvent, String> entry : eventToLabelMapping.entrySet()) {
			pddlGoalBuffer.append("(aligned " + entry.getValue() + ")\n");
		}
		pddlGoalBuffer.append("))\n");
		
		// add objective function to PDDL problem
		pddlGoalBuffer.append("(:metric minimize (total-cost))\n");
		
		pddlProblemBuffer.append(pddlObjectsBuffer);
		pddlProblemBuffer.append(pddlInitBuffer);
		pddlProblemBuffer.append(pddlGoalBuffer);	
		pddlProblemBuffer.append(")");	

		return pddlProblemBuffer.toString();
	}
	
	/**
	 * Get the {@link String} representing the timestamp of the event in a format such that lexicographical and 
	 * chronological orders coincide.
	 * 
	 * @param event The event.
	 * @return The {@link String} representing the timestamp of the event.
	 */
	private static String extractSafeEventTimestamp(XEvent event) {		
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		Date date = XTimeExtension.instance().extractTimestamp(event);
		String timestamp = dateFormat.format(date);
		return timestamp;
	}
	
	/**
	 * Get the isochronous group that is right before the group of the given event in chronological order.
	 * 
	 * @param event The event.
	 * @return The isochronous group that is right before the group of the given event in chronological order, or null
	 * if it does not exist.
	 */
	private ArrayList<XEvent> getPreviousIsochronousGroup(XEvent event) {
		String timestamp = extractSafeEventTimestamp(event);
		Integer groupId = timestampToGroupIdMapping.get(timestamp);
		
		if (groupId == null)
			throw new RuntimeException("Unable to find a group associated with timestamp " + timestamp);
		
		return groupIdToEventsMapping.get(groupId - 1);
	}

}
