/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.*;
import junit.framework.*;
import org.apache.commons.collections.iterators.ObjectArrayIterator;
import org.apache.commons.io.IOUtils;
import org.apache.oro.text.regex.Pattern;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.db.DbManager;
import org.lockss.util.*;


public class LockssTestCase extends TestCase {
  protected static Logger log =
    Logger.getLoggerWithInitialLevel("LockssTest",
				     Logger.getInitialDefaultLevel());
  /** Timeout duration for timeouts that are expected to time out.  Setting
   * this higher makes normal tests take longer, setting it too low might
   * cause failing tests to erroneously succeed on slow or busy
   * machines. */
  public static int TIMEOUT_SHOULD = 300;

  /** Timeout duration for timeouts that are expected not to time out.
   * This should be set high to ensure catching failures. */
  public static final int DEFAULT_TIMEOUT_SHOULDNT = 2000;
  public static int TIMEOUT_SHOULDNT = DEFAULT_TIMEOUT_SHOULDNT;

  static final String TEST_ID_FILE_NAME = ".locksstestcase";
  private static final String TEST_DB_FILE_SPEC = "/org/lockss/db/db.zip";

  private MockLockssDaemon mockDaemon = null;

  List tmpDirs;
  List doLaters = null;
  String javaIoTmpdir;
  TestResult result;

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
   * Return true if should skip tests that rely on a working network
   */
  public boolean isSkipNetworkTests() {
    return Boolean.getBoolean("org.lockss.test.skipNetworkTests");
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
    File res =  getTempDir("locksstest");
    // To aid in finding the cause of temp dirs that don't get deleted,
    // setting -Dorg.lockss.test.idTempDirs=true will record the name of
    // the test creating the dir in <dir>/.locksstestcase .  This may cause
    // tests to fail (expecting empty dir).
    if (!isKeepTempFiles()
	&& Boolean.getBoolean("org.lockss.test.idTempDirs")) {
      FileTestUtil.writeFile(new File(res, TEST_ID_FILE_NAME),
			     StringUtil.shortName(this.getClass()));
    }
    return res;
  }

  /**
   * Create and return the name of a temp dir.  The dir is created within
   * the default temp file dir.
   * It will be deleted following the test, by tearDown().  (So if you
   * override tearDown(), be sure to call <code>super.tearDown()</code>.)
   * @param prefix the prefix of the name of the directory
   * @return The newly created directory
   * @throws IOException
   */
  public File getTempDir(String prefix) throws IOException {
    File tmpdir = FileUtil.createTempDir(prefix, null);
    if (tmpdir != null) {
      if (tmpDirs == null) {
	tmpDirs = new LinkedList();
      }
      tmpDirs.add(tmpdir);
    }
    return tmpdir;
  }

  /**
   * Create and return the name of a temp file.  The file is created within
   * the default temp dir.
   * It will be deleted following the test, by tearDown().  (So if you
   * override tearDown(), be sure to call <code>super.tearDown()</code>.)
   * @param prefix the prefix of the name of the file
   * @param suffis the suffix of the name of the file
   * @return The newly created file
   * @throws IOException
   */
  public File getTempFile(String prefix, String suffis) throws IOException {
    File tmpfile = FileUtil.createTempFile(prefix, suffis);
    if (tmpfile != null) {
      if (tmpDirs == null) {
	tmpDirs = new LinkedList();
      }
      tmpDirs.add(tmpfile);
    }
    return tmpfile;
  }

  /**
   * Configure the platform disk space list to point to a newly-created
   * temp dir.
   * @return The path to the newly created directory (ok for other use by
   * unit test)
   * @throws IOException
   */
  public String setUpDiskSpace() throws IOException {
    String diskList =
      CurrentConfig.getParam(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST);
    if (!StringUtil.isNullString(diskList)) {
      return StringUtil.breakAt(diskList, ";").get(0);
    }
    String tmpdir = getTempDir().getAbsolutePath() + File.separator;
    ConfigurationUtil.addFromArgs(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
				  tmpdir);
    return tmpdir;
  }

  /**
   * Return the MockLockssDaemon instance for this testcase.  All test code
   * should use this method rather than creating a MockLockssDaemon.
   */
  public MockLockssDaemon getMockLockssDaemon() {
    return mockDaemon;
  }

  /** Create a fresh config manager, MockLockssDaemon */
  protected void setUp() throws Exception {
    TimerQueue.setSingleton(new ErrorRecordingTimerQueue());
    javaIoTmpdir = System.getProperty("java.io.tmpdir");
    ConfigManager.makeConfigManager();
    Logger.resetLogs();
    mockDaemon = newMockLockssDaemon();
    super.setUp();
    disableThreadWatchdog();
  }

  protected MockLockssDaemon newMockLockssDaemon() {
    return new MockLockssDaemon();
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

    // Interrupters can result in the thread interrupt flag being set.
    // Clear it here to ensure it doesn't affect ensuing tests.
    Thread.interrupted();

    // XXX this should be folded into LockssDaemon shutdown
    ConfigManager cfg = ConfigManager.getConfigManager();
    if (cfg != null) {
      cfg.stopService();
    }

    TimerQueue.stopTimerQueue();

    // delete temp dirs
    if (tmpDirs != null && !isKeepTempFiles()) {
      for (ListIterator iter = tmpDirs.listIterator(); iter.hasNext(); ) {
	File dir = (File)iter.next();
	File idFile = new File(dir, TEST_ID_FILE_NAME);
	String idContent = null;
	if (idFile.exists()) {
	  idContent = StringUtil.fromFile(idFile);
	}
	if (FileUtil.delTree(dir)) {
	  log.debug2("deltree(" + dir + ") = true");
	  iter.remove();
	} else {
	  log.debug2("deltree(" + dir + ") = false");
	  if (idContent != null) {
	    FileTestUtil.writeFile(idFile, idContent);
	  }
	}
      }
    }
    if (!StringUtil.isNullString(javaIoTmpdir)) {
      System.setProperty("java.io.tmpdir", javaIoTmpdir);
    }
    super.tearDown();
    if (Boolean.getBoolean("org.lockss.test.threadDump")) {
      PlatformUtil.getInstance().threadDump(true);
    }
    // don't reenable the watchdog; some threads may not have exited yet
//     enableThreadWatchdog();
  }

  public static boolean isKeepTempFiles() {
    return Boolean.getBoolean("org.lockss.keepTempFiles");
  }

  protected void disableThreadWatchdog() {
    System.setProperty(LockssRunnable.PARAM_THREAD_WDOG_EXIT_IMM, "false");
  }

  protected void enableThreadWatchdog() {
    System.setProperty(LockssRunnable.PARAM_THREAD_WDOG_EXIT_IMM, "true");
  }

  /** Convenience method for test classes to obtain a URL on a test file.
   * @param name name of file in same directory as <tt>this</tt> (the code
   * making this call), or a modified package name (dots replaced by
   * slashes), interpreted as absolute if starts with shash, else relative
   * to the package containing <tt>this</tt>.  If the resource is not
   * found, an assertion failure will occur.
   * @return The URL of the resource.  Null is never returned.
   */
  protected URL getResource(String name) {
    URL res = getClass().getResource(name);
    String err = "Resource not found: " + name;
    assertNotNull(err, res);
    return res;
  }

  /** Convenience method for test classes to obtain an InputStream on a
   * test file.
   * @param name name of file in same directory as <tt>this</tt> (the code
   * making this call), or a modified package name (dots replaced by
   * slashes), interpreted as absolute if starts with shash, else relative
   * to the package containing <tt>this</tt>.  If the resource is not
   * found, an assertion failure will occur.
   * @return An InputStream open on the resource.  Null is never returned.
   */
  protected InputStream getResourceAsStream(String name) {
    return getResourceAsStream(name, true);
  }

  /** Convenience method for test classes to obtain an InputStream on a
   * test file.
   * @param name name of file in same directory as <tt>this</tt> (the code
   * making this call), or a modified package name (dots replaced by
   * slashes), interpreted as absolute if starts with shash, else relative
   * to the package containing <tt>this</tt>.
   * @param failOnNull A boolean indicating whether an assertion failure should
   * occur if the resource is not found.
   * @return An InputStream open on the resource.
   */
  protected InputStream getResourceAsStream(String name, boolean failOnNull) {
    InputStream res = getClass().getResourceAsStream(name);
    String err = "Resource not found: " + name;
    if (failOnNull) {
      assertNotNull(err, res);
    }
    return res;
  }

  /** Convenience method for test classes to obtain the content of a
   * test file.
   * @param name name of file in same directory as <tt>this</tt> (the code
   * making this call), or a modified package name (dots replaced by
   * slashes), interpreted as absolute if starts with shash, else relative
   * to the package containing <tt>this</tt>.
   * @return The file content
   */
  protected String getResourceContent(String name) throws IOException {
    try (InputStream in = getResourceAsStream(name)) {
      return StringUtil.fromInputStream(in);
    }
  }

  /** Overridden so we can can get ahold of the result object, in order to
   * manually add errors */
  public void run(TestResult result) {
    this.result = result;
    super.run(result);
  }

  // variant harness
  /** Return a TestSuite with the combined tests in all the variant
   * classes.  Useful to run common tests with variant configurations or
   * implementation classes.
   */
  public static Test variantSuites(Class[] variants) {
    TestSuite[] suites = new TestSuite[variants.length];
    for (int ix = 0; ix < variants.length; ix++) {
      final Class var = variants[ix];
      TestSuite varSuite = new TestSuite(var) {
	  public void run(TestResult result) {
	    log.debug("Variant suite: " + StringUtil.shortName(var));
	    super.run(result);
	  }};
      suites[ix] = varSuite;
    }
    TestSuite res = new TestSuite();
    for (int ix = 0; ix < suites.length; ix++) {
      res.addTest(suites[ix]);
    }
    return res;
  }

  /**
   * <p>Returns a test suite with the combined tests in all the
   * nested classes found in <code>thisClass</code> that extend
   * the <code>extendedClass</code> class.</p>
   * <p>Typically this will lead to the parameters being
   * <code>(myTestCase, myTestCase)</code> or
   * <code>(myTestCase, myTesterClass)</code>.</p>
   * @param thisClass     The class whose variant nested classes are
   *                      being extracted by reflection.
   * @param extendedClass The class that variant classes have to extend
   *                      in order to be extracted by reflection.
   * @return A test suite incorporating the tests of all the extracted
   *         nested classes.
   * @see #variantSuites(Class[])
   */
  public static Test variantSuites(Class thisClass, Class extendedClass) {
    ArrayList list = new ArrayList();
    for (Iterator iter = new ObjectArrayIterator(thisClass.getDeclaredClasses()) ;
         iter.hasNext() ; ) {
      Class cla = (Class)iter.next();
      if (extendedClass.isAssignableFrom(cla)) {
        list.add(cla);
      }
    }

    Class[] classes = new Class[list.size()];
    list.toArray(classes);
    return variantSuites(classes);
  }

  /**
   * <p>Returns a test suite with the combined tests in all the
   * nested classes found in <code>thisClass</code> that extend it.
   * <p>This is a convenience call for
   * <code>variantSuites(thisClass, thisClass)</code>.
   * @param thisClass     The class whose variant nested subclasses are
   *                      being extracted by reflection.
   * @return A test suite incorporating the tests of all the extracted
   *         nested subclasses.
   * @see #variantSuites(Class, Class)
   */
  public static Test variantSuites(Class thisClass) {
    return variantSuites(thisClass, thisClass);
  }

  // assertSuccessRate harness
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
	  // log the name of the test case (testFoo() method)
	  log.debug("Testcase " + getName());
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
   * Asserts that two objects are equal, but not the same object
   * @param expected the expected value
   * @param actual the actual value
   */
  static public void assertEqualsNotSame(Object expected, Object actual) {
    assertEqualsNotSame(null, expected, actual);
  }

  /**
   * Asserts that two objects are equal, but not the same object
   * @param message the message to give on failure
   * @param expected the expected value
   * @param actual the actual value
   */
  static public void assertEqualsNotSame(String message,
					 Object expected, Object actual) {
    if (expected == actual) {
      failSame(message);
    }
    if (expected.equals(actual)) {
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
  static public void assertEquals(Map expected, Map actual) {
    assertEquals(null, expected, actual);
  }

  /**
   * Asserts that two Maps are equal (contain the same mappings).
   * If they are not an AssertionFailedError is thrown.
   * @param message the message to give on failure
   * @param expected the expected value
   * @param actual the actual value
   */
  static public void assertEquals(String message, Map expected, Map actual) {
    if (expected == null && actual == null) {
      return;
    }
    if (expected != null && expected.equals(actual)) {
      return;
    }
    failNotEquals(message, expected, actual);
  }

  /**
   * Asserts that an int is positive
   */
  static public void assertPositive(int value) {
    assertPositive(null, value);
  }

  static public void assertPositive(String msg, int value) {
    StringBuffer sb = new StringBuffer();
    if (msg != null) {
      sb.append(msg);
      sb.append(" ");
    }
    sb.append("Expected a positive value but got ");
    sb.append(value);
    assertTrue(sb.toString(), value>0);
  }

  /**
   * Asserts that an int is negative
   */
  static public void assertNegative(int value) {
    assertNegative(null, value);
  }

  static public void assertNegative(String msg, int value) {
    StringBuffer sb = new StringBuffer();
    if (msg != null) {
      sb.append(msg);
      sb.append(" ");
    }
    sb.append("Expected a negative value but got ");
    sb.append(value);
    assertTrue(sb.toString(), value<0);
  }

  /**
   * Asserts that c1.compareTo(c2) > 0 and c2.compareTo(c1) < 0
   */
  static public void assertCompareIsGreaterThan(Comparable c1, Comparable c2) {
    assertCompareIsGreaterThan(null, c1, c2);
  }

  /**
   * Asserts that c1.compareTo(c2) > 0 and c2.compareTo(c1) < 0
   */
  static public void assertCompareIsGreaterThan(String msg,
						Comparable c1, Comparable c2) {
    int comp = c1.compareTo(c2);
    int revComp = c2.compareTo(c1);

    if (comp > 0 && revComp < 0) {
      return; //as asserted
    }

    if (comp == 0 && revComp == 0) {
      if (msg == null) {
	msg = c1 + " is equal to " + c2;
      } else {
	msg += " (equal)";
      }
    } else if (comp * revComp >= 0) {
      //one should be positive and the other neg
      if (msg != null) {
	msg =
	  "Inconsistent comparison\n"+
	  "Forward comparison returns "+comp+"\n"+
	  "While reverse comparison returns "+revComp+"\n";
      } else {
	msg += " (inconsistent)";
      }
    } else { //opposite of what we expected
      if (msg != null) {
	msg = "First comparable ("+c1+") is less than second ("+c2+")";
      }
    }
    fail(msg);
  }

  /**
   * Asserts that c1.compareTo(c2) == 0 and c2.compareTo(c1) == 0
   */
  static public void assertCompareIsEqualTo(Comparable c1, Comparable c2) {
    assertCompareIsEqualTo(null, c1, c2);
  }

  static public void assertCompareIsEqualTo(String msg,
					    Comparable c1, Comparable c2) {
    int comp = c1.compareTo(c2);
    int revComp = c2.compareTo(c1);
    if (comp == 0 && revComp == 0) {
      return;
    }

    if (comp == 0 || revComp == 0) {
      if (msg == null) {
	msg =
	  "Inconsistent results.\n"+
	  "Forward comparison is "+comp+"\n"+
	  "Reverse comparison returns "+revComp+"\n";
      } else {
	msg += " (inconsistent)";
      }
    } else {
      if (msg == null) {
	msg = "First compparable ("+c1+")"
	  +" is not equal to than second ("+c2+")";
      }
    }
    fail(msg);
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
   * Asserts that the collection is isomorphic with the. array If not
   * an AssertionFailedError is thrown.
   * @param message the message to give on failure
   * @param expected the expected value
   * @param actual the actual value
   */
  static public void assertIsomorphic(String message,
				       Collection expected, Object actual[]) {
    if (CollectionUtil.isIsomorphic(expected, actual)) {
      return;
    }
    failNotEquals(message, expected, actual);
  }

  /**
   * Asserts that the collection is isomorphic with the array. If not
   * an AssertionFailedError is thrown.
   * @param expected the expected value
   * @param actual the actual value
   */
  static public void assertIsomorphic(Collection expected, Object actual[]) {
    assertIsomorphic(null, expected, actual);
  }

  /**
   * Asserts that the collections behind the iterator are isomorphic. If
   * not an AssertionFailedError is thrown.
   * @param message the message to give on failure
   * @param expected the expected value
   * @param actual the actual value
   */
  static public void assertIsomorphic(String message,
				      Iterator expected, Iterator actual) {
    if (CollectionUtil.isIsomorphic(expected, actual)) {
      return;
    }
    failNotEquals(message, expected, actual);
  }

  /**
   * Asserts that the collections behind the iterator are isomorphic. If
   * not an AssertionFailedError is thrown.
   * @param expected the expected value
   * @param actual the actual value
   */
  static public void assertIsomorphic(Iterator expected, Iterator actual) {
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
      sb.append("Expected empty Collection, but contained ");
      sb.append(coll);
      fail(sb.toString());
    }
  }

  public static void assertEmpty(Map map) {
    assertEmpty(null, map);
  }

  public static void assertEmpty(String message, Map map) {
    if (map.size() > 0) {
      StringBuffer sb = new StringBuffer();
      if (message != null) {
	sb.append(message);
	sb.append(" ");
      }
      sb.append("Expected empty Map, but contained ");
      sb.append(map);
      fail(sb.toString());
    }
  }

  public static void assertNotEmpty(Collection coll) {
    assertNotEmpty(null, coll);
  }

  public static void assertNotEmpty(String message, Collection coll) {
    if (coll.size() == 0) {
      StringBuffer sb = new StringBuffer();
      if (message != null) {
	sb.append(message);
	sb.append(" ");
      }
      sb.append("Expected non-empty Collection, but was empty");
      fail(sb.toString());
    }
  }

  public static void assertContainsAll(Collection coll,
                                             Object[] elements) {
    for (int i = 0; i < elements.length; i++) {
      assertContains(coll, elements[i]);
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

  public static void assertClass(Class expClass, Object obj) {
    assertClass(null, expClass, obj);
  }

  public static void assertClass(String msg, Class expClass, Object obj) {
    if (! expClass.isInstance(obj)) {
      StringBuffer sb = new StringBuffer();
      if (msg != null) {
	sb.append(msg);
	sb.append(" ");
      }
      sb.append(obj);
      if (obj != null) {
	sb.append(" (a ");
	sb.append(obj.getClass().getName());
	sb.append(")");
      }
      sb.append(" is not a ");
      sb.append(expClass.getName());
      fail(sb.toString());
    }
  }

  public static void assertNotClass(Class expClass, Object obj) {
    assertNotClass(null, expClass, obj);
  }

  public static void assertNotClass(String msg, Class expClass, Object obj) {
    if (expClass.isInstance(obj)) {
      StringBuffer sb = new StringBuffer();
      if (msg != null) {
	sb.append(msg);
	sb.append(" ");
      }
      sb.append(obj);
      sb.append(" is of class ");
      sb.append(expClass);
      fail(sb.toString());
    }
  }

  /** Fail, and output the stack trace of the Throwable */
  protected static void fail(String message, Throwable t) {
    fail(message + ": " + StringUtil.stackTraceString(t));
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
  static protected void failNotEquals(String message,
				    Object expected, Object actual) {
    String formatted= "";
    if (message != null)
      formatted= message+" ";
    fail(formatted+"expected:<"+expected+"> but was:<"+actual+">");
  }

  static protected void failNotEquals(String message,
				    int[] expected, int[] actual) {
    String formatted= "";
    if (message != null)
      formatted= message+" ";
    fail(formatted+"expected:<"+arrayString(expected)+
	 "> but was:<"+arrayString(actual)+">");
  }

  static protected void failSame(String message) {
    String formatted= "";
    if (message != null)
      formatted= message+" ";
    fail(formatted+"expected not same");
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

  static protected String arrayString(Object[] a) {
    return StringUtil.separatedString(a, ", ");
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
    assertSameElements(null, expected, actual);
  }

  /**
   * Asserts that two collections have all the same elements of the same
   * cardinality; tries to give useful output if it fails
   */
  public static void assertSameElements(String message,
					Collection expected,
					Collection actual) {
    if (org.apache.commons.collections.
	CollectionUtils.isEqualCollection(expected, actual)) {
      return;
    }
    String formatted= "";
    if (message != null) {
      formatted= message+" ";
    }
    fail(formatted+"expected same elements:<"+expected+"> " +
	 "but was:<"+actual+">");
  }

  /**
   * Asserts that two collections have all the same elements of the same
   * cardinality; tries to give useful output if it fails
   */
  public static void assertSameElements(Object[] expected,
					Collection actual) {
    assertSameElements(null, expected, actual);
  }

  /**
   * Asserts that two collections have all the same elements of the same
   * cardinality; tries to give useful output if it fails
   */
  public static void assertSameElements(String message,
					Object[] expected,
					Collection actual) {

    if (org.apache.commons.collections.
	CollectionUtils.isEqualCollection(ListUtil.fromArray(expected),
					  actual)) {
      return;
    }
    String formatted= "";
    if (message != null) {
      formatted= message+" ";
    }
    fail(formatted+"expected same elements:<"+arrayString(expected)+"> " +
	 "but was:<"+actual+">");
  }

  /**
   * Asserts that the collection contains no duplicate elements
   */
  public static void assertNoDuplicates(Collection c) {
    assertNoDuplicates("Duplicates found", c);
  }

  /**
   * Asserts that the collection contains no duplicate elements
   */
  public static void assertNoDuplicates(String message, Collection c) {
    if (c.size() != SetUtil.theSet(c).size()) {
      fail(message + ": "  + c);
    }
  }

  /**
   * Asserts that a string matches the content of a reader
   */
  public static void assertReaderMatchesString(String expected, Reader reader)
      throws IOException{
    int len = Math.max(1, expected.length() * 2);
    char[] ca = new char[len];
    StringBuffer actual = new StringBuffer(expected.length());

    int n;
    while ((n = reader.read(ca)) != -1) {
      actual.append(ca, 0, n);
    }
    assertEquals(expected, actual.toString());
  }

  /**
   * Asserts that a string matches the content of a reader read using the
   * specified buffer size.
   */
  public static void assertReaderMatchesString(String expected, Reader reader,
					       int bufsize)
      throws IOException {
    char[] ca = new char[bufsize];
    StringBuffer actual = new StringBuffer(expected.length());

    int n;
    while ((n = reader.read(ca)) != -1) {
      actual.append(ca, 0, n);
    }
    assertEquals("With buffer size " + bufsize + ",",
		 expected, actual.toString());
  }

  /**
   * Asserts that a string matches the content of a reader read using
   * successive offsets of length chunkLen into a larger buffer,
   */
  public static void assertOffsetReaderMatchesString(String expected,
						     Reader reader,
						     int chunkLen)
      throws IOException {
    char[] ca = new char[Math.max(1, expected.length() * 4)];
    int off = 0;
    int n;
    while ((n = reader.read(ca, off, Math.min(chunkLen, ca.length - off)))
	   != -1) {
      off += n;
    }
    StringBuffer actual = new StringBuffer(off);
    actual.append(ca, 0, off);
    assertEquals("With chunk size " + chunkLen + ",",
		 expected, actual.toString());
  }

  /**
   * Asserts that a string matches the content of a reader read with character
   * at a time read().  Should be integrated into tests because it
   * possibly causes different behavior in the reader under test.
   */
  public static void assertReaderMatchesStringSlow(String expected,
						   Reader reader)
      throws IOException {
    StringBuffer actual = new StringBuffer(expected.length());
    int kar;
    while ((kar = reader.read()) != -1) {
      actual.append((char)kar);
    }
    assertEquals("With single char read(),", expected, actual.toString());
  }

  /**
   * Asserts that a string matches the content of an InputStream
   */
  public static void assertInputStreamMatchesString(String expected,
						    InputStream in)
      throws IOException {
    assertInputStreamMatchesString(expected, in, Constants.DEFAULT_ENCODING);
  }

  /**
   * Asserts that a string matches the content of an InputStream
   */
  public static void assertInputStreamMatchesString(String expected,
						    InputStream in,
						    String encoding)
      throws IOException {
    Reader rdr = new InputStreamReader(in, encoding);
    assertReaderMatchesString(expected, rdr);
  }

  /**
   * Asserts that a string matches the content of a reader read using the
   * specified buffer size.
   */
  public static void assertInputStreamMatchesString(String expected,
						    InputStream in,
						    int bufsize)
      throws IOException {
    Reader rdr = new InputStreamReader(in, Constants.DEFAULT_ENCODING);
    assertReaderMatchesString(expected, rdr, bufsize);
  }

  /** Convenience method to compile an RE */
  protected static Pattern compileRe(String re) {
    return RegexpUtil.uncheckedCompile(re);
  }

  /** Convenience method to match an RE */
  protected static boolean isMatchRe(String s, Pattern re) {
    return RegexpUtil.getMatcher().contains(s, re);
  }

  /** Convenience method to compile and match an RE */
  protected static boolean isMatchRe(String s, String re) {
    return isMatchRe(s, RegexpUtil.uncheckedCompile(re));
  }

  /** Convenience method to match an RE */
  protected static boolean isMatchRe(String s, java.util.regex.Pattern pat) {
    return pat.matcher(s).find();
  }

  public static void assertSameBytes(String message,
                                     InputStream expected,
                                     InputStream actual)
      throws IOException {
    if (!IOUtils.contentEquals(expected, actual)) {
      if (message == null) {
        fail();
      }
      else {
        fail(message);
      }
    }
  }
  
  public static void assertSameBytes(InputStream expected,
                                     InputStream actual)
      throws IOException {
    assertSameBytes(null, expected, actual);
  }
  
  public static void assertSameCharacters(String message,
                                          Reader expected,
                                          Reader actual)
      throws IOException {
    if (!IOUtils.contentEquals(expected, actual)) {
      if (message == null) {
        fail();
      }
      else {
        fail(message);
      }
    }
  }
  
  public static void assertSameCharacters(Reader expected,
                                          Reader actual)
      throws IOException {
    assertSameCharacters(null, expected, actual);
  }
  
  static String reFailMsg(String msg, java.util.regex.Pattern pattern,
			  String string) {
    return reFailMsg(msg, pattern.pattern(), string);
  }

  static String reFailMsg(String msg, Pattern pattern, String string) {
    return reFailMsg(msg, pattern.getPattern(), string);
  }

  static String reFailMsg(String msg, String regexp, String string) {
    String failmsg = 
      "No match for " + regexp + " in \"" + string + "\"";
    if (msg != null) {
      failmsg = msg + ": " + failmsg;
    }
    return failmsg;
  }

  /**
   * Asserts that a string matches a regular expression.  The match is
   * unanchored; use "^...$" to ensure that the entire string is matched.
   */
  public static void assertMatchesRE(String regexp, String string) {
    assertMatchesRE(null, regexp, string);
  }

  /**
   * Asserts that a string matches a regular expression.  The match is
   * unanchored; use "^...$" to ensure that the entire string is matched.
   */
  public static void assertMatchesRE(String msg,
				     String regexp, String string) {
    assertTrue(reFailMsg(msg, regexp, string), isMatchRe(string, regexp));
  }

  /**
   * Asserts that a string matches a regular expression.  The match is
   * unanchored; use "^...$" to ensure that the entire string is matched.
   */
  public static void assertMatchesRE(Pattern regexp, String string) {
    assertMatchesRE(null, regexp, string);
  }

  /**
   * Asserts that a string matches a regular expression.  The match is
   * unanchored; use "^...$" to ensure that the entire string is matched.
   */
  public static void assertMatchesRE(String msg,
				     Pattern regexp, String string) {
    assertTrue(reFailMsg(msg, regexp, string), isMatchRe(string, regexp));
  }

  /**
   * Asserts that a string matches a regular expression.  The match is
   * unanchored; use "^...$" to ensure that the entire string is matched.
   */
  public static void assertMatchesRE(java.util.regex.Pattern regexp,
				     String string) {
    assertMatchesRE(null, regexp, string);
  }

  /**
   * Asserts that a string matches a regular expression.  The match is
   * unanchored; use "^...$" to ensure that the entire string is matched.
   */
  public static void assertMatchesRE(String msg,
				     java.util.regex.Pattern regexp,
				     String string) {
    assertTrue(reFailMsg(msg, regexp, string), isMatchRe(string, regexp));
  }

  /**
   * Asserts that a string does not match a regular expression
   */
  public static void assertNotMatchesRE(String regexp, String string) {
    assertNotMatchesRE(null, regexp, string);
  }

  /**
   * Asserts that a string does not match a regular expression
   */
  public static void assertNotMatchesRE(String msg,
					String regexp, String string) {
    if (msg == null) {
      msg = "String \"" + string + "\" should not match RE: " + regexp;
    }
    assertFalse(msg, isMatchRe(string, regexp));
  }

  /**
   * Asserts that a string does not match a regular expression
   */
  public static void assertNotMatchesRE(Pattern regexp, String string) {
    assertNotMatchesRE(null, regexp, string);
  }

  /**
   * Asserts that a string does not match a regular expression
   */
  public static void assertNotMatchesRE(String msg,
					Pattern regexp, String string) {
    if (msg == null) {
      msg = "String \"" + string + "\" should not match RE: " +
	regexp.getPattern();
    }
    assertFalse(msg, isMatchRe(string, regexp));
  }

  /**
   * Asserts that a string does not match a regular expression
   */
  public static void assertNotMatchesRE(java.util.regex.Pattern regexp,
					String string) {
    assertNotMatchesRE(null, regexp, string);
  }

  /**
   * Asserts that a string does not match a regular expression
   */
  public static void assertNotMatchesRE(String msg,
					java.util.regex.Pattern regexp,
					String string) {
    if (msg == null) {
      msg = "String \"" + string + "\" should not match RE: " +
	regexp.pattern();
    }
    assertFalse(msg, isMatchRe(string, regexp));
  }

  /**
   * Asserts that a collection of strings matches a collection of regular
   * expressions.  The matches are unanchored; use "^...$" to ensure that
   * the entire string is matched.
   */
  public static void assertMatchesREs(String[] regexps,
				      Collection<String> strings) {
    assertMatchesREs(null, regexps, strings);
  }

  /**
   * Asserts that a collection of strings matches a collection of regular
   * expressions.  The matches are unanchored; use "^...$" to ensure that
   * the entire string is matched.
   */
  public static void assertMatchesREs(String msg,
				      String[] regexps,
				      Collection<String> strings) {
    int ix = 0;
    for (String str : strings) {
      assertMatchesRE(msg, regexps[ix++], str);
    }
    assertEquals("length", regexps.length, strings.size());
  }

  /** Assert that a collection cannot be modified, <i>ie</i>, that all of
   * the following methods, plus the collection's iterator().remove()
   * method, throw UnsupportedOperationException: add(), addAll(), clear(),
   * remove(), removeAll(), retainAll() */

  public static void assertUnmodifiable(Collection coll) {
    List list = ListUtil.list("bar");
    try {
      coll.add("foo");
      fail("add() didn't throw");
    } catch (UnsupportedOperationException e) {
    }
    try {
      coll.addAll(list);
      fail("addAll() didn't throw");
    } catch (UnsupportedOperationException e) {
    }
    try {
      coll.clear();
      fail("clear() didn't throw");
    } catch (UnsupportedOperationException e) {
    }
    try {
      coll.remove("foo");
      fail("remove() didn't throw");
    } catch (UnsupportedOperationException e) {
    }
    try {
      coll.removeAll(list);
      fail("removeAll() didn't throw");
    } catch (UnsupportedOperationException e) {
    }
    try {
      coll.retainAll(list);
      fail("retainAll() didn't throw");
    } catch (UnsupportedOperationException e) {
    }
    Iterator iter = coll.iterator();
    iter.next();
    try {
      iter.remove();
      fail("iterator().remove() didn't throw");
    } catch (UnsupportedOperationException e) {
    }
  }

  /** Abstraction to do something in another thread, after a delay,
   * unless cancelled.  If the scheduled activity is still pending when the
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
    private boolean threadDump = false;

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
	    if (threadDump) {
	      try {
		PlatformUtil.getInstance().threadDump(true);
	      } catch (Exception e) {
	      }
	    }
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

    /** Get a thread dump before triggering the event */
    public void setThreadDump() {
      threadDump = true;
    }

  }
  /** Interrupter interrupts a thread in a while */
  public class Interrupter extends DoLater {
    private Thread thread;

    Interrupter(long waitMs, Thread thread) {
      super(waitMs);
      setPriority(thread.getPriority() + 1);
      this.thread = thread;
    }

    /** Interrupt the thread */
    protected void doit() {
      log.debug("Interrupting");
      thread.interrupt();
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

  /**
   * Close the socket after a timeout
   * @param inMs interval to wait before interrupting
   * @param sock the Socket to close
   * @return a SockAbort
   */
  public SockAbort abortIn(long inMs, Socket sock) {
    SockAbort sa = new SockAbort(inMs, sock);
    if (Boolean.getBoolean("org.lockss.test.threadDump")) {
      sa.setThreadDump();
    }
    sa.start();
    return sa;
  }

  /**
   * Close the socket after a timeout
   * @param inMs interval to wait before interrupting
   * @param sock the ServerSocket to close
   * @return a SockAbort
   */
  public SockAbort abortIn(long inMs, ServerSocket sock) {
    SockAbort sa = new SockAbort(inMs, sock);
    if (Boolean.getBoolean("org.lockss.test.threadDump")) {
      sa.setThreadDump();
    }
    sa.start();
    return sa;
  }

  /** SockAbort aborts a socket by closing it
   */
  public class SockAbort extends DoLater {
    Socket sock;
    ServerSocket servsock;

    SockAbort(long waitMs, Socket sock) {
      super(waitMs);
      this.sock = sock;
    }

    SockAbort(long waitMs, ServerSocket servsock) {
      super(waitMs);
      this.servsock = servsock;
    }

    protected void doit() {
      try {
	if (sock != null) {
	  log.debug("Closing sock");
	  sock.close();
	}
      } catch (IOException e) {
	log.warning("sock", e);
      }
      try {
	if (servsock != null) {
	  log.debug("Closing servsock");
	  servsock.close();
	}
      } catch (IOException e) {
	log.warning("servsock", e);
      }
    }
  }

  // If enabled, record an error in the result for every exception thrown
  // from a TimerQueue callback.

  private boolean errorIfTimerThrows = true;

  /** If flg is true, exceptions thrown from TimerQueue callbacks will
   * cause test failures, <i>iff they occur while the test is still
   * running</i>. */
  public void setErrorIfTimerThrows(boolean flg) {
    errorIfTimerThrows = flg;
  }

  class ErrorRecordingTimerQueue extends TimerQueue {
    protected void doNotify0(Request req) {
      try {
	super.doNotify0(req);
      } catch (Exception e) {
	if (errorIfTimerThrows) {
	  result.addError(LockssTestCase.this, e);
	}
      }
    }
  }

  /** A RandomManager that doesn't consume kernel randomness.  Some tests
   * create lots of instances; if each one generates its own seed the
   * kernel's entropy pool (/dev/random) gets exhausted and the tests block
   * while more is collected.  This can take several minutes on an
   * otherwise idle machine. */

  // Successive seeds are obtained from a regular LockssRandom.  Using a
  // constant seed triggers an NPE in SSLSessionContextImpl; if every
  // SecureRandom instance produces the same sequence of bytes, duplicate
  // session keys result, causing collisions in the sessionCache.

  public static class TestingRandomManager extends RandomManager {
    LockssRandom lrand = new LockssRandom();
    byte[] rseed = new byte[4];

    @Override
    protected void initRandom(SecureRandom rng) {
      lrand.nextBytes(rseed);
      rng.setSeed(rseed);
    }
  }

  /**
   * Provides a database manager already started that uses a database snapshot
   * previously created.
   * 
   * @param tempDirPath
   *          A String with the path to the temporary directory.
   * @return a DbManager already started that uses a database snapshot
   *         previously created.
   */
  protected DbManager getTestDbManager(String tempDirPath) {
    // Set the database log.
    System.setProperty("derby.stream.error.file",
	new File(tempDirPath, "derby.log").getAbsolutePath());

    try {
      // Extract the database from the zip file.
      ZipUtil.unzip(getResourceAsStream(TEST_DB_FILE_SPEC, false),
	  new File(tempDirPath, "db"));
    } catch (Exception e) {
      log.debug("Unable to unzip database files from file " + TEST_DB_FILE_SPEC,
	  e);
    }

    // Create the database manager skipping the asynchronous updates.
    DbManager dbManager = new DbManager(true);
    mockDaemon.setDbManager(dbManager);
    dbManager.initService(mockDaemon);
    dbManager.startService();

    return dbManager;
  }
}
