/*
 * Copyright (c) 2016  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package de.s_paarmann.homeworkapp.ui;

import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import de.mlessmann.api.data.IHWFuture;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.s_paarmann.homeworkapp.AutomaticReminderManager;
import de.s_paarmann.homeworkapp.HAElement;
import de.s_paarmann.homeworkapp.HAElementArrayAdapter;
import de.s_paarmann.homeworkapp.Log;
import de.s_paarmann.homeworkapp.R;
import de.s_paarmann.homeworkapp.network.HomeworkManager;
import de.s_paarmann.homeworkapp.network.LoginResultListener;

import static android.view.View.GONE;

public class HomeworkDetailFragment extends Fragment {

  public static final String TAG = "HomeworkDetailFragment";

  public static final String EXTRA_DATE = "de.s_paarmann.homeworkapp.extra.date";

  private String strDate;

  public HomeworkDetailFragment() {
  }

  private DialogFragment reminderDialog;
  private ActionMode actionMode;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
    Bundle savedInstanceState) {

    View root = inflater.inflate(R.layout.fragment_homework_detail, container, false);

    strDate = getArguments().getString(EXTRA_DATE);

    final ListView listView = (ListView) root.findViewById(R.id.lsViewHomework);
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
          case R.id.action_delete:
            deleteCurrentItems();
            checkedItems.clear();
            mode.finish();
            return true;
          case R.id.action_done:
            if (markItemsDone) {
              markCurrentItemsAsDone();
            } else {
              markCurrentItemsAsNotDone();
            }
            loadHomework(root);
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
        actionMode = mode;

        getActivity().findViewById(R.id.toolbar).setVisibility(GONE);

        return true;
      }

      @Override
      public void onDestroyActionMode(ActionMode mode) {
        actionMode = null;

        getActivity().findViewById(R.id.toolbar).setVisibility(View.VISIBLE);
      }

      @Override
      public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        SharedPreferences prefs = getActivity().getSharedPreferences(MainActivity.PREF_NAME, 0);
        Set<String>
            doneItems =
            prefs.getStringSet(MainActivity.PREF_DONEITEMS2, new HashSet<String>());

        MenuItem doneMenuItem = menu.findItem(R.id.action_done);
        MenuItem addReminderMenuItem = menu.findItem(R.id.action_remind);
        MenuItem deleteMenuItem = menu.findItem(R.id.action_delete);

        // Check if item is already marked as done, see also markItemsDone. (Done items are not yet
        // using the new HAElement system)
        boolean unmarkItems = false;
        for (int position : checkedItems) {
         HAElement element = (HAElement) listView.getItemAtPosition(position);
          if (doneItems.contains(element.id)) {
            unmarkItems = true;
            break;
          }
        }

        // Only allow deleting one item at a time for now
        boolean allowDeleting = checkedItems.size() == 1;

        // Disallow setting reminders, deleting and marking as done for warnings and errors
        boolean allowActions = true;
        for (int position : checkedItems) {
          HAElement element = (HAElement) listView.getItemAtPosition(position);
          Log.d(TAG, element.title + ": " + element.flags);
          if ((element.flags & (HAElement.FLAG_ERROR
                                | HAElement.FLAG_WARN
                                | HAElement.FLAG_INFO)) != 0) {
            allowActions = false;
            break;
          }
        }

        if (!allowActions) {
          doneMenuItem.setVisible(false);
          deleteMenuItem.setVisible(false);
          addReminderMenuItem.setVisible(false);
        } else {
          if (unmarkItems) {
            doneMenuItem.setIcon(R.drawable.ic_action_cancel);
            doneMenuItem.setTitle("Als nicht erledigt markieren");
            markItemsDone = false;
          } else {
            doneMenuItem.setIcon(R.drawable.ic_action_edit);
            doneMenuItem.setTitle(R.string.action_done);
            markItemsDone = true;
          }

          deleteMenuItem.setVisible(allowDeleting);
        }

        return true;
      }
    });
    listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
      public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        listView.setItemChecked(position, true);
        return true;
      }
    });

    loadHomework(root);

    return root;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    if (reminderDialog != null) {
      reminderDialog.dismiss();
    }
    if (actionMode != null) {
      actionMode.finish();
    }

    super.onSaveInstanceState(outState);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    // TODO ?
    inflater.inflate(R.menu.homework_detail, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_refresh:
        loadHomework(getView(), true);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  /**
   * Copies the description of all selected items to the clipboard and displays a toast.
   */
  private void copyCurrentItems() {
    ClipboardManager clipboard = (ClipboardManager) getActivity()
      .getSystemService(Context.CLIPBOARD_SERVICE);
    ListView listView = (ListView) getView().findViewById(R.id.lsViewHomework);
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
    Toast.makeText(getActivity(),
                   (items.size() == 1 ? "Eintrag" : "Einträge") + " in die Zwischenablage kopiert",
                   Toast.LENGTH_SHORT).show();
  }

  private void setNewReminder() {
    reminderDialog = ReminderDateTimePickerFragment.newInstance(getSelectedListItems());
    reminderDialog.show(getFragmentManager(), "reminderDateTimePickerFragment");
  }

  private void deleteCurrentItems() {
    List<HAElement> selectedItems = getSelectedListItems();

    if (selectedItems.size() != 1) {
      Log.wtf(TAG, "Cannot delete more than one item at a time");
      return;
    }

    HAElement element = selectedItems.get(0);

    HomeworkManager.deleteHomework(getActivity(), element, result -> {
      if (result == IHWFuture.ERRORCodes.OK) {
        getActivity().runOnUiThread(() -> {
          Toast.makeText(getActivity(), "Hausaufgabe gelöscht.", Toast.LENGTH_SHORT).show();
          loadHomework(getView(), true);
        });
      } else {
        String msg;
        if (result == IHWFuture.ERRORCodes.INSUFFPERM) {
          msg = "Du bist nicht berechtigt, diese Hausaufgabe zu löschen.";
        } else {
          msg = "Beim Löschen der Hausaufgabe ist ein Fehler aufgetreten.";
        }
        Log.e(TAG, "Error deleting homework: " + result);
        getActivity().runOnUiThread(() -> {
          Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
          loadHomework(getView(), true);
        });
      }
    });
  }

  private void markCurrentItemsAsDone() {
    List<HAElement> selectedItems = getSelectedListItems();
    Set<String> doneItems = new HashSet<String>();
    SharedPreferences prefs = getActivity().getSharedPreferences(MainActivity.PREF_NAME, 0);

    if (prefs.contains(MainActivity.PREF_DONEITEMS2)) {
      doneItems.addAll(prefs.getStringSet(MainActivity.PREF_DONEITEMS2, null));
    }

    for (HAElement element : selectedItems) {
      element.flags = element.flags | HAElement.FLAG_DONE;
      doneItems.add(element.id);
      AutomaticReminderManager.deleteAutomaticReminder(getActivity(), element);
    }

    SharedPreferences.Editor editor = prefs.edit();
    editor.putStringSet(MainActivity.PREF_DONEITEMS2, doneItems);
    editor.commit();
  }

  private void markCurrentItemsAsNotDone() {
    List<HAElement> selectedItems = getSelectedListItems();
    Set<String> doneItems = new HashSet<String>();
    SharedPreferences prefs = getActivity().getSharedPreferences(MainActivity.PREF_NAME, 0);

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
   *
   * @return the selected items
   */
  private ArrayList<HAElement> getSelectedListItems() {
    ListView listView = (ListView) getView().findViewById(R.id.lsViewHomework);
    SparseBooleanArray selected = listView.getCheckedItemPositions();
    ArrayList<HAElement> selectedItems = new ArrayList<HAElement>();
    for (int i = 0; i < selected.size(); i++) {
      if (selected.valueAt(i)) {
        selectedItems.add((HAElement) listView.getItemAtPosition(selected.keyAt(i)));
      }
    }
    return selectedItems;
  }


  private void loadHomework(View root) {
    loadHomework(root, false);
  }

  private void loadHomework(View root, boolean forceDownload) {
    ProgressBar loadingIcon = (ProgressBar) root.findViewById(R.id.loadingIcon);
    ListView listView = (ListView) root.findViewById(R.id.lsViewHomework);

    listView.setVisibility(View.GONE);
    loadingIcon.setVisibility(View.VISIBLE);

    HomeworkManager.GetHWListener myListener = (hw, loginResult) -> {
      if (getActivity() != null) getActivity().runOnUiThread(() -> {
        if (loginResult == LoginResultListener.Result.NO_CREDENTIALS_PRESENT
            || loginResult == LoginResultListener.Result.INVALID_CREDENTIALS) {
          Toast.makeText(getActivity(), "Ungültige Zugangsdaten", Toast.LENGTH_LONG).show();

          // TODO
          //((MainActivity) getActivity()).showLoginView();
        } else {
          setData(hw);
        }
      });
    };

    if (strDate.equals("all")) {
      Calendar cal = Calendar.getInstance();
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
      if (!prefs.getBoolean(MainActivity.PREF_HOMEWORKTODAY, false)) {
        cal.add(Calendar.DAY_OF_MONTH, 1);
      }
      Date startDate = cal.getTime();
      cal.add(Calendar.DAY_OF_MONTH, 64); // Server limits to 64 days
      Date endDate = cal.getTime();

      HomeworkManager.getHomework(getActivity(), startDate, endDate, myListener);
    } else {
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      try {
        HomeworkManager.getHomework(getActivity(), dateFormat.parse(strDate), myListener);
      } catch (ParseException e) {
        Log.wtf(TAG, "Invalid date format: ", e);
      }
    }
  }

  public void setData(List<HAElement> data) {
    ListView list = (ListView) getView().findViewById(R.id.lsViewHomework);
    list.clearChoices();
    list.requestLayout();

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
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

    SharedPreferences preferences = getActivity().getSharedPreferences(MainActivity.PREF_NAME, 0);
    Set<String> doneItems = preferences.getStringSet(MainActivity.PREF_DONEITEMS2, null);
    if (doneItems == null) {
      doneItems = new HashSet<String>();
    }

    for (HAElement element : filteredData) {
      if (doneItems.contains(element.id)) {
        element.title += " [Erledigt]";
      }
    }

    if (filteredData.size() == 0) {
      HAElement noHomework = new HAElement();
      noHomework.id = "0";
      noHomework.flags = HAElement.FLAG_INFO;
      noHomework.date = "";
      noHomework.title = "Keine Hausaufgaben!";
      noHomework.subject = "";
      noHomework.desc = "Wir haben keine Hausaufgaben!";
      filteredData.add(noHomework);
    }

    list.setAdapter(new HAElementArrayAdapter(getActivity(), filteredData));

    ProgressBar loadingIcon = (ProgressBar) getView().findViewById(R.id.loadingIcon);
    loadingIcon.setVisibility(GONE);
    list.setVisibility(View.VISIBLE);
  }
}
