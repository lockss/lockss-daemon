/*
 * $Id: TestPrintStreamTarget.java,v 1.1 2003-04-17 04:05:35 tal Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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
import gnu.regexp.RE;
import gnu.regexp.REException;
import junit.framework.TestCase;

public class TestPrintStreamTarget extends TestCase{
  public static Class testedClasses[] = {
    org.lockss.util.PrintStreamTarget.class
  };

  public void testOutputStringFormat() throws REException {
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

//      RE regExp = 
//        new RE("\\d(\\d)?:\\d\\d:\\d\\d (A|P)M: Error: "+errorMessage+"\n");
//     RE regExp = 
//       new RE("\\d(\\d)?:\\d\\d:\\d\\d\\.\\d\\d\\d: Error: "+errorMessage+"\n");
    // Should have one Timestamp: message followed by two copies of the message
    String timestampRE = "\\d(\\d)?:\\d\\d:\\d\\d\\.\\d\\d\\d: ";
    String line1 = timestampRE + "Timestamp: .*\\n";
    String line2 = timestampRE + "Error: "+errorMessage+"\\n";
    RE regExp = new RE(line1 + line2 + line2);
    String debugString = baos.toString();
    assertTrue("Debug string: \""+debugString+"\" not of correct format."+
	       " Should be <time>: <error-level>: <error message>",
	       regExp.isMatch(debugString));
  }
}
