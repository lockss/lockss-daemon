/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.clockss;

import java.io.*;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.lockss.daemon.*;
import org.lockss.state.AuState;
import org.lockss.test.*;

public class TestClockssPermission extends LockssPermissionCheckerTestCase {

  private String PERM_STRING =
      "CLOCKSS system has permission to ingest, preserve, and serve this Archival Unit";

  private String OPEN_ACCESS_STRING =
      "CLOCKSS system has permission to ingest, preserve, and serve this open access Archival Unit";

  public void setUp() throws Exception {
    super.setUp();
  }

  public void testString() {
    assertEquals(PERM_STRING, ClockssPermission.CLOCKSS_PERMISSION_STRING);
    assertEquals(OPEN_ACCESS_STRING, ClockssPermission.CLOCKSS_OPEN_ACCESS_PERMISSION_STRING);
  }

  private boolean hasPermission(String page) throws IOException {
    return MiscTestUtil.hasPermission(new ClockssPermission().getCheckers(),
                                      page, mcf);
  }

  public void testNoPermission() throws IOException {
    assertFalse(hasPermission("CLOCKSS system does not have permission to ingest, preserve, and serve this Archival Unit"));
    assertFalse(hasPermission("CLOCKSS system does not have permission to ingest, preserve, and serve this open access Archival Unit"));
    assertNotEquals(AuState.AccessType.OpenAccess, aus.getAccessType());
  }

  public void testClockssPermissionStatement() throws IOException {
    String padding = StringUtils.repeat("Blah ", 50);
    assertTrue(hasPermission(PERM_STRING));
    assertEquals(AuState.AccessType.Subscription, aus.getAccessType());
    assertTrue(hasPermission(padding + PERM_STRING));
    assertEquals(AuState.AccessType.Subscription, aus.getAccessType());
    assertTrue(hasPermission(PERM_STRING + padding));
    assertEquals(AuState.AccessType.Subscription, aus.getAccessType());
    assertTrue(hasPermission(padding + PERM_STRING + padding));
    assertEquals(AuState.AccessType.Subscription, aus.getAccessType());
  }

  public void testClockssPermissionStatementWtihOpenAccessQualification() throws IOException {
    String padding = StringUtils.repeat("Blah ", 50);
    assertTrue(hasPermission(OPEN_ACCESS_STRING));
    assertEquals(AuState.AccessType.OpenAccess, aus.getAccessType());
    assertTrue(hasPermission(padding + OPEN_ACCESS_STRING));
    assertEquals(AuState.AccessType.OpenAccess, aus.getAccessType());
    assertTrue(hasPermission(OPEN_ACCESS_STRING + padding));
    assertEquals(AuState.AccessType.OpenAccess, aus.getAccessType());
    assertTrue(hasPermission(padding + OPEN_ACCESS_STRING + padding));
    assertEquals(AuState.AccessType.OpenAccess, aus.getAccessType());
  }

  public void testNoMatchLockssPermission() throws IOException {
    assertFalse(hasPermission(LockssPermission.LOCKSS_PERMISSION_STRING));
    assertFalse(hasPermission(LockssPermission.LOCKSS_OPEN_ACCESS_PERMISSION_STRING));
    assertFalse(hasPermission(LockssPermission.LOCKSS_OJS_PERMISSION_STRING));
  }

  /**
   * To catch changes to the permission checker list
   */
  public void testGetCheckersHasProperCheckers() {
    List<PermissionChecker> checkers = new ClockssPermission().getCheckers();
    assertEquals("Expected four permission checkers",
                 4, checkers.size());
    assertTrue("First checker was not a StringPermissionChecker",
               checkers.get(0) instanceof StringPermissionChecker);
    assertTrue("Second checker was not a StringPermissionChecker",
               checkers.get(1) instanceof StringPermissionChecker);
    assertTrue("Third checker was not a CreativeCommonsPermissionChecker",
               checkers.get(2) instanceof CreativeCommonsPermissionChecker);
    assertTrue("Fourth checker was not a CreativeCommonsRdfPermissionChecker",
               checkers.get(3) instanceof CreativeCommonsRdfPermissionChecker);
  }

  public void testGetCheckersNotModifiable() {
    List<PermissionChecker> checkers = new ClockssPermission().getCheckers();
    try {
      checkers.set(0, new StringPermissionChecker("anything",
                                                  new StringPermissionChecker.StringFilterRule()));
      fail("Should not be able to modify permission checker list");
    }
    catch (UnsupportedOperationException ignore) {
      // Expected
    }
  }
  
}
