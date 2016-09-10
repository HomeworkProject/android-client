/*
 * Copyright (c) 2016  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package paarmann.physikprofil.network;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import de.mlessmann.api.data.IHWFuture;
import de.mlessmann.api.data.IHWFutureListener;
import de.mlessmann.api.data.IHWProvider;
import de.mlessmann.api.data.IHWSession;
import de.mlessmann.api.data.IHWUser;
import de.mlessmann.api.main.HWMgr;
import de.mlessmann.exceptions.StillConnectedException;
import de.mlessmann.internals.data.HWProvider;
import de.mlessmann.internals.data.HWSession;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import paarmann.physikprofil.Log;
import paarmann.physikprofil.LoginActivity;
import paarmann.physikprofil.MainActivity;

public class LoginManager {

  public interface GetHWMgrListener {
    public void receiveHWMgr(HWMgr mgr, LoginResultListener.Result result);
  }

  public static final String TAG = "LoginManager";

  // TODO: Don't store credentials in shared preferences
  private static IHWProvider provider;
  private static String group, user, auth;

  private static IHWSession session;
  private static boolean loggedIn = false;
  private static HWMgr mgr;
  private static final List<GetHWMgrListener> listenersWaitingForMgr = new ArrayList<>();
  private static boolean nonSilentListenerPresent = false;
  private static boolean creatingManager = false;
  private static boolean waitingForLoginActivity = false;

  public static void setCredentials(Context ctx, IHWProvider provider, String group, String user,
                                    String auth) {
    LoginManager.provider = provider;
    LoginManager.group = group;
    LoginManager.user = user;
    LoginManager.auth = auth;
    session = null;
    loggedIn = false;

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
      if (prefs.contains(MainActivity.PREF_CRED_TOKEN)) {
        session = new HWSession(new JSONObject(prefs.getString(MainActivity.PREF_CRED_TOKEN, "")));
      }
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
    if (session == null) {
      editor.remove(MainActivity.PREF_CRED_TOKEN);
    } else {
      editor.putString(MainActivity.PREF_CRED_TOKEN, session.getJSON().toString());
    }
    editor.putString(MainActivity.PREF_PROVIDER, provider.getJSON().toString());

    editor.apply();
  }

  public static void userCanceledLoginActivity() {
    synchronized (listenersWaitingForMgr) {
      waitingForLoginActivity = false;

      for (int i = listenersWaitingForMgr.size() - 1; i >= 0; i--) {
        listenersWaitingForMgr.get(i).receiveHWMgr(null,
          LoginResultListener.Result.NO_CREDENTIALS_PRESENT);
        listenersWaitingForMgr.remove(i);
      }

      creatingManager = false;
      loggedIn = false;
      nonSilentListenerPresent = false;
    }
  }

  public synchronized static void getHWMgr(Context ctx, GetHWMgrListener l) {
    getHWMgr(ctx, l, false);
  }

  public synchronized static void getHWMgr(Context ctx, GetHWMgrListener listener, boolean silent) {
    if (loggedIn) {
      listener.receiveHWMgr(mgr, LoginResultListener.Result.LOGGED_IN);
      return;
    }

    synchronized (listenersWaitingForMgr) {
      listenersWaitingForMgr.add(listener);
      if (!silent) {
        nonSilentListenerPresent = true;
      }

      if (creatingManager || waitingForLoginActivity) {
        return;
      }

      if (mgr == null) {
        mgr = new HWMgr();
      }
      creatingManager = true;

      if (!loadCredentials(ctx)) {
        if (!nonSilentListenerPresent) {
          for (int i = listenersWaitingForMgr.size() - 1; i >= 0; i--) {
            listenersWaitingForMgr.get(i).receiveHWMgr(null,
                                                       LoginResultListener.Result.NO_CREDENTIALS_PRESENT);
            listenersWaitingForMgr.remove(i);
          }
          return;
        } else {
          waitingForLoginActivity = true;
          Intent loginIntent = new Intent(ctx, LoginActivity.class);
          ctx.startActivity(loginIntent);
          return;
        }
      } else {
        login(ctx, result -> {
          switch (result) {
            case LOGGED_IN:
              for (int i = listenersWaitingForMgr.size() - 1; i >= 0; i--) {
                listenersWaitingForMgr.get(i)
                    .receiveHWMgr(mgr, LoginResultListener.Result.LOGGED_IN);
                listenersWaitingForMgr.remove(i);
              }
              loggedIn = true;
              creatingManager = false;
              nonSilentListenerPresent = false;
              break;
            case INVALID_CREDENTIALS:
              // TODO: Possibly open LoginActivity if not silent
              for (int i = listenersWaitingForMgr.size() - 1; i >= 0; i--) {
                listenersWaitingForMgr.get(i).receiveHWMgr(null,
                  LoginResultListener.Result.INVALID_CREDENTIALS);
                listenersWaitingForMgr.remove(i);
              }
              loggedIn = false;
              creatingManager = false;
              nonSilentListenerPresent = false;
              break;
            default:
              for (int i = listenersWaitingForMgr.size() - 1; i >= 0; i--) {
                listenersWaitingForMgr.get(i).receiveHWMgr(null, result);
                listenersWaitingForMgr.remove(i);
              }
              loggedIn = false;
              creatingManager = false;
              nonSilentListenerPresent = false;
              break;
          }
        });
      }
    }
  }

  private static void login(Context ctx, LoginResultListener listener) {
    try {
      mgr.connect(provider).registerListener(connFuture -> {
        if (connFuture.isPresent()) {
          listener.onLoginDone(LoginResultListener.Result.CONNECTION_FAILED);
        } else {
          mgr.isCompatible().registerListener(compatibleFuture -> {
            IHWFuture<Boolean> compFuture = (IHWFuture<Boolean>) compatibleFuture;
            if (!compFuture.get()) {
              listener.onLoginDone(LoginResultListener.Result.SERVER_INCOMPATIBLE);
            } else {

              IHWFutureListener l = (loginFuture -> {
                IHWFuture<IHWUser> userFuture = (IHWFuture<IHWUser>) loginFuture;
                if (userFuture.errorCode() != IHWFuture.ERRORCodes.LOGGEDIN) {
                  Log.d(TAG, "Error code: " + userFuture.errorCode());
                  if (userFuture.errorCode() == IHWFuture.ERRORCodes.INVALIDCREDERR) {
                    listener.onLoginDone(LoginResultListener.Result.INVALID_CREDENTIALS);
                  } else if (userFuture.errorCode() == IHWFuture.ERRORCodes.NOTFOUNDERR) {
                    // TODO: Consider giving group not found a special Result
                    listener.onLoginDone(LoginResultListener.Result.INVALID_CREDENTIALS);
                  } else if (userFuture.errorCode() == IHWFuture.ERRORCodes.EXPIRED) {
                    // Try again with credentials if session is invalid
                    // TODO
                    Log.d(TAG, "Server says token expired, retrying with creds");
                    session = null;
                    mgr.release();
                    login(ctx, listener);
                  } else {
                    listener.onLoginDone(LoginResultListener.Result.UNKNOWN);
                  }
                } else {
                  session = userFuture.get().session();
                  saveCredentials(ctx);
                  listener.onLoginDone(LoginResultListener.Result.LOGGED_IN);
                }
              });

              if (session != null && !isSessionExpired(session)) {
                Log.d(TAG, "Connecting with session token.");
                mgr.login(session.getToken()).registerListener(l);
              } else {
                Log.d(TAG, "Connecting with credentials.");
                Log.d(TAG, "Session is: " + session);
                mgr.login(group, user, auth).registerListener(l);
              }
            }
          });
        }
      });
    } catch (StillConnectedException e) {
      Log.i(TAG, "StillConnectedException, reconnecting");
      mgr.release();
      login(ctx, listener);
    }
  }


  private static boolean isSessionExpired(IHWSession session) {
    Calendar cal = Calendar.getInstance();

    int[] d = session.expires();
    cal.set(d[0], d[1], d[2]);

    if (cal.compareTo(Calendar.getInstance()) != 1) {
      return true;
    }
    return false;
  }
}
