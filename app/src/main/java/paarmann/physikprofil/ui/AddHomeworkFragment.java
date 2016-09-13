/*
 * Copyright (c) 2016  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package paarmann.physikprofil.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import de.mlessmann.api.data.IHWFuture;
import de.mlessmann.api.main.HWMgr;

import paarmann.physikprofil.HAElement;
import paarmann.physikprofil.R;
import paarmann.physikprofil.network.HomeworkManager;

public class AddHomeworkFragment extends Fragment {

  public static final String TAG = "AddHomeworkFragment";

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View root = inflater.inflate(R.layout.fragment_add_homework, container, false);

    root.findViewById(R.id.btnAddHomework).setOnClickListener(this::onBtnAddHomeworkClick);

    return root;
  }


  public void onBtnAddHomeworkClick(View view) {
    String
        strDate =
        ((EditText) getView().findViewById(R.id.txtDate)).getText()
            .toString(); // TODO: Make proper date selector
    String title = ((EditText) getView().findViewById(R.id.txtTitle)).getText().toString();
    String subject = ((EditText) getView().findViewById(R.id.txtSubject)).getText().toString();
    String desc = ((EditText) getView().findViewById(R.id.txtDescription)).getText().toString();

    final HAElement element = new HAElement();
    element.date = strDate;
    element.title = title;
    element.subject = subject;
    element.desc = desc;

    HomeworkManager.addHomework(getActivity(), element, result -> {
      if (result == IHWFuture.ERRORCodes.OK) {
        if (getActivity() != null) getActivity().runOnUiThread(() -> {
          Toast.makeText(getActivity(), "Hausaufgabe hinzugefügt.", Toast.LENGTH_SHORT).show();
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
        });
      }
    });
  }
}
