/*
 * $Id: LockssTestCase.java,v 1.46 2004-01-24 22:55:42 tlipkis Exp $
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

package org.lockss.test;

import java.util.*;
import java.io.*;
import java.net.*;
import org.lockss.util.*;
import org.lockss.daemon.Configuration;
import org.lockss.daemon.ConfigManager;
import junit.framework.TestCase;
import junit.framework.TestResult;


public class LockssTestCase extends TestCase {
  protected static Logger log =
    Logger.getLogger("LockssTest", Logger.getInitialDefaultLevel());
  /** Timeout duration for timeouts that are expected to time out.  Setting
   * this higher makes normal tests take longer, setting it too low might
   * cause failing tests to erroneously succeed on slow or busy
   * machines. */
  public static int TIMEOUT_SHOULD = 300;

  /** Timeout duration for timeouts that are expected not to time out.
   * This should be set high to ensure catching failures. */
  public static final int DEFAULT_TIMEOUT_SHOULDNT = 2000;
  public static int TIMEOUT_SHOULDNT = DEFAULT_TIMEOUT_SHOULDNT;

  private MockLockssDaemon mockDaemon = null;

  List tmpDirs;
  List doLaters = null;

  public LockssTestCase(String msg) {
    this();
    setName(msg);
  }

  public LockssTestCase() {
    super();
    Integer timeout = Integer.getInteger("org.lockss.test.timeout.shouldnt");
    if (timeout != null) {
      TIMEOUT_SHOULDNT = timeout.intValue();
    }
  }

  /**
   * Create and return the name of a temp dir.  The dir is created within
   * the default temp file dir.
   * It will be deleted following the test, by tearDown().  (So if you
   * override tearDown(), be sure to call <code>super.tearDown()</code>.)
   * @return The newly created directory
   * @throws IOException
   */
  public File getTempDir() throws IOException {
    File tmpdir = FileTestUtil.createTempDir("locksstest", null);
    if (tmpdir != null) {
      if (tmpDirs == null) {
	tmpDirs = new LinkedList();
      }
      tmpDirs.add(tmpdir);
    }
    return tmpdir;
  }

  /**
   * Return the MockLockssDaemon instance for this testcase, creating one
   * if necessary.
   */
  public synchronized MockLockssDaemon getMockLockssDaemon() {
    if (mockDaemon == null) {
      mockDaemon = new MockLockssDaemon();
    }
    return mockDaemon;
  }

  /** Create a fresh config manager, MockLockssDaemon */
  protected void setUp() throws Exception {
    mockDaemon = null;
    super.setUp();
    ConfigManager.makeConfigManager();
  }

  /**
   * Remove any temp dirs, cancel any outstanding {@link
   * org.lockss.test.LockssTestCase.DoLater}s
   * @throws Exception
   */
  protected void tearDown() throws Exception {
    if (doLaters != null) {
      List copy;
      synchronized (this) {
	copy = new ArrayList(doLaters);
      }
      for (Iterator iter = copy.iterator(); iter.hasNext(); ) {
	DoLater doer = (DoLater)iter.next();
	doer.cancel();
      }
      // do NOT set doLaters to null here.  It may be referenced by
      // exiting DoLaters.  It won't hurt anything because the next test
      // will create a new instance of the test case, and get a different
      // doLaters list
    }
    // XXX this should be folded into LockssDaemon shutdown
    ConfigManager cfg = ConfigManager.getConfigManager();
    if (cfg != null) {
      cfg.stopService();
    }

    TimerQueue.stopTimerQueue();

    // delete temp dirs
    boolean leave = Boolean.getBoolean("org.lockss.keepTempFiles");
    if (tmpDirs != null && !leave) {
      for (ListIterator iter = tmpDirs.listIterator(); iter.hasNext(); ) {
	File dir = (File)iter.next();
	if (FileTestUtil.delTree(dir)) {
	  log.debug2("deltree(" + dir + ") = true");
	  iter.remove();
	} else {
	  log.debug2("deltree(" + dir + ") = false");
	}
      }
    }
    super.tearDown();
    if (Boolean.getBoolean("org.lockss.test.threadDump")) {
      DebugUtils.getInstance().threadDump();
      TimerUtil.guaranteedSleep(1000);
    }
  }

  double successRate;
  int successMaxRepetitions;
  int successMaxFailures;

  /** Causes the current test case to be repeated if it fails, ultimately
   * succeeding if the success rate is sufficiently high.  If a test is
   * repeated, a message will be written to System.err.  Repetitions are
   * not reflected in test statistics.
   * @param rate the minimum success rate between 0 and 1 (successes /
   * attempts) necessary for the test ultimately to succeed.
   * @param maxRepetitions the maximum number of times the test will be
   * repeated in an attempt to achieve the specified success rate.
   * @see #successRateSetUp()
   * @see #successRateTearDown()
   */
  protected void assertSuccessRate(double rate, int maxRepetitions) {
    if (successMaxFailures == 0) {
      successRate = rate;
      successMaxRepetitions = maxRepetitions;
      successMaxFailures = maxRepetitions - ((int)(rate * maxRepetitions));
    }
  }

  /**
   * Runs the bare test sequence, repeating if necessary to achieve the
   * specified success rate.
   * @see #assertSuccessRate
   * @exception Throwable if any exception is thrown
   */
  public void runBare() throws Throwable {
    int rpt = 0;
    int failures = 0;
    successRateSetUp();
    try {
      while (true) {
	try {
	  super.runBare();
	} catch (Throwable e) {
	  if (++failures > successMaxFailures) {
	    rpt++;
	    throw e;
	  }
	}
	if (++rpt >= successMaxRepetitions) {
	  break;
	}
	if ((((double)(rpt - failures)) / ((double)rpt)) > successRate) {
	  break;
	}
      }
    } finally {
      if (successMaxFailures > 0 && failures > 0) {
	System.err.println(getName() + " failed " + failures +
			   " of " + rpt + " tries, " +
			   ((failures > successMaxFailures) ? "not " : "") +
			   "achieving a " + successRate + " success rate.");
      }
      successRateTearDown();
    }
  }

  /** Called once (before setUp()) before a set of repetitions of a test
   * case that uses assertSuccessRate().  (setUp() is called before each
   * repetition.) */
  protected void successRateSetUp() {
    successMaxFailures = 0;
  }

  /** Called once (after tearDown()) after a set of repetitions of a test
   * case that uses assertSuccessRate().  (tearDown() is called after each
   * repetition.) */
  protected void successRateTearDown() {
  }

  /**
   * Asserts that two Maps are equal (contain the same mappings).
   * If they are not an AssertionFailedError is thrown.
   * @param message the message to give on failure
   * @param expected the expected value
   * @param actual the actual value
   */
  static public void assertEqual(String message, Map expected, Map actual) {
    if (expected == null && actual == null) {
      return;
    }
    if (expected != null && expected.equals(actual)) {
      return;
    }
    failNotEquals(message, expected, actual);
  }

  /**
   * Asserts that two Maps are equal (contain the same mappings).
   * If they are not an AssertionFailedError is thrown.
   * @param expected the expected value
   * @param actual the actual value
   */
  static public void assertEqual(Map expected, Map actual) {
    assertEqual(null, expected, actual);
  }

  /**
   * Asserts that two collections are isomorphic. If they are not
   * an AssertionFailedError is thrown.
   * @param message the message to give on failure
   * @param expected the expected value
   * @param actual the actual value
   */
  static public void assertIsomorphic(String message,
				      Collection expected, Collection actual) {
    if (CollectionUtil.isIsomorphic(expected, actual)) {
      return;
    }
    failNotEquals(message, expected, actual);
  }

  /**
   * Asserts that two collections are isomorphic. If they are not
   * an AssertionFailedError is thrown.
   * @param expected the expected value
   * @param actual the actual value
   */
  static public void assertIsomorphic(Collection expected, Collection actual) {
    assertIsomorphic(null, expected, actual);
  }

  /**
   * Asserts that the array is isomorphic with the collection. If not
   * an AssertionFailedError is thrown.
   * @param message the message to give on failure
   * @param expected the expected value
   * @param actual the actual value
   */
  static public void assertIsomorphic(String message,
				      Object expected[], Collection actual) {
    if (CollectionUtil.isIsomorphic(expected, actual)) {
      return;
    }
    failNotEquals(message, expected, actual);
  }

  /**
   * Asserts that the array is isomorphic with the collection. If not
   * an AssertionFailedError is thrown.
   * @param expected the expected value
   * @param actual the actual value
   */
  static public void assertIsomorphic(Object expected[], Collection actual) {
    assertIsomorphic(null, expected, actual);
  }

  /**
   * Asserts that the array is isomorphic with the collection behind the
   * iterator. If not an AssertionFailedError is thrown.
   * @param message the message to give on failure
   * @param expected the expected value
   * @param actual the actual value
   */
  static public void assertIsomorphic(String message,
				      Object expected[], Iterator actual) {
    if (CollectionUtil.isIsomorphic(new ArrayIterator(expected), actual)) {
      return;
    }
    failNotEquals(message, expected, actual);
  }

  /**
   * Asserts that the array is isomorphic with the collection behind the
   * iterator. If not an AssertionFailedError is thrown.
   * @param expected the expected value
   * @param actual the actual value
   */
  static public void assertIsomorphic(Object expected[], Iterator actual) {
    assertIsomorphic(null, expected, actual);
  }

  /**
   * Asserts that the two boolean arrays have equal contents
   * @param expected the expected value
   * @param actual the actual value
   */
  public static void assertEquals(boolean[] expected, boolean[] actual) {
    assertEquals(null, expected, actual);
  }

  /**
   * Asserts that the two boolean arrays have equal contents
   * @param message the message to give on failure
   * @param expected the expected value
   * @param actual the actual value
   */
  public static void assertEquals(String message,
				  boolean[] expected, boolean[] actual) {
    if (Arrays.equals(expected, actual)) {
      return;
    }
    failNotEquals(message, expected, actual);
  }

  /**
   * Asserts that the two byte arrays have equal contents
   * @param expected the expected value
   * @param actual the actual value
   */
  public static void assertEquals(byte[] expected, byte[] actual) {
    assertEquals(null, expected, actual);
  }

  /**
   * Asserts that the two byte arrays have equal contents
   * @param message the message to give on failure
   * @param expected the expected value
   * @param actual the actual value
   */
  public static void assertEquals(String message,
				  byte[] expected, byte[] actual) {
    if (Arrays.equals(expected, actual)) {
      return;
    }
    failNotEquals(message, expected, actual);
  }

  /**
   * Asserts that the two char arrays have equal contents
   * @param expected the expected value
   * @param actual the actual value
   */
  public static void assertEquals(char[] expected, char[] actual) {
    assertEquals(null, expected, actual);
  }

  /**
   * Asserts that the two char arrays have equal contents
   * @param message the message to give on failure
   * @param expected the expected value
   * @param actual the actual value
   */
  public static void assertEquals(String message,
				  char[] expected, char[] actual) {
    if (Arrays.equals(expected, actual)) {
      return;
    }
    failNotEquals(message, expected, actual);
  }

  /**
   * Asserts that the two double arrays have equal contents
   * @param expected the expected value
   * @param actual the actual value
   */
  public static void assertEquals(double[] expected, double[] actual) {
    assertEquals(null, expected, actual);
  }

  /**
   * Asserts that the two double arrays have equal contents
   * @param message the message to give on failure
   * @param expected the expected value
   * @param actual the actual value
   */
  public static void assertEquals(String message,
				  double[] expected, double[] actual) {
    if (Arrays.equals(expected, actual)) {
      return;
    }
    failNotEquals(message, expected, actual);
  }

  /**
   * Asserts that the two float arrays have equal contents
   * @param expected the expected value
   * @param actual the actual value
   */
  public static void assertEquals(float[] expected, float[] actual) {
    assertEquals(null, expected, actual);
  }

  /**
   * Asserts that the two float arrays have equal contents
   * @param message the message to give on failure
   * @param expected the expected value
   * @param actual the actual value
   */
  public static void assertEquals(String message,
				  float[] expected, float[] actual) {
    if (Arrays.equals(expected, actual)) {
      return;
    }
    failNotEquals(message, expected, actual);
  }

  /**
   * Asserts that the two int arrays have equal contents
   * @param expected the expected value
   * @param actual the actual value
   */
  public static void assertEquals(int[] expected, int[] actual) {
    assertEquals(null, expected, actual);
  }

  /**
   * Asserts that the two int arrays have equal contents
   * @param message the message to give on failure
   * @param expected the expected value
   * @param actual the actual value
   */
  public static void assertEquals(String message,
				  int[] expected, int[] actual) {
    if (Arrays.equals(expected, actual)) {
      return;
    }
    failNotEquals(message, expected, actual);
  }

  /**
   * Asserts that the two short arrays have equal contents
   * @param expected the expected value
   * @param actual the actual value
   */
  public static void assertEquals(short[] expected, short[] actual) {
    assertEquals(null, expected, actual);
  }

  /**
   * Asserts that the two short arrays have equal contents
   * @param message the message to give on failure
   * @param expected the expected value
   * @param actual the actual value
   */
  public static void assertEquals(String message,
				  short[] expected, short[] actual) {
    if (Arrays.equals(expected, actual)) {
      return;
    }
    failNotEquals(message, expected, actual);
  }

  /**
   * Asserts that the two long arrays have equal contents
   * @param expected the expected value
   * @param actual the actual value
   */
  public static void assertEquals(long[] expected, long[] actual) {
    assertEquals(null, expected, actual);
  }

  /**
   * Asserts that the two long arrays have equal contents
   * @param message the message to give on failure
   * @param expected the expected value
   * @param actual the actual value
   */
  public static void assertEquals(String message,
				  long[] expected, long[] actual) {
    if (Arrays.equals(expected, actual)) {
      return;
    }
    failNotEquals(message, expected, actual);
  }

  /**
   * Asserts that the two Object arrays have equal contents
   * @param expected the expected value
   * @param actual the actual value
   */
  public static void assertEquals(Object[] expected, Object[] actual) {
    assertEquals(null, expected, actual);
  }

  /**
   * Asserts that the two Object arrays have equal contents
   * @param message the message to give on failure
   * @param expected the expected value
   * @param actual the actual value
   */
  public static void assertEquals(String message,
				  Object[] expected, Object[] actual) {
    if (Arrays.equals(expected, actual)) {
      return;
    }
    failNotEquals(message, expected, actual);
  }

  /**
   * Asserts that the two URLs are equal
   * @param expected the expected value
   * @param actual the actual value
   */
  public static void assertEquals(URL expected, URL actual) {
    assertEquals(null, expected, actual);
  }

  /**
   * Asserts that the two URLs are equal
   * @param message the message to give on failure
   * @param expected the expected value
   * @param actual the actual value
   */
  public static void assertEquals(String message,
				  URL expected, URL actual) {
    if (UrlUtil.equalUrls(expected, actual)) {
      return;
    }
    failNotEquals(message, expected, actual);
  }

  /**
   * Asserts that two objects are not equal. If they are not
   * an AssertionFailedError is thrown with the given message.
   * @param message the message to give on failure
   * @param expected the expected value
   * @param actual the actual value
   */
  public static void assertNotEquals(String message,
				     Object expected, Object actual) {
    if ((expected == null && actual == null) ||
	(expected != null && expected.equals(actual))) {
      failEquals(message, expected, actual);
    }
  }

  /**
   * Asserts that two objects are not equal. If they are not
   * an AssertionFailedError is thrown with the given message.
   * @param expected the expected value
   * @param actual the actual value
   */
  public static void assertNotEquals(Object expected, Object actual) {
    assertNotEquals(null, expected, actual);
  }

  public static void assertNotEquals(long expected, long actual) {
    assertNotEquals(null, expected, actual);
  }

  public static void assertNotEquals(String message,
				     long expected, long actual) {
    assertNotEquals(message, new Long(expected), new Long(actual));
  }

  public static void assertNotEquals(int expected, int actual) {
    assertNotEquals(null, expected, actual);
  }

  public static void assertNotEquals(String message,
				     int expected, int actual) {
    assertNotEquals(message, new Integer(expected), new Integer(actual));
  }

  public static void assertNotEquals(short expected, short actual) {
    assertNotEquals(null, expected, actual);
  }

  public static void assertNotEquals(String message,
				     short expected, short actual) {
    assertNotEquals(message, new Short(expected), new Short(actual));
  }

  public static void assertNotEquals(byte expected, byte actual) {
    assertNotEquals(null, expected, actual);
  }

  public static void assertNotEquals(String message,
				     byte expected, byte actual) {
    assertNotEquals(message, new Byte(expected), new Byte(actual));
  }

  public static void assertNotEquals(char expected, char actual) {
    assertNotEquals(null, expected, actual);
  }

  public static void assertNotEquals(String message,
				     char expected, char actual) {
    assertNotEquals(message, new Character(expected), new Character(actual));
  }

  public static void assertNotEquals(boolean expected, boolean actual) {
    assertNotEquals(null, expected, actual);
  }

  public static void assertNotEquals(String message,
				     boolean expected, boolean actual) {
    assertNotEquals(message, new Boolean(expected), new Boolean(actual));
  }

  public static void assertNotEquals(double expected, double actual,
				     double delta) {
    assertNotEquals(null, expected, actual, delta);
  }

  public static void assertNotEquals(String message, double expected,
				     double actual, double delta) {
    // handle infinity specially since subtracting to infinite
    //values gives NaN and the the following test fails
    if (Double.isInfinite(expected)) {
      if (expected == actual){
	failEquals(message, new Double(expected), new Double(actual));
      }
    } else if ((Math.abs(expected-actual) <= delta)) {
    // Because comparison with NaN always returns false
      failEquals(message, new Double(expected), new Double(actual));
    }
  }

  public static void assertNotEquals(float expected, float actual,
				     float delta) {
    assertNotEquals(null, expected, actual, delta);
  }

  public static void assertNotEquals(String message, float expected,
				     float actual, float delta) {
    // handle infinity specially since subtracting to infinite
    //values gives NaN and the the following test fails
    if (Double.isInfinite(expected)) {
      if (expected == actual){
	failEquals(message, new Float(expected), new Float(actual));
      }
    } else if ((Math.abs(expected-actual) <= delta)) {
    // Because comparison with NaN always returns false
      failEquals(message, new Float(expected), new Float(actual));
    }
  }

  public static void assertEmpty(Collection coll) {
    assertEmpty(null, coll);
  }

  public static void assertEmpty(String message, Collection coll) {
    if (coll.size() > 0) {
      StringBuffer sb = new StringBuffer();
      if (message != null) {
	sb.append(message);
	sb.append(" ");
      }
      sb.append("Expected empty Collection, but containted ");
      sb.append(coll);
      fail(sb.toString());
    }
  }

  public static void assertContains(Collection coll, Object element) {
    assertContains(null, coll, element);
  }

  public static void assertContains(String msg, Collection coll,
				    Object element) {
    if (!coll.contains(element)) {
      StringBuffer sb = new StringBuffer();
      if (msg != null) {
	sb.append(msg);
	sb.append(" ");
      }
      sb.append("Collection doesn't contain expected element: ");
      sb.append(element);
      fail(sb.toString());
    }
  }

  public static void assertDoesNotContain(Collection coll, Object element) {
    assertDoesNotContain(null, coll, element);
  }

  public static void assertDoesNotContain(String msg, Collection coll,
					  Object element) {
    if (coll.contains(element)) {
      StringBuffer sb = new StringBuffer();
      if (msg != null) {
	sb.append(msg);
	sb.append(" ");
      }
      sb.append("Collection contains unexpected element: ");
      sb.append(element);
      fail(sb.toString());
    }
  }

  private static void failEquals(String message,
				 Object expected, Object actual) {
    StringBuffer sb = new StringBuffer();
    if (message != null) {
      sb.append(message);
      sb.append(" ");
    }
    sb.append("expected not equals, but both were ");
    sb.append(expected);
    fail(sb.toString());
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
				    int[] expected, int[] actual) {
    String formatted= "";
    if (message != null)
      formatted= message+" ";
    fail(formatted+"expected:<"+arrayString(expected)+
	 "> but was:<"+arrayString(actual)+">");
  }

  static protected Object[] objArray(int[] a) {
    Object[] o = new Object[a.length];
    for (int ix = 0; ix < a.length; ix++) {
      o[ix] = new Integer(a[ix]);
    }
    return o;
  }

  static protected String arrayString(int[] a) {
    return StringUtil.separatedString(objArray(a), ", ");
  }

  static private void failNotEquals(String message,
				    long[] expected, long[] actual) {
    String formatted= "";
    if (message != null)
      formatted= message+" ";
    fail(formatted+"expected:<"+arrayString(expected)+
	 "> but was:<"+arrayString(actual)+">");
  }

  static protected Object[] objArray(long[] a) {
    Object[] o = new Object[a.length];
    for (int ix = 0; ix < a.length; ix++) {
      o[ix] = new Long(a[ix]);
    }
    return o;
  }

  static protected String arrayString(long[] a) {
    return StringUtil.separatedString(objArray(a), ", ");
  }

  static private void failNotEquals(String message,
				    byte[] expected, byte[] actual) {
    String formatted= "";
    if (message != null)
      formatted= message+" ";
    fail(formatted+"expected:<"+ByteArray.toHexString(expected)+
	 "> but was:<"+ByteArray.toHexString(actual)+">");
//      fail(formatted+"expected:<"+arrayString(expected)+
//  	 "> but was:<"+arrayString(actual)+">");
  }

  static protected Object[] objArray(byte[] a) {
    Object[] o = new Object[a.length];
    for (int ix = 0; ix < a.length; ix++) {
      o[ix] = new Integer(a[ix]);
    }
    return o;
  }

  static protected String arrayString(byte[] a) {
    return StringUtil.separatedString(objArray(a), ", ");
  }

  static private void failNotEquals(String message,
				    Object[] expected, Object actual) {
    failNotEquals(message,
		  "[" + StringUtil.separatedString(expected, ", ") + "]",
		  actual);
  }

  /**
   * Asserts that the two DatagramPackets have equal contents
   * @param expected the expected value
   * @param actual the actual value
   */
  public static void assertEquals(DatagramPacket expected,
				  DatagramPacket actual) {
    assertEquals(expected.getAddress(), actual.getAddress());
    assertEquals(expected.getPort(), actual.getPort());
    assertEquals(expected.getLength(), actual.getLength());
    assertEquals(expected.getOffset(), actual.getOffset());
    assertTrue(Arrays.equals(expected.getData(), actual.getData()));
  }


  /**
   * Asserts that two collections have all the same elements of the same
   * cardinality; tries to give useful output if it fails
   */
  public static void assertSameElements(Collection expected,
					Collection actual) {
    assertTrue("Expected "+expected+" but was "+actual,
	       org.apache.commons.collections.
	       CollectionUtils.isEqualCollection(expected, actual));
  }

  /**
   * Asserts that a string matches the content of a reader
   */
  public static void assertReaderMatchesString(String expected, Reader reader)
      throws IOException{
    StringBuffer actual = new StringBuffer(expected.length());
    int kar;
    while ((kar = reader.read()) != -1) {
      actual.append((char)kar);
    }
    assertEquals(expected, actual.toString());
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

    /**
     * Return true iff action was taken
     * @return true iff taken
     */
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
	synchronized (LockssTestCase.this) {
	  if (doLaters == null) {
	    doLaters = new LinkedList();
	  }
	  doLaters.add(this);
	}
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
	synchronized (LockssTestCase.this) {
	  doLaters.remove(this);
	}
      }
    }
  }
  /** Interrupter interrupts a thread in a while */
  public class Interrupter extends DoLater {
    private Thread thread;
    private boolean threadDump = false;

    Interrupter(long waitMs, Thread thread) {
      super(waitMs);
      setPriority(thread.getPriority() + 1);
      this.thread = thread;
    }

    /** Interrupt the thread */
    protected void doit() {
      log.debug("Interrupting");
      if (threadDump) {
	try {
	  DebugUtils.getInstance().threadDump();
	  TimerUtil.guaranteedSleep(1000);
	} catch (Exception e) {
	}
      }
      thread.interrupt();
    }

    /** Interrupt the thread */
    public void setThreadDump() {
      threadDump = true;
    }
  }

  /**
   * Interrupt current thread in a while
   * @param ms interval to wait before interrupting
   * @return an Interrupter
   */
  public Interrupter interruptMeIn(long ms) {
    Interrupter i = new Interrupter(ms, Thread.currentThread());
    i.start();
    return i;
  }

  /**
   * Interrupt current thread in a while, first printing a thread dump
   * @param ms interval to wait before interrupting
   * @param threadDump true if thread dump wanted
   * @return an Interrupter
   */
  public Interrupter interruptMeIn(long ms, boolean threadDump) {
    Interrupter i = new Interrupter(ms, Thread.currentThread());
    if (threadDump) {
      i.setThreadDump();
    }
    i.start();
    return i;
  }

}
