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
import java.lang.reflect.*;
import java.io.PrintStream;
import java.util.*;

/** A <code>LogTarget</code> implementation that outputs to an Ant task's
 * logger.
 */
// It would be better if this class were in the ant hierarchy, as it needs
// access to Ant objects, and it isn't referenced from the main hierarchy.
// (It's invoked by putting its class name in org.lockss.defaultLogTarget.)
// But it needs LogTarget and Logger, which aren't available to the ant
// hierarchy because it's compiled first.

public class AntTaskTarget implements LogTarget {
  static final DateFormat df = new SimpleDateFormat("HH:mm:ss.SSS");

  // The helper is in the ant hierarchy, but we don't want compile-time
  // references into that hierarchy or the ant jars would be required to
  // compile even when not using ant.  So invoke the helper using reflection.
  //   protected AntHelper helper;
  protected Object helper;
  protected Method writeLogMethod;

  /** Create a log target that outputs to an AntTargetHelper */
  AntTaskTarget() {
    try {
      Class helperClass = Class.forName("org.lockss.ant.AntHelper");
      helper = helperClass.newInstance();
      Class argTypes[] = {String.class};
      writeLogMethod = helperClass.getMethod("writeLog", argTypes);
    } catch (Exception e) {
      throw new RuntimeException(e.toString());
    }
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
    sb.append(Logger.nameOf(msgLevel));
    sb.append(": ");
    sb.append(message);
    writeMsg(sb.toString());
  }

  private void writeMsg(String msg) {
    Object args[] = {msg};
    try {
      writeLogMethod.invoke(helper, args);
    } catch (Exception e) {
      System.err.println("Error invoking AntHelper: " + e.toString());
      System.err.println(msg);
    }
  }

}
