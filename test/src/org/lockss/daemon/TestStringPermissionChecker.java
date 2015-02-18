/*
 * $Id$
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/
package org.lockss.daemon;

import org.lockss.test.*;
import java.io.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.filter.*;

/**
 * <p>Title: TestStringPermissionChecker</p>
 * <p>@author Claire Griffin</p>
 * <p>@version 1.0</p>
 */
public class TestStringPermissionChecker extends LockssTestCase {
  private StringPermissionChecker checker = null;
  private static String PERMISSION_STRING = "It's OK by me!";

  public void testCheckPermission() {
    StringBuffer sb = new StringBuffer("laa-dee-dah-LOCK-KCOL\n\n");
    sb.append(PERMISSION_STRING);
    sb.append("\n\nTheEnd!");
    String s_ok = sb.toString();
    String s_rev = sb.reverse().toString();

    checker = new StringPermissionChecker(PERMISSION_STRING);
    // check the correct string
    Reader reader = new StringReader(s_ok);
    assertTrue(checker.checkPermission(null, reader, null));

    // check an incorrect string
    reader = new StringReader(s_rev);
    assertFalse(checker.checkPermission(null, reader, null));

  }

  public void testCheckPermissionRequiresBackup() {
    checker = new StringPermissionChecker("ab");
    Reader reader = new StringReader("aab");
    assertTrue(checker.checkPermission(null, reader, null));
  }

  public void testSetIgnoreCaseFlag() {
    StringBuffer sb = new StringBuffer("laa-dee-dah-LOCK-KCOL\n\n");
    sb.append(PERMISSION_STRING);
    sb.append("\n\nTheEnd!");
    String s_ok = sb.toString();
    String s_lwr = s_ok.toLowerCase();

    // test IGNORE_CASE FALSE
    int bitNumber = StringPermissionChecker.IGNORE_CASE;
    checker = new StringPermissionChecker(PERMISSION_STRING);
    checker.setFlag(bitNumber, false);

    Reader reader = new StringReader(s_ok);
    assertTrue(checker.checkPermission(null, reader, null));

    reader = new StringReader(s_lwr);
    assertFalse(checker.checkPermission(null, reader, null));

    // test INGNORE_CASE TRUE
    checker.setFlag(bitNumber, true);
    reader = new StringReader(s_lwr);
    assertTrue(checker.checkPermission(null, reader, null));
  }

  public void testCheckCrawlPermissionWithWhitespace() {
    int firstWS = PERMISSION_STRING.indexOf(' ');
    if (firstWS <=0) {
      fail("No spaces in permission string, or starts with space");
    }

    String subStr1 = PERMISSION_STRING.substring(0, firstWS);
    String subStr2 = PERMISSION_STRING.substring(firstWS+1);

    // standard
    StringBuffer sb = new StringBuffer("laa-dee-dah-LOCK-KCOL\n\n");
    sb.append(subStr1);
    sb.append(' ');
    sb.append(subStr2);
    sb.append("\n\nTheEnd!");
    String testStr = sb.toString();
    checker = new StringPermissionChecker(PERMISSION_STRING,
                                          new WhiteSpaceFilterRule());
    Reader reader = new StringReader(testStr);
    assertTrue(checker.checkPermission(null, reader, null));

    // different whitespace
    sb = new StringBuffer("laa-dee-dah-LOCK-KCOL\n\n");
    sb.append(subStr1);
    sb.append("\n");
    sb.append(subStr2);
    sb.append("\n\nTheEnd!");
    testStr = sb.toString();

    reader = new StringReader(testStr);
    assertTrue(checker.checkPermission(null, reader, null));

    // extra whitespace
    sb = new StringBuffer("laa-dee-dah-LOCK-KCOL\n\n");
    sb.append(subStr1);
    sb.append(" \n\r\t ");
    sb.append(subStr2);
    sb.append("\n\nTheEnd!");
    testStr = sb.toString();

    reader = new StringReader(testStr);
    assertTrue(checker.checkPermission(null, reader, null));

    // missing whitespace
    sb = new StringBuffer("laa-dee-dah-LOCK-KCOL\n\n");
    sb.append(subStr1);
    sb.append(subStr2);
    sb.append("\n\nTheEnd!");
    testStr = sb.toString();

    reader = new StringReader(testStr);
    assertFalse(checker.checkPermission(null, reader, null));
  }


  static private class WhiteSpaceFilterRule implements FilterRule {
    public Reader createFilteredReader(Reader reader) {
      return new WhiteSpaceFilter(reader);
    }
  }

}
