/*
 * Copyright (c) 2016  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package paarmann.physikprofil.ui;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import org.apache.commons.lang3.ObjectUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import paarmann.physikprofil.HAElement;
import paarmann.physikprofil.Log;
import paarmann.physikprofil.R;
import paarmann.physikprofil.Reminder;
import paarmann.physikprofil.ReminderBroadcastReceiver;

public class ReminderDateTimePickerFragment extends DialogFragment {

  public static final String TAG = "ReminderDateTimePickerFragment";

  private View layout;

  public static ReminderDateTimePickerFragment newInstance(ArrayList<HAElement> selectedListItems) {
    ReminderDateTimePickerFragment frag = new ReminderDateTimePickerFragment();
    Bundle args = new Bundle();
    args.putSerializable("selectedListItems", selectedListItems);
    frag.setArguments(args);
    return frag;
  }

  public ReminderDateTimePickerFragment() {
    super();
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    LayoutInflater inflater = getActivity().getLayoutInflater();

    layout = inflater.inflate(R.layout.reminder_datetime_picker_dialog, null);
    final Button dateButton = (Button) layout.findViewById(R.id.dateButton);
    final Button timeButton = (Button) layout.findViewById(R.id.timeButton);

    final ArrayList<HAElement>
        selectedListItems =
        (ArrayList<HAElement>) getArguments().getSerializable("selectedListItems");

    dateButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        try {
          Date initialDate = selectedListItems.get(0).getDate();
          Calendar initDate = Calendar.getInstance();
          initDate.setTime(initialDate);
          int initYear = initDate.get(Calendar.YEAR);
          int initMonth = initDate.get(Calendar.MONTH);
          int initDay = initDate.get(Calendar.DAY_OF_MONTH);
          DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
              Calendar date = Calendar.getInstance();
              date.set(year, month, day);
              setDate(date);
            }
          };
          DatePickerDialog
              dialog =
              new DatePickerDialog(getActivity(), listener, initYear, initMonth, initDay);
          dialog.show();
        } catch (ParseException e) {
          Log.wtf(TAG, "error parsing date", e);
        }
      }
    });
    timeButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        int initialHour = 14;
        int initialMinute = 0;
        TimePickerDialog.OnTimeSetListener listener = new TimePickerDialog.OnTimeSetListener() {
          @Override
          public void onTimeSet(TimePicker view, int hour, int minute) {
            Calendar time = Calendar.getInstance();
            time.set(Calendar.HOUR_OF_DAY, hour);
            time.set(Calendar.MINUTE, minute);
            setTime(time);
          }
        };
        TimePickerDialog
            dialog =
            new TimePickerDialog(getActivity(), listener, initialHour, initialMinute, true);
        dialog.show();
      }
    });

    Button btnDayBefore = (Button) layout.findViewById(R.id.btnDayBefore);
    if (selectedListItems.size() > 1) {
      btnDayBefore.setEnabled(false);
    } else {
      btnDayBefore.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          try {
            Date due = selectedListItems.get(0).getDate();
            Calendar date = Calendar.getInstance();
            date.setTime(due);
            date.add(Calendar.DAY_OF_MONTH, -1);
            date.set(Calendar.HOUR, 14);
            date.set(Calendar.MINUTE, 0);
            date.set(Calendar.SECOND, 0);
            setDate(date);
            setTime(date);
          } catch (ParseException e) {
            Log.wtf(TAG, "Error while parsing date", e);
          }
        }
      });
    }

    builder.setView(layout)
        .setTitle(R.string.reminder_datetime_picker_title)
        .setPositiveButton(R.string.setReminder, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int id) {
            AlarmManager
                alarmManager =
                (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
            Date when = null;
            try {
              Calendar cal = Calendar.getInstance();
              cal.setTime(getDate().getTime());
              cal.set(Calendar.HOUR_OF_DAY, getTime().get(Calendar.HOUR_OF_DAY));
              cal.set(Calendar.MINUTE, getTime().get(Calendar.MINUTE));
              cal.set(Calendar.SECOND, 0);
              when = cal.getTime();
            } catch (NullPointerException e) {
              return; //No date and/or no time set
            }

            Reminder reminder = new Reminder(when, selectedListItems);
            Uri uri = reminder.toUri();

            Intent
                intent =
                new Intent(MainActivity.ACTION_REMIND, uri, getActivity(),
                           ReminderBroadcastReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, intent, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
              alarmManager.setExact(AlarmManager.RTC_WAKEUP, when.getTime(), pendingIntent);
            } else {
              alarmManager.set(AlarmManager.RTC_WAKEUP, when.getTime(), pendingIntent);
            }

            reminder.save(getActivity());

            Toast.makeText(getActivity(), "Erinnerung erstellt", Toast.LENGTH_SHORT).show();
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

  private Calendar _date;
  private Calendar getDate() {
    return _date;
  }
  private void setDate(Calendar date) {
    Button dateButton = (Button) layout.findViewById(R.id.dateButton);
    String strDate = DateFormat.getDateInstance().format(date.getTime());
    dateButton.setText(strDate);

    _date = date;
  }

  private Calendar _time;
  private Calendar getTime() {
    return _time;
  }
  private void setTime(Calendar time) {
    Button timeButton = (Button) layout.findViewById(R.id.timeButton);
    String strTime = new SimpleDateFormat("HH:mm").format(time.getTime());
    timeButton.setText(strTime);

    _time = time;
  }

}
