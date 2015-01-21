/*
 * Copyright (c) 2015  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package paarmann.physikprofil;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TabHost;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;


public class MainActivity extends Activity implements DatePickerDialog.OnDateSetListener {

  public static final String TAG = "MainActivity";

  public static final String ACTION_REMIND = "paarmann.physikprofil.action.REMIND";
  public static final String ACTION_UPDATEHOMEWORK = "paarmann.physikprofil.action.UPDATEHOMEWORK";

  public static final String PREF_NAME = "paarmann.physikprofil.sharedprefs";
  public static final String PREF_UPDATED = "paarmann.physikprofil.updated";
  public static final String PREF_SETREMINDERS = "paarmann.physikprofil.reminders";
  public static final String PREF_LASTUPDATED = "paarmann.physikprofil.lastupdated";
  public static final String PREF_DONEITEMS = "paarmann.physikprofil.doneitems";
  public static final String PREF_FILTERSUBJECTS = "paarmann.physikprofil.FilterSubjects";
  public static final String PREF_CHOSENSUBJECTS = "paarmann.physikprofil.ChosenSubjects";
  public static final String PREF_AUTOUPDATES = "paarmann.physikprofil.AutomaticUpdates";
  public static final String PREF_AUTOREMINDERS = "paarmann.physikprofil.AutomaticReminders";
  public static final String PREF_REMINDERTIME = "paarmann.physikprofil.ReminderTime";
  public static final String PREF_REMINDERDAY = "paarmann.physikprofil.ReminderDay";
  public static final String PREF_MOBILEDATA = "paarmann.physikprofil.UseMobileData";

  SharedPreferences prefs;

  private boolean isPaused;
  private static List<DialogFragment> dialogsToShow = new ArrayList<DialogFragment>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

    initTabs();

    isPaused = false;

    findViewById(R.id.progressBar2).setVisibility(View.GONE);
    findViewById(R.id.txtUpdating).setVisibility(View.GONE);

    prefs = getSharedPreferences(PREF_NAME, 0);

    Calendar cal = Calendar.getInstance();

    //Set button texts according to days
    Button btnNextDay = (Button) findViewById(R.id.btnTomorrow);
    Button btnAfterNextDay = (Button) findViewById(R.id.btnAfterTomorrow);
    if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY) {
      btnAfterNextDay.setText("Hausaufgaben zu Montag");
    } else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
               || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
      btnNextDay.setText("Hausaufgaben zu Montag");
      btnAfterNextDay.setText("Hausaufgaben zu Dienstag");
    }
    if (prefs.getBoolean(PREF_UPDATED, false)) {
      prefs.edit().putBoolean(PREF_UPDATED, false).apply();
      File file = new File(Environment.getExternalStorageDirectory().getPath() + "/physikbioapp-update.apk");
      file.delete();
	  
      showChangelog();
    }

    checkForUpdates(false);
  }

  private void initTabs() {
    TabHost tabHost = (TabHost) findViewById(R.id.tabHost);
    tabHost.setup();

    TabHost.TabSpec tsHA = tabHost.newTabSpec("TAB_HA");
    tsHA.setIndicator("Hausaufgaben");
    tsHA.setContent(R.id.tabHA);
    tabHost.addTab(tsHA);

    TabHost.TabSpec tsDates = tabHost.newTabSpec("TAB_DATES");
    tsDates.setIndicator("Termine");
    tsDates.setContent(R.id.tabDates);
    tabHost.addTab(tsDates);
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

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();
    switch (id) {
      case R.id.action_update:
        checkForUpdates(true);
        return true;
      case R.id.action_manageReminders:
        startManageRemindersActivity();
        return true;
      case R.id.action_settings:
        startSettingsActivity();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void startManageRemindersActivity() {
    Intent manageReminders = new Intent(this, ManageRemindersActivity.class);
    startActivity(manageReminders);
  }
  
  private void startSettingsActivity() {
    Intent settings = new Intent(this, SettingsActivity.class);
    startActivity(settings);
  }

  public void onBtnAllHomeworkClick(View view) {
    Intent details = new Intent(this, HomeworkDetailActivity.class);
    details.putExtra(HomeworkDetailActivity.EXTRA_DATE, "all");
    startActivity(details);
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
    Intent details = new Intent(this, HomeworkDetailActivity.class);
    String date = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(
        Calendar.DAY_OF_MONTH);
    details.putExtra(HomeworkDetailActivity.EXTRA_DATE, date);
    startActivity(details);
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
    Intent details = new Intent(this, HomeworkDetailActivity.class);
    String date = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(
        Calendar.DAY_OF_MONTH);
    details.putExtra(HomeworkDetailActivity.EXTRA_DATE, date);
    startActivity(details);
  }

  public void onBtnPickDateClick(View view) {
    DialogFragment dialog = new DatePickerFragment().setListener(this);
    dialog.show(getFragmentManager(), "datePicker");
  }

  @Override
  public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
    Intent details = new Intent(this, HomeworkDetailActivity.class);
    String date = year + "-" + (monthOfYear + 1) + "-" + dayOfMonth;
    details.putExtra(HomeworkDetailActivity.EXTRA_DATE, date);
    startActivity(details);
  }

  public void checkForUpdates(boolean userInitiated) {
    new CheckForUpdateTask().execute(getResources().getString(R.string.server_uri), String.valueOf(userInitiated));
  }

  private void askForUpdate(int newVersionCode, String newVersionName) {
    DialogFragment dialog = new UpdateDialog().setVersionName(newVersionName);
    dialog.show(getFragmentManager(), "updateDialog");
  }

  public void update() {
    findViewById(R.id.progressBar2).setVisibility(View.VISIBLE);
    findViewById(R.id.txtUpdating).setVisibility(View.VISIBLE);
    new UpdateTask().execute(getResources().getString(R.string.server_uri) + "/app/physikbioapp-latest.apk");
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

        int currVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
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

      } catch (MalformedURLException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      } catch (PackageManager.NameNotFoundException e) {
        e.printStackTrace();
      }
      return null;
    }
  }

  private class UpdateTask extends AsyncTask<String, Void, String> {

    @Override
    protected String doInBackground(String... params) {
      String path = Environment.getExternalStorageDirectory().getPath() + "/physikbioapp-update.apk";
      try {
        URL url = new URL(params[0]);
        URLConnection connection = url.openConnection();
        connection.connect();

        int fileLength = connection.getContentLength();

        InputStream input = new BufferedInputStream(url.openStream());
        OutputStream output = new FileOutputStream(path);

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
      } catch (Exception e) {
        Log.e(TAG, "Update failed!", e);
      }
      return path;
    }

    @Override
    protected void onPostExecute(String s) {
      Intent i = new Intent();
      i.setAction(Intent.ACTION_VIEW);
      i.setDataAndType(Uri.fromFile(new File(s)), "application/vnd.android.package-archive");

      prefs.edit().putBoolean(PREF_UPDATED, true).apply();

      startActivity(i);
    }
  }

}
