/*
 * Copyright (c) 2014 Sebastian Paarmann
 */

package paarmann.physikprofil;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import java.util.Calendar;

public class SettingsActivity extends Activity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    getFragmentManager().beginTransaction()
      .replace(android.R.id.content, new SettingsFragment())
      .commit();
  }

  public class SettingsFragment extends PreferenceFragment
                implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.preferences);
    }

    public SettingsFragment() {
      super();
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
      } else if (key.equals(MainActivity.PREF_AUTOUPDATES)) {
        Preference updatePref = findPreference(key);
        boolean autoUpdate = preferences.getBoolean(key, false);
        //Update summary text
        if (autoUpdate) {
          updatePref.setSummary(getResources().getString(R.string.pref_autoupdates_summary_true));
        } else {
          updatePref.setSummary(getResources().getString(R.string.pref_autoupdates_summary));
        }

        //Register/Unregister alarms for updates
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Uri uriAfterSchool = Uri.fromParts("homeworkUpdate", "afterSchool", "");
        Uri uriAfternoon = Uri.fromParts("homeworkUpdate", "afternoon", "");

        Intent intentAfterSchool = new Intent(MainActivity.ACTION_UPDATEHOMEWORK, uriAfterSchool,
           SettingsActivity.this, AutomaticUpdateService.class);
        Intent intentAfternoon = new Intent(MainActivity.ACTION_UPDATEHOMEWORK, uriAfternoon,
           SettingsActivity.this, AutomaticUpdateService.class); 

        PendingIntent piAfterSchool = PendingIntent.getService(SettingsActivity.this,
            1, intentAfterSchool, 0);
        PendingIntent piAfternoon = PendingIntent.getService(SettingsActivity.this,
            2, intentAfternoon, 0);

        if (autoUpdate) {
          Calendar afterSchool = Calendar.getInstance();
          afterSchool.set(Calendar.HOUR_OF_DAY, 14);
          afterSchool.set(Calendar.MINUTE, 15);
          Calendar afternoon = Calendar.getInstance();
          afternoon.set(Calendar.HOUR_OF_DAY, 17);
          afternoon.set(Calendar.MINUTE, 15);

          long oneDayMillis = 24 * 60 * 60 * 1000;

          alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, afterSchool.getTimeInMillis(),
              oneDayMillis, piAfterSchool);
          alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, afternoon.getTimeInMillis(),
              oneDayMillis, piAfternoon);
        } else {
          alarmManager.cancel(piAfterSchool);
          alarmManager.cancel(piAfternoon);
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
      onSharedPreferenceChanged(
          getPreferenceScreen().getSharedPreferences(),
          MainActivity.PREF_AUTOUPDATES);
    }

    @Override
    public void onPause() {
      super.onPause();
      getPreferenceScreen().getSharedPreferences()
        .unregisterOnSharedPreferenceChangeListener(this);
    }
  }

}
