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

package org.lockss.app;

/**
 * Base implementation of LockssDaemonManager
 */

public abstract class BaseLockssDaemonManager extends BaseLockssManager {

  /**
   * Calls {@link #initService(LockssDaemon)}, so that daemon-specific
   * managers don't have to cast their <code>app</code> arg.  Such managers
   * that wish to override <code>initService()</code> should define
   * <i>either</i> <code>initService(LockssDaemon)</code> <i>or</i>
   * <code>initService(LockssApp)</code>, and should call
   * <code>super.init(app)</code> or <code>super.init(daemon)</code> from
   * it.
   * @param app the {@link LockssApp}
   * @throws LockssAppException
   */
  public void initService(LockssApp app) throws LockssAppException {
    initService((LockssDaemon)app);
  }

  public void initService(LockssDaemon daemon) throws LockssAppException {
    super.initService(daemon);
  }

  /** Return the daemon instance in which this manager is running */
  public LockssDaemon getDaemon() {
    return (LockssDaemon)theApp;
  }

  /**
   * Return true iff all the daemon services have been initialized.
   * @return true if the daemon is inited
   */
  protected boolean isDaemonInited() {
    return isAppInited();
  }

}
