/*
 * $Id: DoLater.java,v 1.2 2002-09-23 02:57:31 tal Exp $
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



package org.lockss.test;

/** Abstraction to do something in another thread, after a delay,
 * unless cancelled.
 * <br>For one-off use:<pre>
 *  final Object obj = ...;
 *  DoLater doer = new DoLater(1000) {
 *      protected void doit() {
 *        obj.method(...);
 *      }
 *    };
 *  doer.start();</pre>
 *
 * Or, for convenient repeated use of a particular delayed operation,
 * define a class that extends <code>DoLater</code>,
 * with a constructor that calls
 * <code>super(wait)</code> and stores any other necessary args into
 * instance vars, and a <code>doit()</code> method that does whatever needs
 * to be done.  And a convenience method to create and start it.
 * For example, <code>DoLater.Interrupter</code> is defined as:<pre>
 *  public static class Interrupter extends DoLater {
 *    private Thread thread;
 *    Interrupter(long waitMs, Thread thread) {
 *      super(waitMs);
 *      this.thread = thread;
 *    }
 *
 *    protected void doit() {
 *      thread.interrupt();
 *    }
 *  }
 * 
 *  public static Interrupter interruptMeIn(long ms) {
 *    Interrupter i = new Interrupter(ms, Thread.currentThread());
 *    i.start();
 *    return i;
 *  }</pre>
 *
 * Then, to protect a test with a timeout:<pre>
 *  DoLater.Interrupter intr = null;
 *  try {
 *    intr = DoLater.interruptMeIn(1000);
 *    // perform a test that should complete in less than one second
 *    intr.cancel();
 *  } finally {
 *    if (intr.did()) {
 *      fail("operation failed to complete in one second");
 *    }
 *  }</pre>
 * The <code>cancel()</code> ensures that the interrupt will not
 * happen after the try block completes.
*/
public abstract class DoLater extends Thread {
  private long wait;
  private boolean want = true;
  private boolean did = false;

  public DoLater(long waitMs) {
    wait = waitMs;
  }

  /** Must override this to perform desired action */
  protected abstract void doit();

  /** Return true iff action was taken */
  public boolean did() {
    return did;
  }

  /** Cancel the action iff it hasn't already started.  If it has started,
   * wait until it completes.  (Thus when <code>cancel()</code> returns, it
   * is safe to destroy any environment on which the action relies.)
   */
  public synchronized void cancel() {
    if (want) {
      want = false;
      this.interrupt();
    }
  }

  public final void run() {
    try {
      if (wait != 0) {
	TimerUtil.sleep(wait);
      }
      synchronized (this) {
	if (want) {
	  want = false;
	  did = true;
	  doit();
	}
      }
    } catch (InterruptedException e) {
    }
  }

  /** Interrupter interrupts a thread in a while */
  public static class Interrupter extends DoLater {
    Thread thread;
    Interrupter(long waitMs, Thread thread) {
      super(waitMs);
      this.thread = thread;
    }

    /** Interrupt the thread */
    protected void doit() {
      thread.interrupt();
    }
  }

  /** Interrupt current thread in a while */
  public static Interrupter interruptMeIn(long ms) {
    Interrupter i = new Interrupter(ms, Thread.currentThread());
    i.start();
    return i;
  }
}

