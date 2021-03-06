/*
 * Copyright (c) 2016  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package de.s_paarmann.homeworkapp.ui;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Toast;

import de.s_paarmann.homeworkapp.Log;
import de.s_paarmann.homeworkapp.R;
import de.s_paarmann.homeworkapp.ui.login.LoginActivity;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

public class MainFragment extends Fragment implements DatePickerDialog.OnDateSetListener {

  public static final String TAG = "MainFragment";

  private SharedPreferences prefs;

  private final static int PERMISSION_REQUEST_UPDATE = 1;

  private boolean isPaused;
  private static List<DialogFragment> dialogsToShow = new ArrayList<>();

  public MainFragment() {

  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    container.clearDisappearingChildren();

    View layout = inflater.inflate(R.layout.fragment_main, container, false);

    layout.findViewById(R.id.btnAllHomework).setOnClickListener(this::onBtnAllHomeworkClick);
    layout.findViewById(R.id.btnPickDate).setOnClickListener(this::onBtnPickDateClick);
    layout.findViewById(R.id.btnTomorrow).setOnClickListener(this::onBtnTomorrowClick);
    layout.findViewById(R.id.btnAfterTomorrow).setOnClickListener(this::onBtnAfterTomorrowClick);

    return layout;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    isPaused = false;

    getView().findViewById(R.id.progressBar2).setVisibility(View.GONE);
    getView().findViewById(R.id.txtUpdating).setVisibility(View.GONE);

    prefs = getActivity().getSharedPreferences(MainActivity.PREF_NAME, 0);

    Calendar cal = Calendar.getInstance();

    //Set button texts according to days
    Button btnNextDay = (Button) getView().findViewById(R.id.btnTomorrow);
    Button btnAfterNextDay = (Button) getView().findViewById(R.id.btnAfterTomorrow);
    if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY) {
      btnAfterNextDay.setText("Hausaufgaben zu Montag");
    } else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
               || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
      btnNextDay.setText("Hausaufgaben zu Montag");
      btnAfterNextDay.setText("Hausaufgaben zu Dienstag");
    }

    if (prefs.getBoolean(MainActivity.PREF_UPDATED, false)) {
      prefs.edit().putBoolean(MainActivity.PREF_UPDATED, false).apply();
      File file = new File(
          Environment.getExternalStorageDirectory(),
          "homeworkapp-update.apk");
      file.delete();

      showChangelog();
    }

    checkForUpdates(false);
  }

  @Override
  public void onPause() {
    super.onPause();
    isPaused = true;
  }

  @Override
  public void onResume() {
    super.onResume();
    isPaused = false;

    while (dialogsToShow.size() > 0) {
      DialogFragment dialog = dialogsToShow.get(0);
      dialog.show(getFragmentManager(), "dialogsToShow");
      dialogsToShow.remove(0);
    }
  }

  public void onBtnAllHomeworkClick(View view) {
    ((MainActivity) getActivity()).showHomeworkDetailView("all");
  }

  public void onBtnTomorrowClick(View view) {
    Calendar cal = Calendar.getInstance();
    int offset;
    if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
      offset = 3;
    } else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
      offset = 2;
    } else {
      offset = 1;
    }
    cal.add(Calendar.DAY_OF_MONTH, offset);
    String date = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(
        Calendar.DAY_OF_MONTH);

    ((MainActivity) getActivity()).showHomeworkDetailView(date);
  }

  public void onBtnAfterTomorrowClick(View view) {
    Calendar cal = Calendar.getInstance();
    int offset;
    if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY
        || cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
      offset = 4;
    } else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
      offset = 3;
    } else {
      offset = 2;
    }
    cal.add(Calendar.DAY_OF_MONTH, offset);
    String date = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(
        Calendar.DAY_OF_MONTH);

    ((MainActivity) getActivity()).showHomeworkDetailView(date);
  }

  public void onBtnPickDateClick(View view) {
    DialogFragment dialog = new DatePickerFragment().setListener(this);
    dialog.show(getFragmentManager(), "datePicker");
  }

  @Override
  public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
    String date = year + "-" + (monthOfYear + 1) + "-" + dayOfMonth;

    ((MainActivity) getActivity()).showHomeworkDetailView(date);
  }

  public void checkForUpdates(boolean userInitiated) {
    new CheckForUpdateTask()
        .execute(getResources().getString(R.string.update_server_uri),
            String.valueOf(userInitiated));
  }

  private void askForUpdate(int newVersionCode, String newVersionName) {
    DialogFragment dialog = new UpdateDialog().setVersionName(newVersionName);
    if (getActivity() != null) {
      if (isPaused) dialogsToShow.add(dialog);
      else dialog.show(getFragmentManager(), "updateDialog");
    }
  }

  public void update() {
    if (ActivityCompat
            .checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(getActivity(),
          new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_UPDATE);
    } else {
      getView().findViewById(R.id.progressBar2).setVisibility(View.VISIBLE);
      getView().findViewById(R.id.txtUpdating).setVisibility(View.VISIBLE);
      new UpdateTask()
          .execute(
              getResources().getString(R.string.update_server_uri) + "/app/homeworkapp-latest.apk");
    }
  }

  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    if (getActivity() == null) return;
    if (requestCode == PERMISSION_REQUEST_UPDATE) {
      if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
        Toast.makeText(getActivity(), "Update abgebrochen.", Toast.LENGTH_LONG).show();
      } else {
        update();
      }
    }
  }

  private void showChangelog() {
    ChangelogDialog dialog = new ChangelogDialog();
    dialog.setChangelog(getResources().getString(R.string.changelog));
    dialog.show(getFragmentManager(), "changelogDialog");
  }


  private class CheckForUpdateTask extends AsyncTask<String, Void, Void> {

    @Override
    protected Void doInBackground(String... params) {
      InputStream is;
      String strUrl = params[0] + "/version.php";

      try {
        URL url = new URL(strUrl);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setReadTimeout(10000);
        conn.setConnectTimeout(15000);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.connect();

        is = conn.getInputStream();

        String serverResponse = IOUtils.toString(is, "windows-1252");

        Scanner scanner = new Scanner(serverResponse);
        scanner.useDelimiter("~");
        int versionCode = Integer.valueOf(scanner.next());
        String versionName = scanner.next();

        if (getActivity() == null) {
          // Activity has since been closed/destroyed
          return null;
        }

        int currVersionCode = getActivity().getPackageManager().getPackageInfo(
            getActivity().getPackageName(), 0).versionCode;
        if (versionCode > currVersionCode) {
          askForUpdate(versionCode, versionName);
        } else {
          boolean userInitiated = Boolean.parseBoolean(params[1]);
          if (userInitiated) {
            if (!isPaused) {
              DialogFragment dialog = new NoUpdateDialog();
              dialog.show(getFragmentManager(), "noUpdateDialog");
            } else {
              DialogFragment dialog = new NoUpdateDialog();
              dialogsToShow.add(dialog);
            }
          }
        }

      } catch (PackageManager.NameNotFoundException | IOException e) {
        e.printStackTrace();
      }
      return null;
    }
  }

  private class UpdateTask extends AsyncTask<String, Void, File> {

    @Override
    protected File doInBackground(String... params) {
      try {
        URL url = new URL(params[0]);
        URLConnection connection = url.openConnection();
        connection.connect();

        int fileLength = connection.getContentLength();

        InputStream input = new BufferedInputStream(url.openStream());

        File file = new File(
            Environment.getExternalStorageDirectory(),
            "homeworkapp-update.apk");

        OutputStream output = new FileOutputStream(file);

        byte data[] = new byte[1024];
        long total = 0;
        int count;
        while ((count = input.read(data)) != -1) {
          total += count;
          output.write(data, 0, count);
        }

        output.flush();
        output.close();
        input.close();

        return file;
      } catch (Exception e) {
        Log.e(TAG, "Update failed!", e);
        return null;
      }
    }

    @Override
    protected void onPostExecute(File file) {
      if (file == null) {
        if (getActivity() != null) {
          Toast.makeText(getActivity(),
              "Fehler beim Herunterladen des Updates.", Toast.LENGTH_LONG).show();
        }
        return;
      }


      Intent i = new Intent();
      // Android N requires using content:// URIs for sharing private files, however devices with
      // older versions may not have an .apk installer that can deal with those URIs so we use a
      // normal file:// URI there.
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        i.setAction(Intent.ACTION_VIEW);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Uri fileUri = FileProvider.getUriForFile(getActivity(),
            "de.s_paarmann.homeworkapp.fileprovider", file);
        i.setDataAndType(fileUri, "application/vnd.android.package-archive");
      } else {
        i.setAction(Intent.ACTION_VIEW);
        i.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
      }

      prefs.edit().putBoolean(MainActivity.PREF_UPDATED, true).apply();

      startActivity(i);
    }
  }
}
