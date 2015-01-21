/*
 * Copyright (c) 2015  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */
package paarmann.physikprofil;

public final class Log {

  /*
   * NOTE: I would have preferred to just subclass android.util.Log but
   * apparently this is not wanted. That class is declared final, so it
   * is not possible to extend it. Really don't know why. Would have been
   * a lot cleaner.
   */

  public static final String TAG = "Log";

  public static final String APP_NAME = "PhysikBioProfil";

  private Log() {
  }

  public static String formatTag(String tag) {
    return APP_NAME + ", " + tag;
  }

  public static String getStackTraceString(Throwable tr) {
    return android.util.Log.getStackTraceString(tr);
  }

  public static int v(String tag, String msg) {
    return v(tag, msg, null);
  }

  public static int v(String tag, String msg, Throwable tr) {
    return println(android.util.Log.VERBOSE,
        tag, msg, tr);
  }

  public static int d(String tag, String msg) {
    return d(tag, msg, null);
  }

  public static int d(String tag, String msg, Throwable tr) {
    return println(android.util.Log.DEBUG,
        tag, msg, tr);
  }

  public static int i(String tag, String msg) {
    return i(tag, msg, null);
  }

  public static int i(String tag, String msg, Throwable tr) {
    return println(android.util.Log.INFO,
        tag, msg, tr);
  }

  public static int w(String tag, String msg) {
    return w(tag, msg, null);
  }

  public static int w(String tag, Throwable tr) {
    return w(tag, "", tr);
  }

  public static int w(String tag, String msg, Throwable tr) {
    return println(android.util.Log.WARN,
        tag, msg, tr);
  }

  public static int e(String tag, String msg) {
    return e(tag, msg, null);
  }

  public static int e(String tag, String msg, Throwable tr) {
    return println(android.util.Log.ERROR,
        tag, msg, tr);
  }

  public static int wtf(String tag, String msg) {
    return wtf(tag, msg, null);
  }

  public static int wtf(String tag, Throwable tr) {
    return wtf(tag, "", tr);
  }

  public static int wtf(String tag, String msg, Throwable tr) {
    return println(android.util.Log.ASSERT,
        tag, msg, tr);
  }

  public static int println(int priority, String tag, String msg) {
    return android.util.Log.println(priority, formatTag(tag), msg);
  }

  public static int println(int priority, String tag, String msg, Throwable tr) {
    if (tr == null) {
      return println(priority, tag, msg);
    }

    String stackTrace = getStackTraceString(tr);
    String[] stackLines = stackTrace.split("\n");

    int bytes = 0;

    bytes += println(priority, tag, msg);
    for (String s : stackLines) {
      bytes += println(priority, tag, s);
    }

    return bytes;
  }

}
