/*
 * $Id: LockssTestCase.java,v 1.12 2002-11-26 18:00:12 troberts Exp $
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

import java.util.*;
import java.io.*;
import java.net.*;
import org.lockss.util.*;
import junit.framework.TestCase;
import junit.framework.TestResult;


public class LockssTestCase extends TestCase {
  List tmpDirs;
  List doLaters;

  public LockssTestCase(String msg) {
    super(msg);
  }

  /** Create and return the name of a temp dir.  The dir is created within
   * the default temp file dir.
   * It will be deleted following the test, by tearDown().  (So if you
   * override tearDown(), be sure to call <code>super.tearDown()</code>.)
   * @return The newly created directory
   */
  public File getTempDir() throws IOException {
    File tmpdir = FileUtil.createTempDir("locksstest", null);
    if (tmpdir != null) {
      if (tmpDirs == null) {
	tmpDirs = new LinkedList();
      }
      tmpDirs.add(tmpdir);
    }
    return tmpdir;
  }

  /** Remove any temp dirs, cancel any outstanding {@link DoLater}s */
  public void tearDown() throws Exception {
    if (tmpDirs != null) {
      for (ListIterator iter = tmpDirs.listIterator(); iter.hasNext(); ) {
	File dir = (File)iter.next();
	if (FileUtil.delTree(dir)) {
	  iter.remove();
	}
      }
    }
    if (doLaters != null) {
      List copy = new ArrayList(doLaters);
      for (Iterator iter = copy.iterator(); iter.hasNext(); ) {
	DoLater doer = (DoLater)iter.next();
	doer.cancel();
      }
      // do NOT set doLaters to null here.  It may be referenced by
      // exiting DoLaters.  It won't hurt anything because the next test
      // will create a new instance of the test case, and get a different
      // doLaters list
    }
    super.tearDown();
  }

  /** Asserts that two Maps are equal (contain the same mappings).
   * If they are not an AssertionFailedError is thrown. */
  static public void assertEqual(String message, Map expected, Map actual) {
    if (expected == null && actual == null) {
      return;
    }
    if (expected != null && expected.equals(actual)) {
      return;
    }
    failNotEquals(message, expected, actual);
  }
  
  /** Asserts that two Maps are equal (contain the same mappings).
   * If they are not an AssertionFailedError is thrown. */
  static public void assertEqual(Map expected, Map actual) {
    assertEqual(null, expected, actual);
  }

  /** Asserts that two collections are isomorphic. If they are not
   * an AssertionFailedError is thrown. */
  static public void assertIsomorphic(String message,
				      Collection expected, Collection actual) {
    if (CollectionUtil.isIsomorphic(expected, actual)) {
      return;
    }
	failNotEquals(message, expected, actual);
  }
  
  /** Asserts that two collections are isomorphic. If they are not
   * an AssertionFailedError is thrown. */
  static public void assertIsomorphic(Collection expected, Collection actual) {
    assertIsomorphic(null, expected, actual);
  }

  /** Asserts that the array is isomorphic with the collection. If not
   * an AssertionFailedError is thrown. */
  static public void assertIsomorphic(String message,
				      Object expected[], Collection actual) {
    if (CollectionUtil.isIsomorphic(expected, actual)) {
      return;
    }
	failNotEquals(message, expected, actual);
  }
  
  /** Asserts that the array is isomorphic with the collection. If not
   * an AssertionFailedError is thrown. */
  static public void assertIsomorphic(Object expected[], Collection actual) {
    assertIsomorphic(null, expected, actual);
  }

  /** Asserts that the array is isomorphic with the collection behind the
   * iterator. If not an AssertionFailedError is thrown. */
  static public void assertIsomorphic(String message,
				      Object expected[], Iterator actual) {
    if (CollectionUtil.isIsomorphic(new ArrayIterator(expected), actual)) {
      return;
    }
	failNotEquals(message, expected, actual);
  }
  
  /** Asserts that the array is isomorphic with the collection behind the
   * iterator. If not an AssertionFailedError is thrown. */
  static public void assertIsomorphic(Object expected[], Iterator actual) {
    assertIsomorphic(null, expected, actual);
  }

  // tk do a better job of printing collections
  static private void failNotEquals(String message,
					Object expected, Object actual) {
    String formatted= "";
    if (message != null)
      formatted= message+" ";
    fail(formatted+"expected:<"+expected+"> but was:<"+actual+">");
  }

  static private void failNotEquals(String message,
					Object[] expected, Object actual) {
    StringBuffer sb = new StringBuffer(100);
    sb.append("[");
    for (int ix=0; ix<expected.length; ix++) {
      sb.append(expected[ix]);
      sb.append(", ");
    }
    sb.append("]");
    failNotEquals(message, sb.toString(), actual);
  }

  /** Asserts that the two DatagramPackets have equal contents */
  public static void assertEquals(DatagramPacket expected,
				  DatagramPacket actual) {
    assertEquals(expected.getAddress(), actual.getAddress());
    assertEquals(expected.getPort(), actual.getPort());
    assertEquals(expected.getLength(), actual.getLength());
    assertEquals(expected.getOffset(), actual.getOffset());
    assertEquals(expected.getData(), actual.getData());
  }
  /** Abstraction to do something in another thread, after a delay,
   * unless cancelled.  If the sceduled activity is still pending when the
   * test completes, it is cancelled by tearDown().
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
   * For example, <code>Interrupter</code> is defined as:<pre>
   *  public class Interrupter extends DoLater {
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
   *  public Interrupter interruptMeIn(long ms) {
   *    Interrupter i = new Interrupter(ms, Thread.currentThread());
   *    i.start();
   *    return i;
   *  }</pre>
   *
   * Then, to protect a test with a timeout:<pre>
   *  Interrupter intr = null;
   *  try {
   *    intr = interruptMeIn(1000);
   *    // perform a test that should complete in less than one second
   *    intr.cancel();
   *  } finally {
   *    if (intr.did()) {
   *      fail("operation failed to complete in one second");
   *    }
   *  }</pre>
   * The <code>cancel()</code> ensures that the interrupt will not
   * happen after the try block completes.  (This is not necessary at the
   * end of a test case, as any pending interrupters will be cancelled
   * by tearDown.)
   */
  protected abstract class DoLater extends Thread {
    private long wait;
    private boolean want = true;
    private boolean did = false;

    protected DoLater(long waitMs) {
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
	if (doLaters == null) {
	  doLaters = new LinkedList();
	}
	doLaters.add(this);
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
	// exit thread
      } finally {
	doLaters.remove(this);
      }
    }
  }
  /** Interrupter interrupts a thread in a while */
  public class Interrupter extends DoLater {
    private Thread thread;
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
  public Interrupter interruptMeIn(long ms) {
    Interrupter i = new Interrupter(ms, Thread.currentThread());
    i.start();
    return i;
  }
  
}
