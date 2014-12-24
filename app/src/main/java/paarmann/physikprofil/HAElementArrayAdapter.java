/*
 * Copyright (c) 2014  Sebastian Paarmann
 */

package paarmann.physikprofil;


import android.app.Activity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HAElementArrayAdapter extends ArrayAdapter<HomeworkDetailActivity.HAElement> {

  public static final String TAG = "HAElementArrayAdapter";

  private List<HomeworkDetailActivity.HAElement> objects;
  private Activity activity;

  public HAElementArrayAdapter(Activity activity, List<HomeworkDetailActivity.HAElement> objects) {
    super(activity, R.layout.homework_list_item, objects);
    this.activity = activity;
    this.objects = objects;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    LayoutInflater
        inflater =
        (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View root = inflater.inflate(R.layout.homework_list_item, parent, false);

    TextView title = (TextView) root.findViewById(R.id.textTitle);
    TextView date = (TextView) root.findViewById(R.id.textDate);
    TextView subject = (TextView) root.findViewById(R.id.textSubject);
    TextView desc = (TextView) root.findViewById(R.id.textDesc);

    title.setText(objects.get(position).title);
    subject.setText(objects.get(position).subject);
    desc.setText(objects.get(position).desc);

    String[] dateParts = objects.get(position).date.split("-");
    if (dateParts.length == 3) {
      date.setText(dateParts[2] + "." + dateParts[1] + "." + dateParts[0]);
    } else {
      date.setText(objects.get(position).date);
    }

    return root;
  }

}
