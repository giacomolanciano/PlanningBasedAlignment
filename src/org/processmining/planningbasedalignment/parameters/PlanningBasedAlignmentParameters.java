package org.processmining.planningbasedalignment.parameters;

import org.deckfour.xes.model.XLog;
import org.processmining.basicutils.parameters.impl.PluginParametersImpl;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;

public class PlanningBasedAlignmentParameters extends PluginParametersImpl {

	private boolean yourBoolean;
	private int yourInteger;
	private String yourString;
	
	public PlanningBasedAlignmentParameters(XLog log, Petrinet petrinet) {
		super();
		setYourBoolean(log.equals(petrinet));
		setYourInteger(log.toString().length() - petrinet.toString().length());
		setYourString(log.toString() + petrinet.toString());
	}

	public PlanningBasedAlignmentParameters(PlanningBasedAlignmentParameters parameters) {
		super(parameters);
		setYourBoolean(parameters.isYourBoolean());
		setYourInteger(parameters.getYourInteger());
		setYourString(parameters.getYourString());
	}
	
	public boolean equals(Object object) {
		if (object instanceof PlanningBasedAlignmentParameters) {
			PlanningBasedAlignmentParameters parameters = (PlanningBasedAlignmentParameters) object;
			return super.equals(parameters) &&
					isYourBoolean() == parameters.isYourBoolean() &&
					getYourInteger() == parameters.getYourInteger() &&
					getYourString().equals(parameters.getYourString());
		}
		return false;
	}
	
	public void setYourBoolean(boolean yourBoolean) {
		this.yourBoolean = yourBoolean;
	}

	public boolean isYourBoolean() {
		return yourBoolean;
	}

	public void setYourInteger(int yourInteger) {
		this.yourInteger = yourInteger;
	}

	public int getYourInteger() {
		return yourInteger;
	}

	public void setYourString(String yourString) {
		this.yourString = yourString;
	}

	public String getYourString() {
		return yourString;
	}
	
	public String toString() {
		return "(" + getYourString() + "," + getYourInteger() + "," + isYourBoolean() + ")";
	}
}
