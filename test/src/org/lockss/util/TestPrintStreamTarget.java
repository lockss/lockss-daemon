/*
 * $Id$
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

package org.lockss.util;

import java.io.*;
import org.lockss.test.*;

public class TestPrintStreamTarget extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.util.PrintStreamTarget.class
  };

  protected void setUp() throws Exception {
    super.setUp();
    // DateFormat is stored in a static in Logger; ensure it has the
    // default value
    ConfigurationUtil.resetConfig();
  }

  public void testOutputStringFormat1() {
    testOutputStringFormat("\\d(\\d)?:\\d\\d:\\d\\d\\.\\d\\d\\d: ");
  }

  public void testOutputStringFormat2() {
    ConfigurationUtil.setFromArgs(Logger.PARAM_TIMESTAMP_DATEFORMAT,
				  "MM/dd/yyyy HH:mm:ss.SSS");
    testOutputStringFormat("\\d\\d/\\d\\d/\\d\\d\\d\\d \\d(\\d)?:\\d\\d:\\d\\d\\.\\d\\d\\d: ");
  }

  public void testOutputStringFormat(String timestampRE) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    PrintStreamTarget target = new PrintStreamTarget(ps);

    String name = "log-id";
    String errorMessage = "error message";
    // output the message twice
    for (int ix = 2; ix > 0; ix--) {
      target.handleMessage(new Logger(Logger.LEVEL_DEBUG, name),
			   Logger.LEVEL_ERROR,
			   errorMessage);
    }

    // Should have one Timestamp: message followed by two copies of the message
    String line1 = timestampRE + "Timestamp: .* Daemon \\d+\\.\\d+\\.\\d+.*"
      + Constants.EOL_RE;
    String line2 = timestampRE + "Error: "+errorMessage+Constants.EOL_RE;
    String re = line1 + line2 + line2;
    String debugString = baos.toString();
    assertMatchesRE("Debug string: \""+debugString+"\" not of correct format."+
		    " Should be <time>: <error-level>: <error message>",
		    re, debugString);
    assertEquals(3, StringUtil.countOccurences(debugString, "\n"));
  }

  public void testNoBlankLines() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    PrintStreamTarget target = new PrintStreamTarget(ps);

    String name = "log-id";
    String errorMessage = "error message";
    // output the message twice with and without newline on end
    target.handleMessage(new Logger(Logger.LEVEL_DEBUG, name),
			 Logger.LEVEL_ERROR,
			 errorMessage + "\n");
    target.handleMessage(new Logger(Logger.LEVEL_DEBUG, name),
			 Logger.LEVEL_ERROR,
			 errorMessage);
    String debugString = baos.toString();
    // Same three lines as above, and there should be only 3 EOLs
    assertEquals(3, StringUtil.countOccurences(debugString, "\n"));
  }
}
