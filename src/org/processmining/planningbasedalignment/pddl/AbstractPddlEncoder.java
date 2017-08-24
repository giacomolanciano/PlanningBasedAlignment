package org.processmining.planningbasedalignment.pddl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.datapetrinets.DataPetriNet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
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
	public static final String SEPARATOR = "#";
	protected static final String INVISIBLE_TRANSITION_PREFIX = "generatedinv";
	protected static final String DUMMY = "DUMMY";
	
	protected DataPetriNet petrinet;
	protected PlanningBasedAlignmentParameters parameters;
	
	/**
	 * The mapping between PDDL ids and their occurrences.
	 */
	protected Map<String, Integer> pddlIdToOccurrencesMapping = new HashMap<String, Integer>();
	
	/**
	 * The mapping between Petri net nodes and related PDDL ids.
	 */
	protected Map<PetrinetNode, String> petrinetNodeToPddlIdMapping = new HashMap<PetrinetNode, String>();
	
	/**
	 * The mapping between PDDL ids and related Petri net nodes.
	 */
	protected Map<String, PetrinetNode> pddlIdToPetrinetNodeMapping = new HashMap<String, PetrinetNode>();
	
	/**
	 * The mapping between PDDL ids and related event classes.
	 */
	protected Map<String, XEventClass> pddlIdToEventClassMapping = new HashMap<String, XEventClass>();
	
	/**
	 * The encoding of the moves on model that is independent from the traces in the event log.   
	 */
	protected StringBuffer movesOnModelBuffer;
	
	protected AbstractPddlEncoder(DataPetriNet petrinet, PlanningBasedAlignmentParameters parameters) {
		this.petrinet = petrinet;
		this.parameters = parameters;
		
		// build the structures needed to properly encode the problem instances.
		buildMappings();
		
		// compute the part of the encoding that only depends on the model. 
		this.movesOnModelBuffer = getMovesOnModelEncoding();
	}

	/**
	 * Return the encoding of the alignment problem for the given trace in PDDL. 
	 * 
	 * @param trace The event log trace whose alignment has to be encoded.
	 * @return An array of Strings containing respectively the planning Domain and the planning Problem.
	 */
	public String[] getPddlEncoding(XTrace trace) {
		return new String[] {createPropositionalDomain(trace), createPropositionalProblem(trace)};
	}
	
	/**
	 * Create PDDL Domain for the given log trace.
	 * 
	 * @param trace The event log trace whose alignment has to be encoded.
	 * @return A {@link String} containing the PDDL Domain.
	 */
	abstract protected String createPropositionalDomain(XTrace trace);

	/**
	 * Create PDDL Domain for the given log trace.
	 * 
	 * @param trace The event log trace whose alignment has to be encoded.
	 * @return A {@link String} containing the PDDL Problem.
	 */
	abstract protected String createPropositionalProblem(XTrace trace);

	/**
	 * Populate the mappings that relate Petri net nodes and events class with their PDDL identifiers.
	 */
	protected void buildMappings() {
		
		Transition transition;
		String pddlTransitionId;
		XEventClass eventLabel;
		String pddlEventLabelId;
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
				
			// handle VISIBLE transitions aliasing (if needed)
			pddlTransitionId = getAliasedPddlId(pddlTransitionId);
			
			// add a mapping from the transition to the generated id
			petrinetNodeToPddlIdMapping.put(transition, pddlTransitionId);
			
			// add a mapping from the generated id to the transition
			pddlIdToPetrinetNodeMapping.put(pddlTransitionId, transition);

			// get pddl id for event class
			pddlEventLabelId = getCorrectPddlFormat(eventLabel.toString());
			if (!pddlEventLabelId.equals(DUMMY))
				pddlIdToEventClassMapping.put(pddlEventLabelId, eventLabel);
		}

		// get pddl ids for places
		String pddlPlaceId;
		for (Place place : petrinet.getPlaces()) {
			pddlPlaceId = getCorrectPddlFormat(place.getLabel());
			
			// handle places aliasing (if needed)
			pddlPlaceId = getAliasedPddlId(pddlPlaceId);
			
			// add a mapping from the place to the generated id
			petrinetNodeToPddlIdMapping.put(place, pddlPlaceId);
			
			// add a mapping from the generated id to the place
			pddlIdToPetrinetNodeMapping.put(pddlPlaceId, place);
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
	 * Create an alias for the given PDDL id if it has been already taken by another object (otherwise, leave it
	 * unchanged).
	 * 
	 * @param pddlId The PDDL id to be aliased.
	 * @return The (possibly) aliased PDDL id.
	 */
	protected String getAliasedPddlId(String pddlId) {		
		Integer pddlIdOccurrences = pddlIdToOccurrencesMapping.get(pddlId);
		if (pddlIdOccurrences != null) {
			// update occurrences
			pddlIdToOccurrencesMapping.put(pddlId, pddlIdOccurrences + 1);
			
			// create alias
			String newPddlId = pddlId + "_" + pddlIdOccurrences;
			
			// insert new aliased id to avoid new conflicts
			pddlIdToOccurrencesMapping.put(newPddlId, 1);
			
			return newPddlId;
		}
		
		pddlIdToOccurrencesMapping.put(pddlId, 1);
		return pddlId;
	}

	/**
	 * Return a valid PDDL id for the given transition.
	 * 
	 * @param transition
	 * @return
	 */
	protected String encode(Transition transition) {
		return petrinetNodeToPddlIdMapping.get(transition);
	}

	/**
	 * Return a valid PDDL id for the given place.
	 * 
	 * @param place
	 * @return
	 */
	protected String encode(Place place) {
		return petrinetNodeToPddlIdMapping.get(place);
	}

	/**
	 * Return a valid PDDL id for the given event class.
	 * 
	 * @param eventLabel
	 * @return
	 */
	protected String encode(XEventClass eventLabel) {
		return getCorrectPddlFormat(eventLabel.toString());
	}
	
	/**
	 * Return a valid PDDL id for the given event, that is the same we would get by encoding the class this event
	 * belongs to (acording to the event log classifier).
	 * 
	 * @param event
	 * @return
	 */
	protected String encode(XEvent event) {
		XEventClassifier eventClassifier = parameters.getTransitionsEventsMapping().getEventClassifier();
		return getCorrectPddlFormat(eventClassifier.getClassIdentity(event));
	}
	
	/**
	 * Compute the PDDL encoding of the moves on model.
	 * 
	 * @return A {@link StringBuffer} containing the PDDL encoding of the moves on model.
	 */
	protected StringBuffer getMovesOnModelEncoding() {
		
		StringBuffer result = new StringBuffer();
		Map<Transition, Integer> movesOnModelCosts = parameters.getMovesOnModelCosts();
		
		for(Transition transition : petrinet.getTransitions()) {

			String transitionName = encode(transition);
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> transitionInEdgesCollection = petrinet.getInEdges(transition);
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> transitionOutEdgesCollection = petrinet.getOutEdges(transition);
			
			/* Move in the Model */
			result.append("(:action " + MODEL_MOVE_PREFIX + SEPARATOR + transitionName + "\n");
			result.append(":precondition");

			if(transitionInEdgesCollection.size() > 1)
				result.append(" (and");

			for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : transitionInEdgesCollection) {
				Place place = (Place) inEdge.getSource();
				result.append(" (token " + encode(place) + ")");
			}

			if(transitionInEdgesCollection.size() > 1)
				result.append(")\n");
			else
				result.append("\n");

			result.append(":effect (and (not (allowed))");

			for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : transitionInEdgesCollection) {
				Place place = (Place) inEdge.getSource();
				result.append(" (not (token " + encode(place) + "))");
			}
			for(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : transitionOutEdgesCollection) {
				Place place = (Place) inEdge.getTarget();
				result.append(" (token " + encode(place) + ")");
			}				

			result.append(" (increase (total-cost) ");
			result.append(movesOnModelCosts.get(transition) + ")\n");

			result.append(")\n");
			result.append(")\n\n");
		}
		
		return result;
	}
	
	/**
	 * Check whether the given Petri net transition is invisible .
	 * 
	 * @param transition The {@link Transition} to be checked.
	 * @return true if the Petri net transition is invisible.
	 */
	private boolean isInvisibleTransition(Transition transition) {
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

	/* GETTERS & SETTERS */
	
	/**
	 * @return the pddlIdToPetrinetNodeMapping
	 */
	public Map<String, PetrinetNode> getPddlIdToPetrinetNodeMapping() {
		return pddlIdToPetrinetNodeMapping;
	}

	/**
	 * @return the pddlIdToEventClassMapping
	 */
	public Map<String, XEventClass> getPddlIdToEventClassMapping() {
		return pddlIdToEventClassMapping;
	}

}
