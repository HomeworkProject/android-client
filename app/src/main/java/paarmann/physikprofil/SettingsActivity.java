/*
 * Copyright (c) 2014 Sebastian Paarmann
 */

package paarmann.physikprofil;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import java.util.Calendar;

public class SettingsActivity extends Activity {

  public static final String TAG = "SettingsActivity";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    SettingsFragment fragment = new SettingsFragment();
    fragment.setContext(this);

    getFragmentManager().beginTransaction()
      .replace(android.R.id.content, fragment)
      .commit();
  }

  @Override
  public void onResume() {
    super.onResume();
    
    SettingsFragment fragment = new SettingsFragment();
    fragment.setContext(this);

    getFragmentManager().beginTransaction()
      .replace(android.R.id.content, fragment)
      .commit();
  }

  public static class SettingsFragment extends PreferenceFragment
                implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SettingsActivity context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.preferences);
    }

    public SettingsFragment() {
      super();
    }

    public void setContext(SettingsActivity context) {
      this.context = context;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
      updateSummary(preferences, key);

      if (key.equals(MainActivity.PREF_AUTOUPDATES)) {
        boolean autoUpdate = preferences.getBoolean(key, false);

        //Register/Unregister alarms for updates
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Uri uriAfterSchool = Uri.fromParts("homeworkUpdate", "afterSchool", "");
        Uri uriAfternoon = Uri.fromParts("homeworkUpdate", "afternoon", "");

        Intent intentAfterSchool = new Intent(MainActivity.ACTION_UPDATEHOMEWORK, uriAfterSchool,
           context, AutomaticUpdateService.class);
        Intent intentAfternoon = new Intent(MainActivity.ACTION_UPDATEHOMEWORK, uriAfternoon,
           context, AutomaticUpdateService.class); 

        PendingIntent piAfterSchool = PendingIntent.getService(context,
            1, intentAfterSchool, 0);
        PendingIntent piAfternoon = PendingIntent.getService(context,
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
      } else if (key.equals(MainActivity.PREF_AUTOREMINDERS)) {
        if (!preferences.getBoolean(MainActivity.PREF_AUTOUPDATES, false)
            && preferences.getBoolean(MainActivity.PREF_AUTOREMINDERS, false)) {
          DialogFragment warningDialog = new WarningNoAutoUpdatesDialog();
          warningDialog.show(context.getFragmentManager(), "warningNoAutoUpdatesDialog");
        }
      } else if (key.equals(MainActivity.PREF_REMINDERDAY) || key.equals(MainActivity.PREF_REMINDERTIME)) {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(MainActivity.PREF_SETREMINDERS);
        editor.commit();
      }
    }

    private void updateSummary(SharedPreferences preferences, String key) {
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
        if (autoUpdate) {
          updatePref.setSummary(getResources().getString(R.string.pref_autoupdates_summary_true));
        } else {
          updatePref.setSummary(getResources().getString(R.string.pref_autoupdates_summary));
        }
      } else if (key.equals(MainActivity.PREF_AUTOREMINDERS)) {
        Preference reminderPref = findPreference(key);
        boolean autoReminders = preferences.getBoolean(key, false);
        if (autoReminders) {
          reminderPref.setSummary(getResources().getString(R.string.pref_autoreminders_summary_true));
        } else {
          reminderPref.setSummary(getResources().getString(R.string.pref_autoreminders_summary));
        }
      } else if (key.equals(MainActivity.PREF_MOBILEDATA)) {
        Preference dataPref = findPreference(key);
        boolean useMobile = preferences.getBoolean(key, true);
        if (useMobile) {
          dataPref.setSummary(getResources().getString(R.string.pref_mobile_data_summary));
        } else {
          dataPref.setSummary(getResources().getString(R.string.pref_mobile_data_summary_false));
        }
      }
    }

    @Override
    public void onResume() {
      super.onResume();
      SharedPreferences preferences = getPreferenceScreen().getSharedPreferences();

      preferences.registerOnSharedPreferenceChangeListener(this);

      updateSummary(preferences, MainActivity.PREF_FILTERSUBJECTS);
      updateSummary(preferences, MainActivity.PREF_AUTOUPDATES);
      updateSummary(preferences, MainActivity.PREF_AUTOREMINDERS);
      updateSummary(preferences, MainActivity.PREF_MOBILEDATA);
    }

    @Override
    public void onPause() {
      super.onPause();
      getPreferenceScreen().getSharedPreferences()
        .unregisterOnSharedPreferenceChangeListener(this);
    }
  }

  public static class WarningNoAutoUpdatesDialog extends DialogFragment {

    public WarningNoAutoUpdatesDialog() {
      super();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setMessage(R.string.noAutoUpdatesDialogText)
        .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            //Just let the dialog close
          }
        });
      return builder.create();
    }

  }

}
