/*

Copyright (c) 2000-2021 Board of Trustees of Leland Stanford Jr. University,
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
import java.lang.management.ManagementFactory;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

import org.apache.commons.lang3.SystemUtils;
import org.lockss.config.*;

/** Utilities to communicate with platform to get info or take action not
 * possible from Java */
public class PlatformUtil {
  
  private static final Logger log = Logger.getLogger("PlatformInfo");
  
  private static final DecimalFormat percentFmt = new DecimalFormat("0%");

  /** Should be set to the list of allowed TCP ports, based on
   * platform- (and group-) dependent packet filters */
  public static final String PARAM_UNFILTERED_TCP_PORTS =
    Configuration.PLATFORM + "unfilteredTcpPorts";

  public static final String PARAM_UNFILTERED_UDP_PORTS =
    Configuration.PLATFORM + "unfilteredUdpPorts";

  public enum DiskSpaceSource { Java, DF };

  /** Determines how disk space statistics (total, free, available)
   * are obtained.  If <tt>Java</tt>, the builtin Java library methods
   * are used, if <tt>DF</tt>, the <tt>df</tt> utility is run in a sub
   * process.  The former is normally preferred but returns incorrect
   * results on filesystems larger than 8192PB.  The latter currently
   * works on filesystems up to 8192EB, but is slower and could
   * conceivably fail. */
  public static final String PARAM_DISK_SPACE_SOURCE =
    Configuration.PLATFORM + "diskSpaceSource";

  public static final DiskSpaceSource DEFAULT_DISK_SPACE_SOURCE =
    DiskSpaceSource.Java;

  public static final File[] FILE_ROOTS = File.listRoots();

  static PlatformUtil instance;

  /** Return the singleton PlatformInfo instance */
  // no harm if happens in two threads, so not synchronized
  public static PlatformUtil getInstance() {
    if (instance == null) {
      String os = System.getProperty("lockss.os.name");
      if (StringUtil.isNullString(os)) {
	os = System.getProperty("os.name");
      }
      if ("linux".equalsIgnoreCase(os)) {
	instance = new Linux();
      }
      if ("openbsd".equalsIgnoreCase(os)) {
	instance = new OpenBSD();
      }
      if ("force_macos".equalsIgnoreCase(os)) {
	instance = new MacOS();
      }
      if ("force_windows".equalsIgnoreCase(os)) {
        instance = new Windows();
      }
      if ("force_solaris".equalsIgnoreCase(os)) {
        instance = new Solaris();
      }
      if ("force_none".equalsIgnoreCase(os)) {
	instance = new PlatformUtil();
      }
      if (SystemUtils.IS_OS_MAC_OSX) {
	instance = new MacOS();
      }
      if (SystemUtils.IS_OS_WINDOWS) {
        instance = new Windows();
      }
      if (/*SystemUtils.IS_OS_SOLARIS*/ "sunos".equalsIgnoreCase(os)) {
        instance = new Solaris();
      }
      if (instance == null) {
	log.warning("No OS-specific PlatformInfo for '" + os + "'");
	instance = new PlatformUtil();
      }
    }
    return instance;
  }

  /** Return the system temp directory; see {@link
   * ConfigManager#PARAM_TMPDIR}
   */
  public static String getSystemTempDir() {
    return System.getProperty("java.io.tmpdir");
  }

  /** Return the current working dir name */
  public static String getCwd() {
    return new File(".").getAbsoluteFile().getParent();
  }

  public List getUnfilteredTcpPorts() {
    Configuration config = CurrentConfig.getCurrentConfig();
    List lst = config.getList(PARAM_UNFILTERED_TCP_PORTS);
    // CD <= 248 use comma as separator in this string
    String str;
    if (lst.size() == 1 &&
	((str = (String)lst.get(0)).indexOf(',') != -1)) {
      return StringUtil.breakAt(str, ',', 0, true);
    }
    return lst;
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

  /**
   * Determines whether file system is case-sensitive for operations that
   * depend on case sensitivity
   * @return <code>true</code> if the file system is case-sensitive
   */
  public boolean isCaseSensitiveFileSystem() {
    return true;
  }
  
  /**
   * Return the length of the longest filename that can be created in the
   * filesystem.  This is the length of a single directory or filename; for
   * the maximum pathname see maxPathname().  (This should really be
   * filesystem-dependent, not just OS-dependent.) */
  public int maxFilename() {
    return 255;
  }

  /**
   * Return the length of the longest pathname that can be created in the
   * filesystem.  This is total length of an absolute path, including all
   * parent dirs.  (This should really be filesystem-dependent, not just
   * OS-dependent.) */
  public int maxPathname() {
    return 4096;
  }

  /**
   * Return true if the platform includes scripting support */
  public boolean hasScriptingSupport() {
    return true;
  }
  
  /**
   * Return true if the exception was caused by a full filesystem
   */
  public boolean isDiskFullError(IOException e) {
    return StringUtil.indexOfIgnoreCase(e.getMessage(),
					"No space left on device") >= 0;
  }
  
  static Runtime rt() {
    return Runtime.getRuntime();
  }

  /** Return disk usage below path, in bytes */
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

  /** Get disk usage info from Java.  This fails on filesystems that
   * are 8192PB or larger, because Java returns the size, in bytes, in
   * a long.
   */
  public DF getJavaDF(String path) {
    File f = null;
    try {
      f = new File(path).getCanonicalFile();
    } catch (IOException e) {
      f = new File(path).getAbsoluteFile();
    }
    // mirror the df behaviour of returning null if path doesn't exist
    if (!f.exists()) {
      return null;
    }
    DF df = new DF();
    df.path = path;
    df.size = f.getTotalSpace() / 1024;
    df.avail = f.getUsableSpace() / 1024;
    df.used = df.size - (f.getFreeSpace() /1024);
    df.percent = Math.ceil((df.size -df.avail) * 100.00 / df.size);
    df.percentString =  String.valueOf(Math.round(df.percent)) + "%";
    df.percent /= 100.00;
    df.fs = null;
    df.mnt = longestRootFile(f);
    df.source = DiskSpaceSource.Java;
    if (log.isDebug2()) log.debug2(df.toString());
    return df;
  }

  public static String longestRootFile(File file)
  {
    String longestRoot = null;

    for (File root : FILE_ROOTS)
    {
      File parent = file.getParentFile();
      while(parent != null) {
        if(root.equals(parent)) {
          if(longestRoot == null ||
              longestRoot.length() < root.getPath().length())   {
            longestRoot = root.getPath();
          }
        }
        parent = parent.getParentFile();
      }
    }
    return longestRoot;
  }

  /** Get disk space statistics for the filesystem containing the
   * path, either directly from Java or by invoking 'df', according to
   * {@value #PARAM_DISK_SPACE_SOURCE} */

  public DF getDF(String path) {
    Configuration config = CurrentConfig.getCurrentConfig();
    switch (config.getEnumIgnoreCase(DiskSpaceSource.class,
                                     PARAM_DISK_SPACE_SOURCE,
                                     DEFAULT_DISK_SPACE_SOURCE)) {
    case Java:
    default:
      return getJavaDF(path);
    case DF:
      return getPlatformDF(path);
    }
  }


  /** Get disk usage info by running 'df' */
  public DF getPlatformDF(String path) {
    return getPlatformDF(path, "-k -P");
  }

  public DF getPlatformDF(String path, String dfArgs) {
    String cmd = "df " + dfArgs + " " + path;
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
	  if (log.isDebug()) log.debug("cmd: " + cmd + " exit code " + exit);
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
      return makeDFFromLine(path, (String)lines.get(1));
    } catch (Exception e) {
      log.warning("DF(" + path + ")", e);
      return null;
    }
  }

  DF makeDFFromLine(String path, String line) {
    String[] tokens = new String[6];
    StringTokenizer st = new StringTokenizer(line, " \t");
    int ntok = 0;
    while (st.hasMoreTokens()) {
      String tok = st.nextToken();
      if (ntok > 5) {
	break;
      }
      tokens[ntok++] = tok;
    }
    if (ntok < 6) {
      return null;
    }
    DF df = new DF();
    df.path = path;
    df.fs = tokens[0];
    df.size = getLong(tokens[1]);
    df.used = getLong(tokens[2]);
    df.avail = getLong(tokens[3]);
    df.percentString = tokens[4];
    df.mnt = tokens[5];
    df.source = DiskSpaceSource.DF;
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

  long getLong(String s) throws NumberFormatException{
    try {
      return Long.parseLong(s);
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
  
  public static String getLocalHostname() {
    String host = Configuration.getPlatformHostname();
    if (host == null) {
      try {
	host = IPAddr.getLocalHost().getHostName();
      } catch (UnknownHostException ex) {
	log.error("Couldn't determine localhost.", ex);
	return null;
      }
    }
    return host;
  }

  public static double parseDouble(String str) {
    if (isBuggyDoubleString(str)) {
      throw new NumberFormatException("Buggy double string");
    }
    return Double.parseDouble(str);
  }

  // Double.parseDouble("2.2250738585072012e-308") loops.  Disallow it and
  // variants, such as:
  //
  //   0.00022250738585072012e-304 (decimal point placement) (and similar
  //   strings with the decimal point shifted farther to the left,
  //   including far enough that the exponent goes to zero)
  //   22.250738585072012e-309 (decimal point placement)
  //   00000000002.2250738585072012e-308 (leading zeros)
  //   2.225073858507201200000e-308 (trailing zeros)
  //   2.2250738585072012e-00308 (leading zeros in the exponent)
  //   2.2250738585072012997800001e-308 (superfluous digits beyond digit
  //   17)

  // Match the bad sequence of digits, allowing for an embedded decimal
  // point, followed by a large negative three digit exponent
  private static Pattern BUGGY_DOUBLE_PAT1 =
    Pattern.compile("2\\.?2\\.?2\\.?5\\.?0\\.?7\\.?3\\.?8\\.?5\\.?8\\.?5\\.?0\\.?7\\.?2\\.?0\\.?1\\.?2\\d*[eE]-0*[23]\\d\\d");

  // Match the bad sequence of digits preceded by a decimal point and at
  // least 100 zeroes.
  private static Pattern BUGGY_DOUBLE_PAT2 =
    Pattern.compile("\\.0{100,}22250738585072012");

  public static boolean isBuggyDoubleString(String str) {
    Matcher m1 = BUGGY_DOUBLE_PAT1.matcher(str);
    if (m1.find()) {
      return true;
    }
    Matcher m2 = BUGGY_DOUBLE_PAT2.matcher(str);
    return m2.find();
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

  public static class Solaris extends PlatformUtil {
    public String dfArgs = "-k";

    public DF getPlatformDF(String path) {
      return (super.getPlatformDF(path, dfArgs));
    }

    public int getPid() throws UnsupportedException {
      throw new UnsupportedException("Don't know how to get PID on Solaris");
    }

    /** Get PID of main java process */
    public int getMainPid() throws UnsupportedException {
      throw new UnsupportedException("Don't know how to get PID on Solaris");
    }
    
    public Vector getProcStats(String pid) throws UnsupportedException {
      throw new
	UnsupportedException("Don't know how to get proc state on Solaris");
    }
  }


  public static class MacOS extends PlatformUtil {
    public String dfArgs = "-k";

    /**
     * Determines whether file system is case-sensitive for operations that
     * depend on case sensitivity
     * @return <code>true</code> if the file system is case-sensitive
     */
    public boolean isCaseSensitiveFileSystem() {
      return false; // MacOS FS is not case sensitive
    }

    public int maxPathname() {
      return 1024;
    }

    public DF getPlatformDF(String path) {
      return (super.getPlatformDF(path, dfArgs));
    }

    public int getPid() throws UnsupportedException {
      // see http://stackoverflow.com/questions/35842/process-id-in-java
      String pidprop = System.getProperty("pid");
      if (!StringUtil.isNullString(pidprop)) {
        try {
          int pid = Integer.parseInt(pidprop);
          return pid;
        } catch (NumberFormatException ex) {
          System.setProperty("pid", "");
          // shouldn't happen, so fall through and reset it
        }
      }

      // Note: may fail in some JVM implementations
      // therefore fallback has to be provided

      // something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
      final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
      final int index = jvmName.indexOf('@');

      if (index < 1) {
          // part before '@' empty (index = 0) / '@' not found (index = -1)
        throw new UnsupportedException("Don't know how to get PID on MacOS");
      }

      try {
          pidprop = jvmName.substring(0, index);
          int pid = Integer.parseInt(pidprop);
          System.setProperty("pid", pidprop);
          return pid;
      } catch (NumberFormatException e) {
        throw new UnsupportedException("Don't know how to get PID on MacOS");
      }
    }

    /** Get PID of main java process */
    public int getMainPid() throws UnsupportedException {
      return getPid();
    }
    
    public Vector getProcStats(String pid) throws UnsupportedException {
      throw new
	UnsupportedException("Don't know how to get proc state on MacOS");
    }
  }


  public static class Windows extends PlatformUtil {
    
    public DF getPlatformDF(String path, String dfArgs) {
      String cmd = "df " + dfArgs + " " + path;
      if (log.isDebug2()) log.debug2("cmd: " + cmd);
      	try {
          Process p = rt().exec(cmd);
          Reader rdr =
            new InputStreamReader(new BufferedInputStream(p.getInputStream()),
                  Constants.DEFAULT_ENCODING);
          String s;
          try {
            s = StringUtil.fromReader(rdr);
            // ignore exit status because GnuWin32 'df' reports
            // "df: `NTFS': No such file or directory" even though
            // it seems to operate correctly
            int exit = p.waitFor();
            rdr.close();
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
	  return makeDFFromLine(path, (String)lines.get(1));
      	} catch (Exception e) {
      	  log.warning("DF(" + path + ")", e);
          return null;
	}
    }
    
    /**
     * Determines whether file system is case-sensitive for operations that
     * depend on case sensitivity
     * @return <code>true</code> if the file system is case-sensitive
     */
    public boolean isCaseSensitiveFileSystem() {
      return false; // Windows FS is not case sensitive
    }
    
    public int maxPathname() {
      return 260;
    }

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
    protected String path;
    protected String fs;
    protected long size;
    protected long used;
    protected long avail;
    protected String percentString;
    protected double percent = -1.0;
    protected String mnt;
    protected DiskSpaceSource source;

    public static DF makeThreshold(long minFreeMB, double minFreePercent) {
      DF df = new DF();
      df.avail = minFreeMB * 1024;
      df.percent = minFreePercent == 0.0 ? -1.0 : 1.0 - minFreePercent;
      return df;
    }

    public String getFs() {
      return fs;
    }
    public String getPath() {
      return path;
    }
    public long getSize() {
      return size;
    }
    public long getUsed() {
      return used;
    }
    public long getAvail() {
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
    public DiskSpaceSource getSource() {
      return source;
    }
    public String toString() {
      return "[DF: " + fs + "]";
    }
    public boolean isFullerThan(DF threshold) {
      if (threshold.getAvail() > 0 &&
	  threshold.getAvail() >= getAvail()) {
	return true;
      }
      if (threshold.getPercent() > 0 &&
	  threshold.getPercent() <= getPercent()) {
	return true;
      }
      return false;
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
