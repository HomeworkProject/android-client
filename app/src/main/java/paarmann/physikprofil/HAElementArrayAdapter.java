/*
 * Copyright (c) 2014  Sebastian Paarmann
 */

package paarmann.physikprofil;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class HAElementArrayAdapter extends ArrayAdapter<HomeworkDetailActivity.HAElement> {

  private List<HomeworkDetailActivity.HAElement> objects;
  private Context context;

  public HAElementArrayAdapter(Context context, List<HomeworkDetailActivity.HAElement> objects) {
    super(context, R.layout.homework_list_item, objects);
    this.objects = objects;
    this.context = context;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    LayoutInflater
        inflater =
        (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
