/*
 * Copyright (c) 2016  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package paarmann.physikprofil;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import de.mlessmann.api.data.IHWProvider;
import de.mlessmann.api.main.HWMgr;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import paarmann.physikprofil.network.LoginManager;
import paarmann.physikprofil.network.LoginResultListener;

public class LoginActivity extends Activity implements LoginResultListener {

  public static final String TAG = "LoginActivity";

  private HWMgr providerHWMgr;
  private List<IHWProvider> providers;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);
  }

  @Override
  protected void onStart() {
    super.onStart();

    providerHWMgr = new HWMgr();
    loadServerList();
  }

  @Override
  protected void onStop() {
    super.onStop();

    LoginManager.userCanceledLoginActivity();

    providerHWMgr.release();
    providerHWMgr = null;
  }

  private void loadServerList() {
    new GetProvidersTask().execute((String) null);
  }

  private void populateProviderSpinner() {
    Spinner spinner = (Spinner) findViewById(R.id.spinner_school);

    List<String> providerNames = new ArrayList<String>();
    for (IHWProvider provider : providers) {
      providerNames.add(provider.getName());
    }

    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
        android.R.layout.simple_spinner_item, providerNames);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

    spinner.setAdapter(adapter);
  }

  public void onBtnLoginClick(View v) {
    final String group = ((EditText) findViewById(R.id.txtGroup)).getText().toString();
    final String user = ((EditText) findViewById(R.id.txtUser)).getText().toString();
    final String auth = ((EditText) findViewById(R.id.txtAuth)).getText().toString();

    Spinner spinner = (Spinner) findViewById(R.id.spinner_school);
    IHWProvider provider = providers.get(spinner.getSelectedItemPosition());

    LoginManager.setCredentials(this, provider, group, user, auth);
    Log.d(TAG, "Logging in to " + provider.getAddress());

    LoginManager.getHWMgr(this, (mgr, result) -> {
      onLoginDone(result);
    },  false);
  }

  public void onLoginDone(Result result) {
    String resultText;
    Log.i(TAG, "Attempted login: " + result);

    switch (result) {
      case LOGGED_IN:
        resultText = "Erfolgreich eingeloggt.";
        break;
      case CONNECTION_FAILED:
        resultText = "Verbindung zum Server fehlgeschlagen.";
        break;
      case SERVER_INCOMPATIBLE:
        resultText = "Server hat eine inkompatible Version.";
        break;
      case INVALID_CREDENTIALS:
        resultText = "Ungültige Login-Informationen";
        break;
      default:
        resultText = "Unbekannter Fehler beim einloggen.";
        break;
    }

    runOnUiThread(() -> {
      Toast.makeText(this, resultText, Toast.LENGTH_LONG).show();
    });
  }

  private class GetProvidersTask extends AsyncTask<String, Void, List<IHWProvider>> {

    @Override
    protected List<IHWProvider> doInBackground(String... params) {
      JSONArray arr;

      try {
        return providerHWMgr.getAvailableProviders(params[0]);
      } catch (Exception e) {
        Log.e(TAG, "Error getting available providers", e); // TODO: Proper error handling

        return null;
      }
    }

    protected void onPostExecute(List<IHWProvider> result) {
      providers = result;

      populateProviderSpinner();
    }
  }
}
