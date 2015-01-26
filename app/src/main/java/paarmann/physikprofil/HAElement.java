/*
 * Copyright (c) 2015  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package paarmann.physikprofil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class HAElement implements java.io.Serializable {

  private static final long serialVersionUID = 1L;

  public static int FLAG_DONE = 0x1;
  public static int FLAG_WARN = 0x2;
  public static int FLAG_ERROR = 0x4;

  public int id;
  public int flags;
  public String date;
  public String title;
  public String subject;
  public String desc;

  private SimpleDateFormat dateFormat;

  public HAElement() {
    flags = 0x0;
    dateFormat = new SimpleDateFormat("yyyy-MM-dd");
  }

  @Deprecated
  public String getSsp() {
    return id + "~" + date + "~" + title + "~" + subject;
  }

  public Date getDate() throws ParseException {
    return dateFormat.parse(date);
  }

  public String getDate(String format) throws ParseException {
    return new SimpleDateFormat(format).format(getDate());
  }

  public static List<HAElement> createFromSsp(String input) {
    List<HAElement> homework = new ArrayList<HAElement>();

    Scanner elements = new Scanner(input);
    elements.useDelimiter("\\\\");
    while (elements.hasNext()) {
      HAElement elem = createSingleFromString(elements.next());
      if (elem != null) {
        homework.add(elem);
      }
    }

    return homework;
  }

  public static HAElement createSingleFromString(String input) {
    HAElement elem = new HAElement();
    Scanner props = new Scanner(input);
    props.useDelimiter("~");
    if (!props.hasNext()) {
      return null;
    }

    int id = Integer.valueOf(props.next());
    if (id == 0) {
      elem.date = "";
      elem.title = "Keine Hausaufgaben!";
      elem.subject = "";
      elem.desc = "Wir haben keine Hausaufgaben!";
      return elem;
    }
    elem.id = id;
    elem.date = props.next();
    elem.title = props.next();
    elem.subject = props.next();
    elem.desc = props.next();
    return elem;
  }
}
