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

import java.text.*;
import java.util.Date;

/** A <code>LogTarget</code> implementation that outputs to System.err
 */
public class StdErrTarget implements LogTarget {
//    static final DateFormat df = DateFormat.getTimeInstance();
  static final DateFormat df = new SimpleDateFormat("HH:mm:ss.SSS");

  // Rather than synchronizing, build whole string and assume a single
  // println will probably not get interleaved with another thread.
  // If need to explicitly synchronize, do so on the class, not instance,
  // as there could be multiple instances of this.  (Even that's not right,
  // as it should synchronize with all other uses of System.err)

/** A <code>LogTarget</code> implementation that outputs to System.err
 */
  public void handleMessage(Logger log, int msgLevel, String message) {
    StringBuffer sb = new StringBuffer();
    sb.append(df.format(new Date()));
    if (TimeBase.isSimulated()) {
      sb.append("(sim ");
      sb.append(TimeBase.nowMs());
      sb.append(")");
    }
    sb.append(": ");
    sb.append(log.nameOf(msgLevel));
    sb.append(": ");
    sb.append(message);
    System.err.println(sb.toString());
  }
}
