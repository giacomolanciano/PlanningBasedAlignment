package org.processmining.planningbasedalignment.plugins.partialorder.maker;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;

/**
 * Round down timestamp to midnight of the same day.
 * 
 * @author Giacomo Lanciano
 *
 */
public class DailyPartialOrderMaker implements PartialOrderMaker {

	@Override
	public Date modifyTimestamp(Date timestamp) {
		return DateUtils.truncate(timestamp, Calendar.DATE);
	}

}
