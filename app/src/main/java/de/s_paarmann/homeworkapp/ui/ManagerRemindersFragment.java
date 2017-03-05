/*
 * Copyright (c) 2016  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package de.s_paarmann.homeworkapp.ui;

import android.app.AlarmManager;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import de.s_paarmann.homeworkapp.R;
import de.s_paarmann.homeworkapp.Reminder;
import de.s_paarmann.homeworkapp.ReminderArrayAdapter;
import de.s_paarmann.homeworkapp.ReminderBroadcastReceiver;

public class ManagerRemindersFragment extends Fragment {

  public static final String TAG = "ManageRemindersFragment";

  public ManagerRemindersFragment() {
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    container.clearDisappearingChildren();

    View root = inflater.inflate(R.layout.fragment_manage_reminders, container, false);

    final ListView listView = (ListView) root.findViewById(R.id.lsViewReminders);
    TextView emptyView = (TextView) root.findViewById(R.id.remindersEmptyView);
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

    listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
      public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        listView.setItemChecked(position, true);
        return true;
      }
    });

    loadReminders(root);

    return root;
  }

  private void loadReminders(View root) {
    Set<Reminder> setReminders = Reminder.loadSavedReminders(getActivity());

    ListView list = (ListView) root.findViewById(R.id.lsViewReminders);
    list.setAdapter(new ReminderArrayAdapter(getActivity(), new ArrayList<Reminder>(setReminders)));
  }

  private void deleteSelectedReminders() {
    List<Reminder> selectedReminders = getSelectedListItems();
    AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);

    for (Reminder reminder : selectedReminders) {
      Date when = reminder.getDate();
      Uri uri = reminder.toUri();

      Intent intent = new Intent(MainActivity.ACTION_REMIND, uri, getActivity(),
        ReminderBroadcastReceiver.class);
      PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, intent, 0);

      alarmManager.cancel(pendingIntent);

      reminder.delete(getActivity());
    }

    loadReminders(getView());
  }

  private ArrayList<Reminder> getSelectedListItems() {
    ListView listView = (ListView) getView().findViewById(R.id.lsViewReminders);
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
