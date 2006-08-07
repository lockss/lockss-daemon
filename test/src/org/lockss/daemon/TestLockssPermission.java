/*
 * $Id: TestLockssPermission.java,v 1.3 2006-08-07 07:40:21 tlipkis Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.clockss.*;
import org.lockss.test.*;

public class TestLockssPermission extends LockssTestCase {

  private String PERM_STRING = "LOCKSS system has permission to collect, " +
    "preserve, and serve this Archival Unit";

  public void testString() {
    assertEquals(PERM_STRING, LockssPermission.LOCKSS_PERMISSION_STRING);
  }

  private boolean hasPermission(String page) throws IOException {
    return MiscTestUtil.hasPermission(new LockssPermission().getCheckers(),
				      page);
  }

  public void testNoPermission() throws IOException {
    assertFalse(hasPermission("LOCKSS system does not have permission to collect, preserve, and serve this Archival Unit"));
  }

  public void testLockssPermission() throws IOException {
    String padding = org.apache.commons.lang.StringUtils.repeat("Blah ", 50);
    assertTrue(hasPermission("LOCKSS system has permission to collect, preserve, and serve this Archival Unit"));
    assertTrue(hasPermission(padding + "LOCKSS system has permission to collect, preserve, and serve this Archival Unit"));
    assertTrue(hasPermission("LOCKSS system has permission to collect, preserve, and serve this Archival Unit" + padding));
    assertTrue(hasPermission(padding + "LOCKSS system has permission to collect, preserve, and serve this Archival Unit" + padding));
  }

  public void testNoMatchClockssPermission() throws IOException {
    assertFalse(hasPermission(ClockssPermission.CLOCKSS_PERMISSION_STRING));
  }

  /**
   * To catch changes to the permission checker list
   * Currently, this should have two checkers:
   * 1) A StringPermissionChecker with the LOCKSS Permission Statement
   * 2) A CreativeCommonsPermissionChecker
   */
  public void testGetCheckersHasProperCheckers() {
    List checkers = new LockssPermission().getCheckers();
    assertEquals("Expected two Permission checkers, but only found one",
		 2, checkers.size());
    assertTrue("First checker wasn't a StringPermission Checker",
	       checkers.get(0) instanceof StringPermissionChecker);
    assertTrue("Second checker wasn't a CreativeCommonsPermissionChecker",
	       checkers.get(1) instanceof CreativeCommonsPermissionChecker);
  }

}
