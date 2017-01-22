/*
 * Copyright (c) 2016  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package de.s_paarmann.homeworkapp.network;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import de.mlessmann.common.parallel.IFutureListener;
import de.mlessmann.homework.api.CDK;
import de.mlessmann.homework.api.ICDKConnection;
import de.mlessmann.homework.api.error.Error;
import de.mlessmann.homework.api.event.ICDKConnectionEvent;
import de.mlessmann.homework.api.event.ICDKEvent;
import de.mlessmann.homework.api.event.ICDKListener;
import de.mlessmann.homework.api.event.ICDKLogEvent;
import de.mlessmann.homework.api.event.network.ConnectionStatus;
import de.mlessmann.homework.api.event.network.InterruptReason;
import de.mlessmann.homework.api.future.IHWFuture;
import de.mlessmann.homework.api.logging.IHWLogContext;
import de.mlessmann.homework.api.logging.ILogLevel;
import de.mlessmann.homework.api.logging.LogType;
import de.mlessmann.homework.api.provider.IHWProvider;
import de.mlessmann.homework.api.session.IHWSession;
import de.mlessmann.homework.api.session.IHWUser;
import de.mlessmann.homework.internal.event.CDKConnInterruptEvent;
import de.mlessmann.homework.internal.homework.HWSession;
import de.mlessmann.homework.internal.providers.HWProvider;
import de.s_paarmann.homeworkapp.Log;
import de.s_paarmann.homeworkapp.ui.MainActivity;
import de.s_paarmann.homeworkapp.ui.login.LoginActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class LoginManager {

  public interface GetConnectionListener {
    public void receiveConnection(ICDKConnection conn, LoginResultListener.Result result);
  }

  public static final String TAG = "LoginManager";

  // TODO: Don't store credentials in shared preferences
  private static IHWProvider provider;
  private static String group, user, auth;

  private static IHWSession session;
  private static boolean loggedIn = false;
  private static ICDKConnection connection;
  private static final List<GetConnectionListener> listenersWaitingForMgr = new ArrayList<>();
  private static boolean nonSilentListenerPresent = false;
  private static boolean creatingManager = false;
  private static boolean waitingForLoginActivity = false;

  public static ICDKListener LogListener = new ICDKListener() {
    @Override
    public void onEvent(ICDKEvent event) {
      if (event instanceof ICDKConnectionEvent) {
        if (((ICDKConnectionEvent) event).getStatus() == ConnectionStatus.DISCONNECTED) {
          loggedIn = false;
        }
      }

      if (!(event instanceof ICDKLogEvent)) {
        return;
      }
      ICDKLogEvent logEvent = (ICDKLogEvent) event;
      IHWLogContext context = logEvent.getContext();

      String msg = context.getSender().toString() + ": " + context.getPayload().toString();
      int level = context.getLevel();
      boolean isException = context.getType().equals(LogType.EXC)
                            || context.getType().equals(LogType.CDKEXC);
      if (level == ILogLevel.DEBUG) {
        if (isException) {
          Log.d("CDK", msg, (Exception) context.getPayload());
        } else {
          Log.d("CDK", msg);
        }
      } else if (level == ILogLevel.WARNING) {
        if (isException) {
          Log.w("CDK", msg, (Exception) context.getPayload());
        } else {
          Log.w("CDK", msg);
        }
      } else if (level == ILogLevel.SEVERE) {
        if (isException) {
          Log.e("CDK", msg, (Exception) context.getPayload());
        } else {
          Log.e("CDK", msg);
        }
      } else {
        if (isException) {
          Log.i("CDK", msg, (Exception) context.getPayload());
        } else {
          Log.i("CDK", msg);
        }
      }
    }

  };

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

  public static void removeCredentials(Context ctx) {
    provider = null;
    group = null;
    user = null;
    auth = null;
    session = null;
    loggedIn = false;

    if (listenersWaitingForMgr != null && listenersWaitingForMgr.size() > 0) {
      for (int i = listenersWaitingForMgr.size() - 1; i >= 0; i--) {
        listenersWaitingForMgr.get(i).receiveConnection(null,
            LoginResultListener.Result.NO_CREDENTIALS_PRESENT);
        listenersWaitingForMgr.remove(i);
      }
    }
    creatingManager = false;
    waitingForLoginActivity = false;

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

      HWProvider myProvider = new HWProvider(new JSONObject(prefs.getString(MainActivity.PREF_PROVIDER, "")));
      if (myProvider.isValid()) {
        provider = myProvider;
      } else {
        return false;
      }
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

    if (group == null) {
      editor.remove(MainActivity.PREF_CRED_GROUP);
    } else {
      editor.putString(MainActivity.PREF_CRED_GROUP, group);
    }

    if (user == null) {
      editor.remove(MainActivity.PREF_CRED_USER);
    } else {
      editor.putString(MainActivity.PREF_CRED_USER, user);
    }

    if (auth == null) {
      editor.remove(MainActivity.PREF_CRED_AUTH);
    } else {
      editor.putString(MainActivity.PREF_CRED_AUTH, auth);
    }

    if (session == null) {
      editor.remove(MainActivity.PREF_CRED_TOKEN);
    } else {
      editor.putString(MainActivity.PREF_CRED_TOKEN, session.getJSON().toString());
    }

    if (provider == null) {
      editor.remove(MainActivity.PREF_PROVIDER);
    } else {
      editor.putString(MainActivity.PREF_PROVIDER, provider.getJSON().toString());
    }

    editor.apply();
  }

  public static void userCanceledLoginActivity() {
    synchronized (listenersWaitingForMgr) {
      waitingForLoginActivity = false;

      for (int i = listenersWaitingForMgr.size() - 1; i >= 0; i--) {
        listenersWaitingForMgr.get(i).receiveConnection(null,
            LoginResultListener.Result.NO_CREDENTIALS_PRESENT);
        listenersWaitingForMgr.remove(i);
      }

      creatingManager = false;
      loggedIn = false;
      nonSilentListenerPresent = false;
    }
  }

  public synchronized static void getHWMgr(Context ctx, GetConnectionListener l) {
    getHWMgr(ctx, l, false, false);
  }

  public synchronized static void getHWMgr(Context ctx, GetConnectionListener l, boolean silent) {
    getHWMgr(ctx, l, silent, false);
  }

  public synchronized static void getHWMgr(Context ctx, GetConnectionListener listener,
                                           boolean silent,
                                           boolean loginActivity) {
    if (loggedIn) {
      listener.receiveConnection(connection, LoginResultListener.Result.LOGGED_IN);
      return;
    }

    synchronized (listenersWaitingForMgr) {
      listenersWaitingForMgr.add(listener);
      if (!silent) {
        nonSilentListenerPresent = true;
      }

      if ((creatingManager || waitingForLoginActivity) && !loginActivity) {
        return;
      }

      creatingManager = true;

      CDK.getInstance().registerListener(LogListener);

      if (!loadCredentials(ctx)) {
        if (!nonSilentListenerPresent) {
          for (int i = listenersWaitingForMgr.size() - 1; i >= 0; i--) {
            listenersWaitingForMgr.get(i).receiveConnection(null,
                LoginResultListener.Result.NO_CREDENTIALS_PRESENT);
            listenersWaitingForMgr.remove(i);
          }
          creatingManager = false;
          return;
        } else {
          waitingForLoginActivity = true;
          try {
            ctx.startActivity(new Intent(ctx, LoginActivity.class));
          } catch (ClassCastException e) {
            waitingForLoginActivity = false;
            for (int i = listenersWaitingForMgr.size() - 1; i >= 0; i--) {
              listenersWaitingForMgr.get(i)
                  .receiveConnection(null, LoginResultListener.Result.NO_CREDENTIALS_PRESENT);
              listenersWaitingForMgr.remove(i);
            }
            creatingManager = false;
            return;
          }
        }
      } else {
        login(ctx, result -> {
          switch (result) {
            case LOGGED_IN:
              for (int i = listenersWaitingForMgr.size() - 1; i >= 0; i--) {
                listenersWaitingForMgr.get(i)
                    .receiveConnection(connection, LoginResultListener.Result.LOGGED_IN);
                listenersWaitingForMgr.remove(i);
              }
              loggedIn = true;
              creatingManager = false;
              nonSilentListenerPresent = false;
              break;
            case INVALID_CREDENTIALS:
              for (int i = listenersWaitingForMgr.size() - 1; i >= 0; i--) {
                listenersWaitingForMgr.get(i).receiveConnection(null,
                    LoginResultListener.Result.INVALID_CREDENTIALS);
                listenersWaitingForMgr.remove(i);
              }
              loggedIn = false;
              creatingManager = false;
              nonSilentListenerPresent = false;
              break;
            default:
              for (int i = listenersWaitingForMgr.size() - 1; i >= 0; i--) {
                listenersWaitingForMgr.get(i).receiveConnection(null, result);
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
    CDK.getInstance().registerListener(new ICDKListener() {
      @Override
      public void onEvent(ICDKEvent event) {
        if (!(event instanceof ICDKConnectionEvent)) {
          return;
        }

        ICDKConnectionEvent connEvent = (ICDKConnectionEvent) event;
        if (connEvent.getConnection() != connection) {
          connEvent.getConnection().kill();
          return;
        }

        if (connEvent.getStatus() == ConnectionStatus.CONNECTING) {
          return;
        }

        if (connEvent.getStatus() == ConnectionStatus.CONNECTING_INTERRUPTED) {
          CDKConnInterruptEvent interruptEvent = (CDKConnInterruptEvent) connEvent;
          if ( interruptEvent.getInterruptReason() == InterruptReason.POSSIBLY_INCOMPATIBLE) {
            listener.onLoginDone(LoginResultListener.Result.SERVER_INCOMPATIBLE);
            CDK.getInstance().unregisterListener(this);
          }
          return;
        }

        if (connEvent.getStatus() != ConnectionStatus.CONNECTED) {
          listener.onLoginDone(LoginResultListener.Result.CONNECTION_FAILED);
        } else {
          IFutureListener l = (loginFuture -> {
            IHWFuture<IHWUser> userFuture = (IHWFuture<IHWUser>) loginFuture;
            if (userFuture.getError() != Error.OK) {
              Log.d(TAG, "Error code: " + userFuture.getError());
              int errorCode = userFuture.getError().getCode();
              if (errorCode == Error.ErrorCode.BADLOGIN) {
                listener.onLoginDone(LoginResultListener.Result.INVALID_CREDENTIALS);
              } else if (errorCode == Error.ErrorCode.NOTFOUND) {
                listener.onLoginDone(LoginResultListener.Result.INVALID_CREDENTIALS);
              } else if (errorCode == Error.ErrorCode.CLOSED) {
                listener.onLoginDone(LoginResultListener.Result.CONNECTION_CLOSED);
              } else if (errorCode == Error.ErrorCode.BADTOKEN) {
                Log.d(TAG, "Server says token expired, retrying with creds");
                session = null;
                connection.kill();
                CDK.getInstance().unregisterListener(this);
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

          CDK.getInstance().unregisterListener(this);
          if (session != null && !isSessionExpired(session)) {
            Log.d(TAG, "Connecting with session token.");
            connection.login(session).registerListener(l);
          } else {
            Log.d(TAG, "Connecting with credentials.");
            Log.d(TAG, "Session is: " + session);
            connection.login(group, user, auth).registerListener(l);
          }
        }
      }
    });

    connection = CDK.getInstance().connect(provider);
    connection.start();
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
