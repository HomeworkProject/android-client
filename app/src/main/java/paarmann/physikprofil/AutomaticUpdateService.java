/*
 * Copyright (c) 2014 Sebastian Paarmann
 */

package paarmann.physikprofil;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static paarmann.physikprofil.HomeworkDetailActivity.HAElement;

public class AutomaticUpdateService extends IntentService implements HomeworkUpdater.OnHomeworkLoadedListener {

  private boolean done;

  public AutomaticUpdateService() {
    super("AutomaticUpdateService");
    Log.i("AutomaticUpdateService", "New AutomaticUpdateService constructed");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    Log.i("AutomaticUpdateService", "Starting update of homework data");

    try {
      FileOutputStream fos = openFileOutput("serviceCalled", Context.MODE_PRIVATE);
      fos.write(1);
      fos.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    done = false;
    HomeworkUpdater updater = new HomeworkUpdater(this);
    updater.setOnHomeworkLoadedListener(this);
    updater.downloadHomework();

    while (!done) {
      try {
        wait(200);
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
