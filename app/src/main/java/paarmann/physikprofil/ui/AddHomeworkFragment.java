/*
 * Copyright (c) 2016  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package paarmann.physikprofil.ui;

import android.app.DatePickerDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import de.mlessmann.api.data.IHWFuture;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import paarmann.physikprofil.HAElement;
import paarmann.physikprofil.R;
import paarmann.physikprofil.network.HomeworkManager;

public class AddHomeworkFragment extends Fragment {

  public static final String TAG = "AddHomeworkFragment";

  private Calendar selectedDate = null;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View root = inflater.inflate(R.layout.fragment_add_homework, container, false);

    root.findViewById(R.id.btnAddHomework).setOnClickListener(this::onBtnAddHomeworkClick);
    root.findViewById(R.id.btnChooseDate).setOnClickListener(this::onBtnChooseDateClick);

    return root;
  }

  public void onBtnChooseDateClick(View view) {
    Calendar initialDate;
    if (selectedDate != null) {
      initialDate = selectedDate;
    } else {
      initialDate = Calendar.getInstance();
      selectedDate = initialDate;
      initialDate.add(Calendar.DAY_OF_MONTH, 1);
    }

    int initialYear = initialDate.get(Calendar.YEAR);
    int initialMonth = initialDate.get(Calendar.MONTH);
    int initialDay = initialDate.get(Calendar.DAY_OF_MONTH);

    DatePickerDialog dialog = new DatePickerDialog(getActivity(), (v, y, m, d) -> {
      setDate(y, m, d);
    }, initialYear, initialMonth, initialDay);

    dialog.show();
  }

  private void setDate(int y, int m, int d) {
    selectedDate.set(y, m, d, 0, 0, 0);

    Button btnChooseDate = (Button) getView().findViewById(R.id.btnChooseDate);
    btnChooseDate.setText(DateFormat.getDateInstance().format(selectedDate.getTime()));
  }

  public void onBtnAddHomeworkClick(View view) {
    String title = ((EditText) getView().findViewById(R.id.txtTitle)).getText().toString();
    String subject = ((EditText) getView().findViewById(R.id.txtSubject)).getText().toString();
    String desc = ((EditText) getView().findViewById(R.id.txtDescription)).getText().toString();

    if (selectedDate == null) {
      Toast.makeText(getActivity(), "Es muss ein Datum ausgewählt werden.", Toast.LENGTH_SHORT).show();
      return;
    }

    Calendar today = Calendar.getInstance();
    today.set(Calendar.HOUR_OF_DAY, 0);
    today.set(Calendar.MINUTE, 0);
    today.set(Calendar.SECOND, 0);

    if (selectedDate.before(today)) {
      Toast.makeText(getActivity(), "Das Datum darf nicht vor heute liegen!", Toast.LENGTH_SHORT).show();
      return;
    }

    String strDate = new SimpleDateFormat("yyyy-MM-dd").format(selectedDate.getTime());

    final HAElement element = new HAElement();
    element.date = strDate;
    element.title = title;
    element.subject = subject;
    element.desc = desc;

    final ProgressDialog dialog = ProgressDialog.show(getActivity(), "",
      "Hausaufgabe wird hinzugefügt.", true, false);

    HomeworkManager.addHomework(getActivity(), element, result -> {
      if (result == IHWFuture.ERRORCodes.OK) {
        if (getActivity() != null) getActivity().runOnUiThread(() -> {
          Toast.makeText(getActivity(), "Hausaufgabe hinzugefügt.", Toast.LENGTH_SHORT).show();
          dialog.dismiss();

          ((MainActivity) getActivity()).showMainView();
        });
      } else {
        String msg;
        if (result == IHWFuture.ERRORCodes.INSUFFPERM) {
          msg = "Du bist nicht berechtigt Hausaufgaben hinzuzufügen.";
        } else {
          msg = "Unbekannter Fehler beim hinzufügen der Hausaufgabe.";
        }
        if (getActivity() != null) getActivity().runOnUiThread(() -> {
          Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
          dialog.dismiss();
        });
      }
    });
  }
}
