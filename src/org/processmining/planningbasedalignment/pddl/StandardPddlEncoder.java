package org.processmining.planningbasedalignment.pddl;

import java.util.Collection;
import java.util.Map;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.datapetrinets.DataPetriNet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.planningbasedalignment.parameters.PlanningBasedAlignmentParameters;

/**
 * A standard implementation of the PDDL encoder.
 * 
 * @author Giacomo Lanciano
 *
 */
public class StandardPddlEncoder extends AbstractPddlEncoder {

	
	public StandardPddlEncoder(DataPetriNet petrinet, PlanningBasedAlignmentParameters parameters) {
		super(petrinet, parameters);
	}


	/**
	 * Create PDDL Domain for the given log trace.
	 * 
	 * @param trace
	 * @param parameters
	 * @return
	 */
	@Override
	public StringBuffer createPropositionalDomain(XTrace trace) {

		StringBuffer pddlDomainBuffer = new StringBuffer();
		
		Map<Transition, Integer> movesOnModelCosts = parameters.getMovesOnModelCosts();
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

		for(Transition transition : petrinet.getTransitions()) {

			/* Move Sync */			
			String transitionName = encode(transition);
			String mappedEventClass = encode(parameters.getTransitionsEventsMapping().get(transition));
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> transitionInEdgesCollection = petrinet.getInEdges(transition);
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> transitionOutEdgesCollection = petrinet.getOutEdges(transition);
			
			int i = 0;
			for(XEvent event : trace) {

				int currentEventIndex = i + 1;
				int nextEventIndex = currentEventIndex + 1;
				String currentEventLabel = "ev" + currentEventIndex;				
				String eventName = encode(event);

				if(eventName.equalsIgnoreCase(mappedEventClass)) {
					
					pddlDomainBuffer.append("(:action " + SYNCH_MOVE_PREFIX + "#" + transitionName + "#" + currentEventLabel + "\n");
					pddlDomainBuffer.append(":precondition (and");
					
					for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : transitionInEdgesCollection) {
						Place place = (Place) inEdge.getSource();
						pddlDomainBuffer.append(" (token " + encode(place) + ")");
					}
					
					pddlDomainBuffer.append(" (tracePointer "+ currentEventLabel + ")");
					pddlDomainBuffer.append(")\n");

					pddlDomainBuffer.append(":effect (and (allowed)");
					for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : transitionInEdgesCollection) {
						Place place = (Place) inEdge.getSource();
						pddlDomainBuffer.append(" (not (token " + encode(place) + "))");
					}
					for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge : transitionOutEdgesCollection) {
						Place place = (Place) outEdge.getTarget();
						pddlDomainBuffer.append(" (token " + encode(place) + ")");
					}

					String nextEventLabel;
					if(currentEventIndex == trace.size())
						nextEventLabel = "evEND";
					else
						nextEventLabel = "ev" + nextEventIndex;

					pddlDomainBuffer.append(" (not (tracePointer "+ currentEventLabel + ")) (tracePointer "+ nextEventLabel + ")");
					pddlDomainBuffer.append(")\n");
					pddlDomainBuffer.append(")\n\n");
				}
				
				i++;
			}


			/* Move in the Model */
			pddlDomainBuffer.append("(:action " + MODEL_MOVE_PREFIX + "#" + transitionName + "\n");
			pddlDomainBuffer.append(":precondition");

			if(transitionInEdgesCollection.size() > 1)
				pddlDomainBuffer.append(" (and");

			for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : transitionInEdgesCollection) {
				Place place = (Place) inEdge.getSource();
				pddlDomainBuffer.append(" (token " + encode(place) + ")");
			}

			if(transitionInEdgesCollection.size() > 1)
				pddlDomainBuffer.append(")\n");
			else
				pddlDomainBuffer.append("\n");

			pddlDomainBuffer.append(":effect (and (not (allowed))");

			for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : transitionInEdgesCollection) {
				Place place = (Place) inEdge.getSource();
				pddlDomainBuffer.append(" (not (token " + encode(place) + "))");
			}
			for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : transitionOutEdgesCollection) {
				Place place = (Place) inEdge.getTarget();
				pddlDomainBuffer.append(" (token " + encode(place) + ")");
			}				

			//if(Globals.getPlannerPerspective().getCostCheckBox().isSelected()) {
			pddlDomainBuffer.append(" (increase (total-cost) ");
			pddlDomainBuffer.append(movesOnModelCosts.get(transition) + ")\n");
			//}

			pddlDomainBuffer.append(")\n");
			pddlDomainBuffer.append(")\n\n");
		}
		
		
		/* Move in the Log */
		int i = 0;
		for(XEvent event : trace) {

			String eventName = encode(event);
			
			int currentTraceIndex = i + 1;
			int nextTraceIndex = currentTraceIndex + 1;
			String currentEventLabel = "ev" + currentTraceIndex;

			String nextEventLabel;
			if(currentTraceIndex == trace.size())
				nextEventLabel = "evEND";
			else
				nextEventLabel = "ev" + nextTraceIndex;

			pddlDomainBuffer.append("(:action " + LOG_MOVE_PREFIX + "#" + eventName + "#" + currentEventLabel + "-" + nextEventLabel + "\n");
			pddlDomainBuffer.append(":precondition (and (tracePointer " + currentEventLabel  + ") (allowed))\n");
			pddlDomainBuffer.append(":effect (and (not (tracePointer " + currentEventLabel  + ")) (tracePointer " + nextEventLabel  + ")");

			pddlDomainBuffer.append(" (increase (total-cost) ");
			
			// TODO exploit map
			for(XEventClass entry : movesOnLogCosts.keySet()) {
				String eventClass = encode(entry);
				if(eventClass.equalsIgnoreCase(eventName)) {
					pddlDomainBuffer.append(movesOnLogCosts.get(entry) + ")\n");
					break;
				}
			}

			pddlDomainBuffer.append(")");
			pddlDomainBuffer.append(")\n\n");
			
			i++;
		}

		pddlDomainBuffer.append(")");
		return pddlDomainBuffer;
	}

	
	/**
	 * Create PDDL Domain for the given log trace.
	 * 
	 * @param trace
	 * @return
	 */
	@Override
	public StringBuffer createPropositionalProblem(XTrace trace) {

		StringBuffer pddlObjectsBuffer = new StringBuffer();	
		StringBuffer pddlInitBuffer = new StringBuffer();
		StringBuffer pddlCostBuffer = new StringBuffer();
		StringBuffer pddlGoalBuffer = new StringBuffer();
		StringBuffer pddlProblemBuffer = new StringBuffer();

		pddlObjectsBuffer.append("(define (problem Align) (:domain Mining)\n");
		pddlObjectsBuffer.append("(:objects\n");	

		pddlInitBuffer = new StringBuffer("(:init\n");
		pddlInitBuffer.append("(tracePointer ev1)\n");
		pddlInitBuffer.append("(allowed)\n");

		pddlGoalBuffer.append("(:goal\n");
		pddlGoalBuffer.append("(and\n");

		Collection<Place> places = petrinet.getPlaces();
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

		for(Place place : places) {
			pddlObjectsBuffer.append(encode(place) + " - place\n");
		}

		int traceLength = trace.size();
		for(int i = 0; i < traceLength; i++) {	
			int currentEventIndex = i + 1;
			pddlObjectsBuffer.append("ev" + currentEventIndex + " - event\n");		

			if(currentEventIndex == traceLength) {
				pddlObjectsBuffer.append("evEND - event\n");					
			}
		}	
		pddlObjectsBuffer.append(")\n");		
		pddlCostBuffer.append("(= (total-cost) 0)\n");
		pddlInitBuffer.append(pddlCostBuffer);
		pddlInitBuffer.append(")\n");
		pddlGoalBuffer.append("(tracePointer evEND)\n");
		pddlGoalBuffer.append("))\n");
		pddlGoalBuffer.append("(:metric minimize (total-cost))\n");	
		pddlProblemBuffer.append(pddlObjectsBuffer);
		pddlProblemBuffer.append(pddlInitBuffer);
		pddlProblemBuffer.append(pddlGoalBuffer);	
		pddlProblemBuffer.append(")");	

		return pddlProblemBuffer;
	}
	
}
