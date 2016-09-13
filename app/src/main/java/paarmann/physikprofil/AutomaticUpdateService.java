/*
 * Copyright (c) 2015  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package paarmann.physikprofil;

import android.app.IntentService;
import android.content.Intent;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import paarmann.physikprofil.network.HomeworkManager;

/**
 * Service that updates homework data in the background.
 */
public class AutomaticUpdateService extends IntentService {

  public static final String TAG = "AutomaticUpdateService";

  private boolean done;

  public AutomaticUpdateService() {
    super("AutomaticUpdateService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    Log.i(TAG, "Starting update of homework data");

    done = false;

    Calendar cal = Calendar.getInstance();
    Date startDate = cal.getTime();
    cal.add(Calendar.DAY_OF_MONTH, 64); // Server limits to 64 days
    Date endDate = cal.getTime();
    HomeworkManager.getHomework(this, startDate, endDate, (hw, loginResult) -> {
      done = true;
    }, true);

    while (!done) {
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        // Just keep waiting
      }
    }

    Log.i(TAG, "Finished update of homework data");
  }

}
