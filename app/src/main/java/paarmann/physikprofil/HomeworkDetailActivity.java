/*
 * Copyright (c) 2014  Sebastian Paarmann
 */

package paarmann.physikprofil;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;


public class HomeworkDetailActivity extends Activity {

  public static String EXTRA_DATE = "paarmann.physikprofil.extra_date";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_homework_detail);

    loadHomework(getIntent().getStringExtra(EXTRA_DATE));
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.homework_detail, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();
    return super.onOptionsItemSelected(item);
  }

  private void loadHomework(String date) {
    boolean following = false;

    if (date.equals("all")) {
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.DAY_OF_MONTH, 1);
      int month = cal.get(Calendar.MONTH) + 1;
      date = cal.get(Calendar.YEAR) + "-" + month + "-" + cal.get(Calendar.DAY_OF_MONTH);
      following = true;
    }

    ListView listView = (ListView) findViewById(R.id.lsViewHomework);
    TextView emptyView = (TextView) findViewById(R.id.emptyView);
    listView.setEmptyView(emptyView);

    DownloadHATask task = new DownloadHATask();
    task.execute(date, String.valueOf(following), getResources().getString(R.string.server_uri));
  }

  private void setData(List<HAElement> data) {
    if (data.isEmpty()) {
      TextView emptyView = (TextView) findViewById(R.id.emptyView);
      emptyView.setText("Keine Hausaufgaben!");
    }
    ListView list = (ListView) findViewById(R.id.lsViewHomework);
    list.setAdapter(new HAElementArrayAdapter(this, data));
  }

  private class DownloadHATask extends AsyncTask<String, Void, List<HAElement>> {

    private HAElement errorElement = new HAElement();
    List<HAElement> errorList = new ArrayList<HAElement>();

    @Override
    protected List<HAElement> doInBackground(String... params) {
      errorElement.title = "";
      errorElement.subject = "";
      errorElement.date = "";

      String date = params[0];
      boolean following = Boolean.valueOf(params[1]);
      String server = params[2];

      String url = server + "/homework.php?date=" + date + (following ? "&following=true" : "");

      try {
        return downloadHA(url);
      } catch (IOException e) {
        Log.e("HomeworkDetailActivity.DownloadHATask", "ERROR: 1", e);
        errorElement.desc = "Es konnte keine Verbindung zum Server hergestellt werden.";
        errorList.add(errorElement);
        return errorList;
      }
    }

    @Override
    protected void onPostExecute(List<HAElement> result) {
      setData(result);
    }

    private List<HAElement> downloadHA(String myurl) throws IOException, ConnectException {
      InputStream is;

      Log.d("HomeworkDetailActivity.DownloadHATask", "URL to download HA: " + myurl);

      URL url = new URL(myurl);
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
//        try {
        element.date = properties.next();
        element.title = properties.next();
        element.subject = properties.next();
        element.desc = properties.next();
        homework.add(element);
//        } catch (NoSuchElementException e) {
//          Log.d("HomeworkDetailActivity.DownloadHATask", element.date + element.title + element.subject + element.desc);
//        }
      }

      return homework;
    }
  }


  public static class HAElement {

    public String date;
    public String title;
    public String subject;
    public String desc;
  }

}
