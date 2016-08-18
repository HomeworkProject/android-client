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

import de.mlessmann.api.data.IHWFuture;
import de.mlessmann.api.data.IHWFutureListener;
import de.mlessmann.api.data.IHWProvider;
import de.mlessmann.api.data.IHWUser;
import de.mlessmann.api.main.HWMgr;
import de.mlessmann.exceptions.StillConnectedException;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends Activity {

  public static final String TAG = "LoginActivity";

  private HWMgr hwmgr;
  private List<IHWProvider> providers;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);

    hwmgr = new HWMgr();

    loadServerList();
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

    try {
      hwmgr.connect(provider).registerListener(ihwFuture -> {
        if (ihwFuture.isPresent()) {
          Log.e(TAG, "Could not connect to chosen provider.",
              (Exception) ihwFuture.get()); // TODO: Error handling
        } else {
          hwmgr.isCompatible().registerListener(ihwFuture1 -> {
            if ((Boolean) ihwFuture1.get()) {
              hwmgr.login(group, user, auth).registerListener(future -> {
                IHWFuture<IHWUser> userFuture = (IHWFuture<IHWUser>) future;
                IHWUser user1 = userFuture.get();

                Log.d(TAG, "Logged in as " + user1.name() + " with group " + user1.group());
              });
            } else {
              Log.e(TAG, "Server not compatible!");
            }
          });
        }
      });
    } catch (StillConnectedException e) {
      Log.e(TAG, "Failed to connect because still connected to another server.");
    }

  }

  private class GetProvidersTask extends AsyncTask<String, Void, List<IHWProvider>> {

    @Override
    protected List<IHWProvider> doInBackground(String... params) {
      JSONArray arr;

      try {
        return hwmgr.getAvailableProviders(params[0]);
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
