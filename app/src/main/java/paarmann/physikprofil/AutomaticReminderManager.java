/*
 * Copyright (c) 2015  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package paarmann.physikprofil;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public abstract class AutomaticReminderManager {

  public static final String TAG = "AutomaticReminderManager";

  public static void setReminders(Context context, List<HAElement> homework) {
    SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF_NAME, 0);
    Set<Reminder> setReminders = Reminder.loadSavedReminders(context);
    Set<String> doneItems;

    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    if (!settings.getBoolean(MainActivity.PREF_AUTOREMINDERS, false)) {
      return;
    }

    String chosenSubjects = settings.getString(MainActivity.PREF_CHOSENSUBJECTS, "");
    List<String> displayedSubjects = Arrays.asList(chosenSubjects.split("\n"));

    doneItems = prefs.getStringSet(MainActivity.PREF_DONEITEMS, null);
    if (doneItems == null) {
      doneItems = new HashSet<String>();
    }

    Reminder.cleanDeletedReminders(context);

    for (HAElement element : homework) {
      if (element.subject == null || element.subject.equals("")) {
        continue;
      }

      //TODO mode doneItems to new system
      if (doneItems.contains(element.id + "~" + element.title)) {
        continue;
      }

      Date when = null;
      try {
        when = element.getDate();
        Calendar reminderTime = Calendar.getInstance();
        reminderTime
            .setTimeInMillis(settings.getLong(MainActivity.PREF_REMINDERTIME, 1420290000000L));
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

      List<HAElement> currElement = new ArrayList<HAElement>();
      currElement.add(element);

      Reminder reminder = new Reminder(when, currElement);
      reminder.flags = Reminder.FLAG_AUTO;

      // Check if reminder was already deleted once, don't re-create it if it was
      if (reminder.wasDeleted(context)) {
        continue;
      }

      boolean filterSubjects = settings.getBoolean(MainActivity.PREF_FILTERSUBJECTS, false);
      if (!setReminders.contains(reminder) && (displayedSubjects.contains(element.subject)
                                               || !filterSubjects)) {
        Uri uri = reminder.toUri();

        Intent
            intent =
            new Intent(MainActivity.ACTION_REMIND, uri, context, ReminderBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
          alarmManager.setExact(AlarmManager.RTC_WAKEUP, when.getTime(), pendingIntent);
        } else {
          alarmManager.set(AlarmManager.RTC_WAKEUP, when.getTime(), pendingIntent);
        }

        reminder.save(context);
      }
    }
  }

  public static void deleteAutomaticReminders(Context context) {
    Set<Reminder> reminders = Reminder.loadSavedReminders(context);
    for (Reminder reminder : reminders) {
      if ((reminder.flags & Reminder.FLAG_AUTO) == Reminder.FLAG_AUTO) {
        reminder.delete(context);
      }
    }
  }

  public static void deleteAutomaticReminder(Context context,
                                             HAElement element) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    Date when = null;
    try {
      when = element.getDate();
      Calendar reminderTime = Calendar.getInstance();
      reminderTime
          .setTimeInMillis(settings.getLong(MainActivity.PREF_REMINDERTIME, 1420290000000L));
      Calendar cal = Calendar.getInstance();
      cal.setTime(when);
      cal.add(Calendar.DAY_OF_MONTH, -settings.getInt(MainActivity.PREF_REMINDERDAY, 1));
      cal.set(Calendar.HOUR_OF_DAY, reminderTime.get(Calendar.HOUR_OF_DAY));
      cal.set(Calendar.MINUTE, reminderTime.get(Calendar.MINUTE));
      when = cal.getTime();
    } catch (ParseException e) {
      throw new RuntimeException("The date '" + element.date + "' could not be parsed");
    }

    List<HAElement> currElement = new ArrayList<HAElement>();
    currElement.add(element);

    Reminder reminder = new Reminder(when, currElement);
    reminder.flags = Reminder.FLAG_AUTO;

    reminder.delete(context);
  }
}
