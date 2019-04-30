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
	 * Conversione di una data e ora in formato data di eXtraWay (yyyyMMdd)
	 * @param date
	 * @return
	 */
	public static String dateToXwFormat(Date date) {
		String xwdate = "";
		if (date != null) {
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
				xwdate = sdf.format(date);
			}
			catch(Exception e) {
				logger.warn("DateUtils.dateToXwFormat(): Unable to parse datetime in xw date format... " + e.getMessage(), e);
			}
		}
		return xwdate;
	}
	
	/**
	 * Conversione di una data e ora in formato ora di eXtraWay (HHmmss)
	 * @param date
	 * @return
	 */
	public static String timeToXwFormat(Date date) {
		String xwtime = "";
		if (date != null) {
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("HHmmss");
				xwtime = sdf.format(date);
			}
			catch(Exception e) {
				logger.warn("DateUtils.timeToXwFormat(): Unable to parse datetime in xw time format... " + e.getMessage(), e);
			}
		}
		return xwtime;
	}
	
	public static void main(String[] args) {
		Date now = new Date();
		System.out.println(dateToXwFormat(now));
		System.out.println(timeToXwFormat(now));
	}
	
}
