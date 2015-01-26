/*
 * Copyright (c) 2015  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package paarmann.physikprofil;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ManageRemindersActivity extends Activity {

  public static final String TAG = "ManageRemindersActivity";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_manage_reminders);

    final ListView listView = (ListView) findViewById(R.id.lsViewReminders);
    TextView emptyView = (TextView) findViewById(R.id.remindersEmptyView);
    listView.setEmptyView(emptyView);

    listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
    listView.setMultiChoiceModeListener(new ListView.MultiChoiceModeListener() {
      @Override
      public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                                            boolean checked) {

      }

      @Override
      public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
          case R.id.action_reminder_delete:
            deleteSelectedReminders();
            mode.finish();
            return true;
          default:
            return false;
        }
      }

      @Override
      public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.manage_reminders_context_menu, menu);
        return true;
      }

      @Override
      public void onDestroyActionMode(ActionMode mode) {

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

    loadReminders();
  }

  private void loadReminders() {
    Set<Reminder> setReminders = Reminder.loadSavedReminders(this);

    ListView list = (ListView) findViewById(R.id.lsViewReminders);
    list.setAdapter(new ReminderArrayAdapter(this, new ArrayList<Reminder>(setReminders)));
  }

  private void deleteSelectedReminders() {
    List<Reminder> selectedReminders = getSelectedListItems();
    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

    for (Reminder reminder : selectedReminders) {
      Date when = reminder.getDate();
      Uri uri = reminder.toUri();

      Intent
          intent =
          new Intent(MainActivity.ACTION_REMIND, uri, this, ReminderBroadcastReceiver.class);
      PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

      alarmManager.cancel(pendingIntent);

      reminder.delete(this);
    }

    loadReminders();
  }

  private ArrayList<Reminder> getSelectedListItems() {
    ListView listView = (ListView) findViewById(R.id.lsViewReminders);
    SparseBooleanArray selected = listView.getCheckedItemPositions();
    ArrayList<Reminder> selectedItems = new ArrayList<Reminder>();
    for (int i = 0; i < selected.size(); i++) {
      if (selected.valueAt(i)) {
        selectedItems.add((Reminder) listView.getItemAtPosition(selected.keyAt(i)));
      }
    }
    return selectedItems;
  }

}
