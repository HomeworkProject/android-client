/*
 * Copyright (c) 2016  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package paarmann.physikprofil.ui;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import de.mlessmann.api.data.IHWProvider;
import de.mlessmann.api.main.HWMgr;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import paarmann.physikprofil.Log;
import paarmann.physikprofil.R;
import paarmann.physikprofil.network.LoginManager;
import paarmann.physikprofil.network.LoginResultListener;

public class LoginFragment extends Fragment {

  public static final String TAG = "LoginFragment";

  private HWMgr providerHWMgr;
  private List<IHWProvider> providers;

  public LoginFragment() {

  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View root = inflater.inflate(R.layout.fragment_login, container, false);

    root.findViewById(R.id.btnLogin).setOnClickListener(this::onBtnLoginClick);

    return root;
  }

  @Override
  public void onStart() {
    super.onStart();

    providerHWMgr = new HWMgr();
    loadServerList();
  }

  @Override
  public void onStop() {
    super.onStop();

    LoginManager.userCanceledLoginActivity();

    providerHWMgr.release();
    providerHWMgr = null;
  }


  private void loadServerList() {
    new GetProvidersTask().execute((String) null);
  }

  private void populateProviderSpinner() {
    Spinner spinner = (Spinner) getView().findViewById(R.id.spinner_school);

    List<String> providerNames = new ArrayList<String>();
    for (IHWProvider provider : providers) {
      providerNames.add(provider.getName());
    }

    ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
      android.R.layout.simple_spinner_item, providerNames);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

    spinner.setAdapter(adapter);
  }

  public void onBtnLoginClick(View v) {
    final String group = ((EditText) getView().findViewById(R.id.txtGroup)).getText().toString();
    final String user = ((EditText) getView().findViewById(R.id.txtUser)).getText().toString();
    final String auth = ((EditText) getView().findViewById(R.id.txtAuth)).getText().toString();

    Spinner spinner = (Spinner) getView().findViewById(R.id.spinner_school);
    IHWProvider provider = providers.get(spinner.getSelectedItemPosition());

    LoginManager.setCredentials(getActivity(), provider, group, user, auth);
    Log.d(TAG, "Logging in to " + provider.getAddress());

    LoginManager.getHWMgr(getActivity(), (mgr, result) -> {
      onLoginDone(result);
    }, false, true);
  }

  public void onLoginDone(LoginResultListener.Result result) {
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

    getActivity().runOnUiThread(() -> {
      Toast.makeText(getActivity(), resultText, Toast.LENGTH_LONG).show();

      if (result == LoginResultListener.Result.LOGGED_IN) {
        if (getActivity() != null) ((MainActivity) getActivity()).showMainView();
      }
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
