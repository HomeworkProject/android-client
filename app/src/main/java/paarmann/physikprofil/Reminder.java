/*
 * Copyright (c) 2015  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package paarmann.physikprofil;

import android.content.Context;
import android.net.Uri;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Reminder implements Serializable {

  private static final long serialVersionUID = 0L;

  public static final String TAG = "Reminder";

  private static final String REMINDER_FILE = "reminders.ser";
  private static final String DELETED_FILE = "deleted_reminders.ser";

  private static HashMap<Integer, Reminder> cachedDeletedReminders;
  private static boolean invalidatedDeletedReminders;

  public static final int FLAG_AUTO = 0x1;

  public int flags;
  private List<HAElement> homework;
  private Date date;

  public Reminder(Date date, List<HAElement> homework) {
    this.date = date;
    this.homework = homework;
    this.flags = 0x0;
  }

  public void save(Context context) {
    HashMap<Integer, Reminder> savedReminders = loadSavedRemindersInternal(context);
    savedReminders.put(getId(), this);
    try {
      FileOutputStream fos = context.openFileOutput(REMINDER_FILE, Context.MODE_PRIVATE);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(savedReminders);
      oos.close();
    } catch (IOException e) {
      Log.e(TAG, "Failed to save reminder: ", e);
    }
  }

  public void delete(Context context) {
    HashMap<Integer, Reminder> savedReminders = loadSavedRemindersInternal(context);
    savedReminders.remove(getId());

    try {
      FileOutputStream fos = context.openFileOutput(REMINDER_FILE, Context.MODE_PRIVATE);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(savedReminders);
      oos.close();
    } catch (IOException e) {
      Log.e(TAG, "Failed to delete reminder: ", e);
    }

    if ((flags & FLAG_AUTO) == FLAG_AUTO) {
      HashMap<Integer, Reminder> deletedReminders = loadDeletedRemindersInternal(context);
      try {
        FileOutputStream fos = context.openFileOutput(DELETED_FILE, Context.MODE_PRIVATE);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(deletedReminders);
        oos.close();
      } catch (IOException e) {
        Log.e(TAG, "Failed to mark automatic reminder as deleted: ", e);
      }
      invalidatedDeletedReminders = true;
    }
  }

  public String getTitle() {
    if (homework.isEmpty()) {
      return "";
    }

    String title = homework.get(0).title;
    for (int i = 1; i < homework.size(); i++) {
      title += ", " + homework.get(i).title;
    }

    return title;
  }

  public int getId() {
    if (homework.isEmpty()) {
      return -1;
    }
    int id = 0;
    for (HAElement elem : homework) {
      id += elem.id; // Not technically guaranteed to be unique, but unlikely to cause problems
    }

    return id;
  }

  public Date getDate() {
    return date;
  }

  public String getDate(String format) {
    return new SimpleDateFormat(format).format(date);
  }

  public List<HAElement> getHAElements() {
    return homework;
  }

  public Uri toUri() {
    String scheme = "reminder";
    String ssp = String.valueOf(getId());
    return Uri.fromParts(scheme, ssp, "");
  }

  public static Reminder fromUri(Context context, Uri uri) {
    HashMap<Integer, Reminder> map = loadSavedRemindersInternal(context);
    Integer id = Integer.valueOf(uri.getSchemeSpecificPart());
    return map.get(id);
  }

  public boolean wasDeleted(Context context) {
    Set<Reminder> deletedReminders = loadDeletedReminders(context);
    return deletedReminders.contains(this);
  }

  public static Set<Reminder> loadSavedReminders(Context context) {
    return toSet(loadSavedRemindersInternal(context));
  }

  private static HashMap<Integer, Reminder> loadSavedRemindersInternal(Context context) {
    try {
      FileInputStream fis = context.openFileInput(REMINDER_FILE);
      ObjectInputStream ois = new ObjectInputStream(fis);
      Object readObject = ois.readObject();
      ois.close();

      if (readObject != null && readObject instanceof HashMap) {
        return (HashMap<Integer, Reminder>) readObject;
      }
    } catch (IOException e) {
      Log.e(TAG, "Failed to load saved reminders: ", e);
    } catch (ClassNotFoundException e) {
      Log.e(TAG, "Failed to load saved reminders: ", e);
    }

    Log.w(TAG, "Could not load saved reminders, returning empty set");
    return new HashMap<Integer, Reminder>();
  }

  public static Set<Reminder> loadDeletedReminders(Context context) {
    return toSet(loadDeletedRemindersInternal(context));
  }

  private static HashSet<Reminder> toSet(HashMap<Integer, Reminder> map) {
    HashSet<Reminder> set = new HashSet<Reminder>();
    set.addAll(map.values());
    return set;
  }

  private static HashMap<Integer, Reminder> loadDeletedRemindersInternal(Context context) {
    if (!invalidatedDeletedReminders || cachedDeletedReminders == null) {
      return cachedDeletedReminders;
    }
    try {
      FileInputStream fis = context.openFileInput(DELETED_FILE);
      ObjectInputStream ois = new ObjectInputStream(fis);
      Object readObject = ois.readObject();
      ois.close();

      if (readObject != null && readObject instanceof HashMap) {
        cachedDeletedReminders = (HashMap<Integer, Reminder>) readObject;
        invalidatedDeletedReminders = false;
        return (HashMap<Integer, Reminder>) readObject;
      }
    } catch (IOException e) {
      Log.e(TAG, "Failed to load deleted reminders: ", e);
    } catch (ClassNotFoundException e) {
      Log.e(TAG, "Failed to load deleted reminders: ", e);
    }

    Log.w(TAG, "Could not load list of deleted reminders, returning empty set.");
    return new HashMap<Integer, Reminder>();
  }

  public static void cleanDeletedReminders(Context context) {
    HashMap<Integer, Reminder> deletedReminders = loadDeletedRemindersInternal(context);
    HashMap<Integer, Reminder> leftReminders = new HashMap<Integer, Reminder>();
    for (Map.Entry<Integer, Reminder> r : deletedReminders.entrySet()) {
      if (HomeworkUpdater.getDateDiff(r.getValue().getDate(), new Date(), TimeUnit.MINUTES) > 0) {
        leftReminders.put(r.getKey(), r.getValue());
      }
    }

    try {
      FileOutputStream fos = context.openFileOutput(DELETED_FILE, Context.MODE_PRIVATE);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(leftReminders);
      oos.close();
      Reminder.cachedDeletedReminders = leftReminders;
      invalidatedDeletedReminders = false;
    } catch (IOException e) {
      Log.e(TAG, "Failed to update saved deleted reminder list: ", e);
    }
  }

}
