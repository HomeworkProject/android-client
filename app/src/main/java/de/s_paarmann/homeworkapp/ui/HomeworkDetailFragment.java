/*
 * Copyright (c) 2017  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package de.s_paarmann.homeworkapp.ui;


import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import de.s_paarmann.homeworkapp.HAElement;
import de.s_paarmann.homeworkapp.R;
import de.s_paarmann.homeworkapp.network.HomeworkManager;
import de.s_paarmann.homeworkapp.network.LoginResultListener;
import de.s_paarmann.homeworkapp.ui.login.LoginActivity;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public class HomeworkDetailFragment extends Fragment {

  public static final String TAG = "HomeworkDetailFragment";

  public static final String EXTRA_HOMEWORK_ID = "de.s_paarmann.homeworkapp.homework_id";
  public static final String EXTRA_HOMEWORK_DATE = "de.s_paarmann.homeworkapp.homework_date";

  private HAElement element;

  public HomeworkDetailFragment() {
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    container.clearDisappearingChildren();

    String id = getArguments().getString(EXTRA_HOMEWORK_ID);
    Date date = (Date) getArguments().getSerializable(EXTRA_HOMEWORK_DATE);

    View root = inflater.inflate(R.layout.fragment_homework_detail, container, false);

    loadHomework(root, id, date);

    return root;
  }

  private void loadHomework(View root, String id, Date date) {
    ProgressBar loadingIcon = (ProgressBar) root.findViewById(R.id.detail_loadingIcon);
    LinearLayout contentView = (LinearLayout) root.findViewById(R.id.content_detail);

    loadingIcon.setVisibility(View.VISIBLE);
    contentView.setVisibility(View.GONE);

    HomeworkManager.getHomework(getActivity(), date, (homework, loginResult, error) -> {
      if (getActivity() != null) {
        getActivity().runOnUiThread(() -> {
          if (loginResult == LoginResultListener.Result.NO_CREDENTIALS_PRESENT
              || loginResult == LoginResultListener.Result.INVALID_CREDENTIALS) {
            Toast.makeText(getActivity(), "Ungültige Zugangsdaten", Toast.LENGTH_LONG).show();

            startActivity(new Intent(getActivity(), LoginActivity.class));
          } else {
            setData(id, homework);
          }
        });
      }
    });
  }

  private void setData(String id, List<HAElement> data) {
    if (getView() == null) {
      return;
    }

    ProgressBar loadingIcon = (ProgressBar) getView().findViewById(R.id.detail_loadingIcon);
    LinearLayout contentView = (LinearLayout) getView().findViewById(R.id.content_detail);

    loadingIcon.setVisibility(View.GONE);
    contentView.setVisibility(View.VISIBLE);

    TextView title = (TextView) contentView.findViewById(R.id.txtDetailTitle);
    TextView date = (TextView) contentView.findViewById(R.id.txtDetailDate);
    TextView subject = (TextView) contentView.findViewById(R.id.txtDetailSubject);
    TextView description = (TextView) contentView.findViewById(R.id.txtDetailDescription);

    Optional<HAElement> elementOptional = data.stream().filter(e -> e.id.equals(id)).findFirst();

    if (elementOptional.isPresent()) {
      element = elementOptional.get();
      title.setText(element.title);
      date.setText(element.date);
      subject.setText(element.subject);
      description.setText(element.desc);
    } else {
      // TODO: Error
    }

  }

}
