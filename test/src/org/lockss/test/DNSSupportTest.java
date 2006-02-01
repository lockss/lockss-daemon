/*
 * $Id: DNSSupportTest.java,v 1.5 2006-02-01 05:05:43 tlipkis Exp $
 */

/*

Copyright (c) 2004 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.*;
import java.net.*;
import org.lockss.util.*;
import org.lockss.test.*;

/**
 */
public class DNSSupportTest extends LockssTestCase {

  // Tests that exercise multiple threads doing things that cause
  // DNS lookups.  The reason for inculding these tests is that the
  // various JVMs we have used have had various problems in this
  // area.
  // Test parameters
  static int callTimeSlice = 10000;
  // NB - gethostbyname takes 150 seconds to time out an unresponsive
  // name server,  so waitTimeSlice * numWaits needs to be a good deal
  // bigger than 150000
  static int waitTimeSlice = 30000;
  static int numWaits = 10;
  static int numThreads = 5;
  static int delayMax = 0;
  static boolean enableDNS = true;  // Set false to prevent actual DNS calls
  static boolean enablePrint = false;
  static boolean singleThreadDNS = false;
  static boolean useRealAddresses = true;
  static {
    enablePrint =
      System.getProperty("org.lockss.devtools.testdnssupport.enableprint", "false").equals("true");
    enableDNS =
      System.getProperty("org.lockss.devtools.testdnssupport.enabledns", "true").equals("true");
    singleThreadDNS =
      System.getProperty("java.net.inetaddress.singlethreaddns", "false").equals("true");
    useRealAddresses =
      System.getProperty("java.net.inetaddress.userealaddresses", "true").equals("true");
    try {
      delayMax =
        Integer.parseInt(System.getProperty("org.lockss.devtools.testdnssupport.delaymax", "200"));
    } catch (NumberFormatException e) {
      // No action intended
    }
    try {
      callTimeSlice =
        Integer.parseInt(System.getProperty("org.lockss.devtools.testdnssupport.calltimeslice", "10000"));
    } catch (NumberFormatException e) {
      // No action intended
    }
    try {
      numThreads =
        Integer.parseInt(System.getProperty("org.lockss.devtools.testdnssupport.numthreads", "5"));
    } catch (NumberFormatException e) {
      // No action intended
    }
    try {
      numWaits =
        Integer.parseInt(System.getProperty("org.lockss.devtools.testdnssupport.numwaits", "10"));
    } catch (NumberFormatException e) {
      // No action intended
    }
    try {
       waitTimeSlice =
        Integer.parseInt(System.getProperty("org.lockss.devtools.testdnssupport.waittimeslice", "300000"));
    } catch (NumberFormatException e) {
      // No action intended
    }
  }

  static final int RANDOM_CHOICE = 0;
  static final int GET_ALL_BY_NAME = 1;
  static final int GET_BY_NAME = 2;
  static final int GET_LOCAL_HOST = 3;
  static final int GET_HOST_NAME = 4;
  static final int NUM_CHOICES = 5;
  static Random rand = new Random(System.currentTimeMillis());
  static boolean anyInCall = false;
  static String[] names = {
    "www.lockss.org",
    "beta1.lockss.org",
    "beta2.lockss.org",
    "beta3.lockss.org",
    "beta4.lockss.org",
    "beta5.lockss.org",
    "beta6.lockss.org",
    "dev1.lockss.org",
    "dev2.lockss.org",
    "dev3.lockss.org",
    "dev4.lockss.org",
    "128.101.98.17",
    "128.103.151.241",
    "128.104.61.100",
    "128.118.88.75",
    "128.143.166.238",
    "128.163.226.19",
    "128.173.125.34",
    "128.187.233.114",
    "128.2.20.112",
    "128.210.126.253",
    "128.220.8.19",
    "128.227.228.239",
    "128.250.49.71",
    "128.253.51.42",
    "128.255.53.85",
    "128.32.238.64",
    "128.59.153.87",
    "129.133.36.186",
    "129.170.116.62",
    "129.174.55.28",
    "129.177.69.19",
    "129.186.11.214",
    "129.215.146.174",
  "129.22.96.62",
  "129.59.149.8",
  "129.69.235.7",
  "129.79.35.17",
  "130.132.21.8",
  "130.209.6.17",
  "130.233.216.6",
  "130.91.117.146",
  "131.111.163.151",
  "131.111.163.154",
  "131.193.154.81",
  "131.252.180.30",
  "132.246.153.17",
  "134.174.151.131",
  "134.197.60.201",
  "134.76.163.110",
  "137.120.22.129",
  "137.138.124.185",
  "139.80.59.42",
  "140.147.242.244",
  "141.161.91.7",
  "141.211.43.136",
  "142.150.192.44",
  "143.89.104.80",
  "145.18.84.80",
  "146.48.85.108",
  "146.6.140.9",
  "147.134.201.39",
  "148.137.188.242",
  "150.199.21.213",
  "152.1.190.9",
  "155.198.4.17",
  "157.142.66.26",
  "159.226.100.36",
  "160.36.180.55",
  "160.36.190.176",
  "160.36.192.220",
  "170.140.208.253",
  "170.140.208.43",
  "171.66.236.34",
  "171.66.236.35",
  "171.66.236.36",
  "171.66.236.38",
  "171.66.236.39",
  "171.66.236.51",
  "171.66.236.52",
  "171.66.236.53",
  "171.66.236.54",
  "171.66.236.55",
  "171.66.236.56",
  "172.17.8.122",
  "18.51.0.202",
  "192.16.197.238",
  "192.168.0.27",
  "193.136.149.42",
  "195.224.176.122",
  "199.75.75.172",
  "200.6.42.3",
  "204.121.6.38",
  "216.143.112.42",
  "35.8.222.234",
  };

  int callsStarted = 0;
  int callsCompleted = 0;
  int callsExcepted = 0;

  int randomChoice(int range) {
    return (rand.nextInt(range));
  }
  String chooseName() {
    if (useRealAddresses) {
      int i = randomChoice(names.length);
      return names[i];
    } else {
      int i = randomChoice(1000000);
      return (i + ".test.pss.com");
    }
  }

  synchronized void startCall(String target) {
    callsStarted++;
    if (enablePrint) System.err.println(Thread.currentThread().getName() + ": call " + target +
		       " at " + (new Date()).toString());
  }

  synchronized void completeCall(String target) {
    callsCompleted++;
    if (enablePrint) System.err.println(Thread.currentThread().getName() + ": return " + target +
		       " at " + (new Date()).toString());
  }

  synchronized void exceptCall(String target) {
    callsExcepted++;
    if (enablePrint) System.err.println(Thread.currentThread().getName() + ": except " + target +
		       " at " + (new Date()).toString());
  }

  class OneThreadOfTest implements Runnable {
    // Each test runs for callTimeSlice ms with numThreads
    // threads each doing a loop which waits for [0..delayMax] ms
    // then does one of GetAllByName(), getByName() and getLocalHost()
    // on a randomly chosen one of the array of sample names.  It then
    // runs for a further waitTimeSlice ms during which no calls are issued
    // but some of the calls initiated during the first phase may return.
    // The test counts the number of calls and returns during both
    // phases.  It cleans up by terminating all the threads
    // and determines success or failure according as the number of calls
    // is or is not equal to the number of returns.

    long ts;
    int dm;
    int choice;
    boolean keepGoing = true;
    boolean inCall;
    OneThreadOfTest(long t, int d, int c) {
      ts = t;
      dm = d;
      choice = c;
      inCall = false;
    }
    public void run() {
      long startTime = System.currentTimeMillis();
      while ((System.currentTimeMillis() - startTime < ts) && keepGoing) {
	String name = chooseName();
	if (name == null || name.length() <= 0)
		fail("bad name");
	if (inCall)
		fail("inCall: " + name);
	int myChoice = ( choice == RANDOM_CHOICE ?
			 ((randomChoice(NUM_CHOICES-1)) +1 ) :
			 choice );
	startCall(name);
	switch (myChoice) {
	case RANDOM_CHOICE:
	  fail("Choice error in test");
	  break;
	case GET_ALL_BY_NAME:
	  anyInCall = inCall = true;
	  if (enableDNS) try {
	    InetAddress[] ia = InetAddress.getAllByName(name);
	  } catch (UnknownHostException e) {
	    exceptCall(name);
	  }
	  anyInCall = inCall = false;
	  break;
	case GET_BY_NAME:
	  anyInCall = inCall = true;
	  if (enableDNS) try {
	    InetAddress ia =InetAddress.getByName(name);
	  } catch (UnknownHostException e) {
	    exceptCall(name);
	  }
	  anyInCall = inCall = false;
	  break;
	case GET_LOCAL_HOST:
	  anyInCall = inCall = true;
	  if (enableDNS) try {
	    InetAddress ia = InetAddress.getLocalHost();
	  } catch (UnknownHostException e) {
	    exceptCall(name);
	  }
	  anyInCall = inCall = false;
	  break;
	case GET_HOST_NAME:
	  anyInCall = inCall = true;
	  if (enableDNS) try {
	    if (Character.isDigit(name.charAt(0))) {
		/* Its a dotted quad */
		InetAddress ia = InetAddress.getByName(name);
		String s = ia.getHostName();
	    } else {
		InetAddress ia = InetAddress.getByName(name);
		String s = ia.getHostName();
	    }
	  } catch (UnknownHostException e) {
	    exceptCall(name);
	  }
	  anyInCall = inCall = false;
	  break;
	}
	completeCall(name);
	if (dm > 0) {
	  long delay = randomChoice(dm);
	  try {
	    Thread.sleep(delay);
	  } catch (InterruptedException e) {
	    // No action intended
	  }
	}
      }
    }
    public void pleaseStop() {
      keepGoing = false;
    }
    public boolean busy() {
      return inCall;
    }
  }
  void PerformTheTest(int numThr, int what, String prefix) {
    Thread thr[] = new Thread[numThr];
    OneThreadOfTest otot[] = new OneThreadOfTest[numThr];
    long startTime = System.currentTimeMillis();
    callsStarted = 0;
    callsCompleted = 0;
    callsExcepted = 0;
    for (int i = 0; i < thr.length; i++) {
      otot[i] = new OneThreadOfTest(callTimeSlice, delayMax, what);
      thr[i] = new Thread(otot[i], prefix + i);
      thr[i].start();
    }
    try {
      Thread.sleep(callTimeSlice);
    } catch (InterruptedException e) {
      // No action intended
    }
    for (int i = 0; i < thr.length; i++) {
      otot[i].pleaseStop();
    }
    if (callsStarted <= 0) {
      fail("No calls started");
    }
    while ((callsStarted > callsCompleted) &&
	   ((System.currentTimeMillis() - startTime) <
	    (callTimeSlice + numWaits*waitTimeSlice))) {
      if (enablePrint) System.err.println("waiting for " + (callsStarted - callsCompleted) +
			 " calls in " + prefix +
			 " at " + (new Date()).toString());
      try {
	Thread.sleep(waitTimeSlice);
      } catch (InterruptedException e) {
	// No action intended
      }
    }
    if (anyInCall) {
      DebugUtils.getInstance().threadDump(true);
    }

    for (int i = 0; i < thr.length; i++) {
      thr[i].interrupt();
      thr[i] = null;
    }
    try {
      Thread.sleep(waitTimeSlice);
    } catch (InterruptedException e) {
      // No action intended
    }
    System.err.println("stop test " + prefix +
		       " at " + (new Date()).toString() + " " +
			callsCompleted + "/" + callsStarted + " (" + callsExcepted + ")");
    if (callsStarted != callsCompleted) {
      fail("calls started: " + callsStarted + " but calls completed: " +
	   callsCompleted);
    }
  }
  public void testOneThreadAndGetAllByName() {
    PerformTheTest(1, GET_ALL_BY_NAME, "OneThreadAndGetAllByName_");
  }
  public void testOneThreadAndGetByName() {
    PerformTheTest(1, GET_BY_NAME, "OneThreadAndGetByName_");
  }
  public void dontTestOneThreadAndGetLocalHost() {
    PerformTheTest(1, GET_LOCAL_HOST, "OneThreadAndGetLocalHost_");
  }
  public void dontTestOneThreadAndGetHostName() {
    PerformTheTest(1, GET_HOST_NAME, "OneThreadAndGetHostName_");
  }
  public void dontTestOneThreadAndMixtureOfCalls() {
    PerformTheTest(1, RANDOM_CHOICE, "OneThreadAndRandomChoice_");
  }
  public void testMultipleThreadsAndGetAllByName() {
    PerformTheTest(numThreads, GET_ALL_BY_NAME, "MultipleThreadsAndGetAllByName_");
  }
  public void testMultipleThreadsAndGetByName() {
    PerformTheTest(numThreads, GET_BY_NAME, "MultipleThreadsAndGetByName_");
  }
  public void dontTestMultipleThreadsAndGetLocalHost() {
    PerformTheTest(numThreads, GET_LOCAL_HOST, "MultipleThreadsAndGetLocalHost_");
  }
  public void testMultipleThreadsAndGetHostName() {
    PerformTheTest(numThreads, GET_HOST_NAME, "MultipleThreadsAndGetHostName_");
  }
  public void testMultipleThreadsAndMixtureOfCalls() {
    PerformTheTest(numThreads, RANDOM_CHOICE, "MultipleThreadsAndRandomChoice_");
  }
  public static void main(String[] argv) {
    String[] testCaseList = {DNSSupportTest.class.getName()};
    junit.textui.TestRunner.main(testCaseList);
  }
}
