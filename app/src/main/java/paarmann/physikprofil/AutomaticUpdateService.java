/*
 * Copyright (c) 2015  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package paarmann.physikprofil;

import android.app.IntentService;
import android.content.Intent;

import java.util.List;

public class AutomaticUpdateService extends IntentService
    implements HomeworkUpdater.OnHomeworkLoadedListener {

  public static final String TAG = "AutoamticUpdateService";

  private boolean done;

  public AutomaticUpdateService() {
    super("AutomaticUpdateService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    Log.i(TAG, "Starting update of homework data");

    done = false;
    HomeworkUpdater updater = new HomeworkUpdater(this);
    updater.setOnHomeworkLoadedListener(this);
    updater.downloadHomework();

    while (!done) {
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        // Just keep waiting
      }
    }

    Log.i(TAG, "Finished update of homework data");
  }

  @Override
  public void setData(List<HAElement> data) {
    done = true;
  }
}
