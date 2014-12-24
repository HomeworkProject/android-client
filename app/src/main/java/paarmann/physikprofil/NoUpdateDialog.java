/*
 * Copyright (c) 2014  Sebastian Paarmann
 */

package paarmann.physikprofil;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class NoUpdateDialog extends DialogFragment {

  public static final String TAG = "NoUpdateDialog";

  public NoUpdateDialog() {
    super();
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setMessage(R.string.noUpdateDialogText)
        .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            // Do nothing, just let the dialog close
          }
        });
    return builder.create();
  }
}
