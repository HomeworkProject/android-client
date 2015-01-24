/*
 * Copyright (c) 2015  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package paarmann.physikprofil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class Reminder {

  public static final String TAG = "Reminder";

  private List<HAElement> homework;
  private Date newDate;

  // Use the old interface for now, until we're ready to move over
  @Deprecated
  public String ssp;
  @Deprecated
  public String title;
  @Deprecated
  public String date;

  public String getTitle() {
    if (homework != null) {
      // This will be used when we are ready to move reminders over completely
      if (homework.isEmpty()) {
        return "";
      }

      String title = homework.get(0).title;
      for (int i = 1; i < homework.size(); i++) {
        title += ", " + homework.get(i).title;
      }

      return title;
    } else {
      // If still using the old format
      return title;
    }
  }

  public Date getDate() {
    return newDate;
  }

  @Deprecated
  public static List<Reminder> createFromSspSet(Set<String> input) {
    List<Reminder> reminders = new ArrayList<Reminder>();
    for (String ssp : input) {
      Reminder reminder = new Reminder();
      reminder.ssp = ssp;
      reminder.title = "";
      String[] strElements = reminder.ssp.split("\\\\");
      reminder.date = strElements[0];
      for (int i = 1; i < strElements.length; i++) {
        if (strElements[i] != null && strElements[i].length() != 0) {
          String[] parts = strElements[i].split("~");
          reminder.title += parts[2];
          if (i != strElements.length - 1) {
            reminder.title += ", ";
          }
        }
      }
      reminders.add(reminder);
    }

    return reminders;
  }
}
