/*
 * Copyright (c) 2016  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package de.s_paarmann.homeworkapp.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Dialog to display the changelog for the current version.
 */
public class ChangelogDialog extends DialogFragment {

  public static final String TAG = "ChangelogDialog";

  private String changelog = "";

  public ChangelogDialog() {
    super();
  }

  public void setChangelog(String changelog) {
    this.changelog = changelog;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setMessage(changelog)
        .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            // Just let the dialog close
          }
        });
    return builder.create();
  }
}
