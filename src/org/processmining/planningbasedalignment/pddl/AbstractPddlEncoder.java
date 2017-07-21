package org.processmining.planningbasedalignment.pddl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.datapetrinets.DataPetriNet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.planningbasedalignment.parameters.PlanningBasedAlignmentParameters;

/**
 * Abstract class that can be extended to provide different ways of encoding an alignment-based conformance checking
 * problem in PDDL.
 * 
 * @author Giacomo Lanciano
 *
 */
public abstract class AbstractPddlEncoder {

	public static final String SYNCH_MOVE_PREFIX = "movesync";
	public static final String MODEL_MOVE_PREFIX = "moveinthemodel";
	public static final String LOG_MOVE_PREFIX = "moveinthelog";
	protected static final String INVISIBLE_TRANSITION_PREFIX = "generatedinv";
	protected static final String DUMMY = "DUMMY";
	
	protected DataPetriNet petrinet;
	protected PlanningBasedAlignmentParameters parameters;
	
	/**
	 * The mapping between Petri net invisible transitions and related PDDL ids.
	 */
	protected Map<Transition, String> transitionToPddlIdMapping = new HashMap<Transition, String>();
	
	/**
	 * The mapping between PDDL ids and related Petri net nodes.
	 */
	protected Map<String, PetrinetNode> pddlIdToPetrinetNodeMapping = new HashMap<String, PetrinetNode>();
	
	/**
	 * The mapping between PDDL ids and related event classes.
	 */
	protected Map<String, XEventClass> pddlIdToEventClassMapping = new HashMap<String, XEventClass>();
	
	protected AbstractPddlEncoder(DataPetriNet petrinet, PlanningBasedAlignmentParameters parameters) {
		this.petrinet = petrinet;
		this.parameters = parameters;
		buildMappings();
	}

	/**
	 * Create PDDL Domain for the given log trace.
	 * 
	 * @param trace The log trace.
	 * @return A {@link StringBuffer} containing the PDDL Domain.
	 */
	abstract public StringBuffer createPropositionalDomain(XTrace trace);

	/**
	 * Create PDDL Domain for the given log trace.
	 * 
	 * @param trace The log trace.
	 * @return A {@link StringBuffer} containing the PDDL Problem.
	 */
	abstract public StringBuffer createPropositionalProblem(XTrace trace);

	/**
	 * Populate the mappings that relate Petri net nodes and events class with their PDDL identifiers.
	 */
	protected void buildMappings() {
		
		// The mapping between PDDL ids and their occurrences.
		Map<String, Integer> pddlIdToOccurrencesMapping = new HashMap<String, Integer>();
		
		Transition transition;
		XEventClass eventLabel;
		String pddlTransitionId;
		String pddlEventLabelId;
		Integer pddlIdOccurrences;
		String pddlPlace;
		int invisibleTransitionsCount = 0;
		for (Entry<Transition, XEventClass> entry : parameters.getTransitionsEventsMapping().entrySet()) {
			transition = entry.getKey();
			eventLabel = entry.getValue();

			// get pddl id for transition
			pddlTransitionId = getCorrectPddlFormat(transition.getLabel());
			
			if (isInvisibleTransition(transition) && isEmptyLabel(pddlTransitionId)) {
				// transition label is empty, generate an id
				pddlTransitionId = getCorrectPddlFormat(
						new String(INVISIBLE_TRANSITION_PREFIX + invisibleTransitionsCount));
				invisibleTransitionsCount++;
			}
				
			// handle VISIBLE transitions aliasing
			pddlIdOccurrences = pddlIdToOccurrencesMapping.get(pddlTransitionId);
			if (pddlIdOccurrences != null) {
				pddlIdToOccurrencesMapping.put(pddlTransitionId, pddlIdOccurrences + 1);
				pddlTransitionId += "" + pddlIdOccurrences;
			} else {
				pddlIdToOccurrencesMapping.put(pddlTransitionId, 1);
			}
			
			// add a mapping from the transition to the generated id
			transitionToPddlIdMapping.put(transition, pddlTransitionId);
			
			// add a mapping from the generated id to the transition
			pddlIdToPetrinetNodeMapping.put(pddlTransitionId, transition);

			// get pddl id for event class
			pddlEventLabelId = getCorrectPddlFormat(eventLabel.toString());
			if (!pddlEventLabelId.equals(DUMMY))
				pddlIdToEventClassMapping.put(pddlEventLabelId, eventLabel);
		}

		// get pddl ids for places
		for (Place place : petrinet.getPlaces()) {
			pddlPlace = getCorrectPddlFormat(place.getLabel());
			pddlIdToPetrinetNodeMapping.put(pddlPlace, place);
		}

	}

	/**
	 * Format string to be a valid PDDL identifier. Notice that lower-case ids are safer.
	 * 
	 * @param string The string to be formatted.
	 * @return The correctly formatted string.
	 */
	protected static String getCorrectPddlFormat(String string)  {
		string = string.replaceAll(" ", "");
		string = string.replaceAll("\\/", "");
		string = string.replaceAll("\\(", "");
		string = string.replaceAll("\\)", "");
		string = string.replaceAll("\\<", "");
		string = string.replaceAll("\\>", "");
		string = string.replaceAll("\\.", "");
		string = string.replaceAll("\\,", "_");
		string = string.replaceAll("\\+", "_");
		string = string.replaceAll("\\-", "_");
		return string.toLowerCase();
	}

	/**
	 * Return a valid PDDL id for the given transition.
	 * 
	 * @param transition
	 * @return
	 */
	public String encode(Transition transition) {
		return transitionToPddlIdMapping.get(transition);
	}

	/**
	 * Return a valid PDDL id for the given place.
	 * 
	 * @param place
	 * @return
	 */
	public String encode(Place place) {
		return getCorrectPddlFormat(place.getLabel());
	}

	/**
	 * Return a valid PDDL id for the given event class.
	 * 
	 * @param eventLabel
	 * @return
	 */
	public String encode(XEventClass eventLabel) {
		return getCorrectPddlFormat(eventLabel.toString());
	}
	
	/**
	 * Return a valid PDDL id for the given event class.
	 * 
	 * @param eventLabel
	 * @return
	 */
	public String encode(XEvent event) {
		XEventClassifier eventClassifier = parameters.getTransitionsEventsMapping().getEventClassifier();
		return getCorrectPddlFormat(eventClassifier.getClassIdentity(event));
	}
	
	/**
	 * Check whether the given Petri net transition is invisible .
	 * 
	 * @param transition The {@link Transition} to be checked.
	 * @return true if the Petri net transition is invisible.
	 */
	public boolean isInvisibleTransition(Transition transition) {
		String transitionLabel = transition.getLabel();
		boolean isInvisible = transition.isInvisible() || isEmptyLabel(transitionLabel);
		if (isInvisible)
			transition.setInvisible(true);	// make sure the transition invisible property is correctly set
		return isInvisible;
	}
	
	/**
	 * Check whether the given label is associated to an invisible Petri net transition.
	 * 
	 * @param transition The String representing the label.
	 * @return true if the given label is associated to an invisible Petri net transition.
	 */
	private boolean isEmptyLabel(String transitionLabel) {
		return transitionLabel.isEmpty()
				|| transitionLabel.equalsIgnoreCase("")
				|| transitionLabel.equalsIgnoreCase(" ")
				|| transitionLabel.equalsIgnoreCase("\"");
	}

	/**
	 * @return the pddlIdToPetrinetNodeMapping
	 */
	public Map<String, PetrinetNode> getPddlIdToPetrinetNodeMapping() {
		return pddlIdToPetrinetNodeMapping;
	}

	/**
	 * @param pddlIdToPetrinetNodeMapping the pddlIdToPetrinetNodeMapping to set
	 */
	public void setPddlIdToPetrinetNodeMapping(Map<String, PetrinetNode> pddlIdToPetrinetNodeMapping) {
		this.pddlIdToPetrinetNodeMapping = pddlIdToPetrinetNodeMapping;
	}

	/**
	 * @return the pddlIdToEventClassMapping
	 */
	public Map<String, XEventClass> getPddlIdToEventClassMapping() {
		return pddlIdToEventClassMapping;
	}

	/**
	 * @param pddlIdToEventClassMapping the pddlIdToEventClassMapping to set
	 */
	public void setPddlIdToEventClassMapping(Map<String, XEventClass> pddlIdToEventClassMapping) {
		this.pddlIdToEventClassMapping = pddlIdToEventClassMapping;
	}

}
