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
import java.util.Locale;
import java.util.Scanner;

/**
 * Represents a homework element and provides various helper methods.
 */
public class HAElement implements java.io.Serializable {

  private static final long serialVersionUID = 1L;

  /** Was marked as done by user */
  public static int FLAG_DONE = 0x1;
  /** Is a pseudo element for displaying a warning to the user */
  public static int FLAG_WARN = 0x2;
  /** Is a pseudo element for displaying an error to the user */
  public static int FLAG_ERROR = 0x4;
  /** Is a pseudo element for displaying an information to the user */
  public static int FLAG_INFO = 0x8;

  public String id;
  public int flags;
  /** Due date of the element, must be of the form yyyy-MM-dd. */
  public String date;
  public String title;
  public String subject;
  public String desc;

  private SimpleDateFormat dateFormat;

  public HAElement() {
    flags = 0x0;
    dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
  }

  /**
   * Constructs a scheme-specific part to represent this element in URIs.
   * Should not be used anymore, representation of homework elements in URIs is no longer necessary.
   *
   * @return the scheme-specific part
   */
  @Deprecated
  public String getSsp() {
    return id + "~" + date + "~" + title + "~" + subject + "~" + desc;
  }

  /**
   * Parses the date of this element to a {@code Date} object.
   *
   * @return the parsed {@link Date}
   * @throws ParseException if the date is not in the format yyyy-MM-dd.
   */
  public Date getDate() throws ParseException {
    return dateFormat.parse(date);
  }

  /**
   * Parses the date of this element to {@code Date} object, using the specified format.
   *
   * @param format the date format to use when parsing the date. See {@link SimpleDateFormat} for formatting values.
   * @return the parsed {@link Date}
   * @throws ParseException if the date is not in the specified format.
   */
  public String getDate(String format) throws ParseException {
    return new SimpleDateFormat(format).format(getDate());
  }

  /**
   * Creates homework elements based on a scheme-specific part.
   * <p>
   * This expects the ssp to be in the following format: {@code id1~date1~title1~subject1~desc1\id2~date2~title2...}.
   *
   * @param input the scheme-specific part from which to create the elements
   * @return the created homework elements
   */
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

  /**
   * Creates a single homework element from a string of the following format: {@code id~date~title~subject~desc}.
   *
   * @param input the string from which to create the element
   * @return the created homework element, null if string does not contain {@code ~}
   */
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
    elem.id = String.valueOf(id);
    elem.date = props.next();
    elem.title = props.next();
    elem.subject = props.next();
    elem.desc = props.next();
    return elem;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    HAElement haElement = (HAElement) o;

    if (!id.equals(haElement.id)) {
      return false;
    }
    if (flags != haElement.flags) {
      return false;
    }
    if (!date.equals(haElement.date)) {
      return false;
    }
    if (!title.equals(haElement.title)) {
      return false;
    }
    if (!subject.equals(haElement.subject)) {
      return false;
    }
    return desc.equals(haElement.desc);

  }

  @Override
  public int hashCode() {
    int result = id.hashCode();
    result = 31 * result + flags;
    result = 31 * result + date.hashCode();
    result = 31 * result + title.hashCode();
    result = 31 * result + subject.hashCode();
    result = 31 * result + desc.hashCode();
    return result;
  }
}
