package com.schneenet.android.weatherwatch;

import java.util.Arrays;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment
{
	public static SettingsFragment newInstance()
	{
		return new SettingsFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		ListPreference prefUpdateFreq = (ListPreference) findPreference(getString(R.string.prefs_updatefreq_key));
		prefUpdateFreq.setOnPreferenceChangeListener(mSummaryValuePreferenceChangeListener);
		prefUpdateFreq.setSummary(prefUpdateFreq.getEntry());
		
		ListPreference prefTempUnit = (ListPreference) findPreference(getString(R.string.prefs_tempunit_key));
		prefTempUnit.setOnPreferenceChangeListener(mSummaryValuePreferenceChangeListener);
		prefTempUnit.setSummary(prefTempUnit.getEntry());

		ListPreference prefSpeedUnit = (ListPreference) findPreference(getString(R.string.prefs_speedunit_key));
		prefSpeedUnit.setOnPreferenceChangeListener(mSummaryValuePreferenceChangeListener);
		prefSpeedUnit.setSummary(prefSpeedUnit.getEntry());

		ListPreference prefPressureUnit = (ListPreference) findPreference(getString(R.string.prefs_pressureunit_key));
		prefPressureUnit.setOnPreferenceChangeListener(mSummaryValuePreferenceChangeListener);
		prefPressureUnit.setSummary(prefPressureUnit.getEntry());
		
		EditTextPreference prefApiKey = (EditTextPreference) findPreference(getString(R.string.prefs_apikey_key));
		prefApiKey.setOnPreferenceChangeListener(mSummaryValuePreferenceChangeListener);
		if (prefApiKey.getText() != null && !"".equals(prefApiKey.getText()))
		{
			prefApiKey.setSummary(prefApiKey.getText());
		}

		ListPreference prefLocationSource = (ListPreference) findPreference(getString(R.string.prefs_locationsource_key));
		prefLocationSource.setOnPreferenceChangeListener(mSummaryValuePreferenceChangeListener);
		prefLocationSource.setSummary(prefLocationSource.getEntry());
	}
	
	private OnPreferenceChangeListener mSummaryValuePreferenceChangeListener = new OnPreferenceChangeListener()
	{
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue)
		{
			if (preference instanceof ListPreference)
			{
				ListPreference lp = (ListPreference) preference;
				int newValueIndex = Arrays.asList(lp.getEntryValues()).indexOf(newValue);
				if (newValueIndex > -1)
				{
					lp.setSummary(lp.getEntries()[newValueIndex]);
				}
				else
				{
					lp.setSummary(getString(R.string.prefs_default_summary));
				}
			} else if (preference instanceof EditTextPreference)
			{
				EditTextPreference etp = (EditTextPreference) preference;
				etp.setSummary(newValue.toString());
			}
			return true;
		}
	};

}
