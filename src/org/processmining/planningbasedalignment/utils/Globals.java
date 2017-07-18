package org.processmining.planningbasedalignment.utils;

import java.util.Hashtable;
import java.util.Vector;

import org.processmining.planningbasedalignment.models.PetrinetTransition;
import org.processmining.planningbasedalignment.models.Trace;

public class Globals {

	public static final String TIME_UNIT = " ms.";

	/**
	 * Vector that records the alphabet of activities that appear in the log traces.
	 */
	private static Vector<String> logActivitiesRepositoryVector = new Vector<String>();	

	/**
	 * Vector that records all the traces (represented as java objects "Trace") of the log.	
	 */
	private static Vector<Trace> allTracesVector = new Vector<Trace>();	

	/**
	 * Hashtable that records the name of a reference trace and its corresponding content.
	 */
	private static Hashtable<String, String> allTracesHashtable = new Hashtable<String, String>();	

	/**
	 * Vector that records all the transitions (represented as java objects "PetriNetTransition") of the log.	
	 */
	private static Vector<PetrinetTransition> allTransitionsVector = new Vector<PetrinetTransition>();
	
	/**
	 * Vector that records all the places (represented as java Strings) of the log.	
	 */
	private static Vector<String> allPlacesVector = new Vector<String>();

	/**
	 * Vectors that record the places having one token in the initial marking.	
	 */
	private static Vector<String> initialMarkingPlacesVector = new Vector<String>();
	
	/**
	 * Vectors that record the places having one token in the final marking.	
	 */
	private static Vector<String> finalMarkingPlacesVector = new Vector<String>();	

	/**
	 * Vector that records the cost of moving activities only in the model or in the log.
	 * It is a Vector of Vectors, where each Vector is built in the following way:
	 * - the first element is the name of the activity, 
	 * - the second element is the cost of moving an activity in the model
	 * - the third element is the cost of moving an activity in the log
	 */
	private static Vector<Vector<String>> activitiesCostsVector = new Vector<Vector<String>>();	

	/**
	 * Vector that records the initial/final marking of any place included in the Petri Net.
	 * It is a Vector of Vectors, where each Vector is built in the following way:
	 * - the first element is the name of the place,
	 * - the second element is the number of tokens in the initial marking of the place
	 * - the third element is the number of tokens in the final marking of the place		
	 */
	private static Vector<Vector<String>> petrinetMarkingVector = new Vector<Vector<String>>();	
	
	/**
	 * Vector that records the complete alphabet of activities that appear in the log and in the Petri Net.
	 * Notice that the alphabet of activities may include activities that are included in some trace,
	 * but never used in the Petri Net, and vice-versa.		
	 */
	private static Vector<String> allActivitiesVector = new Vector<String>();

	private static int traceMinLength = 0;
	private static int traceMaxLength = 0;
	private static boolean discardDuplicatedTraces = false;
	private static String eventLogFileName = new String("Created from scratch");
	private static String petrinetFileName = new String();

	
	/* GETTERS AND SETTERS */

	public static Vector<String> getLogActivitiesRepositoryVector() {
		return logActivitiesRepositoryVector;
	}
	public static void setLogActivitiesRepositoryVector(Vector<String> v) {
		logActivitiesRepositoryVector = v;
	}
	public static Vector<Trace> getAllTracesVector() {
		return allTracesVector;
	}
	public static void setAllTracesVector(Vector<Trace> allTracesVector) {
		Globals.allTracesVector = allTracesVector;
	}
	public static Vector<Vector<String>> getActivitiesCostsVector() {
		return activitiesCostsVector;
	}
	public static void setActivitiesCostsVector(Vector<Vector<String>> costsVector) {
		Globals.activitiesCostsVector = costsVector;
	}
	public static Vector<Vector<String>> getPetrinetMarkingVector() {
		return petrinetMarkingVector;
	}
	public static void setPetrinetMarkingVector(Vector<Vector<String>> petrinetMarkingVector) {
		Globals.petrinetMarkingVector = petrinetMarkingVector;
	}
	public static Vector<PetrinetTransition> getAllTransitionsVector() {
		return allTransitionsVector;
	}
	public static void setAllTransitionsVector(Vector<PetrinetTransition> allTransitionsVector) {
		Globals.allTransitionsVector = allTransitionsVector;
	}
	public static Vector<String> getAllPlacesVector() {
		return allPlacesVector;
	}
	public static void setAllPlacesVector(Vector<String> allPlacesVector) {
		Globals.allPlacesVector = allPlacesVector;
	}
	public static Vector<String> getInitialMarkingPlacesVector() {
		return initialMarkingPlacesVector;
	}
	public static void setInitialMarkingPlacesVector(Vector<String> initialMarkingPlacesVector) {
		Globals.initialMarkingPlacesVector = initialMarkingPlacesVector;
	}
	public static Vector<String> getFinalMarkingPlacesVector() {
		return finalMarkingPlacesVector;
	}
	public static void setFinalMarkingPlacesVector(Vector<String> finalMarkingPlacesVector) {
		Globals.finalMarkingPlacesVector = finalMarkingPlacesVector;
	}
	public static Vector<String> getAllActivitiesVector() {
		return allActivitiesVector;
	}
	public static void setAllActivitiesVector(Vector<String> allActivitiesVector) {
		Globals.allActivitiesVector = allActivitiesVector;
	}
	public static Hashtable<String, String> getAllTracesHashtable() {
		return allTracesHashtable;
	}
	public static void setAllTracesHashtable(Hashtable<String, String> allTracesHashtable) {
		Globals.allTracesHashtable = allTracesHashtable;
	}
	public static int getTraceMinLength() {
		return traceMinLength;
	}
	public static int getTraceMaxLength() {
		return traceMaxLength;
	}
	public static void setTraceMinLength(int traceMinLength) {
		Globals.traceMinLength = traceMinLength;
	}
	public static void setTraceMaxLength(int traceMaxLength) {
		Globals.traceMaxLength = traceMaxLength;
	}
	public static boolean isDiscardDuplicatedTraces() {
		return discardDuplicatedTraces;
	}
	public static void setDiscardDuplicatedTraces(boolean discardDuplicatedTraces) {
		Globals.discardDuplicatedTraces = discardDuplicatedTraces;
	}
	public static String getEventLogFileName() {
		return eventLogFileName;
	}
	public static void setEventLogFileName(String eventLogFileName) {
		Globals.eventLogFileName = eventLogFileName;
	}
	public static String getPetrinetFileName() {
		return petrinetFileName;
	}
	public static void setPetrinetFileName(String petrinetFileName) {
		Globals.petrinetFileName = petrinetFileName;
	}	

}
