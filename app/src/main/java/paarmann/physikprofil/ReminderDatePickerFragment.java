/*
 * Copyright (c) 2014 Sebastian Paarmann
 */

package paarmann.physikprofil;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CalendarView;

import java.util.Date;

public class ReminderDatePickerFragment extends DialogFragment {

  private CalendarView.OnDateChangeListener mDateChangeListener;

  public static ReminderDatePickerFragment newInstance(Date initialDate) {
    ReminderDatePickerFragment frag = new ReminderDatePickerFragment();
	Bundle args = new Bundle();
	args.putSerializable("initialDate", initialDate);
	frag.setArguments(args);
	return frag;
  }

  private ReminderDatePickerFragment() {
	super();
  }

  public void setOnDateChangeListener(CalendarView.OnDateChangeListener onDateChangeListener) {
    mDateChangeListener = onDateChangeListener;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	LayoutInflater inflater = getActivity().getLayoutInflater();

	View layout = inflater.inflate(R.layout.reminder_date_picker_dialog, null);
    CalendarView calReminder = (CalendarView) layout.findViewById(R.id.calReminder);
	
    builder.setView(layout);

	calReminder.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
      @Override
	  public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
        if (mDateChangeListener != null) {
		  mDateChangeListener.onSelectedDayChange(view, year, month, dayOfMonth);
		}
		dismiss();
	  }
	});

	Date initialDate = (Date) getArguments().getSerializable("initialDate");
    calReminder.setDate(initialDate.getTime());

	return builder.create();
  }

}
