package it.tredi.msa.mailboxmanager.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utilities di gestione delle date
 */
public class DateUtils {

	private static final Logger logger = LogManager.getLogger(DateUtils.class.getName());
	
	/**
	 * Conversione di una data e ora in formato eXtraWay (yyyyMMddHHmmss)
	 * @param date
	 * @return
	 */
	public static String dateTimeToXwFormat(Date date) {
		String xwdate = "";
		if (date != null) {
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
				xwdate = sdf.format(date);
			}
			catch(Exception e) {
				logger.warn("DateUtils.dateTimeToXwFormat(): Unable to parse date in xw format... " + e.getMessage(), e);
			}
		}
		return xwdate;
	}
	
}
