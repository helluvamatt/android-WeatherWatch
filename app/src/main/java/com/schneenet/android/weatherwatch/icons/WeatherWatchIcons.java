package com.schneenet.android.weatherwatch.icons;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

import com.mikepenz.iconics.typeface.IIcon;
import com.mikepenz.iconics.typeface.ITypeface;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Iconics icon set for our custom icon font
 */
public class WeatherWatchIcons implements ITypeface
{
	private static final String TTF_FILE = "WeatherWatchIcons.ttf";

	private static Typeface typeface = null;

	private static HashMap<String, Character> mChars;

	@Override
	public IIcon getIcon(String s)
	{
		return Icon.valueOf(s);
	}

	@Override
	public HashMap<String, Character> getCharacters()
	{
		if (mChars == null)
		{
			HashMap<String, Character> aChars = new HashMap<>();
			for (Icon v : Icon.values())
			{
				aChars.put(v.name(), v.getCharacter());
			}
			mChars = aChars;
		}

		return mChars;
	}

	@Override
	public String getMappingPrefix()
	{
		return "wwi";
	}

	@Override
	public String getFontName()
	{
		return "WeatherWatch Icons";
	}

	@Override
	public String getVersion()
	{
		return "1.0.0";
	}

	@Override
	public int getIconCount()
	{
		return mChars.size();
	}

	@Override
	public Collection<String> getIcons()
	{
		Collection<String> icons = new LinkedList<>();
		for (Icon value : Icon.values())
		{
			icons.add(value.name());
		}
		return icons;
	}

	@Override
	public String getAuthor()
	{
		return "";
	}

	@Override
	public String getUrl()
	{
		return "";
	}

	@Override
	public String getDescription()
	{
		return "";
	}

	@Override
	public String getLicense()
	{
		return "CUSTOM";
	}

	@Override
	public String getLicenseUrl()
	{
		return "";
	}

	@Override
	public Typeface getTypeface(Context context)
	{
		if (typeface == null)
		{
			try
			{
				typeface = Typeface.createFromAsset(context.getAssets(), "fonts/" + TTF_FILE);
			}
			catch (Exception e)
			{
				Log.e("WeatherWatchIcons", "Failed to load font asset: fonts/" + TTF_FILE, e);
				return null;
			}
		}
		return typeface;
	}

	public static enum Icon implements IIcon {
		wwi_refresh('a'),
		wwi_error('b'),
		wwi_clear_day('c'),
		wwi_clear_night('d'),
		wwi_fog('e'),
		wwi_wind('f'),
		wwi_cold('g'),
		wwi_partly_cloudy_day('h'),
		wwi_partly_cloudy_night('i'),
		wwi_fog_alt('j'),
		wwi_cloudy('k'),
		wwi_storm('l'),
		wwi_light_rain('m'),
		wwi_rain('n'),
		wwi_snow('o'),
		wwi_light_snow('p'),
		wwi_heavy_snow('q'),
		wwi_hail_sleet('r'),
		wwi_mostly_cloudy('s'),
		wwi_heavy_storm('t'),
		wwi_hot('u'),
		wwi_na('v');

		Icon(char c)
		{
			mChar = c;
		}

		private char mChar;

		@Override
		public String getFormattedName()
		{
			return null;
		}

		@Override
		public String getName()
		{
			return null;
		}

		@Override
		public char getCharacter()
		{
			return mChar;
		}

		// remember the typeface so we can use it later
		private static ITypeface typeface;

		@Override
		public ITypeface getTypeface()
		{
			if (typeface == null)
			{
				typeface = new WeatherWatchIcons();
			}
			return typeface;
		}
	}
}
