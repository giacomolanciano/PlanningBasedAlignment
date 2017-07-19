package org.processmining.planningbasedalignment.pddl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.extension.std.XConceptExtension;
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
	protected Map<Transition, String> invisibleTransitionToPddlIdMapping = new HashMap<Transition, String>();
	
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
	 * @return A {@link StringBuffer} containing the PDDL Domain.}
	 */
	abstract public StringBuffer createPropositionalDomain(XTrace trace);

	/**
	 * Create PDDL Domain for the given log trace.
	 * 
	 * @param trace The log trace.
	 * @return A {@link StringBuffer} containing the PDDL Problem.}
	 */
	abstract public StringBuffer createPropositionalProblem(XTrace trace);

	/**
	 * Populate the mappings that relate Petri net nodes and events class with their PDDL identifiers.
	 */
	protected void buildMappings() {

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
				pddlTransition = AbstractPddlEncoder.getCorrectPddlFormat(
						new String(INVISIBLE_TRANSITION_PREFIX + invisibleTransitionsCount));

				// add a reference from the invisible transition to the generated id
				invisibleTransitionToPddlIdMapping.put(transition, pddlTransition);
				invisibleTransitionsCount++;
			}
			pddlTransition = AbstractPddlEncoder.getCorrectPddlFormat(pddlTransition);
			pddlIdToPetrinetNodeMapping.put(pddlTransition, transition);	// TODO handle aliasing

			// get pddl id for event class
			String pddlEventLabel = AbstractPddlEncoder.getCorrectPddlFormat(eventLabel.toString());
			if (!pddlEventLabel.equals(DUMMY))
				pddlIdToEventClassMapping.put(pddlEventLabel, eventLabel);
		}

		// get pddl ids for places
		for (Place place : petrinet.getPlaces()) {
			String pddlPlace = AbstractPddlEncoder.getCorrectPddlFormat(place.getLabel());
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
		String pddlTransition = transition.getLabel();
		if(isInvisibleTransitionLabel(pddlTransition)) {
			return invisibleTransitionToPddlIdMapping.get(transition);
		}
		pddlTransition = AbstractPddlEncoder.getCorrectPddlFormat(pddlTransition);
		return pddlTransition;
	}

	/**
	 * Return a valid PDDL id for the given place.
	 * 
	 * @param place
	 * @return
	 */
	public String encode(Place place) {
		return AbstractPddlEncoder.getCorrectPddlFormat(place.getLabel());
	}

	/**
	 * Return a valid PDDL id for the given event class.
	 * 
	 * @param eventLabel
	 * @return
	 */
	public String encode(XEventClass eventLabel) {
		return AbstractPddlEncoder.getCorrectPddlFormat(eventLabel.toString());
	}
	
	/**
	 * Return a valid PDDL id for the given event class.
	 * 
	 * @param eventLabel
	 * @return
	 */
	public String encode(XEvent event) {
		return AbstractPddlEncoder.getCorrectPddlFormat(XConceptExtension.instance().extractName(event));
	}
	
	/**
	 * Tells whether the given label is associated to an invisible Petri net transition.
	 * 
	 * @param label The String representing the label.
	 * @return true if the given label is associated to an invisible Petri net transition.
	 */
	public boolean isInvisibleTransitionLabel(String label) {
		return label.isEmpty() 
				|| label.equalsIgnoreCase("") 
				|| label.equalsIgnoreCase(" ") 
				|| label.equalsIgnoreCase("\"");
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
