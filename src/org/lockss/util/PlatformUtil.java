/*
 * $Id: PlatformUtil.java,v 1.1.2.1 2006-11-09 00:48:12 thib_gc Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;

import java.io.*;
import java.text.*;
import java.util.*;

import org.apache.commons.lang.SystemUtils;
import org.lockss.config.*;

/** Utilities to communicate with platform to get info or take action not
 * possible from Java */
public class PlatformUtil {
  protected static Logger log = Logger.getLogger("PlatformInfo");
  private static final DecimalFormat percentFmt = new DecimalFormat("0%");

  /** Should be set to allowed TCP ports, based on platform- (and group-)
   * dependent packet filters */
  public static final String PARAM_UNFILTERED_TCP_PORTS =
    Configuration.PLATFORM + "unfilteredTcpPorts";

  public static final String PARAM_UNFILTERED_UDP_PORTS =
    Configuration.PLATFORM + "unfilteredUdpPorts";

  /** Set to tmp dir appropriate for platform.  If not set, java.io.tmpdir
   * system property is used
   */
  public static final String PARAM_TMPDIR = Configuration.PLATFORM + "tmpDir";

  static PlatformUtil instance;

  /** Return the singleton PlatformInfo instance */
  // no harm if happens in two threads, so not synchronized
  public static PlatformUtil getInstance() {
    if (instance == null) {
      String os = System.getProperty("os.name");
      if ("linux".equalsIgnoreCase(os)) {
	instance = new Linux();
      }
      if ("openbsd".equalsIgnoreCase(os)) {
	instance = new OpenBSD();
      }
      if (SystemUtils.IS_OS_WINDOWS) {
        instance = new Windows();
      }
      if (instance == null) {
	log.warning("No OS-specific PlatformInfo for '" + os + "'");
	instance = new PlatformUtil();
      }
    }
    return instance;
  }

  /** Return the system temp directory, from config parameter if specified
   * else java.io.tmpdir System property
   */
  public static String getSystemTempDir() {
    Configuration config = CurrentConfig.getCurrentConfig();
    return config.get(PARAM_TMPDIR, System.getProperty("java.io.tmpdir"));
  }

  public List getUnfilteredTcpPorts() {
    Configuration config = CurrentConfig.getCurrentConfig();
    return config.getList(PARAM_UNFILTERED_TCP_PORTS);
  }

  public List getUnfilteredUdpPorts() {
    Configuration config = CurrentConfig.getCurrentConfig();
    return config.getList(PARAM_UNFILTERED_UDP_PORTS);
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

  /** Return disk usage below path, in K (du -sk) */
  public long getDiskUsage(String path) {
    String cmd = "du -k -s " + path;
    if (log.isDebug2()) log.debug2("cmd: " + cmd);
    try {
      Process p = rt().exec(cmd);
      Reader rdr =
	new InputStreamReader(new BufferedInputStream(p.getInputStream()),
			      Constants.DEFAULT_ENCODING);
      String s;
      try {
	s = StringUtil.fromReader(rdr);
	int exit = p.waitFor();
	rdr.close();
	// any unreadable dirs cause exit=1; process if got any output
      } catch (IOException e) {
	log.error("Couldn't read from '" + cmd + "'", e);
	return -1;
      }
      List lines = StringUtil.breakAt(s, '\n');
      if (log.isDebug2()) {
	for (Iterator iter = lines.iterator(); iter.hasNext(); ) {
	  log.debug2("DU: " + (String)iter.next());
	}
      }
      if (lines == null || lines.isEmpty()) {
	return -1;
      }
      String ks = StringUtil.truncateAtAny((String)lines.get(0), " \t\n");
      return Long.parseLong(ks) * 1024;
    } catch (Exception e) {
      log.warning("DU(" + path + ")", e);
      return -1;
    }

  }

  public DF getDF(String path) throws UnsupportedException {
    String cmd = "df -k -P " + path;
    if (log.isDebug2()) log.debug2("cmd: " + cmd);
    try {
      Process p = rt().exec(cmd);
      Reader rdr =
	new InputStreamReader(new BufferedInputStream(p.getInputStream()),
			      Constants.DEFAULT_ENCODING);
      String s;
      try {
	s = StringUtil.fromReader(rdr);
	int exit = p.waitFor();
	rdr.close();
	if (exit != 0) {
	  return null;
	}
      } catch (IOException e) {
	log.error("Couldn't read from '" + cmd + "'", e);
	return null;
      }

      List lines = StringUtil.breakAt(s, '\n');
      if (log.isDebug2()) {
	for (Iterator iter = lines.iterator(); iter.hasNext(); ) {
	  log.debug2("DF: " + (String)iter.next());
	}
      }
      if (lines == null || lines.size() < 2) {
	return null;
      }
      return makeDFFromLine((String)lines.get(1));
    } catch (Exception e) {
      log.warning("DF(" + path + ")", e);
      return null;
    }

  }

  DF makeDFFromLine(String line) {
    String[] tokens = new String[6];
    StringTokenizer st = new StringTokenizer(line, " \t");
    int ntok = 0;
    while (st.hasMoreTokens()) {
      if (ntok > 5) {
	return null;
      }
      tokens[ntok++] = st.nextToken();
    }
    if (ntok != 6) {
      return null;
    }
    DF df = new DF();
    df.fs = tokens[0];
    df.size = getInt(tokens[1]);
    df.used = getInt(tokens[2]);
    df.avail = getInt(tokens[3]);
    df.percentString = tokens[4];
    df.mnt = tokens[5];
    try {
      df.percent = percentFmt.parse(df.percentString).doubleValue();
    } catch (ParseException e) {
    }
    if (log.isDebug2()) log.debug2(df.toString());
    return df;
  }

  int getInt(String s) throws NumberFormatException{
    try {
      return Integer.parseInt(s);
    } catch (NumberFormatException e) {
      log.warning("Illegal number in DF output: " + s);
      return 0;
    }
  }

  /**
   * <p>Convenience call to the current {@link PlatformUtil}
   * instance's {@link #updateFileAtomically} method.</p>
   * @param updated An updated version of the target file.
   * @param target  A target file, which is to be updated by the
   *                updated file.
   * @return True if and only if the update succeeded; false
   *         otherwise.
   * @see #updateFileAtomically
   */
  public static boolean updateAtomically(File updated, File target) {
    return getInstance().updateFileAtomically(updated, target);
  }
  
  /**
   * <p>Attempts to update <code>target</code> atomically by renaming
   * <code>updated</code> to <code>target</code> if possible or by
   * using any similar filesystem-specific mechanism.</p>
   * <p>Atomicity is not guaranteed. An atomic update is more likely
   * if the files are in the same directory. Even if they are, some
   * platforms may not support atomic renames.</p>
   * <p>When this method returns, <code>updated</code>will exist if
   * and only if the update failed.</p>
   * @param updated An updated version of the target file.
   * @param target  A target file, which is to be updated by the
   *                updated file.
   * @return True if and only if the update succeeded; false
   *         otherwise.
   * @see #updateAtomically
   * @see PlatformUtil.Windows#updateFileAtomically
   */
  public boolean updateFileAtomically(File updated, File target) {
    try {
      return updated.renameTo(target); // default
    }
    catch (SecurityException se) {
      // Just log and rethrow
      log.warning("Security exception reported in atomic update", se);
      throw se;
    }
  }
  
  /** Linux implementation of platform-specific code */
  public static class Linux extends PlatformUtil {
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
  public static class OpenBSD extends PlatformUtil {
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

  public static class Windows extends PlatformUtil {
    
    public synchronized boolean updateFileAtomically(File updated, File target) {
      try {
        File saveTarget = null;

        // Move target out of the way if necessary
        if (target.exists()) {
          // Create temporary file
          saveTarget = new File(target.getParent(),
                                target.getName() + ".windows." + System.currentTimeMillis());

          // Rename target
          if (!target.renameTo(saveTarget)) {
            log.error("Windows platform: "
                      + target.toString()
                      + " exists but could not be renamed to "
                      + saveTarget.toString());
            return false; // fail unconditionally
          }
        }

        // Update target
        if (updated.renameTo(target)) {
          // Delete original if needed if the update is successful
          if (saveTarget != null) {
            if (!saveTarget.delete()) {
              log.warning("Windows platform: "
                          + saveTarget.toString()
                          + " could not be deleted at the end of an update");
            }
          }
          
          return true; // succeed
        }
        else {
          // Log an error message if the update is unsuccessful
          log.error("Windows platform: "
                    + updated.toString()
                    + " could not be renamed to "
                    + target.toString());
          
          // Try to restore the original (unlikely to succeed)
          if (!saveTarget.renameTo(target)) {
            log.error("Windows platform: "
                      + target.toString()
                      + " could not be restored from "
                      + saveTarget.toString());
          }
          
          return false; // fail
        }
      }
      catch (SecurityException se) {
        // Log and rethrow
        log.warning("Windows Platform: security exception reported in atomic update", se);
        throw se;
      }
    }
  }
  
  /** Struct holding disk space info (from df) */
  public static class DF {
    String fs;
    int size;
    int used;
    int avail;
    String percentString;
    double percent = -1.0;
    String mnt;

    public String getFs() {
      return fs;
    }
    public int getSize() {
      return size;
    }
    public int getUsed() {
      return used;
    }
    public int getAvail() {
      return avail;
    }
    public String getPercentString() {
      return percentString;
    }
    public double getPercent() {
      return percent;
    }
    public String getMnt() {
      return mnt;
    }
    public String toString() {
      return "[DF: " + fs + "]";
    }
  }

  /** Exception thrown if no implementation is available for the current
   * platform, or a platform-dependent error occurs.
   * In the case of an error, the original exception is available. */
  public static class UnsupportedException extends Exception {
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
