/*
 * Copyright (c) 2014  Sebastian Paarmann
 */

package paarmann.physikprofil;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

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
