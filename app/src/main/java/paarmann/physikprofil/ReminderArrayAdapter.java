/*
 * Copyright (c) 2015  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package paarmann.physikprofil;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ReminderArrayAdapter extends ArrayAdapter<ManageRemindersActivity.Reminder> {

  public static final String TAG = "ReminderArrayAdapter";

  private List<ManageRemindersActivity.Reminder> objects;
  private Context context;

  public ReminderArrayAdapter(Context context, List<ManageRemindersActivity.Reminder> objects) {
    super(context, R.layout.reminder_list_item, objects);
    this.objects = objects;
    this.context = context;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    LayoutInflater inflater =
      (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View root = inflater.inflate(R.layout.reminder_list_item, parent, false);

    TextView title = (TextView) root.findViewById(R.id.reminderTitle);
    TextView date = (TextView) root.findViewById(R.id.reminderDate);

    Date dDate = new Date(Long.valueOf(objects.get(position).date));
    String strDate = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(dDate);

    title.setText(objects.get(position).title);
    date.setText(strDate);

    return root;
  }
}
