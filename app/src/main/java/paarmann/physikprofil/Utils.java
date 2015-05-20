/*
 * Copyright (c) 2015  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */
package paarmann.physikprofil;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public abstract class Utils {

  /**
   * Get the difference of two dates.
   *
   * @param date1    the older date
   * @param date2    the newer date
   * @param timeUnit the unit in which you want the diff
   * @return the diff value, in the provided unit
   */
  public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
    long diffInMillies = date2.getTime() - date1.getTime();
    return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
  }
}
