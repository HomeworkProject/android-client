/*
 * Copyright (c) 2015  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

/**
 * Created by basti on 26.09.15.
 */
package paarmann.physikprofil.test;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import paarmann.physikprofil.HAElement;

import static org.junit.Assert.*;

public class HAElementTest {

  @Test
  public void testCreateSingleFromString()
  {
    HAElement actual = HAElement.createSingleFromString(
        "42~2015-01-01~Some Homework~testing~This is a test item");
    HAElement expected = new HAElement();
    expected.id = 42;
    expected.date = "2015-01-01";
    expected.title = "Some Homework";
    expected.subject = "testing";
    expected.desc = "This is a test item";

    assertEquals(expected, actual);
  }

  @Test
  public void testCreateFromSsp()
  {
    String ssp = "1~2015-01-01~Test 1~testing~A test item\\"
        + "2~2015-01-02~Test 2~testing~Another test item";
    HAElement[] actual = HAElement.createFromSsp(ssp)
      .toArray(new HAElement[2]);
    HAElement[] expected = new HAElement[2];
    HAElement expected1 = new HAElement();
    expected1.id = 1;
    expected1.date = "2015-01-01";
    expected1.title = "Test 1";
    expected1.subject = "testing";
    expected1.desc = "A test item";
    HAElement expected2 = new HAElement();
    expected2.id = 2;
    expected2.date = "2015-01-02";
    expected2.title = "Test 2";
    expected2.subject = "testing";
    expected2.desc = "Another test item";
    expected[0] = expected1;
    expected[1] = expected2;
    assertArrayEquals(expected, actual);
  }

}
