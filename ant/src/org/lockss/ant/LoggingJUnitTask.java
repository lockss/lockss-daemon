/*
 * $Id$
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.ant;

import java.util.*;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.junit.*;

/**
 * Extension of JUnit task the stores the current task in the System
 * properties so the LOCKSS Logger target can find it and log to it.
 */
public class LoggingJUnitTask extends JUnitTask {
  public static String TASK_PROPERTY = "org.lockss.ant.task";

  public LoggingJUnitTask() throws Exception {
    super();
  }

  public void execute() throws BuildException {
    Properties p = System.getProperties();
    p.put(TASK_PROPERTY, this);
    super.execute();
  }

  public void handleErrorOutput(String line) {
    super.handleErrorOutput(line);
  }


  public void logErrorOutput(String line) {
    handleErrorOutput( line);
  }

}
