/*
 * $Id: AntTaskTarget.java,v 1.1 2003-05-26 03:49:51 tal Exp $
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

import java.text.*;
import java.io.PrintStream;
import java.util.Date;

/** A <code>LogTarget</code> implementation that outputs to an Ant task's
    logger
 */
public class AntTaskTarget implements LogTarget {
  static final DateFormat df = new SimpleDateFormat("HH:mm:ss.SSS");

  protected org.lockss.ant.AntHelper helper;

  /** Create a log target that outputs to an AntTargetHelper */
  AntTaskTarget() {
    this.helper = new org.lockss.ant.AntHelper();
  }

  public void init() {
  }

  /** Write the message to the Ant task
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
    helper.writeLog(sb.toString());
  }
}
