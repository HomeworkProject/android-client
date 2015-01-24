/*
 * Copyright (c) 2015  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package paarmann.physikprofil;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ReminderBroadcastReceiver extends BroadcastReceiver {

  public static final String TAG = "ReminderBroadcastReceiver";

  @Override
  public void onReceive(Context context, Intent intent) {
    Uri data = intent.getData();
    String ssp = data.getSchemeSpecificPart();

    SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF_NAME, 0);
    SharedPreferences.Editor editor = prefs.edit();
    Set<String> setReminders = new HashSet<String>();
    setReminders.addAll(prefs.getStringSet(MainActivity.PREF_SETREMINDERS, null));
    setReminders.remove(ssp);
    editor.putStringSet(MainActivity.PREF_SETREMINDERS, setReminders);
    editor.commit();

    ArrayList<HAElement> elements = new ArrayList<HAElement>();
    String[] strElements = ssp.split("\\\\");
    for (int i = 1; i < strElements.length; i++) {
      if (strElements[i] != null && strElements[i].length() != 0) {
        HAElement element = new HAElement();
        String[] parts = strElements[i].split("~");
        element.id = Integer.valueOf(parts[0]);
        element.date = parts[1];
        element.title = parts[2];
        element.subject = parts[3];
        element.desc = parts[4];
        elements.add(element);
      }
    }

    Intent clickIntent = new Intent(context, HomeworkDetailActivity.class);
    clickIntent.putExtra(HomeworkDetailActivity.EXTRA_DATE, "all");

    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
    stackBuilder.addParentStack(HomeworkDetailActivity.class);
    stackBuilder.addNextIntent(clickIntent);

    PendingIntent
        pendingClickIntent =
        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

    String notificationText = "";
    int notificationId = 0;
    for (int i = 0; i < elements.size(); i++) {
      notificationText += elements.get(i).title;
      notificationId += elements.get(i).id; //Not guaranteed to always be unique,
      //but convenient and extremely unlikely
      //to cause problems
      if (i != elements.size() - 1) {
        notificationText += ", ";
      }
    }

    Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

    Notification.Builder builder =
        new Notification.Builder(context)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Hausaufgaben")
            .setContentText(notificationText)
            .setAutoCancel(true)
            .setContentIntent(pendingClickIntent)
            .setSound(soundUri);

    NotificationManager
        notificationManager =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    notificationManager.notify(notificationId, builder.build());

    Vibrator vib = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    vib.vibrate(1000);
  }

}
