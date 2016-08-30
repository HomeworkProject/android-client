/*
 * Copyright (c) 2015  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

/**
 * Created by basti on 26.09.15.
 */
package paarmann.physikprofil.test;

import org.junit.Test;

import paarmann.physikprofil.HAElement;

import static org.junit.Assert.*;

public class HAElementTest {

  private HAElement createElement(String id, String date, String title,
                                  String subject, String desc) {
    HAElement e = new HAElement();
    e.id = id;
    e.date = date;
    e.title = title;
    e.subject = subject;
    e.desc = desc;
    return e;
  }

  @Test
  public void testCreateSingleFromString()
  {
    HAElement actual = HAElement.createSingleFromString(
        "42~2015-01-01~Some Homework~testing~This is a test item");
    HAElement expected = createElement("42", "2015-01-01", "Some Homework",
        "testing", "This is a test item");

    assertEquals(expected, actual);
  }

  @Test
  public void testCreateFromSsp()
  {
    String ssp = "1~2015-01-01~Test 1~testing~A test item\\"
        + "2~2015-01-02~Test 2~testing~Another test item";
    HAElement[] actual = HAElement.createFromSsp(ssp)
      .toArray(new HAElement[2]);
    HAElement[] expected = {
        createElement("1", "2015-01-01", "Test 1", "testing", "A test item"),
        createElement("2", "2015-01-02", "Test 2", "testing", "Another test item")
    };
    assertArrayEquals(expected, actual);
  }

  @Test
  public void testGetSsp() {
    String actual = createElement("1", "2015-01-01", "Test", "testing", "Test")
        .getSsp();
    String expected = "1~2015-01-01~Test~testing~Test";
    assertEquals(expected, actual);
  }
}
