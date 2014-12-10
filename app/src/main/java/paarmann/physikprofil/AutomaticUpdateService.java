/*
 * Copyright (c) 2014 Sebastian Paarmann
 */

package paarmann.physikprofil;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;

import static paarmann.physikprofil.HomeworkDetailActivity.HAElement;

public class AutomaticUpdateService extends IntentService implements HomeworkUpdater.OnHomeworkLoadedListener {

  private boolean done;

  public AutomaticUpdateService() {
    super("AutomaticUpdateService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    Log.i("AutomaticUpdateService", "Starting update of homework data");

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

    Log.i("AutomaticUpdateService", "Finished update of homework data");
  }

  @Override
  public void setData(List<HAElement> data) {
    done = true;
  }
}
