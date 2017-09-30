package org.processmining.planningbasedalignment.plugins.partialorder.maker.algorithms;

import java.util.Date;

/**
 * A class to modify timestamps of events according to a time-wise criteria.
 * 
 * @author Giacomo Lanciano
 *
 */
public interface TimeWisePartialOrderMaker {
	
	/**
	 * Modify the given timestamp.
	 * 
	 * @param timestamp The timestamp to be modified.
	 * @return The new different modified timestamp.
	 */
	Date modifyTimestamp(Date timestamp);
	
}
