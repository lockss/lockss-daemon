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

import java.text.*;
import java.io.PrintStream;
import java.util.Date;

import org.lockss.config.ConfigManager;

/** A <code>LogTarget</code> implementation that outputs to a PrintStream
 */
public class PrintStreamTarget implements LogTarget {
//    static final DateFormat df = DateFormat.getTimeInstance();
  static final DateFormat df = new SimpleDateFormat("HH:mm:ss.SSS");

  private Deadline nextTimestamp = Deadline.in(0);

  protected PrintStream stream;

  /** Create a log target that outputs to the supplied PrintStream */
  PrintStreamTarget(PrintStream toStream) {
    this.stream = toStream;
    nextTimestamp.expire();
  }

  public void init() {
  }

  // Rather than synchronizing, build whole string and assume a single
  // println will probably not get interleaved with another thread.
  // If need to explicitly synchronize, do so on the class, not instance,
  // as there could be multiple instances of this.  (Even that's not right,
  // as it should synchronize with all other uses of the stream)

  /** Write the message to the stream, prefaced by a timestamp if more than
   * one day has passed since the last timestamp
   */
  public void handleMessage(Logger log, int msgLevel, String message) {
    if (nextTimestamp.expired()) {
      emitTimestamp(log);
      nextTimestamp.expireIn(Constants.DAY);
    }
    writeMessage(log, Logger.nameOf(msgLevel), message);
  }

  protected void emitTimestamp(Logger log) {
    StringBuilder sb = new StringBuilder();
    sb.append(TimeBase.nowDate().toString());
    DaemonVersion dver = ConfigManager.getDaemonVersion();
    if (dver != null) {
      sb.append("   Daemon ");
      sb.append(dver.displayString());
    }
    writeMessage(log, "Timestamp", sb.toString());
  }

  /** Write the message to stdout */
  void writeMessage(Logger log, String msgLevel, String message) {
    StringBuilder sb = new StringBuilder();
    sb.append(log.getTimeStampFormat().format(new Date()));
    if (TimeBase.isSimulated()) {
      sb.append("(sim ");
      sb.append(TimeBase.nowMs());
      sb.append(")");
    }
    sb.append(": ");
    sb.append(msgLevel);
    sb.append(": ");
    sb.append(message);
    PrintStream s = getPrintStream();
    String str = sb.toString();
    if (str.endsWith("\n")) {
	s.print(str);
    } else {
	s.println(str);
    }
    s.flush();
  }

  /** Return the stored stream; override this if need to fetch the stream
      on each call. */
  protected PrintStream getPrintStream() {
    return stream;
  }

}
