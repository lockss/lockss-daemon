/*
 * $Id: LockssTiming.java,v 1.1 2004-04-05 08:05:15 tlipkis Exp $
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
import org.lockss.daemon.*;
import junit.framework.TestCase;
import junit.framework.TestResult;

// timing utilities
public class LockssTiming extends LockssTestCase {
  static final long DEFAULT_DURATION = 5 * Constants.SECOND;
  static final int DEFAULT_BUFSIZE = 4096;

  int bufsize = DEFAULT_BUFSIZE;
  long duration = DEFAULT_DURATION;
  long bytes;
  byte[] buf;
  char[] cbuf;

  public void setUp() throws Exception {
    buf = new byte[bufsize];
    cbuf = new char[bufsize];
    bytes = 0;
  }

  /** Call before setUp() */
  protected void setBufsize(int n) {
    bufsize = n;
  }


  protected void setDuration(long n) {
    duration = n;
  }


  public void time(String msg, Computation c) throws Exception {
    time(null, msg, c);
  }

  public void time(File file, String msg, Computation c) throws Exception {
    long start = System.currentTimeMillis();
    int cnt = 0;
    long delta;
    while (true) {
      c.execute();
      cnt++;
      long now = System.currentTimeMillis();
      if ((delta = (now - start)) > duration) {
	break;
      }
    }
    StringBuffer sb = new StringBuffer();
    sb.append(msg);
    sb.append(":  ");
    sb.append(Long.toString(delta/cnt));
    sb.append(" ms");
    if (file != null) {
      sb.append(",  ");
      long b = (file.length() * cnt) / delta;
      sb.append(Long.toString(b));
      sb.append(" b/ms(in)");
    }
    if (bytes > 0) {
      sb.append(",  ");
      long b = bytes / delta;
      sb.append(Long.toString(b));
      sb.append(" b/ms(out)");
    }
    System.out.println(sb.toString());
  }

  public void incrBytes(long n) {
    bytes += n;
  }

  public int readAll(Reader rdr, boolean efficiently) throws Exception {
    int tot = 0;
    int res;
    if (efficiently) {
      while ((res = rdr.read(cbuf, 0, bufsize)) != -1) {
	tot += res;
      }
    } else {
      while (rdr.read() >= 0) {
	tot++;
      }
    }
    return tot;
  }

  public int readAll(InputStream is, boolean efficiently) throws Exception {
    int tot = 0;
    int res;
    if (efficiently) {
      while ((res = is.read(buf, 0, bufsize)) != -1) {
	tot += res;
      }
    } else {
      while (is.read() >= 0) {
	tot++;
      }
    }
    return tot;
  }

  public interface Computation {
    public void execute() throws Exception;
  }
}
