/*
 * Copyright (c) 2014 Sebastian Paarmann
 */

package paarmann.physikprofil;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

public class BootCompleteBroadcastReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF_NAME, 0);

    if (prefs.contains(MainActivity.PREF_SETREMINDERS)) {
      Set<String> setReminders = prefs.getStringSet(MainActivity.PREF_SETREMINDERS, null);
      for (Iterator<String> it = setReminders.iterator(); it.hasNext(); ) {
        String currentReminder = it.next();
        long when = Long.valueOf(currentReminder.split("\\\\")[0]);
        Uri uri = Uri.fromParts("homework", currentReminder, "");
        
        Intent alarmIntent = new Intent(MainActivity.ACTION_REMIND, uri, context, ReminderBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
          alarmManager.setExact(AlarmManager.RTC_WAKEUP, when, pendingIntent);
        } else {
          alarmManager.set(AlarmManager.RTC_WAKEUP, when, pendingIntent);
        }
      }
    }
  }

}
