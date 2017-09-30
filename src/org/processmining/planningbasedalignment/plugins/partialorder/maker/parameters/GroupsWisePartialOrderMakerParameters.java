package org.processmining.planningbasedalignment.plugins.partialorder.maker.parameters;

import org.processmining.basicutils.parameters.impl.PluginParametersImpl;
import org.processmining.planningbasedalignment.plugins.partialorder.maker.PartialOrderMakerPlugin;

/**
 * A class representing the parameters that the user has to provide to run the groups-wise variant of 
 * {@link PartialOrderMakerPlugin}.
 * 
 * @author Giacomo Lanciano
 *
 */
public class GroupsWisePartialOrderMakerParameters extends PluginParametersImpl {

	/**
	 * The expected value of the size of the isochronous groups.
	 */
	private int expectedGroupSize;
	
	/**
	 * The radius of the interval (centered in {@link #expectedGroupSize}) where to pick the random values from.
	 */
	private int intervalRadius;
	
	public GroupsWisePartialOrderMakerParameters() {
		super();
	}

	/* GETTERS & SETTERS */
	
	public int getExpectedGroupSize() {
		return expectedGroupSize;
	}

	public void setExpectedGroupSize(int expectedGroupSize) {
		this.expectedGroupSize = expectedGroupSize;
	}

	public int getIntervalRadius() {
		return intervalRadius;
	}

	public void setIntervalRadius(int intervalRadius) {
		this.intervalRadius = intervalRadius;
	}

}
