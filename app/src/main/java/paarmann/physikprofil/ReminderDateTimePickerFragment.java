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
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;

import java.util.ArrayList;

public class ReminderDateTimePickerFragment extends DialogFragment {

  public static ReminderDateTimePickerFragment newInstance(ArrayList<View> selectedListItems) {
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

	View layout = inflater.inflate(R.layout.reminder_datetime_picker_dialog, null);
	Spinner dateSpinner = (Spinner) layout.findViewById(R.id.dateSpinner);
	Spinner timeSpinner = (Spinner) layout.findViewById(R.id.timeSpinner);
	
	dateSpinner.setOnTouchListener(new View.OnTouchListener() {
      @Override
	  public boolean onTouch(View view, MotionEvent event) {
        //TODO open calendar dialog
		return false;
	  }
	});
	timeSpinner.setOnTouchListener(new View.OnTouchListener() {
	  @Override
	  public boolean onTouch(View view, MotionEvent event) {
		//TODO open "clock" dialog
		return false;
	  }
	});

	ArrayList<View> selectedListItems = (ArrayList<View>) getArguments().getSerializable("selectedListItems");
    Button btnDayBefore = (Button) layout.findViewById(R.id.btnDayBefore);
    if (selectedListItems.size() > 1) {
      btnDayBefore.setEnabled(false);
	} else {
      btnDayBefore.setOnClickListener(new View.OnClickListener() {
        @Override
		public void onClick(View view) {
		  //TODO set date to day before homework due
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

}
