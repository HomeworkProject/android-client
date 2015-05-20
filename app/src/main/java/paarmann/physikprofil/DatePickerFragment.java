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

/**
 * Dialog fragment for displaying a date picker that provides a method for callers to be notified of the chosen date.
 * Initializes the date picker to the current date.
 */
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

  /**
   * Register a {@code DatePickerDialog.OnDateSetListener} to be notified when a date is chosen.
   *
   * @param listener the listener to be notified
   * @return this
   */
  public DatePickerFragment setListener(DatePickerDialog.OnDateSetListener listener) {
    this.listener = listener;
    return this;
  }

  @Override
  public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
    listener.onDateSet(view, year, monthOfYear, dayOfMonth);
  }
}
