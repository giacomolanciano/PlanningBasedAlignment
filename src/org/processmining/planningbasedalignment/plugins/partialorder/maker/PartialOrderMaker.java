package org.processmining.planningbasedalignment.plugins.partialorder.maker;

import java.util.Date;

/**
 * A class to modify timestamps of events according to some criteria.
 * 
 * @author Giacomo Lanciano
 *
 */
public interface PartialOrderMaker {
	
	/**
	 * Modify the given timestamp.
	 * 
	 * @param timestamp The timestamp to be modified.
	 * @return The new different modified timestamp.
	 */
	Date modifyTimestamp(Date timestamp);
	
}
