/*
 * $Id: TestCreativeCommonsPermissionChecker.java,v 1.14 2015-02-09 05:42:07 tlipkis Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.CreativeCommonsPermissionChecker;
import org.lockss.state.*;

/**
 * <p>Title: TestCreativeCommonsV3PermissionChecker</p>
 * <p>@author Claire Griffin</p>
 * <p>@version 1.0</p>
 */
public class TestCreativeCommonsPermissionChecker
  extends LockssPermissionCheckerTestCase {

  String template = "<html>/n<head>/n<title>FOO</title>\n</head>" +
    "some text%smore text\n" +
    "</body>\n</html>\n";

  private String VALID_URL_STEM = "http://creativecommons.org/licenses";
  private String VALID_URL_STEM_S = "https://creativecommons.org/licenses";

  private String[] VALID_TAGS = {"a", "link"};
  private String[] VALID_LICENSES = {
    "by", "by-sa", "by-nc", "by-nd", "by-nc-sa", "by-nc-nd",
    "BY", "BY-SA", "BY-nc", "by-ND", "by-NC-sa", "by-NC-ND",
  };
  private String[] VALID_VERSIONS = {"1.0", "2.0", "2.5", "3.0", "4.0"};
  private String TEST_URL = "http://www.example.com/";

  public void setUp() throws Exception {
    super.setUp();
  }

  // return a CC license URL for the specified license terms and version
  private String lu(String lic, String ver) {
    return VALID_URL_STEM + "/" + lic + "/" + ver + "/";
  }

  private String lus(String lic, String ver) {
    return VALID_URL_STEM_S + "/" + lic + "/" + ver + "/";
  }

  // return a CC license tag element
  private String htext(String tag) {
    return String.format(template, tag);
  }

  private void assertPerm(String tag) {
    String text = htext(tag);
    CreativeCommonsPermissionChecker checker =
      new CreativeCommonsPermissionChecker();
    assertTrue(tag + " expected permission, wasn't",
	       checker.checkPermission(mcf,
				       new StringReader(text), TEST_URL));
    assertEquals(AuState.AccessType.OpenAccess, aus.getAccessType());
  }

  private void assertNoPerm(String tag) {
    String text = htext(tag);
    CreativeCommonsPermissionChecker checker =
      new CreativeCommonsPermissionChecker();
    assertFalse(tag + " expected no permission, but was",
	       checker.checkPermission(mcf,
				       new StringReader(text), TEST_URL));
  }


  public void testValidPermissions(String[] licenses,
				   String[] versions) {
    for (String lic : licenses) {
      for (String ver : versions) {
	assertPerm("<a href=\"" + lu(lic, ver) + "\" rel=\"license\" />");
	assertPerm("<a href=\"" + lus(lic, ver) + "\" rel=\"license\" />");
	assertPerm("<link href=\"" + lu(lic, ver) + "\" rel=\"license\" />");
	assertPerm("<link href=\"" + lus(lic, ver) + "\" rel=\"license\" />");
	assertPerm("<a rel=\"license\" href=\"" + lu(lic, ver) + "\" />");
	assertPerm("<a rel=\"license\" href=\"" + lus(lic, ver) + "\" />");
	assertPerm("<link class=\"bar\" rel=\"license\" href=\"" +
		   lu(lic, ver) + "\" />");
	assertPerm("<link class=\"bar\" rel=\"license\" href=\"" +
		   lus(lic, ver) + "\" />");

	assertNoPerm("<a href=\"" + lu(lic + "fnord", ver) + "\" rel=\"license\" />");
	assertNoPerm("<a href=\"" + lus(lic + "fnord", ver) + "\" rel=\"license\" />");
	assertNoPerm("<a href=\"" + lu(lic, ver + "gorp") + "\" rel=\"license\" />");
	assertNoPerm("<a href=\"" + lus(lic, ver + "gorp") + "\" rel=\"license\" />");
      }
    }
  }

  public void testValidPermissionsDefault() {
    testValidPermissions(VALID_LICENSES, VALID_VERSIONS);
  }

  public void testValidPermissionsConfig() {
    ConfigurationUtil.addFromArgs(CreativeCommonsPermissionChecker.PARAM_VALID_LICENSE_TYPES,
				  "abc;ddd",
				  CreativeCommonsPermissionChecker.PARAM_VALID_LICENSE_VERSIONS,
				  "1.2;3.0");
				  
    testValidPermissions(new String[] {"abc", "ddd"},
			 new String[] {"1.2", "3.0"});

    // CreativeCommonsPermissionChecker stores config in static fields;
    // restore default config to avoid impacting other tests.
    ConfigurationUtil.resetConfig();
  }

  public void testCase() {
    assertPerm("<a href=\"" + lu("by", "3.0") + "\" rel=\"license\" />");
    assertPerm("<a href=\"" + lu("by", "3.0") + "\" rel=\"license\" />".toUpperCase());
    assertPerm("<a href=\"" + lus("by", "3.0") + "\" rel=\"license\" />");
    assertPerm("<a href=\"" + lus("by", "3.0") + "\" rel=\"license\" />".toUpperCase());
  }

  public void testWhitespace() {
    assertPerm("<a href=\"" + lu("by", "3.0") + "\" rel=\"license\" />");
    assertPerm("<a href=\"" + lus("by", "3.0") + "\" rel=\"license\" />");
    assertPerm("<a\nhref=\"" + lu("by", "3.0") + "\"\nrel=\"license\"\n/>");
    assertPerm("<a\thref=\"" + lu("by", "3.0") + "\"\trel=\"license\"\t/>");
    assertPerm("<a\rhref=\"" + lu("by", "3.0") + "\"\rrel=\"license\"\r/>");
    assertPerm("<a\r\nhref=\"" + lu("by", "3.0") + "\"\r\nrel=\"license\"\r\n/>");
  }

  public void testInvalidPermissions() {
    assertPerm("<a href=\"" + lu("by", "3.0") + "\" rel=\"license\" />");
    assertPerm("<a href=\"" + lus("by", "3.0") + "\" rel=\"license\" />");
    assertNoPerm("<img href=\"" + lu("by", "3.0") + "\" rel=\"license\" />");
    assertNoPerm("<img href=\"" + lus("by", "3.0") + "\" rel=\"license\" />");
    assertNoPerm("<a nohref=\"" + lu("by", "3.0") + "\" rel=\"license\" />");
    assertNoPerm("<a href=\"" + lu("not", "3.0") + "\" rel=\"license\" />");
    assertNoPerm("<a href=\"" + lu("by", "5.0") + "\" rel=\"license\" />");
    assertNoPerm("<a href=\"" + lu("by", "3.0") + "\" norel=\"license\" />");
    assertNoPerm("<a href=\"" + lu("by", "3.0") + "\" rel=\"uncle\" />");
    assertNoPerm("<a href=\"" + lu("by", "3.0") + "\" />");
    assertNoPerm("<a href=\"http://example.com\" rel=\"license\" />");
    assertNoPerm("<a href=\"" + lu("", "3.0") + "\" rel=\"license\" />");
    assertNoPerm("<a href=\"" + lu("by", "") + "\" rel=\"license\" />");
  }

}
