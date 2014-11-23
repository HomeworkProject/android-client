/*
 * Copyright (c) 2014 Sebastian Paarmann
 */

package paarmann.physikprofil;

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
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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
    final Button dateButton = (Button) layout.findViewById(R.id.dateButton);
    final Button timeButton = (Button) layout.findViewById(R.id.timeButton);

    dateButton.setText("Datum wählen");
    timeButton.setText("Zeit wählen");
    
    final ArrayList<HomeworkDetailActivity.HAElement> selectedListItems = (ArrayList<HomeworkDetailActivity.HAElement>) getArguments().getSerializable("selectedListItems");

    dateButton.setOnClickListener(new View.OnClickListener() {
      @Override
        public void onClick(View view) {
          try {
            Date initialDate = new SimpleDateFormat("yyyy-MM-dd").parse(selectedListItems.get(0).date);
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
            DatePickerDialog dialog = new DatePickerDialog(getActivity(), listener, initYear, initMonth, initDay); 
            dialog.getDatePicker().setSpinnersShown(false);
            dialog.getDatePicker().setCalendarViewShown(true);
            dialog.show();
          } catch (ParseException e) {
            Log.wtf("ParseDates", "error parsing date", e);
          }
        }
      });
      timeButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          int initialHour = 14;
          int initialMinute = 00;
          TimePickerDialog.OnTimeSetListener listener = new TimePickerDialog.OnTimeSetListener() {
            @Override
              public void onTimeSet(TimePicker view, int hour, int minute) {
                Calendar time = Calendar.getInstance();
                time.set(Calendar.HOUR_OF_DAY, hour);
                time.set(Calendar.MINUTE, minute);
                setTime(time);
              }
            };
          TimePickerDialog dialog = new TimePickerDialog(getActivity(), listener, initialHour, initialMinute, true);
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
            Date due = new SimpleDateFormat("yyyy-MM-dd").parse(selectedListItems.get(0).date);
            Calendar date = Calendar.getInstance();
            date.setTime(due);
            date.add(Calendar.DAY_OF_MONTH, -1);
            date.set(Calendar.HOUR, 14);
            date.set(Calendar.MINUTE, 0);
            date.set(Calendar.SECOND, 0);
            setDate(date);
            setTime(date);
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
              AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
              String strDate = dateButton.getText() + " " + timeButton.getText();
              Date when = null;
              try {
                when = new SimpleDateFormat("dd.MM.yyyy HH:mm").parse(strDate);
              } catch (ParseException e) {throw new RuntimeException("Somehow, the date failed to parse");}
              String scheme = "homework";
              String ssp = when.getTime() + "\\";			// AlarmTime\id~Date~Title~Subject~Desc\id~Date~Title~Subject~Desc\....
              for (int i = 0; i < selectedListItems.size(); i++) {
                HomeworkDetailActivity.HAElement element = selectedListItems.get(i);
                      ssp += element.id + "~"
                       + element.date + "~"
                       + element.title + "~"
                       + element.subject + "~"
                       + element.desc + "\\";
              }

              Uri uri = Uri.fromParts(scheme, ssp, "");

              Intent intent = new Intent(MainActivity.ACTION_REMIND, uri, getActivity(), ReminderBroadcastReceiver.class);
              PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, intent, 0);
              if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, when.getTime(), pendingIntent);
              } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, when.getTime(), pendingIntent);
              }
              
              SharedPreferences prefs = getActivity().getSharedPreferences(MainActivity.PREF_NAME, 0);
              SharedPreferences.Editor editor = prefs.edit();
              Set<String> setReminders;
              if (prefs.contains(MainActivity.PREF_SETREMINDERS)) {
                setReminders = new HashSet<String>();
                setReminders.addAll(prefs.getStringSet(MainActivity.PREF_SETREMINDERS, null));
              } else {
                setReminders = new HashSet<String>();
              }
              setReminders.add(ssp);
              editor.putStringSet(MainActivity.PREF_SETREMINDERS, setReminders);
              editor.commit();
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
    String strDate = DateFormat.getDateInstance().format(date.getTime());
    dateButton.setText(strDate);
  }

  private void setTime(Calendar time) {
    Button timeButton = (Button) layout.findViewById(R.id.timeButton);
    String strTime = new SimpleDateFormat("HH:mm").format(time.getTime());
    timeButton.setText(strTime);
  }

}
