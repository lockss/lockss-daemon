/*
 * $Id: TestCreativeCommonsV3PermissionChecker.java,v 1.2.60.1 2012-06-20 00:02:42 nchondros Exp $
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.state.*;
import org.lockss.util.*;
import org.lockss.filter.*;

/**
 * <p>Title: TestCreativeCommonsV3PermissionChecker</p>
 * <p>@author Claire Griffin</p>
 * <p>@version 1.0</p>
 */
public class TestCreativeCommonsV3PermissionChecker
  extends LockssPermissionCheckerTestCase {

  String template = "<html>/n<head>/n<title>FOO</title>\n</head>" +
    "some text%smore text\n" +
    "</body>\n</html>\n";

  private String VALID_URL_STEM = "http://creativecommons.org/licenses";

  private String[] VALID_TAGS = {"a", "link"};
  private String[] VALID_LICENSES = {
    "by", "by-sa", "by-nc", "by-nd", "by-nc-sa", "by-nc-nd",
    "BY", "BY-SA", "BY-nc", "by-ND", "by-NC-sa", "by-NC-ND",
  };
  private String[] VALID_VERSIONS = {"1.0", "2.0", "2.5", "3.0"};
  private String TEST_URL = "http://www.example.com/";

  public void setUp() throws Exception {
    super.setUp();
  }

  // return a CC license URL for the specified license terms and version
  private String lu(String lic, String ver) {
    return VALID_URL_STEM + "/" + lic + "/" + ver + "/";
  }

  // return a CC license tag element
  private String htext(String tag) {
    return String.format(template, tag);
  }

  private void assertPerm(String tag) {
    String text = htext(tag);
    CreativeCommonsV3PermissionChecker checker =
      new CreativeCommonsV3PermissionChecker();
    assertTrue(tag + " expected permission, wasn't",
	       checker.checkPermission(pHelper,
				       new StringReader(text), TEST_URL));
    assertEquals(AuState.AccessType.OpenAccess, aus.getAccessType());
  }

  private void assertNoPerm(String tag) {
    String text = htext(tag);
    CreativeCommonsV3PermissionChecker checker =
      new CreativeCommonsV3PermissionChecker();
    assertFalse(tag + " expected no permission, but was",
	       checker.checkPermission(pHelper,
				       new StringReader(text), TEST_URL));
  }


  public void testValidPermissions() {
    for (String lic : VALID_LICENSES) {
      for (String ver : VALID_VERSIONS) {
	assertPerm("<a href=\"" + lu(lic, ver) + "\" rel=\"license\" />");
	assertPerm("<link href=\"" + lu(lic, ver) + "\" rel=\"license\" />");
	assertPerm("<a rel=\"license\" href=\"" + lu(lic, ver) + "\" />");
	assertPerm("<link class=\"bar\" rel=\"license\" href=\"" +
		   lu(lic, ver) + "\" />");
      }
    }
  }

  public void testCase() {
    assertPerm("<a href=\"" + lu("by", "3.0") + "\" rel=\"license\" />");
    assertPerm("<a href=\"" + lu("by", "3.0") + "\" rel=\"license\" />".toUpperCase());
  }

  public void testWhitespace() {
    assertPerm("<a href=\"" + lu("by", "3.0") + "\" rel=\"license\" />");
    assertPerm("<a\nhref=\"" + lu("by", "3.0") + "\"\nrel=\"license\"\n/>");
    assertPerm("<a\thref=\"" + lu("by", "3.0") + "\"\trel=\"license\"\t/>");
    assertPerm("<a\rhref=\"" + lu("by", "3.0") + "\"\rrel=\"license\"\r/>");
    assertPerm("<a\r\nhref=\"" + lu("by", "3.0") + "\"\r\nrel=\"license\"\r\n/>");
  }

  public void testInValidPermissions() {
    assertPerm("<a href=\"" + lu("by", "3.0") + "\" rel=\"license\" />");
    assertNoPerm("<img href=\"" + lu("by", "3.0") + "\" rel=\"license\" />");
    assertNoPerm("<a nohref=\"" + lu("by", "3.0") + "\" rel=\"license\" />");
    assertNoPerm("<a href=\"" + lu("not", "3.0") + "\" rel=\"license\" />");
    assertNoPerm("<a href=\"" + lu("by", "4.0") + "\" rel=\"license\" />");
    assertNoPerm("<a href=\"" + lu("by", "3.0") + "\" norel=\"license\" />");
    assertNoPerm("<a href=\"" + lu("by", "3.0") + "\" rel=\"uncle\" />");
    assertNoPerm("<a href=\"" + lu("by", "3.0") + "\" />");
    assertNoPerm("<a href=\"http://example.com\" rel=\"license\" />");
    assertNoPerm("<a href=\"" + lu("", "3.0") + "\" rel=\"license\" />");
    assertNoPerm("<a href=\"" + lu("by", "") + "\" rel=\"license\" />");
  }

}
