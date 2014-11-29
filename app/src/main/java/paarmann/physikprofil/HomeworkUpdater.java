/*
 * Copyright (c) 2014 Sebastian Paarmann
 */

package paarmann.physikprofil;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import static paarmann.physikprofil.HomeworkDetailActivity.HAElement;

public class HomeworkUpdater {

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
    Date now = new Date();
    SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF_NAME, 0);
    Date lastUpdated = new Date(prefs.getLong(MainActivity.PREF_LASTUPDATED, 0));
    long diffMinutes = getDateDiff(lastUpdated, now, TimeUnit.MINUTES);

    if (diffMinutes >= 90) {
      //Last updated longer than 90 minutes ago
      downloadHomework();
    } else {
      loadHomeworkFromFile();
    }
  }

  public void downloadHomework() {
    DownloadTask task = new DownloadTask();
    task.execute();
  }

  public void loadHomeworkFromFile() {
    //FileTask task = new FileTask();
    //task.execute();
  }

  /**
   * Get a diff between two dates
   * @param date1 the oldest date
   * @param date2 the newest date
   * @param timeUnit the unit in which you want the diff
   * @return the diff value, in the provided unit
   */
  public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
    long diffInMillies = date2.getTime() - date1.getTime();
    return timeUnit.convert(diffInMillies,TimeUnit.MILLISECONDS);
  }

  private class DownloadTask extends AsyncTask<Void, Void, List<HAElement>> {
    private HAElement errorElement = new HAElement();
    List<HAElement> errorList = new ArrayList<HAElement>();

    @Override
    protected List<HAElement> doInBackground(Void... params) {
      errorElement.title = "";
      errorElement.subject = "";
      errorElement.date = "";

      try {
        return downloadHA();
      } catch (IOException e) {
        errorElement.desc = "Es konnte keine Verbindung zum Server hergestellt werden.";
        errorList.add(errorElement);
        return errorList;
      }
    }

    @Override
    protected void onPostExecute(List<HAElement> result) {
      if (listener != null) {
        listener.setData(result);
      }
    }

    private List<HAElement> downloadHA() throws IOException, ConnectException {
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

      List<HAElement> homework = new ArrayList<HAElement>();

      Scanner elements = new Scanner(serverResponse);
      elements.useDelimiter("\\\\");
      while (elements.hasNext()) {
        HAElement element = new HAElement();
        Scanner properties = new Scanner(elements.next());
        properties.useDelimiter("~");
        if (!properties.hasNext()) {
          continue;
        }
        int id = Integer.valueOf(properties.next());
        if (id == 0) {
          element.date = "";
          element.title = "Keine Hausaufgaben!";
          element.subject = "";
          element.desc = "Wir haben keine Hausaufgaben!";
          homework.add(element);
          continue;
        }
        element.id = id;
        element.date = properties.next();
        element.title = properties.next();
        element.subject = properties.next();
        element.desc = properties.next();
        homework.add(element);
      }
      return homework;
    }
  }

}
