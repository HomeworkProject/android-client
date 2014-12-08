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
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;


public class HomeworkDetailActivity extends Activity implements HomeworkUpdater.OnHomeworkLoadedListener {

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

    loadHomework();
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

  private void loadHomework() {
    TextView emptyView = (TextView) findViewById(R.id.emptyView);
    ListView listView = (ListView) findViewById(R.id.lsViewHomework);
    listView.setEmptyView(emptyView);
    HomeworkUpdater loader = new HomeworkUpdater(this);
    loader.setOnHomeworkLoadedListener(this);
    loader.getData();
  }

  @Override
  public void setData(List<HAElement> data) {
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

      for (int i = 0; i < data.size(); i++) {
        if (displayedSubjects.contains(data.get(i).subject)) {
          displayedObjects.add(data.get(i));
        }
      }

      filteredData = displayedObjects;
    } else {
      filteredData = data;
    }

    List<HAElement> selectedData = new ArrayList<HAElement>();
    String strDate = getIntent().getStringExtra(EXTRA_DATE);
    boolean all;
    Date date = new Date();

    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    if (strDate.equals("all")) {
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.DAY_OF_MONTH, 1);
      date = cal.getTime();
      all = true;
    } else {
      try {
        date = dateFormatter.parse(strDate);
      } catch (ParseException e) {
        Log.wtf("HomeworkParsing", "Invalid date format: ", e);
      }
      all = false;
    }
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    cal.set(Calendar.HOUR_OF_DAY, 00);
    cal.set(Calendar.MINUTE, 00);
    cal.set(Calendar.SECOND, 00);
    cal.set(Calendar.MILLISECOND, 00);

    date = cal.getTime();

    for (int i = 0; i < filteredData.size(); i++) {
      Date elemDate = new Date();
      try {
        elemDate = dateFormatter.parse(filteredData.get(i).date);
      } catch (ParseException e) {
        Log.e("Homework", "Failed parsing homework date: ", e);
        continue;
      }

      if (all) {
        if (elemDate.getTime() >= date.getTime()) {
          selectedData.add(filteredData.get(i));
        }
      } else {
        Calendar elemCal = Calendar.getInstance();
        elemCal.setTime(elemDate);
        if (elemCal.get(Calendar.MONTH) == cal.get(Calendar.MONTH) &&
            elemCal.get(Calendar.DAY_OF_MONTH) == cal.get(Calendar.DAY_OF_MONTH)) {
          selectedData.add(filteredData.get(i));
        }
      }

    }

    if (selectedData.size() == 0) {
      HAElement noHomework = new HAElement();
      noHomework.date = "";
      noHomework.title = "Keine Hausaufgaben!";
      noHomework.subject = "";
      noHomework.desc = "Wir haben keine Hausaufgaben!";
      selectedData.add(noHomework);
    }

    ListView list = (ListView) findViewById(R.id.lsViewHomework);
    list.setAdapter(new HAElementArrayAdapter(this, selectedData));
  }

  public static class HAElement implements java.io.Serializable {
    private static final long serialVersionUID = 0L;
    public int id;
    public String date;
    public String title;
    public String subject;
    public String desc;
  }

}
