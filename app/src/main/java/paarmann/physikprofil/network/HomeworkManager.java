/*
 * Copyright (c) 2016  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package paarmann.physikprofil.network;

import de.mlessmann.api.data.IHWFuture;
import de.mlessmann.api.data.IHWObj;
import de.mlessmann.api.main.HWMgr;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import paarmann.physikprofil.HAElement;
import paarmann.physikprofil.Log;

// TODO: Caching
public class HomeworkManager {

  public interface GetHWListener {
    public void onHomeworkReceived(List<HAElement> homework);
  }

  public static final String TAG = "HomeworkManager";

  public static void getHomework(HWMgr mgr, Date date, GetHWListener listener) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    mgr.getHWOn(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)).registerListener(future -> {
      OnGetHWDone((IHWFuture<List<IHWObj>>) future, listener);
    });
  }

  public static void getHomework(HWMgr mgr, Date startDate, Date endDate,
                                 GetHWListener listener) {
    Calendar calStart = Calendar.getInstance();
    Calendar calEnd = Calendar.getInstance();
    calStart.setTime(startDate);
    calEnd.setTime(endDate);
    mgr.getHWBetween(calStart.get(Calendar.YEAR), calStart.get(Calendar.MONTH) + 1,
        calStart.get(Calendar.DAY_OF_MONTH),
        calEnd.get(Calendar.YEAR), calEnd.get(Calendar.MONTH) + 1,
        calEnd.get(Calendar.DAY_OF_MONTH)).registerListener(future -> {
      OnGetHWDone((IHWFuture<List<IHWObj>>) future, listener);
    });
  }

  private static void OnGetHWDone(IHWFuture<List<IHWObj>> hwFuture, GetHWListener listener) {
    if (hwFuture.errorCode() != IHWFuture.ERRORCodes.OK) {
      Log.e(TAG, "GetHW returned error: " + hwFuture.errorCode());
      HAElement error = new HAElement();
      error.id = "0";
      error.flags = HAElement.FLAG_ERROR;
      error.date = "";
      error.title = "Fehler";
      error.subject = String.valueOf(hwFuture.errorCode()); // TODO
      error.desc = "Beim herunterladen der Hausaufgaben ist ein Fehler aufgetreten.";
      List<HAElement> list = new ArrayList<>(1);
      list.add(error);
      listener.onHomeworkReceived(list);
      return;
    }

    List<IHWObj> hwObjs = hwFuture.get();
    List<HAElement> elements = new ArrayList<>(hwObjs.size());

    for (IHWObj obj : hwObjs) {
      // TODO
      HAElement elem = new HAElement();
      elem.id = obj.id();
      int[] d = obj.date();
      elem.date = d[0] + "-" + d[1] + "-" + d[2];
      elem.subject = obj.subject();
      elem.title = "No titles yet"; // TODO: Possibly titles as optionals
      elem.desc = obj.getDescription(true);
      elements.add(elem);
    }

    listener.onHomeworkReceived(elements);
  }

}
