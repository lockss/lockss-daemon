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

package org.lockss.remote;

import java.io.*;
import java.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.test.*;

/**
 * Test class for org.lockss.remote.InactiveAuProxy
 */
public class TestInactiveAuProxy extends LockssTestCase {

  static final String AUID1 = "AUID_1";
  static final String PID1 = "PID_1";

  MockLockssDaemon daemon;
  MyMockRemoteApi mrapi;
  MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    daemon = getMockLockssDaemon();
    mrapi = new MyMockRemoteApi();
    daemon.setRemoteApi(mrapi);
    daemon.setDaemonInited(true);
    mau = new MockArchivalUnit();
  }

  public void tearDown() throws Exception {
    daemon.stopDaemon();
    super.tearDown();
  }

  public void testIsActiveAu() {
    AuProxy aup = new InactiveAuProxy("idid", mrapi);
    assertFalse(aup.isActiveAu());
  }

  class MyMockRemoteApi extends RemoteApi {
    Map aumap = new HashMap();

    ArchivalUnit getAuFromId(String auid) {
      return (ArchivalUnit)aumap.get(auid);
    }

    void setAuFromId(String auid, ArchivalUnit au) {
      aumap.put(auid, au);
    }
  }
}
