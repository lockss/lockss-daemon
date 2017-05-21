/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;

import java.util.*;
import org.lockss.test.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Test class for org.lockss.util.Slf4jLoggerAdapter and Slf4jLoggerFactory
 */

public class FuncSlf4jLoggerAdaptor extends LockssTestCase {

  // ensure that slf4j logging goes to lockss log
  public void testLog() {
    String msg = "medium medium medium medium medium medium medium familiar";
    String logName = "TestLog";
    MockLogTarget target = new MockLogTarget();
    Logger log = LoggerFactory.getLogger(logName);
    org.lockss.util.Logger lockssLog =
      org.lockss.util.Logger.getLogger(logName);
    org.lockss.util.Logger.setTarget(target);
    // disable thread id, makes message order dependent on debug level
    lockssLog.setIdThread(false);
    assertEmpty(target.getMessages());
    log.warn(msg);
    List m = target.getMessages();
    System.out.println("msgs recorded: " + m);
    assertEquals(1, m.size());
    String m0 = (String)m.get(0);
    assertNotEquals(-1, m0.indexOf(msg));
  }
}
