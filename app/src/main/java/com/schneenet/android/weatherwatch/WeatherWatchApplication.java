package com.schneenet.android.weatherwatch;

import android.app.Application;

import com.mikepenz.iconics.Iconics;
import com.schneenet.android.weatherwatch.icons.WeatherWatchIcons;

/**
 * Extended application class used to register fonts
 */
public class WeatherWatchApplication extends Application
{
	@Override
	public void onCreate()
	{
		super.onCreate();
		Iconics.registerFont(new WeatherWatchIcons());
	}
}
