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

package org.lockss.util;

import java.util.*;
import org.lockss.test.*;
import org.apache.commons.logging.*;

/**
 * Test class for org.lockss.util.CommonsLoggingLogger
 */

public class FuncCommonsLoggingLogger extends LockssTestCase {

  // ensure that commons log goes to lockss log
  public void testLog() {
    String msg = "test message 42";
    String logName = "TestLog";
    MockLogTarget target = new MockLogTarget();
    Log log = LogFactory.getLog(logName);
    Logger lockssLog = Logger.getLogger(logName);
    Logger.setTarget(target);
    // disable thread id, makes message order dependent on debug level
    lockssLog.setIdThread(false);
    assertEmpty(target.getMessages());
    log.warn(msg);
    List m = target.getMessages();
    System.out.println("loglog: " + m);
    assertEquals(1, m.size());
    String m0 = (String)m.get(0);
    assertNotEquals(-1, m0.indexOf(msg));
  }
}
