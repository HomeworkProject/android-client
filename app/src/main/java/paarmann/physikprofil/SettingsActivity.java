/*
 * Copyright (c) 2014 Sebastian Paarmann
 */

package paarmann.physikprofil;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

public class SettingsActivity extends Activity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    getFragmentManager().beginTransaction()
      .replace(android.R.id.content, new SettingsFragment())
      .commit();
  }

  public static class SettingsFragment extends PreferenceFragment
                implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
      if (key.equals(MainActivity.PREF_FILTERSUBJECTS)) {
        Preference filterPref = findPreference(key);
        boolean filter = preferences.getBoolean(key, false);
        if (filter) {
          filterPref.setSummary(getResources().getString(R.string.pref_filter_summary_true));
        } else {
          filterPref.setSummary(getResources().getString(R.string.pref_filter_summary));
        }
      }
    }

    @Override
    public void onResume() {
      super.onResume();
      getPreferenceScreen().getSharedPreferences()
        .registerOnSharedPreferenceChangeListener(this);
      onSharedPreferenceChanged(
          getPreferenceScreen().getSharedPreferences(),
          MainActivity.PREF_FILTERSUBJECTS);
    }

    @Override
    public void onPause() {
      super.onPause();
      getPreferenceScreen().getSharedPreferences()
        .unregisterOnSharedPreferenceChangeListener(this);
    }
  }

}
