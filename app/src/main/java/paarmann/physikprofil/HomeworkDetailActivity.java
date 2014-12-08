/*
 * Copyright (c) 2014  Sebastian Paarmann
 */

package paarmann.physikprofil;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URL;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;


public class HomeworkDetailActivity extends Activity {

  public static String EXTRA_DATE = "paarmann.physikprofil.extra_date";

  private DialogFragment reminderDialog;
  private ActionMode mActionMode;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_homework_detail);

    final ListView listView = (ListView) findViewById(R.id.lsViewHomework);
    listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
    listView.setMultiChoiceModeListener(new ListView.MultiChoiceModeListener() {
      @Override
      public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                                            boolean checked) {
      }
      @Override
      public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
          case R.id.action_copy:
            copyCurrentItems();
            mode.finish();
            return true;
          case R.id.action_remind:
            setNewReminder();
            mode.finish();
            return true;
          default:
            return false;
        }
      }
      @Override
      public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.detail_context_menu, menu);
        mActionMode = mode;
        return true;
      }
      @Override
      public void onDestroyActionMode(ActionMode mode) {
        mActionMode = null;
      }
      @Override
      public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
      }
    });
    listView.setOnItemLongClickListener(new OnItemLongClickListener() {
      public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        listView.setItemChecked(position, true);
        return true;
      }
    });

    loadHomework(getIntent().getStringExtra(EXTRA_DATE));
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    if (reminderDialog != null) {
      reminderDialog.dismiss();
    }
    if (mActionMode != null) {
      mActionMode.finish();
    }
    super.onSaveInstanceState(outState);
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

  private void copyCurrentItems() {
    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    ListView listView = (ListView) findViewById(R.id.lsViewHomework);
    SparseBooleanArray items = listView.getCheckedItemPositions();
    String toCopy = "";
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i)) {
        View listItem = listView.getChildAt(i);
        TextView desc = (TextView) listItem.findViewById(R.id.textDesc);
        toCopy += desc.getText() + (items.size() == 1 ? "" : "\n\n");
      }
    }
    ClipData data = ClipData.newPlainText("homework", toCopy);
    clipboard.setPrimaryClip(data);
    Toast.makeText(this, (items.size() == 1 ? "Eintrag" : "Einträge") + " in die Zwischenablage kopiert", 1000).show();
  }

  private void setNewReminder() {
    reminderDialog = ReminderDateTimePickerFragment.newInstance(getSelectedListItems());
    reminderDialog.show(getFragmentManager(), "reminderDateTimePickerFragment");
  }

  private ArrayList<HAElement> getSelectedListItems() {
    ListView listView = (ListView) findViewById(R.id.lsViewHomework);
    SparseBooleanArray selected = listView.getCheckedItemPositions();
    ArrayList<HAElement> selectedItems = new ArrayList<HAElement>();
    for (int i = 0; i < selected.size(); i++) {
      if (selected.valueAt(i)) {
        selectedItems.add((HAElement) listView.getItemAtPosition(selected.keyAt(i)));
      }
    }
    return selectedItems;
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

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    boolean filter = prefs.getBoolean(MainActivity.PREF_FILTERSUBJECTS, false);
    String chosenSubjects = prefs.getString(MainActivity.PREF_CHOSENSUBJECTS, "");
    
    List<HAElement> filteredData;

    if (filter) {
      List<HAElement> displayedObjects = new ArrayList<HAElement>();
      List<String> displayedSubjects = Arrays.asList(chosenSubjects.split("\n"));

      for (int i = 0; i < displayedSubjects.size(); i++) {
        Log.d("Homework", displayedSubjects.get(i));
      }

      for (int i = 0; i < data.size(); i++) {
        if (displayedSubjects.contains(data.get(i).subject)) {
          displayedObjects.add(data.get(i));
          Log.d("Homework", "Adding " + data.get(i).title + " to displayedObjects");
        } else {
          Log.d("Homework", "Not adding " + data.get(i).title + " with subject " + data.get(i).subject);
        }
      }

      filteredData = displayedObjects;
    } else {
      filteredData = data;
    }

    ListView list = (ListView) findViewById(R.id.lsViewHomework);
    list.setAdapter(new HAElementArrayAdapter(this, filteredData));
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
        element.id = id;
        element.date = properties.next();
        element.title = properties.next();
        element.subject = properties.next().trim();
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

    public int id;
    public String date;
    public String title;
    public String subject;
    public String desc;
  }

}
