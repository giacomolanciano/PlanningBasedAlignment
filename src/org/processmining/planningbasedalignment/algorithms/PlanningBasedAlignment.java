package org.processmining.planningbasedalignment.algorithms;

import java.util.Vector;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.planningbasedalignment.models.PetrinetTransition;
import org.processmining.planningbasedalignment.models.Trace;
import org.processmining.planningbasedalignment.parameters.PlanningBasedAlignmentParameters;
import org.processmining.planningbasedalignment.utils.Globals;
import org.processmining.planningbasedalignment.utils.Utilities;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;

public class PlanningBasedAlignment {

	/**
	 * The method that performs the alignment of an event log and a Petri net using Automated Planning.
	 * 
	 * @param context The context where to run in.
	 * @param log The event log.
	 * @param petrinet The Petri net.
	 * @param parameters The parameters to use.
	 * @return The result of the replay of the event log on the Petri net.
	 */
	public PNRepResult apply(PluginContext context, XLog log, Petrinet petrinet, PlanningBasedAlignmentParameters parameters) {
		/**
		 * Put your algorithm here, which computes an output form the inputs provided the parameters.
		 */
		PNRepResult output = null;

		//		long time = -System.currentTimeMillis();
		//		parameters.displayMessage("[PlanningBasedAlignment] Start");
		//		parameters.displayMessage("[PlanningBasedAlignment] First input = " + log.toString());
		//		parameters.displayMessage("[PlanningBasedAlignment] Second input = " + petrinet.toString());
		//		parameters.displayMessage("[PlanningBasedAlignment] Parameters = " + parameters.toString());
		//		time += System.currentTimeMillis();
		//		parameters.displayMessage("[PlanningBasedAlignment] Output = " + output.toString());
		//		parameters.displayMessage("[PlanningBasedAlignment] End (took " + time/1000.0 + "  seconds).");

		return output;
	}


	/**
	 * Create PDDL Domain for the given log trace.
	 * 
	 * @param trace
	 * @param parameters
	 * @return
	 */
	public StringBuffer createPropositionalDomain(Trace trace, PlanningBasedAlignmentParameters parameters) {

		StringBuffer pddlDomainBuffer = new StringBuffer();
		Vector<Vector<String>> activitiesCostsVector = Globals.getActivitiesCostsVector();

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

		//if(Globals.getPlannerPerspective().getCostCheckBox().isSelected()) {
		// define total-cost function
		pddlDomainBuffer.append("(:functions\n");	
		pddlDomainBuffer.append("(total-cost)\n");			
		pddlDomainBuffer.append(")\n\n");		
		//}

		for(PetrinetTransition transition : Globals.getAllTransitionsVector()) {

			/* Move Sync */
			Vector<String> traceContentVector = trace.getTraceContentVector();
			for(int i=0; i < traceContentVector.size(); i++) {

				int currentEventIndex = i + 1;
				int nextEventIndex = currentEventIndex + 1;
				String traceElement = traceContentVector.elementAt(i);
				String currentEventLabel = "ev" + currentEventIndex;

				if(traceElement.equalsIgnoreCase(transition.getName())) {  //TODO generalize mapping
					
					pddlDomainBuffer.append("(:action moveSync" + "#" + transition.getName() + "#" + currentEventLabel + "\n");
					pddlDomainBuffer.append(":precondition (and");
					
					for(Place p : transition.getInputPlacesVector()) {
						pddlDomainBuffer.append(" (token " + Utilities.getCorrectPddlFormat(p.getLabel()) + ")");
					}
					
					pddlDomainBuffer.append(" (tracePointer "+ currentEventLabel + ")");
					pddlDomainBuffer.append(")\n");

					pddlDomainBuffer.append(":effect (and (allowed)");
					for(Place p : transition.getInputPlacesVector()) {
						pddlDomainBuffer.append(" (not (token " + Utilities.getCorrectPddlFormat(p.getLabel()) + "))");
					}
					for(Place p : transition.getOutputPlacesVector()) {
						pddlDomainBuffer.append(" (token " + Utilities.getCorrectPddlFormat(p.getLabel()) + ")");
					}

					String nextEventLabel;
					if(currentEventIndex == trace.getTraceContentVector().size())
						nextEventLabel = "evEND";
					else
						nextEventLabel = "ev" + nextEventIndex;

					pddlDomainBuffer.append(" (not (tracePointer "+ currentEventLabel + ")) (tracePointer "+ nextEventLabel + ")");
					pddlDomainBuffer.append(")\n");
					pddlDomainBuffer.append(")\n\n");
				}

			}


			/* Move in the Model */
			pddlDomainBuffer.append("(:action moveInTheModel" + "#" + transition.getName() + "\n");
			pddlDomainBuffer.append(":precondition");

			if(transition.getInputPlacesVector().size() > 1)
				pddlDomainBuffer.append(" (and");

			for(Place p : transition.getInputPlacesVector()) {
				pddlDomainBuffer.append(" (token " + Utilities.getCorrectPddlFormat(p.getLabel()) + ")");
			}

			if(transition.getInputPlacesVector().size()>1)
				pddlDomainBuffer.append(")\n");
			else
				pddlDomainBuffer.append("\n");

			pddlDomainBuffer.append(":effect (and (not (allowed))");

			for(Place p : transition.getInputPlacesVector()) {
				pddlDomainBuffer.append(" (not (token " + Utilities.getCorrectPddlFormat(p.getLabel()) + "))");
			}
			for(Place p : transition.getInputPlacesVector()) {
				pddlDomainBuffer.append(" (token " + Utilities.getCorrectPddlFormat(p.getLabel()) + ")");
			}				

			//if(Globals.getPlannerPerspective().getCostCheckBox().isSelected()) {
			pddlDomainBuffer.append(" (increase (total-cost) ");
			for(Vector<String> entry : activitiesCostsVector) {
				if(entry.elementAt(0).equalsIgnoreCase(transition.getName())) {
					pddlDomainBuffer.append(entry.elementAt(1) + ")\n");
					break;
				}
			}
			//}

			pddlDomainBuffer.append(")\n");
			pddlDomainBuffer.append(")\n\n");
		}
		
		
		/* Move in the Log */
		for(int i=0; i < trace.getTraceContentVector().size(); i++) {

			String eventRelatedActivity = trace.getTraceContentVector().elementAt(i);

			int currentTraceIndex = i + 1;
			int nextTraceIndex = currentTraceIndex + 1;
			String currentEventLabel = "ev" + currentTraceIndex;

			String nextEventLabel;
			if(currentTraceIndex==trace.getTraceContentVector().size())
				nextEventLabel = "evEND";
			else
				nextEventLabel = "ev" + nextTraceIndex;

			pddlDomainBuffer.append("(:action moveInTheLog#" + eventRelatedActivity + "#" + currentEventLabel + "-" + nextEventLabel + "\n");
			pddlDomainBuffer.append(":precondition (and (tracePointer " + currentEventLabel  + ") (allowed))\n");
			pddlDomainBuffer.append(":effect (and (not (tracePointer " + currentEventLabel  + ")) (tracePointer " + nextEventLabel  + ")");

			//if(Globals.getPlannerPerspective().getCostCheckBox().isSelected()) {
			pddlDomainBuffer.append(" (increase (total-cost) ");
			for(Vector<String> entry : activitiesCostsVector) {					
				if(entry.elementAt(0).equalsIgnoreCase(eventRelatedActivity)) {
					pddlDomainBuffer.append(entry.elementAt(2) + ")\n");
					break;
				}
			}
			//}

			pddlDomainBuffer.append(")");
			pddlDomainBuffer.append(")\n\n");	
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
	public StringBuffer createPropositionalProblem(Trace trace) {

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

		for(Vector<String> entry : Globals.getPetrinetMarkingVector()) {
			if(entry.elementAt(1).equalsIgnoreCase("1")) 
				pddlInitBuffer.append("(token " + entry.elementAt(0) + ")\n");

			if(entry.elementAt(2).equalsIgnoreCase("1")) 
				pddlGoalBuffer.append("(token " + entry.elementAt(0) + ")\n");
			else if(entry.elementAt(2).equalsIgnoreCase("0")) 
				pddlGoalBuffer.append("(not (token " + entry.elementAt(0) + "))\n");
		}

		for(String placeName : Globals.getAllPlacesVector()) {
			pddlObjectsBuffer.append(placeName + " - place\n");
		}

		for(int i=0; i < trace.getTraceContentVector().size(); i++) {	
			int currentEventIndex = i + 1;
			pddlObjectsBuffer.append("ev" + currentEventIndex + " - event\n");		

			if(currentEventIndex == trace.getTraceContentVector().size()) {
				pddlObjectsBuffer.append("evEND - event\n");					
			}
		}	
		pddlObjectsBuffer.append(")\n");		


		//if(Globals.getPlannerPerspective().getCostCheckBox().isSelected()) {
		pddlCostBuffer.append("(= (total-cost) 0)\n");
		pddlInitBuffer.append(pddlCostBuffer);
		//}

		pddlInitBuffer.append(")\n");

		pddlGoalBuffer.append("(tracePointer evEND)\n");
		pddlGoalBuffer.append("))\n");

		//if(Globals.getPlannerPerspective().getCostCheckBox().isSelected()) 
		pddlGoalBuffer.append("(:metric minimize (total-cost))\n");	

		pddlProblemBuffer.append(pddlObjectsBuffer);
		pddlProblemBuffer.append(pddlInitBuffer);
		pddlProblemBuffer.append(pddlGoalBuffer);	
		pddlProblemBuffer.append(")");	

		return pddlProblemBuffer;
	}

}
