package com.schneenet.android.weatherwatch.utils;

/**
 * Created by Matt on 5/4/2015.
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


}
