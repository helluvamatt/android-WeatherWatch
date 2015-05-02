package com.schneenet.android.wristweather;

import java.util.Arrays;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;

public class SettingsFragment extends PreferenceFragment
{
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
		
		EditTextPreference prefApiKey = (EditTextPreference) findPreference(getString(R.string.prefs_apikey_key));
		prefApiKey.setOnPreferenceChangeListener(mSummaryValuePreferenceChangeListener);
		if (prefApiKey.getText() != null && !"".equals(prefApiKey.getText()))
		{
			prefApiKey.setSummary(prefApiKey.getText());
		}
	}
	
	private OnPreferenceChangeListener mSummaryValuePreferenceChangeListener = new OnPreferenceChangeListener()
	{
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue)
		{
			if (preference instanceof ListPreference)
			{
				ListPreference lp = (ListPreference) preference;
				int selectedIndex = Arrays.asList(lp.getEntryValues()).indexOf(lp.getValue());
				if (selectedIndex > -1)
				{
					lp.setSummary(lp.getEntries()[selectedIndex]);
				}
				else
				{
					lp.setSummary("");
				}
			}
			else if (preference instanceof EditTextPreference)
			{
				EditTextPreference etp = (EditTextPreference) preference;
				etp.setSummary(etp.getText());
			}
			return true;
		}
	};

}
