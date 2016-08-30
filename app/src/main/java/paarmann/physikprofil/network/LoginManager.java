/*
 * Copyright (c) 2016  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package paarmann.physikprofil.network;

import android.content.Context;
import android.content.SharedPreferences;

import de.mlessmann.api.data.IHWFuture;
import de.mlessmann.api.data.IHWProvider;
import de.mlessmann.api.data.IHWUser;
import de.mlessmann.api.main.HWMgr;
import de.mlessmann.exceptions.StillConnectedException;
import de.mlessmann.internals.data.HWProvider;

import org.json.JSONException;
import org.json.JSONObject;

import paarmann.physikprofil.Log;
import paarmann.physikprofil.MainActivity;

public class LoginManager {

  public static final String TAG = "LoginManager";

  // TODO: Don't store credentials in shared preferences
  private static IHWProvider provider;
  private static String group, user, auth;

  public static void setCredentials(Context ctx, IHWProvider provider, String group, String user,
                                    String auth) {
    LoginManager.provider = provider;
    LoginManager.group = group;
    LoginManager.user = user;
    LoginManager.auth = auth;

    saveCredentials(ctx);
  }

  public static boolean loadCredentials(Context ctx) {
    SharedPreferences prefs = ctx.getSharedPreferences(MainActivity.PREF_NAME, 0);

    if (!prefs.contains(MainActivity.PREF_CRED_GROUP)
        || !prefs.contains(MainActivity.PREF_CRED_USER)
        || !prefs.contains(MainActivity.PREF_CRED_AUTH)
        || !prefs.contains(MainActivity.PREF_PROVIDER)) {
      return false;
    }

    group = prefs.getString(MainActivity.PREF_CRED_GROUP, "");
    user = prefs.getString(MainActivity.PREF_CRED_USER, "");
    auth = prefs.getString(MainActivity.PREF_CRED_AUTH, "");
    try {
      provider = new HWProvider(new JSONObject(prefs.getString(MainActivity.PREF_PROVIDER, "")));
    } catch (JSONException e) {
      // Basically impossible, unless something else messed with the prefs
      Log.wtf(TAG, e);
      return false;
    }
    return true;
  }

  private static void saveCredentials(Context ctx) {
    SharedPreferences prefs = ctx.getSharedPreferences(MainActivity.PREF_NAME, 0);
    SharedPreferences.Editor editor = prefs.edit();

    editor.putString(MainActivity.PREF_CRED_GROUP, group);
    editor.putString(MainActivity.PREF_CRED_USER, user);
    editor.putString(MainActivity.PREF_CRED_AUTH, auth);
    editor.putString(MainActivity.PREF_PROVIDER, provider.getJSON().toString());

    editor.apply();
  }

  public static void login(Context ctx, LoginResultListener listener) {
    login(ctx, new HWMgr(), listener);
  }

  public static void login(Context ctx, HWMgr mgr, LoginResultListener listener) {
    if (!loadCredentials(ctx)) {
      listener.onLoginDone(LoginResultListener.Result.NO_CREDENTIALS_PRESENT);
      return;
    }

    try {
      mgr.connect(provider).registerListener(connFuture -> {
        if (connFuture.isPresent()) {
          listener.onLoginDone(LoginResultListener.Result.CONNECTION_FAILED);
        } else {
          mgr.isCompatible().registerListener(compatibleFuture -> {
            IHWFuture<Boolean> compFuture = (IHWFuture<Boolean>) compatibleFuture;
            if (!compFuture.get()) {
              listener.onLoginDone(LoginResultListener.Result.SERVER_INCOMPATIBLE);
            }
            else {
              mgr.login(group, user, auth).registerListener(loginFuture -> {
                IHWFuture<IHWUser> userFuture = (IHWFuture<IHWUser>) loginFuture;
                if (userFuture.errorCode() != IHWFuture.ERRORCodes.LOGGEDIN) {
                  if (userFuture.errorCode() == IHWFuture.ERRORCodes.INVALIDCREDERR) {
                    listener.onLoginDone(LoginResultListener.Result.INVALID_CREDENTIALS);
                  } else {
                    listener.onLoginDone(LoginResultListener.Result.UNKNOWN);
                  }
                } else {
                  listener.onLoginDone(LoginResultListener.Result.LOGGED_IN);
                }
              });
            }
          });
        }
      });
    } catch (StillConnectedException e) {
      mgr.release();
      login(ctx, mgr, listener);
    }
  }

}
