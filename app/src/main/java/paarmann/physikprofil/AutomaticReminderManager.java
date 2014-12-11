/*
 * Copyright (c) 2014 Sebastian Paarmann
 */

package paarmann.physikprofil;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

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

  public static void setReminders(Context context, List<HAElement> homework) {
    SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF_NAME, 0);
    Set<String> setReminders = new HashSet<String>();
    
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    String chosenSubjects = settings.getString(MainActivity.PREF_CHOSENSUBJECTS, "");
    List<String> displayedSubjects = Arrays.asList(chosenSubjects.split("\n"));

    if (prefs.contains(MainActivity.PREF_SETREMINDERS)) {
      setReminders.addAll(prefs.getStringSet(MainActivity.PREF_SETREMINDERS, null));
    }

    for (HAElement element : homework) {
      Date when = null;
      try {
        when = new SimpleDateFormat("yyyy-MM-dd").parse(element.date);
        Calendar cal = Calendar.getInstance();
        cal.setTime(when);
        cal.add(Calendar.DAY_OF_MONTH, -1);
        cal.set(Calendar.HOUR_OF_DAY, 14);
        cal.set(Calendar.MINUTE, 00);
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
        + element.title + "~"
        + element.subject + "~"
        + element.desc + "\\";

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
      
      SharedPreferences.Editor editor = prefs.edit();
      editor.putStringSet(MainActivity.PREF_SETREMINDERS, setReminders);
      editor.commit();
    }
  }

}
