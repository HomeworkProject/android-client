/*
 * Copyright (c) 2015  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package paarmann.physikprofil;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.widget.DatePicker;

import java.util.Calendar;

public class DatePickerFragment extends DialogFragment
    implements DatePickerDialog.OnDateSetListener {

  public static final String TAG = "DatePickerFragment";

  private DatePickerDialog.OnDateSetListener listener;

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Calendar cal = Calendar.getInstance();
    int year = cal.get(Calendar.YEAR);
    int month = cal.get(Calendar.MONTH);
    int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

    return new DatePickerDialog(getActivity(), this, year, month, dayOfMonth);
  }

  public DatePickerFragment setListener(DatePickerDialog.OnDateSetListener listener) {
    this.listener = listener;
    return this;
  }

  @Override
  public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
    listener.onDateSet(view, year, monthOfYear, dayOfMonth);
  }
}
