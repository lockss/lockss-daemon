/*
 * $Id: TreeWalker.java,v 1.1 2004-08-21 06:52:51 tlipkis Exp $
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

package org.lockss.state;

import org.lockss.util.*;
import org.lockss.daemon.*;

/**
 * Interface for classes that implement a treewalk.  Instances may be
 * called repeatedly, or may be created for each treewalk.  (<i.Ie</8>,
 * implementations should neither attempt to save state between
 * invocations, nor assume a virgin object.
 */
public interface TreeWalker {

  /** Set the watchdog I should poke occasionally during the treewalk */
  void setWDog(LockssWatchdog wdog);

  /** Return true iff the most recent treewalk did not end early for any
   * reason */
  boolean didFullTreewalk();

  /**
   * Do a single treewalk
   * @param finishBy the end of the scheduled window
   * @return true if the treewalk actually happened false if it couldn't
   * start because it couldn't get the activity lock
   */
  public boolean doTreeWalk(Deadline finishBy);

  /** Cause the current treewalk (running in another thread) to terminate
   * soon */
  void abort();

  /**
   * Perform any action that should happen after the treewalk background
   * task has been removed from the schedule, <i> eg</i>, starting a poll.
   */
  void doDeferredAction();
}
