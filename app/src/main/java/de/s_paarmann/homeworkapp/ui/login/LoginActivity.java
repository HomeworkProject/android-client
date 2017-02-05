/*
 * Copyright (c) 2016  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package de.s_paarmann.homeworkapp.ui.login;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import de.mlessmann.homework.api.CDK;
import de.mlessmann.homework.api.ICDKConnection;
import de.mlessmann.homework.api.error.Error;
import de.mlessmann.homework.api.event.ICDKConnectionEvent;
import de.mlessmann.homework.api.event.ICDKEvent;
import de.mlessmann.homework.api.event.ICDKListener;
import de.mlessmann.homework.api.event.network.ConnectionStatus;
import de.mlessmann.homework.api.future.IHWFuture;
import de.mlessmann.homework.api.provider.IHWProvider;
import de.mlessmann.homework.api.session.IHWGroupMapping;
import de.mlessmann.homework.internal.providers.HWProvider;
import de.s_paarmann.homeworkapp.R;
import de.s_paarmann.homeworkapp.network.LoginManager;
import de.s_paarmann.homeworkapp.network.LoginResultListener;
import de.s_paarmann.homeworkapp.network.ManualHWProvider;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

  public static final String TAG = "LoginActivity";

  private FrameLayout contentFrame;
  private LayoutInflater inflater;

  private CDK cdk;
  private ICDKConnection connection;
  private List<IHWProvider> providers;

  private IHWProvider selectedProvider;
  private IHWGroupMapping groups;
  private String selectedGroup;
  private String selectedUser;
  private String password;

  private interface BackAction {
    void onBackPressed();
  }

  private BackAction backAction;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    inflater = getLayoutInflater();
    contentFrame = (FrameLayout) findViewById(R.id.content_login);

    loadProviders();
  }

  @Override
  protected void onStop() {
    super.onStop();

    LoginManager.userCanceledLoginActivity();
  }

  @Override
  public void onBackPressed() {
    if (backAction == null) {
      super.onBackPressed();
    } else {
      backAction.onBackPressed();
    }
  }

  private void loadProviders() {
    if (cdk == null) {
      cdk = CDK.getInstance();
      cdk.registerListener(LoginManager.LogListener);
    }

    cdk.listProviders(null).registerListener(future -> {
      IHWFuture<List<IHWProvider>> providerFuture = (IHWFuture<List<IHWProvider>>) future;

      if (providerFuture.getError() == Error.OK) {
        providers = providerFuture.get();
        runOnUiThread(this::displayProviderSelect);
      } else {
        runOnUiThread(() -> {
          new AlertDialog.Builder(this)
              .setTitle("Error")
              .setMessage("Fehler beim Herunterladen der Liste von Schulen.")
              .setNeutralButton("Erneut versuchen", ((dialog, which) -> {
                dialog.dismiss();
                loadProviders();
              }))
              .show();
        });
      }
    });
  }

  private void displayProviderSelect() {
    findViewById(R.id.login_loadingIcon).setVisibility(View.GONE);

    inflater.inflate(R.layout.login_provider_select, contentFrame, true);

    ListView lsProviders = (ListView) contentFrame.findViewById(R.id.lsViewProviders);
    Button btnManualEntry = (Button) contentFrame.findViewById(R.id.btnManualProviderEntry);

    List<String> providerNames = new ArrayList<>(providers.size());
    for (IHWProvider provider : providers) {
      providerNames.add(provider.getName());
    }

    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
        providerNames);
    lsProviders.setAdapter(adapter);

    lsProviders.setOnItemClickListener((parent, view, position, id) -> {
      selectedProvider = providers.get(position);

      loadGroups();
    });

    btnManualEntry.setOnClickListener(v -> {
      loadManualEntry();
    });

    backAction = () -> {
      if (LoginManager.loadCredentials(this)) {
        finish();
      } else {
        finishAffinity();
      }
    };
  }

  private void loadManualEntry() {
    contentFrame.removeView(contentFrame.findViewById(R.id.content_login_provider));
    inflater.inflate(R.layout.login_manual_entry, contentFrame, true);

    Button btnSubmit = (Button) contentFrame.findViewById(R.id.btnManualEntrySubmit);
    btnSubmit.setOnClickListener(v -> {
      EditText txtHost = (EditText) contentFrame.findViewById(R.id.txtManualEntryServer);
      EditText txtPort = (EditText) contentFrame.findViewById(R.id.txtManualEntryPort);
      CheckBox checkSSL = (CheckBox) contentFrame.findViewById(R.id.checkManualEntrySSL);

      String host = txtHost.getText().toString();
      int port = -1;
      try {
        port = Integer.valueOf(txtPort.getText().toString());
      } catch (NumberFormatException e) {
        // Leave port at -1, the error will be caught below
      }

      if (!(port > 0 && port < 65536)) {
        Toast.makeText(this, "Ungültiger Port.", Toast.LENGTH_LONG).show();
        return;
      }

      boolean ssl = checkSSL.isChecked();

      selectedProvider = ManualHWProvider.createProvider(host, port, ssl);

      loadGroups();
    });
  }

  private void loadGroups() {
    findViewById(R.id.login_loadingIcon).setVisibility(View.VISIBLE);

    // We can come here through either of these, so just remove both to be sure.
    // Attempting to remove a view that does not exist does not cause any issues.
    contentFrame.removeView(contentFrame.findViewById(R.id.content_login_provider));
    contentFrame.removeView(contentFrame.findViewById(R.id.content_login_manual_entry));

    cdk.registerListener(new ICDKListener() {
      @Override
      public void onEvent(ICDKEvent event) {
        if (event instanceof ICDKConnectionEvent) {
          if (event.getConnection() != connection) {
            event.getConnection().kill();
            return;
          }

          ICDKConnectionEvent connEvent = (ICDKConnectionEvent) event;

          if (connEvent.getStatus() == ConnectionStatus.CONNECTING) {
            return;
          }

          if (connEvent.getStatus() == ConnectionStatus.CONNECTED) {
            connection.getGroups(null).registerListener(getGrpFuture -> {
              if (((IHWFuture) getGrpFuture).getError() == Error.OK) {
                groups = ((IHWFuture<IHWGroupMapping>) getGrpFuture).get();

                runOnUiThread(LoginActivity.this::displayGroupSelect);
              } else {
                runOnUiThread(() -> {
                  new AlertDialog.Builder(LoginActivity.this)
                      .setTitle("Error")
                      .setMessage("Fehler beim Herunterladen der Liste von Klassen.")
                      .setNeutralButton("Erneut versuchen", ((dialog, which) -> {
                        dialog.dismiss();
                        loadGroups();
                      }))
                      .show();
                });
              }
            });
            /*} else {
              runOnUiThread(() -> {
                new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("Der ausgewählte Server hat eine inkompatible Version.")
                    .setNeutralButton("Anderen Server auswählen", ((dialog, which) -> {
                      dialog.dismiss();
                      loadProviders();
                    }))
                    .show();
              });
            }*/
          } else if (connEvent.getStatus() == ConnectionStatus.CONNECTING_INTERRUPTED) {
            // TODO: Handle this
            return;
          } else if (connEvent.getStatus() != ConnectionStatus.CONNECTING) {
            runOnUiThread(() -> {
              new AlertDialog.Builder(LoginActivity.this)
                  .setTitle("Error")
                  .setMessage("Fehler beim Verbinden zum Server")
                  .setNeutralButton("Erneut versuchen", ((dialog, which) -> {
                    dialog.dismiss();
                    loadGroups();
                  }))
                  .setNegativeButton("Abbrechen", ((dialog, which) -> {
                    dialog.dismiss();
                    loadProviders();
                  }))
                  .show();
            });
          }
        }

        cdk.unregisterListener(this);
      }
    });

    connection = cdk.connect(selectedProvider);
    connection.start();
  }

  private void displayGroupSelect() {
    findViewById(R.id.login_loadingIcon).setVisibility(View.GONE);

    inflater.inflate(R.layout.login_group_select, contentFrame, true);

    TextView txtProvider = (TextView) contentFrame.findViewById(R.id.txtLoginGroupSelectProvider);
    txtProvider.setText(selectedProvider.getName());

    ListView lsGroups = (ListView) contentFrame.findViewById(R.id.lsViewGroups);

    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
        groups.getGroups());
    lsGroups.setAdapter(adapter);

    lsGroups.setOnItemClickListener((parent, view, position, id) -> {
      selectedGroup = groups.getGroups().get(position);

      displayUserSelect();
    });

    backAction = () -> {
      contentFrame.removeView(contentFrame.findViewById(R.id.content_login_group));
      findViewById(R.id.login_loadingIcon).setVisibility(View.VISIBLE);
      loadProviders();
    };
  }

  private void displayUserSelect() {
    contentFrame.removeView(contentFrame.findViewById(R.id.content_login_group));

    inflater.inflate(R.layout.login_user_select, contentFrame, true);

    TextView txtProvider = (TextView) contentFrame.findViewById(R.id.txtLoginUserSelectProvider);
    TextView txtGroup = (TextView) contentFrame.findViewById(R.id.txtLoginUserSelectGroup);

    txtProvider.setText(selectedProvider.getName());
    txtGroup.setText(selectedGroup);

    ListView lsUsers = (ListView) contentFrame.findViewById(R.id.lsViewUsers);

    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
        groups.getUsersFor(selectedGroup));
    lsUsers.setAdapter(adapter);

    lsUsers.setOnItemClickListener((parent, view, position, id) -> {
      selectedUser = groups.getUsersFor(selectedGroup).get(position);

      displayPasswordForm();
    });

    backAction = () -> {
      contentFrame.removeView(contentFrame.findViewById(R.id.content_login_user));
      findViewById(R.id.login_loadingIcon).setVisibility(View.VISIBLE);
      loadGroups();
    };
  }

  private void displayPasswordForm() {
    contentFrame.removeView(contentFrame.findViewById(R.id.content_login_user));

    inflater.inflate(R.layout.login_password_form, contentFrame, true);

    TextView txtProvider = (TextView) contentFrame.findViewById(R.id.txtLoginPasswordFormProvider);
    TextView txtGroup = (TextView) contentFrame.findViewById(R.id.txtLoginPasswordFormGroup);
    TextView txtUser = (TextView) contentFrame.findViewById(R.id.txtLoginPasswordFormUser);

    txtProvider.setText(selectedProvider.getName());
    txtGroup.setText(selectedGroup);
    txtUser.setText(selectedUser);

    EditText txtPassword = (EditText) contentFrame.findViewById(R.id.txtLoginPasswordFormPassword);
    Button btnLogin = (Button) contentFrame.findViewById(R.id.btnLoginPasswordFormLogin);

    btnLogin.setOnClickListener(v -> {
      password = txtPassword.getText().toString();

      contentFrame.findViewById(R.id.content_login_password).setVisibility(View.GONE);
      contentFrame.findViewById(R.id.login_loadingIcon).setVisibility(View.VISIBLE);

      LoginManager.setCredentials(this, selectedProvider, selectedGroup, selectedUser, password);
      LoginManager.getHWMgr(this, (unused, result) -> {
        if (result == LoginResultListener.Result.LOGGED_IN) {
          runOnUiThread(() -> {
            Toast.makeText(this, "Erfolgreich eingeloggt.", Toast.LENGTH_SHORT).show();
            finish();
          });
        } else {
          runOnUiThread(() -> {
            String msg = "";
            if (result == LoginResultListener.Result.INVALID_CREDENTIALS) {
              msg = "Ungültige Zugangsdaten";
            } else if (result == LoginResultListener.Result.CONNECTION_FAILED) {
              msg = "Verbindung zum Server fehlgeschlagen.";
            } else {
              msg = "Unbekannter Fehler beim einloggen.";
            }

            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            contentFrame.findViewById(R.id.login_loadingIcon).setVisibility(View.GONE);
            contentFrame.findViewById(R.id.content_login_password).setVisibility(View.VISIBLE);
          });
        }
      });
    });

    backAction = () -> {
      contentFrame.removeView(contentFrame.findViewById(R.id.content_login_password));
      displayUserSelect();
    };
  }
}
