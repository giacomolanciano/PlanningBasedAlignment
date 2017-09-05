package org.processmining.planningbasedalignment.plugins.partialorder.maker;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;

/**
 * Round down timestamp to the closest previous checkpoint of the same day. The checkpoints are set every 8 hours, 
 * i.e.: 00:00, 08:00, 16:00.
 * 
 * @author Giacomo Lanciano
 *
 */
public class EightHoursPartialOrderMaker implements PartialOrderMaker {
	
	private static final int FIRST_CHECKPOINT_HOUR = 8;
	private static final int SECOND_CHECKPOINT_HOUR = 16;
	
	@Override
	public Date modifyTimestamp(Date timestamp) {
		Date result = null;
		Calendar calendar = DateUtils.toCalendar(timestamp);
		int hours = calendar.get(Calendar.HOUR_OF_DAY);
		
		if (hours < FIRST_CHECKPOINT_HOUR) {
			return DateUtils.truncate(timestamp, Calendar.DATE);
			
		} else if (hours < SECOND_CHECKPOINT_HOUR) {
			result = DateUtils.setHours((Date) timestamp.clone(), FIRST_CHECKPOINT_HOUR);
			
		} else {
			result = DateUtils.setHours((Date) timestamp.clone(), SECOND_CHECKPOINT_HOUR);
			
		}
		
		result = DateUtils.truncate(result, Calendar.HOUR_OF_DAY);
		return result;
	}

}
