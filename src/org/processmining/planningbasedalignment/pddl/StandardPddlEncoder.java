package org.processmining.planningbasedalignment.pddl;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.deckfour.xes.classification.XEventClass;
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
 * A standard implementation of the PDDL encoder that does NOT take into account a possible partial ordering in the
 * event log to be replayed on the Petri net.
 * 
 * @author Giacomo Lanciano
 *
 */
public class StandardPddlEncoder extends AbstractPddlEncoder {
	
	public StandardPddlEncoder(Petrinet petrinet, PlanningBasedAlignmentParameters parameters) {
		super(petrinet, parameters);
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
		pddlDomainBuffer.append("(tracePointer ?e - event)\n");
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

					int i = 0;
					String mappedEventClass = encode(parameters.getTransitionsEventsMapping().get(transition));
					for (XEvent event : trace) {

						int currentEventIndex = i + 1;
						int nextEventIndex = currentEventIndex + 1;
						String currentEventLabel = "ev" + currentEventIndex;
						String eventPddlId = encode(event);

						if (eventPddlId.equalsIgnoreCase(mappedEventClass)) {

							syncMovesBuffer.append("(:action " + SYNCH_MOVE_PREFIX + SEPARATOR + transitionName
									+ SEPARATOR + currentEventLabel + "\n");
							
							/* add action pre-conditions */
							syncMovesBuffer.append(":precondition (and");

							// add firing rules constraint
							for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : transitionInEdgesCollection) {
								Place place = (Place) inEdge.getSource();
								syncMovesBuffer.append(" (token " + encode(place) + ")");
							}

							// add "tracepointer" constraint
							syncMovesBuffer.append(" (tracePointer " + currentEventLabel + ")");
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

							// add "tracepointer" effect
							String nextEventLabel;
							if (currentEventIndex == traceLength)
								nextEventLabel = "evEND";
							else
								nextEventLabel = "ev" + nextEventIndex;

							syncMovesBuffer.append(" (not (tracePointer " + currentEventLabel + ")) "
									+ "(tracePointer " + nextEventLabel + ")");
							
							syncMovesBuffer.append(")\n");
							syncMovesBuffer.append(")\n\n");
						}

						i++;
					}
				}
			} 
		}
		
		/* Moves in the Log */
		int i = 0;
		for(XEvent event : trace) {

			String eventName = encode(event);
			
			int currentTraceIndex = i + 1;
			int nextTraceIndex = currentTraceIndex + 1;
			String currentEventLabel = "ev" + currentTraceIndex;

			String nextEventLabel;
			if(currentTraceIndex == traceLength)
				nextEventLabel = "evEND";
			else
				nextEventLabel = "ev" + nextTraceIndex;

			movesOnLogBuffer.append(
					"(:action " + LOG_MOVE_PREFIX + SEPARATOR + eventName + SEPARATOR 
					+ currentEventLabel + "-" + nextEventLabel + "\n");
			
			/* add action pre-conditions */
			movesOnLogBuffer.append(":precondition (and (allowed) (tracePointer " + currentEventLabel  + "))\n");
			
			/* add action post-conditions */
			movesOnLogBuffer.append(
					":effect (and (not (tracePointer " + currentEventLabel  + ")) "
					+ "(tracePointer " + nextEventLabel  + ")");
			
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
			
			i++;
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
		int traceLength = trace.size();
		Collection<Place> places = petrinet.getPlaces();
		
		/* add objects to PDDL problem */
		pddlObjectsBuffer.append("(define (problem Align) (:domain Mining)\n");
		pddlObjectsBuffer.append("(:objects\n");	
		for(Place place : places) {
			pddlObjectsBuffer.append(encode(place) + " - place\n");
		}

		// create an object for each event in the trace
		for(int i = 0; i < traceLength; i++) {	
			int currentEventIndex = i + 1;
			pddlObjectsBuffer.append("ev" + currentEventIndex + " - event\n");		

			if(currentEventIndex == traceLength) {
				pddlObjectsBuffer.append("evEND - event\n");					
			}
		}
		
		pddlObjectsBuffer.append(")\n");
		
		/* add init and goal conditions to PDDL problem */
		pddlInitBuffer.append("(:init\n");
		
		if (traceLength > 0)
			// if trace is non-empty, set trace pointer in init condition
			pddlInitBuffer.append("(tracePointer ev1)\n");
		
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
		
		if (traceLength > 0)
			// if trace is non-empty, set trace pointer in goal condition
			pddlGoalBuffer.append("(tracePointer evEND)\n");
		
		pddlGoalBuffer.append("))\n");
		
		// add objective function to PDDL problem
		pddlGoalBuffer.append("(:metric minimize (total-cost))\n");
		
		pddlProblemBuffer.append(pddlObjectsBuffer);
		pddlProblemBuffer.append(pddlInitBuffer);
		pddlProblemBuffer.append(pddlGoalBuffer);	
		pddlProblemBuffer.append(")");	

		return pddlProblemBuffer.toString();
	}
	
}
