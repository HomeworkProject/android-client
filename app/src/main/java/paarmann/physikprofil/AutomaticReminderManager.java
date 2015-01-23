/*
 * Copyright (c) 2015  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package paarmann.physikprofil;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static paarmann.physikprofil.HomeworkDetailActivity.HAElement;

public abstract class AutomaticReminderManager {

  public static final String TAG = "AutomaticReminderManager";

  public static void setReminders(Context context, List<HAElement> homework) {
    SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF_NAME, 0);
    Set<String> setReminders = new HashSet<String>();
    Set<String> doneItems;
    
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    if (!settings.getBoolean(MainActivity.PREF_AUTOREMINDERS, false)) {
      return;
    }

    String chosenSubjects = settings.getString(MainActivity.PREF_CHOSENSUBJECTS, "");
    List<String> displayedSubjects = Arrays.asList(chosenSubjects.split("\n"));

    if (prefs.contains(MainActivity.PREF_SETREMINDERS)) {
      setReminders.addAll(prefs.getStringSet(MainActivity.PREF_SETREMINDERS, null));
    }

    doneItems = prefs.getStringSet(MainActivity.PREF_DONEITEMS, null);
    if (doneItems == null) {
      doneItems = new HashSet<String>();
    }

    // Clean up the deleted reminder data
    Set<String> deletedReminders = new HashSet<String>();
    if (prefs.contains(MainActivity.PREF_DELETEDREMINDERS)) {
      deletedReminders.addAll(prefs.getStringSet(MainActivity.PREF_DELETEDREMINDERS, null));
    }
    for (String ssp : deletedReminders) {
      Date when = new Date();
      when.setTime(Long.valueOf(ssp.split("\\\\")[0]));
      if (HomeworkUpdater.getDateDiff(new Date(), when, TimeUnit.MILLISECONDS) < 0) {
        deletedReminders.remove(ssp);
      }
    }

    for (HAElement element : homework) {
      if (element.subject == null || element.subject.equals("")) {
        continue;
      }

      if (doneItems.contains(element.id + "~" + element.title)) {
        continue;
      }

      Date when = null;
      try {
        when = new SimpleDateFormat("yyyy-MM-dd").parse(element.date);
        Calendar reminderTime = Calendar.getInstance();
        reminderTime.setTimeInMillis(settings.getLong(MainActivity.PREF_REMINDERTIME, 1420290000000L));
        Calendar cal = Calendar.getInstance();
        cal.setTime(when);
        cal.add(Calendar.DAY_OF_MONTH, -settings.getInt(MainActivity.PREF_REMINDERDAY, 1));
        cal.set(Calendar.HOUR_OF_DAY, reminderTime.get(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, reminderTime.get(Calendar.MINUTE));
        when = cal.getTime();
      } catch (ParseException e) {
        throw new RuntimeException("The date '" + element.date + "' could not be parsed");
      }

      if (HomeworkUpdater.getDateDiff(new Date(), when, TimeUnit.MILLISECONDS) < 0) {
        continue;
      }
      
      String scheme = "homework";
      String ssp = when.getTime() + "\\";
      ssp += element.id + "~"
        + element.date + "~"
        + element.title + " [Automatisch]" + "~"
        + element.subject + "~"
        + element.desc + "\\";

      // Check if reminder was already deleted once, don't re-create it if it was
      if (deletedReminders.contains(ssp)) {
        continue;
      }

      boolean filterSubjects = settings.getBoolean(MainActivity.PREF_FILTERSUBJECTS, false);
      if (!setReminders.contains(ssp) && (displayedSubjects.contains(element.subject) || !filterSubjects)) {
        Uri uri = Uri.fromParts(scheme, ssp, "");

        Intent intent = new Intent(MainActivity.ACTION_REMIND, uri, context, ReminderBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
          alarmManager.setExact(AlarmManager.RTC_WAKEUP, when.getTime(), pendingIntent);
        } else {
          alarmManager.set(AlarmManager.RTC_WAKEUP, when.getTime(), pendingIntent);
        }

        setReminders.add(ssp);
      }
    }

    SharedPreferences.Editor editor = prefs.edit();
    editor.putStringSet(MainActivity.PREF_SETREMINDERS, setReminders);
    editor.putStringSet(MainActivity.PREF_DELETEDREMINDERS, deletedReminders);
    editor.commit();
  }

  public static void deleteAutomaticReminders(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF_NAME, 0);
    Set<String> setReminders = new HashSet<String>();
    Set<String> leftReminders = new HashSet<String>();

    if (prefs.contains(MainActivity.PREF_SETREMINDERS)) {
      setReminders.addAll(prefs.getStringSet(MainActivity.PREF_SETREMINDERS, null));
    }

    for (String ssp : setReminders) {
      String[] parts = ssp.split("~");
      String title = parts[2];
      if (!title.endsWith(" [Automatisch]")) {
        leftReminders.add(ssp);
      }
    }

    SharedPreferences.Editor editor = prefs.edit();
    editor.putStringSet(MainActivity.PREF_SETREMINDERS, leftReminders);
    editor.commit();
  }

  public static void deleteAutomaticReminder(Context context,
                                             HAElement element) {
    SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF_NAME, 0);
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    Set<String> setReminders = new HashSet<String>();

    if (prefs.contains(MainActivity.PREF_SETREMINDERS)) {
      setReminders.addAll(prefs.getStringSet(MainActivity.PREF_SETREMINDERS, null));
    }

    Date when = null;
    try {
      when = new SimpleDateFormat("yyyy-MM-dd").parse(element.date);
      Calendar reminderTime = Calendar.getInstance();
      reminderTime.setTimeInMillis(settings.getLong(MainActivity.PREF_REMINDERTIME, 1420290000000L));
      Calendar cal = Calendar.getInstance();
      cal.setTime(when);
      cal.add(Calendar.DAY_OF_MONTH, -settings.getInt(MainActivity.PREF_REMINDERDAY, 1));
      cal.set(Calendar.HOUR_OF_DAY, reminderTime.get(Calendar.HOUR_OF_DAY));
      cal.set(Calendar.MINUTE, reminderTime.get(Calendar.MINUTE));
      when = cal.getTime();
    } catch (ParseException e) {
      throw new RuntimeException("The date '" + element.date + "' could not be parsed");
    }

    String scheme = "homework";
    String ssp = when.getTime() + "\\";
    ssp += element.id + "~"
      + element.date + "~"
      + element.title + " [Automatisch]" + "~"
      + element.subject + "~"
      + element.desc + "\\";

    setReminders.remove(ssp);

    SharedPreferences.Editor editor = prefs.edit();
    editor.putStringSet(MainActivity.PREF_SETREMINDERS, setReminders);
    editor.commit();
  }
}
