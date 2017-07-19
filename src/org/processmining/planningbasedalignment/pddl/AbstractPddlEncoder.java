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
	
	protected Map<Transition, String> invisibleTransitionToPddlIdMapping = new HashMap<Transition, String>();
	protected Map<String, PetrinetNode> pddlIdToPetrinetNodeMapping = new HashMap<String, PetrinetNode>();
	protected Map<String, XEventClass> pddlIdToEventClassMapping = new HashMap<String, XEventClass>();

	protected AbstractPddlEncoder(DataPetriNet petrinet, PlanningBasedAlignmentParameters parameters) {
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
				pddlTransition = AbstractPddlEncoder.getCorrectPddlFormat(
						new String(INVISIBLE_TRANSITION_PREFIX + invisibleTransitionsCount));

				// add a reference from the invisible transition to the generated id
				invisibleTransitionToPddlIdMapping.put(transition, pddlTransition);
				invisibleTransitionsCount++;
			}
			pddlTransition = AbstractPddlEncoder.getCorrectPddlFormat(pddlTransition);
			pddlIdToPetrinetNodeMapping.put(pddlTransition, transition);	// TODO handle aliasing

//			System.out.println("\t" + pddlTransition);

			// get pddl id for event class
			String pddlEventLabel = AbstractPddlEncoder.getCorrectPddlFormat(eventLabel.toString());
			if (!pddlEventLabel.equals(DUMMY))
				pddlIdToEventClassMapping.put(pddlEventLabel, eventLabel);

//			System.out.println("\t" + pddlEventLabel);
//			System.out.println();
		}

		// get pddl ids for places
		for (Place place : petrinet.getPlaces()) {
			String pddlPlace = AbstractPddlEncoder.getCorrectPddlFormat(place.getLabel());
			pddlIdToPetrinetNodeMapping.put(pddlPlace, place);

//			System.out.println("\t" + pddlPlace);
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
	
	public boolean isInvisibleTransitionLabel(String label) {
		return label.isEmpty() 
				|| label.equalsIgnoreCase("") 
				|| label.equalsIgnoreCase(" ") 
				|| label.equalsIgnoreCase("\"");
	}

	/**
	 * @return the invisibleTransitionToPddlIdMapping
	 */
	public Map<Transition, String> getInvisibleTransitionToPddlIdMapping() {
		return invisibleTransitionToPddlIdMapping;
	}

	/**
	 * @param invisibleTransitionToPddlIdMapping the invisibleTransitionToPddlIdMapping to set
	 */
	public void setInvisibleTransitionToPddlIdMapping(Map<Transition, String> invisibleTransitionToPddlIdMapping) {
		this.invisibleTransitionToPddlIdMapping = invisibleTransitionToPddlIdMapping;
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
