/*
 * $Id: TestCreativeCommonsV3PermissionChecker.java,v 1.1 2007-08-08 22:45:27 dshr Exp $
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
 * <p>Title: TestCreativeCommonsV3PermissionChecker</p>
 * <p>@author Claire Griffin</p>
 * <p>@version 1.0</p>
 */
public class TestCreativeCommonsV3PermissionChecker extends LockssTestCase {
  private CreativeCommonsV3PermissionChecker checker = null;
  private String LEAD_IN = "<html>/n<head>/n<title>FOO</title>\n</head>"+
    "laa-dee-dah-LOCK-KCOL\n\n";
  private String RUN_OUT = "\n\nTheEnd!\n" + "</body>\n</html>\n";
  private String BEGIN_STRING = "<a ";
  private String END_STRING = " >";
  private String HREF_STEM = "href=\"http://creativecommons.org/licenses/";
  private String REL_STRING = "rel=\"license\"";
  private String VERSION_STRING = "3.0";
  private String[] TYPE_STRINGS = {
    "by/", "by-sa/", "by-nc/", "by-nd/", "by-nc-sa/", "by-nc-nd/",
  };
  private String[] WHITE_SPACE = {
    " ", "\n", " \n", "\t\n ", "   ", "\n\n\t",
  };
  StringBuffer sb;
  private String TEST_URL = "http://www.example.com/";

  public void testValidPermissions() {
    for (int i = 0; i < TYPE_STRINGS.length; i++) {
      // <a href="foo" rel="license">
      sb = new StringBuffer(LEAD_IN);
      sb.append(BEGIN_STRING);
      sb.append(HREF_STEM);
      sb.append(TYPE_STRINGS[i]);
      sb.append(VERSION_STRING);
      sb.append("/\" ");
      sb.append(REL_STRING);
      sb.append(END_STRING);
      sb.append(RUN_OUT);
      String s_ok = sb.toString();

      checker = new CreativeCommonsV3PermissionChecker();
      // check the correct string
      Reader reader = new StringReader(s_ok);
      assertTrue(checker.checkPermission(null, reader, TEST_URL));
    }
    for (int i = 0; i < TYPE_STRINGS.length; i++) {
      // <a rel="license" href="foo">
      sb = new StringBuffer(LEAD_IN);
      sb.append(BEGIN_STRING);
      sb.append(REL_STRING);
      sb.append(" ");
      sb.append(HREF_STEM);
      sb.append(TYPE_STRINGS[i]);
      sb.append(VERSION_STRING);
      sb.append("/\" ");
      sb.append(END_STRING);
      sb.append(RUN_OUT);
      String s_ok = sb.toString();

      checker = new CreativeCommonsV3PermissionChecker();
      // check the correct string
      Reader reader = new StringReader(s_ok);
      assertTrue(checker.checkPermission(null, reader, TEST_URL));
    }
  }

  public void testIgnoreCasePermissions() {
    StringBuffer sb = null;
    for (int i = 0; i < TYPE_STRINGS.length; i++) {
      // <a href="foo" rel="license">
      sb = new StringBuffer(LEAD_IN);
      sb.append(BEGIN_STRING);
      sb.append(HREF_STEM);
      sb.append(TYPE_STRINGS[i]);
      sb.append(VERSION_STRING);
      sb.append("/\" ");
      sb.append(REL_STRING);
      sb.append(END_STRING);
      sb.append(RUN_OUT);
      String s_ok = sb.toString().toLowerCase();

      checker = new CreativeCommonsV3PermissionChecker();
      // check the correct string
      Reader reader = new StringReader(s_ok);
      assertTrue(checker.checkPermission(null, reader, TEST_URL));
    }
    for (int i = 0; i < TYPE_STRINGS.length; i++) {
      // <a rel="license" href="foo">
      sb = new StringBuffer(LEAD_IN);
      sb.append(BEGIN_STRING);
      sb.append(REL_STRING);
      sb.append(" ");
      sb.append(HREF_STEM);
      sb.append(TYPE_STRINGS[i]);
      sb.append(VERSION_STRING);
      sb.append("/\" ");
      sb.append(END_STRING);
      sb.append(RUN_OUT);
      String s_ok = sb.toString().toUpperCase();

      checker = new CreativeCommonsV3PermissionChecker();
      // check the correct string
      Reader reader = new StringReader(s_ok);
      assertTrue(checker.checkPermission(null, reader, TEST_URL));
    }
  }

  public void testWhiteSpacePermissions() {
    StringBuffer sb = null;
    for (int i = 0; i < WHITE_SPACE.length; i++) {
      // <a href="foo" rel="license">
      sb = new StringBuffer(LEAD_IN);
      sb.append(BEGIN_STRING);
      sb.append(WHITE_SPACE[i]);
      sb.append(HREF_STEM);
      sb.append(TYPE_STRINGS[i]);
      sb.append(VERSION_STRING);
      sb.append("/\" ");
      sb.append(WHITE_SPACE[i]);
      sb.append(REL_STRING);
      sb.append(WHITE_SPACE[i]);
      sb.append(END_STRING);
      sb.append(RUN_OUT);
      String s_ok = sb.toString();

      checker = new CreativeCommonsV3PermissionChecker();
      // check the correct string
      Reader reader = new StringReader(s_ok);
      assertTrue(checker.checkPermission(null, reader, TEST_URL));
    }
  }

  public void testNoURL() {
    for (int i = 0; i < TYPE_STRINGS.length; i++) {
      // <a rel="license" href="foo">
      sb = new StringBuffer(LEAD_IN);
      sb.append(BEGIN_STRING);
      sb.append(HREF_STEM);
      sb.append(TYPE_STRINGS[i]);
      sb.append(VERSION_STRING);
      sb.append("/\" ");
      sb.append(REL_STRING);
      sb.append(END_STRING);
      sb.append(RUN_OUT);
      String s_ok = sb.toString();

      checker = new CreativeCommonsV3PermissionChecker();
      // check the correct string
      Reader reader = new StringReader(s_ok);
      assertFalse(checker.checkPermission(null, reader, null));
    }
  }

  public void testNoLink() {
    for (int i = 0; i < TYPE_STRINGS.length; i++) {
      // <a rel="license" href="foo">
      sb = new StringBuffer(LEAD_IN);
      sb.append(BEGIN_STRING);
      sb.append(REL_STRING);
      sb.append(END_STRING);
      sb.append(RUN_OUT);
      String s_ok = sb.toString();

      checker = new CreativeCommonsV3PermissionChecker();
      // check the correct string
      Reader reader = new StringReader(s_ok);
      assertFalse(checker.checkPermission(null, reader, TEST_URL));
    }
  }

  public void testNoLicense() {
    for (int i = 0; i < TYPE_STRINGS.length; i++) {
      // <a rel="license" href="foo">
      sb = new StringBuffer(LEAD_IN);
      sb.append(BEGIN_STRING);
      sb.append(HREF_STEM);
      sb.append(TYPE_STRINGS[i]);
      sb.append(VERSION_STRING);
      sb.append("/\" ");
      sb.append(END_STRING);
      sb.append(RUN_OUT);
      String s_ok = sb.toString();

      checker = new CreativeCommonsV3PermissionChecker();
      // check the correct string
      Reader reader = new StringReader(s_ok);
      assertFalse(checker.checkPermission(null, reader, TEST_URL));
    }
  }

  public void testNoLicenseType() {
    for (int i = 0; i < TYPE_STRINGS.length; i++) {
      // <a rel="license" href="foo">
      sb = new StringBuffer(LEAD_IN);
      sb.append(BEGIN_STRING);
      sb.append(REL_STRING);
      sb.append(" ");
      sb.append(HREF_STEM);
      sb.append(VERSION_STRING);
      sb.append("/\" ");
      sb.append(END_STRING);
      sb.append(RUN_OUT);
      String s_ok = sb.toString();

      checker = new CreativeCommonsV3PermissionChecker();
      // check the correct string
      Reader reader = new StringReader(s_ok);
      assertFalse(checker.checkPermission(null, reader, TEST_URL));
    }
  }

  public void testNoLicenseVersion() {
    for (int i = 0; i < TYPE_STRINGS.length; i++) {
      // <a rel="license" href="foo">
      sb = new StringBuffer(LEAD_IN);
      sb.append(BEGIN_STRING);
      sb.append(REL_STRING);
      sb.append(" ");
      sb.append(HREF_STEM);
      sb.append(TYPE_STRINGS[i]);
      sb.append("/\" ");
      sb.append(END_STRING);
      sb.append(RUN_OUT);
      String s_ok = sb.toString();

      checker = new CreativeCommonsV3PermissionChecker();
      // check the correct string
      Reader reader = new StringReader(s_ok);
      assertFalse(checker.checkPermission(null, reader, TEST_URL));
    }
  }

  public void testBadLicenseVersion() {
    for (int i = 0; i < TYPE_STRINGS.length; i++) {
      // <a rel="license" href="foo">
      sb = new StringBuffer(LEAD_IN);
      sb.append(BEGIN_STRING);
      sb.append(REL_STRING);
      sb.append(" ");
      sb.append(HREF_STEM);
      sb.append(TYPE_STRINGS[i]);
      sb.append("2.7");
      sb.append("/\" ");
      sb.append(END_STRING);
      sb.append(RUN_OUT);
      String s_ok = sb.toString();

      checker = new CreativeCommonsV3PermissionChecker();
      // check the correct string
      Reader reader = new StringReader(s_ok);
      assertFalse(checker.checkPermission(null, reader, TEST_URL));
    }
  }
}
