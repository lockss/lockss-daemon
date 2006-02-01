/*
 * $Id: DebugUtils.java,v 1.11 2006-02-01 05:05:43 tlipkis Exp $
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

/** Debugging utilities */
public class DebugUtils {
  protected static Logger log = Logger.getLogger("DebugUtils");

  static DebugUtils instance;

  /** Return the singleton DebugUtils instance */
  // no harm if happens in two threads, so not synchronized
  public static DebugUtils getInstance() {
    if (instance == null) {
      String os = System.getProperty("os.name");
      if ("linux".equalsIgnoreCase(os)) {
	instance = new Linux();
      }
      if ("openbsd".equalsIgnoreCase(os)) {
	instance = new OpenBSD();
      }
      if (instance == null) {
	log.warning("No OS-specific DebugUtils for '" + os + "'");
	instance = new DebugUtils();
      }
    }
    return instance;
  }


  /** Return the PID of the executing process, if possible.
   * @return PID of executing process, which is not necessarily the top
   * java process.
   * @throws UnsupportedException if unsupported on current platform
   */
  public int getPid() throws UnsupportedException {
    throw new UnsupportedException("No OS-independent way to find PID");
  }

  /** Return the PID of the top process of this JVM, if possible.
   * @return PID of top java process, suitable for killing or sending
   * SIGQUIT, etc.
   * @throws UnsupportedException if unsupported on current platform
   */
  public int getMainPid() throws UnsupportedException {
    throw new UnsupportedException("No OS-independent way to find main PID");
  }

  /** Request a thread dump of this JVM.  Dump is output to JVM's stderr,
   * which may not be the same as System.err.  If unsupported on this
   * platform, logs an info message.
   * @param wait if true will attempt to wait until dump is complete before
   * returning
   */
  public void threadDump(boolean wait) {
    int pid;
    try {
      pid = getMainPid();
    } catch (UnsupportedException e) {
      log.info("Thread dump requested, not supported in this environment", e);
      return;
    }
    // thread dump is more likely to appear on System.err than
    // wherever the current log is.
    System.err.println("Thread dump at " + new Date());
    String cmd = "kill -QUIT " + pid;
    try {
      Process p = rt().exec(cmd);
//     InputStream is = p.getInputStream();
//     org.mortbay.util.IO.copy(is, System.out);
      p.waitFor();
      if (wait) {
	try {
	  Thread.sleep(Constants.SECOND);
	} catch (InterruptedException ignore) {}
      }
    } catch (IOException e) {
      log.error("Couldn't exec '" + cmd + "'", e);
    } catch (InterruptedException e) {
      log.error("waitFor()", e);
    }
  }

  static Runtime rt() {
    return Runtime.getRuntime();
  }

  /** Linux implementation of platform-specific code */
  public static class Linux extends DebugUtils {
    // offsets into /proc/<n>/stat
    static final int STAT_OFFSET_PID = 0;
    static final int STAT_OFFSET_PPID = 3;
    static final int STAT_OFFSET_FLAGS = 8;
    // flag bits
    static final int PF_FORKNOEXEC = 0x40;	// forked but didn't exec

    /** Get PID of current process */
    public int getPid() throws UnsupportedException {
      return getProcPid();
    }

    /** Get PID of main java process */
    public int getMainPid() throws UnsupportedException {
      String pid;
      String ppid = "self";
      int flags;
      do {
	Vector v = getProcStats(ppid);
	pid = (String)v.elementAt(STAT_OFFSET_PID);
	ppid = (String)v.elementAt(STAT_OFFSET_PPID);
	flags = getInt(v, STAT_OFFSET_FLAGS);
//  	log.debug("getMainPid: pid = " + pid + ", ppid = " + ppid +
//  		  ", flags = 0x" + Integer.toHexString(flags));
      } while ((flags & PF_FORKNOEXEC) != 0);
      return Integer.parseInt(pid);
    }

    /** Get PID from linux /proc/self/stat */
    private int getProcPid() throws UnsupportedException {
      Vector v = getMyProcStats();
      String pid = (String)v.elementAt(STAT_OFFSET_PID);
      return Integer.parseInt(pid);
    }

    // return int from string in vector
    private int getInt(Vector v, int pos) {
      String s = (String)v.elementAt(pos);
      return Integer.parseInt(s);
    }

    /** Get stat vector of this java process from /proc/self/stat .
   * Read the stat file with java code so the executing process (self) is
   * java. */
    Vector getMyProcStats() throws UnsupportedException {
      return getProcStats("self");
    }

    /** Get stat vector for specified process from /proc/<n>/stat .
     * @param pid the process for which to get stats, or "self"
     * @return vector of strings of values in stat file
     */
    public Vector getProcStats(String pid) throws UnsupportedException {
      String filename = "/proc/" + pid + "/stat";
      try {
	Reader r = new FileReader(new File(filename));
	String s = StringUtil.fromReader(r);
	Vector v = StringUtil.breakAt(s, ' ');
	return v;
      } catch (FileNotFoundException e) {
	throw new UnsupportedException("Can't open " + filename, e);
      } catch (IOException e) {
	throw new UnsupportedException("Error reading " + filename, e);
      }
    }

    /** Get vector of stat vectors for all processes from /proc/<n>/stat .
     * Read the stat files with a shell with java code so the executing
     * process (self) is java. */
    Vector getAllProcStats() {
      String[] cmd = {"sh",  "-c",  "cat /proc/[0-9]*/stat"};
      Process p;
      try {
	p = rt().exec(cmd);
	//        p.waitFor();
      } catch (IOException e) {
	log.error("Couldn't exec '" + cmd + "'", e);
	return null;
	//      } catch (InterruptedException e) {
	//        log.error("waitFor()", e);
	//        return null;
      }
      Reader r =
	new InputStreamReader(new BufferedInputStream(p.getInputStream()));
      String s;
      try {
	s = StringUtil.fromReader(r);
      } catch (IOException e) {
	log.error("Couldn't read from '" + cmd + "'", e);
	return null;
      }
      System.out.println(s);
      System.out.println(StringUtil.breakAt(s, '\n').size());
      return StringUtil.breakAt(s, '\n');
    }
  }

  /** OpenBSD implementation of platform-specific code */
  public static class OpenBSD extends DebugUtils {
    // offsets into /proc/<n>/status
    static final int STAT_OFFSET_CMD = 1;
    static final int STAT_OFFSET_PID = 1;
    static final int STAT_OFFSET_PPID = 2;

    /** Get PID of current process */
    public int getPid() throws UnsupportedException {
      return getProcPid();
    }

    /** Get PID of main java process */
    public int getMainPid() throws UnsupportedException {
      // tk - not sure how to find top process on OpenBSD.
      // This is right for green threads only.
      return getProcPid();
    }

    /** Get PID from OpenBSD /proc/curproc/status */
    private int getProcPid() throws UnsupportedException {
      Vector v = getMyProcStats();
      String pid = (String)v.elementAt(STAT_OFFSET_PID);
      return Integer.parseInt(pid);
    }

    /** Get stat vector of this java process from /proc/curproc.status .
   * Read the stat file with java code so the executing process (self) is
   * java. */
    Vector getMyProcStats() throws UnsupportedException {
      return getProcStats("curproc");
    }

    /** Get stat vector for specified process from /proc/<n>/status .
     * @param pid the process for which to get stats, or "self"
     * @return vector of strings of values in stat file
     */
    public Vector getProcStats(String pid) throws UnsupportedException {
      String filename = "/proc/" + pid + "/status";
      try {
	Reader r = new FileReader(new File(filename));
	String s = StringUtil.fromReader(r);
	Vector v = StringUtil.breakAt(s, ' ');
	return v;
      } catch (FileNotFoundException e) {
	throw new UnsupportedException("Can't open " + filename, e);
      } catch (IOException e) {
	throw new UnsupportedException("Error reading " + filename, e);
      }
    }
  }

// old, unused
//   static {
//     System.loadLibrary("DebugUtils");
//   }
//   public static native int getPid();

  /** Exception thrown if no implementation is available for the current
   * platform, or a platform-dependent error occurs.
   * In the case of an error, the original exception is available. */
  public class UnsupportedException extends Exception {
    Throwable e;

    public UnsupportedException(String msg) {
      super(msg);
    }

    public UnsupportedException(String msg, Throwable e) {
      super(msg);
      this.e = e;
    }

    /** Return the nested Throwable */
    public Throwable getError() {
      return e;
    }
  }
}
