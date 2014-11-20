/*
 * Copyright (c) 2014 Sebastian Paarmann
 */

package paarmann.physikprofil;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Vibrator;
import android.widget.Toast;

import java.util.ArrayList;

public class ReminderBroadcastReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    Uri data = intent.getData();
	String ssp = data.getSchemeSpecificPart();

	ArrayList<HomeworkDetailActivity.HAElement> elements = new ArrayList<HomeworkDetailActivity.HAElement>();
    String[] strElements = ssp.split("\\\\");
	for (int i = 0; i < strElements.length; i++) {
      if (strElements[i] != null && strElements[i].length() != 0) {
        HomeworkDetailActivity.HAElement element = new HomeworkDetailActivity.HAElement();
		String[] parts = strElements[i].split("~");
		element.date = parts[0];
		element.title = parts[1];
		element.subject = parts[2];
		element.desc = parts[3];
		elements.add(element);
	  }
	}

    Notification.Builder builder =
		new Notification.Builder(context)
		.setSmallIcon(R.drawable.ic_launcher)
		.setContentTitle("Hausaufgaben")
		.setContentText(elements.get(0).title);

	NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    notificationManager.notify(0, builder.build());

	Vibrator vib = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
	vib.vibrate(1000);
  }

}
