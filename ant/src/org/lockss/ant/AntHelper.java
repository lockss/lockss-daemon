/*
 * $Id$
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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
import org.apache.tools.ant.*;

/**
 * Helper for AntTaskTarget.  This tree is compiled before main LOCKSS
 * source tree, so can't reference any LOCKSS classes.  All references to
 * Ant classes are here, to keep them out of main tree.
 */
public class AntHelper {
  private Task task;
  private Project project;

  /** Create instance, get current Ant task from where LoggingJUnitTask
   * stashed it.
   * @throw IllegalStateException if no Ant task found (means we're not
   * running in one of our customized Ant tasks). */
  public AntHelper() {
    Properties pr = System.getProperties();
    task = (Task)pr.get(LoggingJUnitTask.TASK_PROPERTY);
    if (task == null) {
      throw new IllegalStateException("No ant task stored in " +
				      LoggingJUnitTask.TASK_PROPERTY +
				      " property");
    }
    project = task.getProject();
  }

  /** Output log msg to the stored Ant task */
  public void writeLog(String msg) {
    // Would like to call task's handleErrorOutput() directly, but it's not
    // public.  Could add public method to LoggingJUnitTask, which calls
    // handleErrorOutput(), but the task instance was created from a
    // different class loader than this, so straightforward cast doesn't
    // work.  Instead, call project.demuxOutput() with the current thread
    // mapped to the task.

    boolean unreg = false;
    Task threadTask = project.getThreadTask(Thread.currentThread());
    if (threadTask == null) {
      project.registerThreadTask(Thread.currentThread(), task);
      unreg = true;
    }
    project.demuxOutput(msg, true);
    if (unreg) {
      project.registerThreadTask(Thread.currentThread(), null);
    }
  }
}
