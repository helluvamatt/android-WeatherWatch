package com.schneenet.android.weatherwatch.utils;

import java.util.Calendar;
import java.util.Date;

/**
 * Weather utility methods
 */
public class WeatherUtils
{
	public static final double C_ABSOLUTE_ZERO = -273.15;

	public static double tempCelsiusToKelvin(double c)
	{
		return c - C_ABSOLUTE_ZERO;
	}

	public static double tempCelsiusToFahrenheit(double c)
	{
		return c * 1.8 + 32.00;
	}

	public static float speedMpsToMph(float mps)
	{
		return mps * 2.23694f;
	}

	public static float speedMpsToKph(float mps)
	{
		return mps * 3.6f;
	}

	public static float speedMpsToKnots(float mps)
	{
		return mps * 1.94384f;
	}

	public static float pressureHpaToInhg(float hpa)
	{
		return hpa * 0.0295299830714f;
	}

	public static float pressureHpaToMmhg(float hpa)
	{
		return hpa * 0.750061561303f;
	}

	/**
	 * <p>Checks if two date objects are on the same day ignoring time.</p>
	 *
	 * <p>28 Mar 2002 13:45 and 28 Mar 2002 06:01 would return true.
	 * 28 Mar 2002 13:45 and 12 Mar 2002 13:45 would return false.
	 * </p>
	 *
	 * @param date1  the first date, not altered, not null
	 * @param date2  the second date, not altered, not null
	 * @return true if they represent the same day
	 * @throws IllegalArgumentException if either date is <code>null</code>
	 * @since 2.1
	 */
	public static boolean isSameDay(final Date date1, final Date date2) {
		if (date1 == null || date2 == null) {
			throw new IllegalArgumentException("The date must not be null");
		}
		final Calendar cal1 = Calendar.getInstance();
		cal1.setTime(date1);
		final Calendar cal2 = Calendar.getInstance();
		cal2.setTime(date2);
		return isSameDay(cal1, cal2);
	}

	/**
	 * <p>Checks if two calendar objects are on the same day ignoring time.</p>
	 *
	 * <p>28 Mar 2002 13:45 and 28 Mar 2002 06:01 would return true.
	 * 28 Mar 2002 13:45 and 12 Mar 2002 13:45 would return false.
	 * </p>
	 *
	 * @param cal1  the first calendar, not altered, not null
	 * @param cal2  the second calendar, not altered, not null
	 * @return true if they represent the same day
	 * @throws IllegalArgumentException if either calendar is <code>null</code>
	 * @since 2.1
	 */
	public static boolean isSameDay(final Calendar cal1, final Calendar cal2) {
		if (cal1 == null || cal2 == null) {
			throw new IllegalArgumentException("The date must not be null");
		}
		return cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
				cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
				cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
	}

}
