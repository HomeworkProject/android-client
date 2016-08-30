/*
 * Copyright (c) 2015  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package paarmann.physikprofil;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import de.mlessmann.api.data.IHWFuture;
import de.mlessmann.api.data.IHWObj;
import de.mlessmann.api.main.HWMgr;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import paarmann.physikprofil.network.LoginManager;
import paarmann.physikprofil.network.LoginResultListener;

/**
 * Displays a list of homework elements and provides an interface for various actions on them.
 * Users can set reminders, mark homework as done/not done and copy them to the clipboard.
 * Data is provided by {@link HomeworkUpdater}.
 */
public class HomeworkDetailActivity extends Activity
    implements HomeworkUpdater.OnHomeworkLoadedListener {

  public static final String TAG = "HomeworkDetailActivity";

  /** Extra for specifying the date. Can be {@code 'all'} or a date in yyyy-mm-DD  */
  public static String EXTRA_DATE = "paarmann.physikprofil.extra_date";

  private DialogFragment reminderDialog;
  private ActionMode mActionMode;

  private HWMgr hwmgr;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_homework_detail);

    hwmgr = new HWMgr();

    final ListView listView = (ListView) findViewById(R.id.lsViewHomework);
    listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
    listView.setMultiChoiceModeListener(new ListView.MultiChoiceModeListener() {
      // All items currently selected
      private List<Integer> checkedItems = new ArrayList<Integer>();

      // If true, selected items should be marked as done when the done button is pressed.
      // If false, instead mark them as not done if they aren't already.
      private boolean markItemsDone = true;

      @Override
      public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                                            boolean checked) {
        if (checked) {
          checkedItems.add(position);
        } else {
          checkedItems.remove(checkedItems.indexOf(position));
        }

        mode.invalidate();
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
          case R.id.action_done:
            if (markItemsDone) {
              markCurrentItemsAsDone();
            } else {
              markCurrentItemsAsNotDone();
            }
            loadHomework();
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
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREF_NAME, 0);
        Set<String>
            doneItems =
            prefs.getStringSet(MainActivity.PREF_DONEITEMS2, new HashSet<String>());
        boolean unmarkItems = false;
        for (int position : checkedItems) {
          // Check if item is already marked as done, see also markItemsDone. (Done items are not yet
          // using the new HAElement system)
          HAElement element = (HAElement) listView.getItemAtPosition(position);
          if (doneItems.contains(element.id)) {
            unmarkItems = true;
            break;
          }
        }

        MenuItem item = menu.findItem(R.id.action_done);
        if (unmarkItems) {
          item.setIcon(R.drawable.ic_action_cancel);
          item.setTitle("Als nicht erledigt markieren");
          markItemsDone = false;
        } else {
          item.setIcon(R.drawable.ic_action_edit);
          item.setTitle(R.string.action_done);
          markItemsDone = true;
        }
        return true;
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
    switch (id) {
      case R.id.action_refresh:
        loadHomework(true);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  /**
   * Copies the description of all selected items to the clipboard and displays a toast.
   */
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
    Toast.makeText(this,
                   (items.size() == 1 ? "Eintrag" : "Einträge") + " in die Zwischenablage kopiert",
                   Toast.LENGTH_SHORT).show();
  }

  private void setNewReminder() {
    reminderDialog = ReminderDateTimePickerFragment.newInstance(getSelectedListItems());
    reminderDialog.show(getFragmentManager(), "reminderDateTimePickerFragment");
  }

  private void markCurrentItemsAsDone() {
    List<HAElement> selectedItems = getSelectedListItems();
    Set<String> doneItems = new HashSet<String>();
    SharedPreferences prefs = getSharedPreferences(MainActivity.PREF_NAME, 0);

    if (prefs.contains(MainActivity.PREF_DONEITEMS2)) {
      doneItems.addAll(prefs.getStringSet(MainActivity.PREF_DONEITEMS2, null));
    }

    for (HAElement element : selectedItems) {
      element.flags = element.flags | HAElement.FLAG_DONE;
      doneItems.add(element.id);
      AutomaticReminderManager.deleteAutomaticReminder(this, element);
    }

    SharedPreferences.Editor editor = prefs.edit();
    editor.putStringSet(MainActivity.PREF_DONEITEMS2, doneItems);
    editor.commit();
  }

  private void markCurrentItemsAsNotDone() {
    List<HAElement> selectedItems = getSelectedListItems();
    Set<String> doneItems = new HashSet<String>();
    SharedPreferences prefs = getSharedPreferences(MainActivity.PREF_NAME, 0);

    if (prefs.contains(MainActivity.PREF_DONEITEMS2)) {
      doneItems.addAll(prefs.getStringSet(MainActivity.PREF_DONEITEMS2, null));
    }

    for (HAElement element : selectedItems) {
      element.flags = element.flags & (~HAElement.FLAG_DONE);
      element.title = element.title.replace(" [Erledigt]", "");
      doneItems.remove(element.id);
    }

    SharedPreferences.Editor editor = prefs.edit();
    editor.putStringSet(MainActivity.PREF_DONEITEMS2, doneItems);
    editor.commit();
  }

  /**
   * Get all items currently selected in the list view.
   * @return the selected items
   */
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
    loadHomework(false);
  }

  /**
   * Use {@link HomeworkUpdater} to load the homework, setting {@code this} as listener.
   * @param forceDownload if true, no cached results will be used
   */
  private void loadHomework(boolean forceDownload) {
    TextView emptyView = (TextView) findViewById(R.id.emptyView);
    ListView listView = (ListView) findViewById(R.id.lsViewHomework);
    listView.setEmptyView(emptyView);

    /*
    if (forceDownload) {
      clearData();
    }
    HomeworkUpdater loader = new HomeworkUpdater(this);
    loader.setOnHomeworkLoadedListener(this);
    loader.getData(forceDownload);
    */

    if (LoginManager.loadCredentials(this)) {
      LoginManager.login(this, hwmgr, result -> {
        if (result == LoginResultListener.Result.LOGGED_IN) {
          String strDate = getIntent().getStringExtra(EXTRA_DATE);

          if (strDate.equals("all")) {

          } else {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            try {
              Calendar cal = Calendar.getInstance();
              cal.setTime(dateFormat.parse(strDate));
              hwmgr.getHWOn(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
                  cal.get(Calendar.DAY_OF_MONTH))
              .registerListener(future -> {
                if (future.errorCode() == IHWFuture.ERRORCodes.OK) {
                  List<IHWObj> homework = (List<IHWObj>) future.get();

                  List<HAElement> elements = new ArrayList<HAElement>(homework.size());
                  for (IHWObj obj : homework) {
                    HAElement element = new HAElement();
                    element.id = obj.id();
                    element.date = obj.date()[0] + "-" + obj.date()[1] + "-" + obj.date()[2];
                    element.subject = obj.subject();
                    element.title = "No titles yet";
                    element.desc = obj.getDescription(true);
                    elements.add(element);
                  }
                  runOnUiThread(() -> {
                    setData(elements);
                  });
                } else {
                  // TODO: Error handling
                  Log.e(TAG, "Error code: " + future.errorCode());
                }
              });
            } catch (ParseException e) {
              Log.wtf(TAG, "Invalid date format: ", e);
            }
          }
        } else {
          // TODO: Error handling
          Log.e(TAG, "result: " + result);
        }
      });
    } else {
      // TODO: Open Login screen
      Log.e(TAG, "No credentials");
    }
  }

  private void clearData() {
    TextView emptyView = (TextView) findViewById(R.id.emptyView);
    emptyView.setText(getResources().getString(R.string.emptyText));

    ListView lsView = (ListView) findViewById(R.id.lsViewHomework);
    lsView.setEmptyView(emptyView);
    lsView.setAdapter(new HAElementArrayAdapter(this, new ArrayList<HAElement>()));
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

    // Filter by subjects if the option is turned on
    if (filter) {
      List<HAElement> displayedObjects = new ArrayList<HAElement>();
      List<String> displayedSubjects = Arrays.asList(chosenSubjects.split("\n"));

      for (int i = 0; i < data.size(); i++) {
        if (displayedSubjects.contains(data.get(i).subject)
            || data.get(i).subject.equals("")) {
          displayedObjects.add(data.get(i));
        }
      }
      filteredData = displayedObjects;
    } else {
      filteredData = data;
    }

    // Filter by date, according to EXTRA_DATE
    List<HAElement> selectedData = new ArrayList<HAElement>();
    String strDate = getIntent().getStringExtra(EXTRA_DATE);
    boolean all;
    Date date = new Date();

    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    if (strDate.equals("all")) {
      Calendar cal = Calendar.getInstance();
      if (!prefs.getBoolean(MainActivity.PREF_HOMEWORKTODAY, false)) {
        cal.add(Calendar.DAY_OF_MONTH, 1);
      }
      date = cal.getTime();
      all = true;
    } else {
      try {
        date = dateFormatter.parse(strDate);
      } catch (ParseException e) {
        Log.wtf(TAG, "Invalid date format: ", e);
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
      if (filteredData.get(i).date.equals("")) {
        selectedData.add(filteredData.get(i));
        continue;
      }
      Date elemDate = new Date();
      try {
        elemDate = dateFormatter.parse(filteredData.get(i).date);
      } catch (ParseException e) {
        Log.e(TAG, "Failed parsing homework date: ", e);
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

    SharedPreferences preferences = getSharedPreferences(MainActivity.PREF_NAME, 0);
    Set<String> doneItems = preferences.getStringSet(MainActivity.PREF_DONEITEMS2, null);
    if (doneItems == null) {
      doneItems = new HashSet<String>();
    }

    for (HAElement element : selectedData) {
      if (doneItems.contains(element.id)) {
        element.title += " [Erledigt]";
      }
    }

    if (selectedData.size() == 0) {
      HAElement noHomework = new HAElement();
      noHomework.id = "0";
      noHomework.date = "";
      noHomework.title = "Keine Hausaufgaben!";
      noHomework.subject = "";
      noHomework.desc = "Wir haben keine Hausaufgaben!";
      selectedData.add(noHomework);
    }

    ListView list = (ListView) findViewById(R.id.lsViewHomework);
    list.setAdapter(new HAElementArrayAdapter(this, selectedData));
  }
}
