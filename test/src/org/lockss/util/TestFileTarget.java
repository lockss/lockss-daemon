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

import java.io.*;
import junit.framework.TestCase;
import org.lockss.test.*;

public class TestFileTarget extends LockssTestCase {

  // needs to log to file, rename file, advance time by 10 minutes,
  // make sure file gets closed and reopened and a timestamp generated
  public void testLogToFile() throws Exception {
    File dir = getTempDir();
    File file = new File(dir, "logfiletarget");
    FileTarget seTarget = new FileTarget(file.toString());

    String name = "log-id";
    String errorMessage = "error message";
    // output the message twice
    for (int ix = 2; ix > 0; ix--) {
      seTarget.handleMessage(new Logger(Logger.LEVEL_DEBUG, name),
			     Logger.LEVEL_ERROR,
			     errorMessage);
    }

    // Should have one Timestamp: message followed by two copies of the message
    String timestampRE = "\\d(\\d)?:\\d\\d:\\d\\d\\.\\d\\d\\d: ";
    String line1 = timestampRE + "Timestamp: .*\\n";
    String line2 = timestampRE + "Error: "+errorMessage+"\\n";
    String re = line1 + line2 + line2;
    String debugString = StringUtil.fromFile(file);
     assertTrue("Debug string: \""+debugString+"\" not of correct format."+
 	       " Should be <time>: <error-level>: <error message>",
		isMatchRe(debugString, re));
  }

}
