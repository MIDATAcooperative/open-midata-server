package utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeUtils {

	private static final String COMPLETE = "yyyy-MM-dd HH:mm:ss";
	private static final String DATE = "EEE, MMM d, yyyy";
	private static final String TIME = "HH:mm:ss";

	public static Date now() {
		return new Date(); //SimpleDateFormat(COMPLETE).format(new Date());
	}

	public static String getDate(String dateTimeString) {
		return extract(dateTimeString, DATE);
	}

	public static String getTime(String dateTimeString) {
		return extract(dateTimeString, TIME);
	}

	private static String extract(String dateTimeString, String format) {
		Date date;
		try {
			date = new SimpleDateFormat(COMPLETE).parse(dateTimeString);
		} catch (ParseException e) {
			return "Unable to parse date and time.";
		}
		return new SimpleDateFormat(format).format(date);
	}
	
	public static Date toDate(String dateTimeString) {
		Date date;
		try {
			date = new SimpleDateFormat(COMPLETE).parse(dateTimeString);
		} catch (ParseException e) {
			throw new NullPointerException(dateTimeString);
		}
		return date;
	}

}
