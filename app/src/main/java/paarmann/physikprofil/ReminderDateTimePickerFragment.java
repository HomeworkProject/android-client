/*
 * Copyright (c) 2014 Sebastian Paarmann
 */

package paarmann.physikprofil;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class ReminderDateTimePickerFragment extends DialogFragment {

  private View layout;

  public static ReminderDateTimePickerFragment newInstance(ArrayList<HomeworkDetailActivity.HAElement> selectedListItems) {
    ReminderDateTimePickerFragment frag = new ReminderDateTimePickerFragment();
	Bundle args = new Bundle();
	args.putSerializable("selectedListItems", selectedListItems);
	frag.setArguments(args);
	return frag;
  }

  private ReminderDateTimePickerFragment() {
	super();
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    LayoutInflater inflater = getActivity().getLayoutInflater();

	layout = inflater.inflate(R.layout.reminder_datetime_picker_dialog, null);
	Button dateButton = (Button) layout.findViewById(R.id.dateButton);
	Button timeButton = (Button) layout.findViewById(R.id.timeButton);

	dateButton.setText("Datum wählen");
	timeButton.setText("Zeit wählen");
	
	dateButton.setOnClickListener(new View.OnClickListener() {
      @Override
	  public void onClick(View view) {
        //TODO open calendar dialog
	  }
	});
	timeButton.setOnClickListener(new View.OnClickListener() {
	  @Override
	  public void onClick(View view) {
		//TODO open "clock" dialog
	  }
	});

	final ArrayList<HomeworkDetailActivity.HAElement> selectedListItems = (ArrayList<HomeworkDetailActivity.HAElement>) getArguments().getSerializable("selectedListItems");
    Button btnDayBefore = (Button) layout.findViewById(R.id.btnDayBefore);
    if (selectedListItems.size() > 1) {
      btnDayBefore.setEnabled(false);
	} else {
      btnDayBefore.setOnClickListener(new View.OnClickListener() {
        @Override
		public void onClick(View view) {
		  try {
		  Date due = new SimpleDateFormat("yyyy-MM-dd").parse(selectedListItems.get(0).date);
		  Calendar date = Calendar.getInstance();
		  date.setTime(due);
		  date.add(Calendar.DAY_OF_MONTH, -1);
		  date.set(Calendar.HOUR, 14);
		  date.set(Calendar.MINUTE, 0);
		  date.set(Calendar.SECOND, 0);
		  setDate(date);
		  } catch (ParseException e) {
		    Log.wtf("Parsing", "Error while parsing date", e);
		  }
	    }
	  });
	}

	builder.setView(layout)
	  .setTitle(R.string.reminder_datetime_picker_title)
	  .setPositiveButton(R.string.setReminder, new DialogInterface.OnClickListener() {
        @Override
		public void onClick(DialogInterface dialog, int id) {
		  ///TODO create new reminder
		}
	  })
	  .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
        @Override
		public void onClick(DialogInterface dialog, int id) {
		  //do nothing, just let the dialog close
		}
	  });

	return builder.create();
  }

  private void setDate(Calendar date) {
    Button dateButton = (Button) layout.findViewById(R.id.dateButton);
	Button timeButton = (Button) layout.findViewById(R.id.timeButton);

	String strDate = DateFormat.getDateInstance().format(date.getTime());
	String strTime = new SimpleDateFormat("kk:mm").format(date.getTime());
	
	dateButton.setText(strDate);
	timeButton.setText(strTime);
  }

}
