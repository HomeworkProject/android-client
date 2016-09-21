/*
 * Copyright (c) 2015  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package de.s_paarmann.homeworkapp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;

import java.util.List;

import de.s_paarmann.homeworkapp.ui.HomeworkDetailFragment;
import de.s_paarmann.homeworkapp.ui.MainActivity;

/**
 * {@code BroadcastReceiver} that shows a notification for the received Reminder, plays the users
 * notification sound and vibrates the device.
 */
public class ReminderBroadcastReceiver extends BroadcastReceiver {

  public static final String TAG = "ReminderBroadcastReceiver";

  @Override
  public void onReceive(Context context, Intent intent) {
    Uri data = intent.getData();

    Reminder reminder = Reminder.fromUri(context, data);

    Intent clickIntent = new Intent(context, MainActivity.class);
    clickIntent.putExtra(HomeworkDetailFragment.EXTRA_DATE, "all");
    clickIntent.putExtra(MainActivity.EXTRA_OPEN_VIEW, MainActivity.Views.HOMEWORK_DETAIL.name());

    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
    stackBuilder.addParentStack(MainActivity.class);
    stackBuilder.addNextIntent(clickIntent);

    PendingIntent
        pendingClickIntent =
        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

    String notificationText = "";
    List<HAElement> elements = reminder.getHAElements();
    for (int i = 0; i < elements.size(); i++) {
      notificationText += elements.get(i).title;
      if (i != elements.size() - 1) {
        notificationText += ", ";
      }
    }

    Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

    Notification.Builder builder =
        new Notification.Builder(context)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Hausaufgaben")
            .setContentText(notificationText)
            .setAutoCancel(true)
            .setContentIntent(pendingClickIntent)
            .setSound(soundUri);

    NotificationManager
        notificationManager =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    notificationManager.notify(reminder.getId(), builder.build());

    reminder.delete(context);

    AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL
      || audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
      Vibrator vib = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
      vib.vibrate(1000);
    }
  }

}
