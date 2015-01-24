/*
 * Copyright (c) 2015  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package paarmann.physikprofil;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

public class HomeworkUpdater {

  public static final String TAG = "HomeworkUpdater";

  public static String HOMEWORK_FILE = "homework.ser";

  public interface OnHomeworkLoadedListener {

    public void setData(List<HAElement> data);
  }

  private Context context;
  private OnHomeworkLoadedListener listener;

  public HomeworkUpdater(Context context) {
    this.context = context;
  }

  public void setOnHomeworkLoadedListener(OnHomeworkLoadedListener listener) {
    this.listener = listener;
  }

  public void getData() {
    getData(false);
  }

  public void getData(boolean forceDownload) {
    Date now = new Date();
    SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF_NAME, 0);
    Date lastUpdated = new Date(prefs.getLong(MainActivity.PREF_LASTUPDATED, 0));
    long diffMinutes = getDateDiff(lastUpdated, now, TimeUnit.MINUTES);

    if (diffMinutes >= 90 || forceDownload) {
      //Last updated longer than 90 minutes ago
      downloadHomework();
    } else {
      loadHomeworkFromFile();
    }
  }

  public void downloadHomework() {
    Log.i(TAG, "Downloading homework");
    DownloadTask task = new DownloadTask();
    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
  }

  public void loadHomeworkFromFile() {
    Log.i(TAG, "Loading homework from file");
    FileTask task = new FileTask();
    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, false);
  }

  /**
   * Get a diff between two dates
   *
   * @param date1    the oldest date
   * @param date2    the newest date
   * @param timeUnit the unit in which you want the diff
   * @return the diff value, in the provided unit
   */
  public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
    long diffInMillies = date2.getTime() - date1.getTime();
    return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
  }

  private boolean saveHomeworkToFile(List<HAElement> data) {
    ArrayList<HAElement> homework = new ArrayList<HAElement>();
    homework.addAll(data);
    try {
      FileOutputStream fos = context.openFileOutput(HOMEWORK_FILE, Context.MODE_PRIVATE);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(homework);
      oos.close();
    } catch (IOException e) {
      e.printStackTrace();
      Log.e(TAG, "Failed to save homework", e);
      return false;
    }
    return true;
  }

  private class FileTask extends AsyncTask<Boolean, Void, List<HAElement>> {

    private boolean triedDownload;

    @Override
    protected List<HAElement> doInBackground(Boolean... params) {
      triedDownload = params[0];
      try {
        FileInputStream fis = context.openFileInput(HOMEWORK_FILE);
        ObjectInputStream ois = new ObjectInputStream(fis);
        Object readObject = ois.readObject();
        ois.close();

        if (readObject != null && readObject instanceof ArrayList) {
          return (ArrayList<HAElement>) readObject;
        }
      } catch (IOException e) {
        Log.e(TAG, "Failed to load homework from file", e);
      } catch (ClassNotFoundException e) {
        Log.e(TAG, "Failed to load homework from file", e);
      }
      Log.e(TAG, "ERROR reading list from file.");
      return null;
    }

    @Override
    protected void onPostExecute(List<HAElement> result) {
      if (result != null) {
        if (triedDownload) {
          HAElement warning = new HAElement();
          warning.id = 0;
          warning.flags = HAElement.FLAG_WARN;
          warning.date = "";
          warning.title = "Achtung";
          warning.subject = "";
          warning.desc =
              "Die Hausaufgaben konnten nicht neu heruntergeladen werden, diese Daten könnten veraltet sein.";
          result.add(0, warning);
        }
        AutomaticReminderManager.setReminders(context, result);
        if (listener != null) {
          listener.setData(result);
        }
      } else {
        if (!triedDownload) {
          downloadHomework();
        } else {
          result = new ArrayList<HAElement>();
          HAElement error = new HAElement();
          error.id = 0;
          error.flags = HAElement.FLAG_ERROR;
          error.date = "";
          error.title = "Fehler";
          error.subject = "";
          error.desc =
              "Die Hausaufgaben konnten weder heruntergeladen werden noch konnten gespeicherte Daten verwendet werden.";
          result.add(error);
          if (listener != null) {
            listener.setData(result);
          }
        }
      }
    }
  }

  private class DownloadTask extends AsyncTask<Void, Void, List<HAElement>> {

    private HAElement errorElement = new HAElement();
    List<HAElement> errorList = new ArrayList<HAElement>();
    boolean error = false;

    @Override
    protected List<HAElement> doInBackground(Void... params) {
      errorElement.title = "";
      errorElement.subject = "";
      errorElement.date = "";

      try {
        return downloadHA();
      } catch (IOException e) {
        /*errorElement.desc = "Es konnte keine Verbindung zum Server hergestellt werden.";
        errorList.add(errorElement);
        return errorList;*/
        error = true;
        return null;
      }
    }

    @Override
    protected void onPostExecute(List<HAElement> result) {
      if (error) {
        Log.i(TAG, "Could not download homework, instead loading from file.");
        FileTask task = new FileTask();
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, true);
        return;
      }
      AutomaticReminderManager.setReminders(context, result);
      saveHomeworkToFile(result);
      if (listener != null) {
        listener.setData(result);
      }

      Date now = new Date();
      SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF_NAME, 0);
      SharedPreferences.Editor editor = prefs.edit();
      editor.putLong(MainActivity.PREF_LASTUPDATED, now.getTime());
      editor.commit();
    }

    private List<HAElement> downloadHA() throws IOException, ConnectException {
      SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
      boolean useMobile = settings.getBoolean(MainActivity.PREF_MOBILEDATA, true);
      boolean mobileActive = false;

      ConnectivityManager
          cm =
          (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo netinfo = cm.getActiveNetworkInfo();
      if (netinfo != null && netinfo.getTypeName().equalsIgnoreCase("MOBILE")) {
        mobileActive = true;
      }

      if (!useMobile && mobileActive) {
        throw new IOException("User chose to not use mobile data");
      }

      InputStream is;

      String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
      String strUrl = context.getResources().getString(R.string.server_uri)
                      + "/homework.php?date=" + date + "&following=true";
      URL url = new URL(strUrl);
      HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
      conn.setReadTimeout(10000);
      conn.setConnectTimeout(15000);
      conn.setRequestMethod("GET");
      conn.setDoInput(true);
      conn.connect();

      is = conn.getInputStream();

      String serverResponse = IOUtils.toString(is, "windows-1252");

      return HAElement.createFromSsp(serverResponse);
    }
  }

}
