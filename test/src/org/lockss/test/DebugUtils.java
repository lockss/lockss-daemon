/*
 * $Id: DebugUtils.java,v 1.1 2002-12-21 21:10:16 tal Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

public class DebugUtils {
  protected static Logger log = Logger.getLogger("DebugUtils");
  private static Runtime rt = Runtime.getRuntime();

//   static {
//     System.loadLibrary("DebugUtils");
//   }

//   public static native int getPid();

  public static int getPid() {
    int pid = getLinuxPid();
    System.err.println("PID = " + pid);
    return pid;
  }

  public static int getLinuxPid() {
    try {
    Reader r = new FileReader(new File("/proc/self/stat"));
    char[] buf = new char[1000];
    if (r.read(buf) < 0) {
      throw new RuntimeException("EOF reading /proc/self/stat");
    }
    Vector v = breakString(new String(buf), ' ');
    String ppid = (String)v.elementAt(3);
    return Integer.parseInt(ppid);
    } catch (FileNotFoundException e) {
      throw new RuntimeException("Can't open /proc/self/stat");
    } catch (IOException e) {
      throw new RuntimeException("Error reading /proc/self/stat");
    }      
  }

  public static void threadDump() {
    int pid = getPid();
    try {
      Process p = rt.exec("kill -QUIT " + pid);
      p.waitFor();
    } catch (IOException e) {
      log.error("Couldn't exec 'kill -QUIT'", e);
    } catch (InterruptedException e) {
      log.error("waitFor()", e);
    }
//     InputStream is = p.getInputStream();
//     org.mortbay.util.IO.copy(is, System.out);
  }

  /** Break a string at a separator char. */
  public static Vector breakString(String s, char sep, int maxLines) {
    Vector res = new Vector();
    if (s == null) {
      return res;
    }
    if (maxLines <= 0) {
      maxLines = Integer.MAX_VALUE;
    }
    for (int pos = 0; maxLines > 0; maxLines-- ) {
      int end = s.indexOf(sep, pos);
      if (end == -1) {
	if (pos >= s.length()) {
	  break;
	}
	end = s.length();
      }
      res.addElement((Object) s.substring(pos, end));
      pos = end + 1;
    }
    return res;
  }

  public static Vector breakString(String s, char sep) {
    return breakString(s, sep, 0);
  }
}
