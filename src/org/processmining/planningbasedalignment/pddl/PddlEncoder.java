package org.processmining.planningbasedalignment.pddl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.planningbasedalignment.parameters.PlanningBasedAlignmentParameters;

/**
 * Abstract class that can be extended to provide different ways of encoding an alignment-based conformance checking
 * problem in PDDL.
 * 
 * @author Giacomo
 *
 */
public abstract class PddlEncoder {

	protected static final String INVISIBLE_TRANSITION_PREFIX = "generatedINV";
	protected static final String DUMMY = "DUMMY";
	
	protected Petrinet petrinet;
	protected PlanningBasedAlignmentParameters parameters;
	protected Map<Transition, String> invisibleTransitionToPddlIdMapping = new HashMap<Transition, String>();
	protected Map<String, PetrinetNode> pddlIdToPetrinetNodeMapping = new HashMap<String, PetrinetNode>();
	protected Map<String, XEventClass> pddlIdToEventClassMapping = new HashMap<String, XEventClass>();

	protected PddlEncoder(Petrinet petrinet, PlanningBasedAlignmentParameters parameters) {
		this.petrinet = petrinet;
		this.parameters = parameters;
		buildPddlEncodingMappings();
	}

	/**
	 * Create PDDL Domain for the given log trace.
	 * 
	 * @param trace
	 * @param parameters
	 * @return
	 */
	abstract public StringBuffer createPropositionalDomain(XTrace trace);

	/**
	 * Create PDDL Domain for the given log trace.
	 * 
	 * @param trace
	 * @return
	 */
	abstract public StringBuffer createPropositionalProblem(XTrace trace);

	
	protected void buildPddlEncodingMappings() {

		Transition transition;
		XEventClass eventLabel;
		int invisibleTransitionsCount = 0;
		for (Entry<Transition, XEventClass> entry : parameters.getTransitionsEventsMapping().entrySet()) {
			transition = entry.getKey();
			eventLabel = entry.getValue();

			// get pddl id for transition
			String pddlTransition = transition.getLabel();
			if(isInvisibleTransitionLabel(pddlTransition)) {
				// if transition is invisible, generate an id
				pddlTransition = PddlEncoder.getCorrectPddlFormat(
						new String(INVISIBLE_TRANSITION_PREFIX + invisibleTransitionsCount));

				// add a reference from the invisible transition to the generated id
				invisibleTransitionToPddlIdMapping.put(transition, pddlTransition);
				invisibleTransitionsCount++;
			}
			pddlTransition = PddlEncoder.getCorrectPddlFormat(pddlTransition);
			pddlIdToPetrinetNodeMapping.put(pddlTransition, transition);	// TODO handle alias

//			System.out.println("\t" + pddlTransition);

			// get pddl id for event class
			String pddlEventLabel = PddlEncoder.getCorrectPddlFormat(eventLabel.toString());
			if (!pddlEventLabel.equals(DUMMY))
				pddlIdToEventClassMapping.put(pddlEventLabel, eventLabel);

//			System.out.println("\t" + pddlEventLabel);
//			System.out.println();
		}

		// get pddl ids for places
		for (Place place : petrinet.getPlaces()) {
			String pddlPlace = PddlEncoder.getCorrectPddlFormat(place.getLabel());
			pddlIdToPetrinetNodeMapping.put(pddlPlace, place);

//			System.out.println("\t" + pddlPlace);
		}

	}

	/**
	 * Format string to be a valid PDDL identifier.
	 * 
	 * @param string The string to be formatted.
	 * @return The correctly formatted string.
	 */
	public static String getCorrectPddlFormat(String string)  {

		if(string.contains(" "))
			string = string.replaceAll(" ", "_");

		if(string.contains("/"))
			string = string.replaceAll("\\/", "");

		if(string.contains("("))
			string = string.replaceAll("\\(", "");

		if(string.contains(")"))
			string = string.replaceAll("\\)", "");

		if(string.contains("<"))
			string = string.replaceAll("\\<", "");

		if(string.contains(">"))
			string = string.replaceAll("\\>", "");

		if(string.contains("."))
			string = string.replaceAll("\\.", "");

		if(string.contains(","))
			string = string.replaceAll("\\,", "_");

		if(string.contains("+"))
			string = string.replaceAll("\\+", "_");

		if(string.contains("-"))
			string = string.replaceAll("\\-", "_");

		return string;
	}

	/**
	 * Return a valid PDDL id for the given transition.
	 * 
	 * @param transition
	 * @return
	 */
	public String encode(Transition transition) {
		String pddlTransition = transition.getLabel();
		if(isInvisibleTransitionLabel(pddlTransition)) {
			return invisibleTransitionToPddlIdMapping.get(transition);
		}
		pddlTransition = PddlEncoder.getCorrectPddlFormat(pddlTransition);
		return pddlTransition;
	}

	/**
	 * Return a valid PDDL id for the given place.
	 * 
	 * @param place
	 * @return
	 */
	public String encode(Place place) {
		return PddlEncoder.getCorrectPddlFormat(place.getLabel());
	}

	/**
	 * Return a valid PDDL id for the given event class.
	 * 
	 * @param eventLabel
	 * @return
	 */
	public String encode(XEventClass eventLabel) {
		return PddlEncoder.getCorrectPddlFormat(eventLabel.toString());
	}
	
	/**
	 * Return a valid PDDL id for the given event class.
	 * 
	 * @param eventLabel
	 * @return
	 */
	public String encode(XEvent event) {
		return PddlEncoder.getCorrectPddlFormat(XConceptExtension.instance().extractName(event));
	}
	
	public boolean isInvisibleTransitionLabel(String label) {
		return label.isEmpty() 
				|| label.equalsIgnoreCase("") 
				|| label.equalsIgnoreCase(" ") 
				|| label.equalsIgnoreCase("\"");
	}

}
