/*
 * Copyright (c) 2016  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package de.s_paarmann.homeworkapp.network;

import android.content.Context;

import de.mlessmann.homework.api.error.Error;
import de.mlessmann.homework.api.future.IHWFuture;
import de.mlessmann.homework.api.homework.IHWCarrier;
import de.mlessmann.homework.api.homework.IHomework;
import de.s_paarmann.homeworkapp.AutomaticReminderManager;
import de.s_paarmann.homeworkapp.HAElement;
import de.s_paarmann.homeworkapp.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

// TODO: Caching
public class HomeworkManager {

  public interface GetHWListener {
    public void onHomeworkReceived(List<HAElement> homework,
                                   LoginResultListener.Result loginResult,
                                   Object error);
  }

  public interface AddHWListener {
    public void onHomeworkAdded(int result);
  }

  public interface DeleteHWListener {
    public void onHomeworkDeleted(int result);
  }

  public static final String TAG = "HomeworkManager";

  public static void getHomework(Context ctx, Date date, GetHWListener listener) {
    getHomework(ctx, date, listener, false);
  }

  public static void getHomework(Context ctx, Date date, GetHWListener listener, boolean silent) {
    LoginManager.getHWMgr(ctx, (mgr, result) -> {

      if (result == LoginResultListener.Result.LOGGED_IN) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        mgr.getHWOn(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)).registerListener(future -> {
          OnGetHWDone(ctx, (IHWFuture<List<IHomework>>) future, listener, result);
        });
      } else {
        OnGetHWDone(ctx, null, listener, result);
      }
    }, silent);
  }

  public static void getHomework(Context ctx, Date startDate, Date endDate,
                                 GetHWListener listener) {
    getHomework(ctx, startDate, endDate, listener, false);
  }

  public static void getHomework(Context ctx, Date startDate, Date endDate,
                                 GetHWListener listener, boolean silent) {
    LoginManager.getHWMgr(ctx, (mgr, result) -> {

      if (result == LoginResultListener.Result.LOGGED_IN) {
        Calendar calStart = Calendar.getInstance();
        Calendar calEnd = Calendar.getInstance();
        calStart.setTime(startDate);
        calEnd.setTime(endDate);
        mgr.getHWBetween(calStart.get(Calendar.YEAR), calStart.get(Calendar.MONTH) + 1,
            calStart.get(Calendar.DAY_OF_MONTH),
            calEnd.get(Calendar.YEAR), calEnd.get(Calendar.MONTH) + 1,
            calEnd.get(Calendar.DAY_OF_MONTH)).registerListener(future -> {
          OnGetHWDone(ctx, (IHWFuture<List<IHomework>>) future, listener, result);
        });
      } else {
        OnGetHWDone(ctx, null, listener, result);
      }
    }, silent);
  }

  public static void addHomework(Context ctx, HAElement element, AddHWListener listener) {
    addHomework(ctx, element, listener, false);
  }

  public static void addHomework(Context ctx, HAElement element, AddHWListener listener,
                                 boolean silent) {
    LoginManager.getHWMgr(ctx, (connection, result) -> {
      if (result == LoginResultListener.Result.LOGGED_IN) {
        IHWCarrier.Builder builder = IHWCarrier.Builder.builder();
        IHWCarrier.JSONBuilder jsonBuilder = new IHWCarrier.JSONBuilder();

        String[] d = element.date.split("-");
        jsonBuilder.date(Integer.parseInt(d[0]),
            Integer.parseInt(d[1]),
            Integer.parseInt(d[2]));
        jsonBuilder.subject(element.subject);
        jsonBuilder.title(element.title);
        jsonBuilder.description(element.desc);

        IHWCarrier carrier = builder.json(jsonBuilder.build()).build();

        connection.postHW(carrier).registerListener(future -> {
          if (((IHWFuture) future).getError() == Error.OK) {
            OnAddHWDone((IHWFuture<Boolean>) future, listener);
          } else {
            OnAddHWDone(null, listener);
          }
        });
      } else {
        OnAddHWDone(null, listener);
      }
    }, silent);
  }

  public static void deleteHomework(Context ctx, HAElement element, DeleteHWListener listener) {
    deleteHomework(ctx, element, listener, false);
  }

  public static void deleteHomework(Context ctx, HAElement element, DeleteHWListener listener,
                                    boolean silent) {
    LoginManager.getHWMgr(ctx, (connection, result) -> {
      if (result == LoginResultListener.Result.LOGGED_IN) {
        String[] sd = element.date.split("-");
        int[] d = new int[]{
            Integer.parseInt(sd[0]),
            Integer.parseInt(sd[1]),
            Integer.parseInt(sd[2]),
            };

        connection.delHW(element.id, d[0], d[1], d[2]).registerListener(future -> {
          OnDeleteHWDone((IHWFuture<Boolean>) future, listener);
        });
      } else {
        OnDeleteHWDone(null, listener);
      }
    }, silent);
  }

  private static void OnGetHWDone(Context ctx, IHWFuture<List<IHomework>> hwFuture,
                                  GetHWListener listener, LoginResultListener.Result loginResult) {
    HAElement error = new HAElement();
    error.id = "0";
    error.flags = HAElement.FLAG_ERROR;
    error.date = "";
    error.title = "Fehler";
    error.desc = "Beim Herunterladen der Hausaufgaben ist ein Fehler aufgetreten.";
    List<HAElement> list = new ArrayList<>(1);

    if (hwFuture == null) {
      if (loginResult == LoginResultListener.Result.CONNECTION_FAILED) {
        error.subject = "Verbindung fehlgeschlagen.";
      } else {
        error.subject = "Fehler beim einloggen.";
      }
    } else if (hwFuture.getError() != Error.OK) {
      Log.e(TAG, "GetHW returned error: " + hwFuture.getError());
      error.subject = String.valueOf(hwFuture.getError().getCode()); // TODO
    }
    if (hwFuture == null || hwFuture.getError() != Error.OK) {
      list.add(error);
      listener.onHomeworkReceived(list, loginResult, hwFuture == null ? null : hwFuture.getError());
      return;
    }

    List<IHomework> hwObjs = hwFuture.get();
    List<HAElement> elements = new ArrayList<>(hwObjs.size());

    for (IHomework obj : hwObjs) {
      HAElement elem = new HAElement();
      elem.id = obj.getId();
      int[] d = obj.getDate();
      elem.date = d[0] + "-" + d[1] + "-" + d[2];
      elem.subject = obj.getSubject();
      elem.title = obj.getTitle();
      elem.desc = obj.getDescription();
      elements.add(elem);
    }

    AutomaticReminderManager.setReminders(ctx, elements);

    listener.onHomeworkReceived(elements, loginResult, hwFuture.getError());
  }

  private static void OnAddHWDone(IHWFuture<Boolean> future, AddHWListener listener) {
    listener
        .onHomeworkAdded(future == null ? Error.ErrorCode.UNKNOWN : future.getError().getCode());
  }

  private static void OnDeleteHWDone(IHWFuture<Boolean> future, DeleteHWListener listener) {
    listener
        .onHomeworkDeleted(future.getError().getCode());
  }

}
