/*
 * $Id: LockssTiming.java,v 1.7 2010-03-14 08:09:57 tlipkis Exp $
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
import java.text.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import junit.framework.TestCase;
import junit.framework.TestResult;

// timing utilities
public class LockssTiming extends LockssTestCase {
  public static final long DEFAULT_DURATION = 5 * Constants.SECOND;
  public static final int DEFAULT_BUFSIZE = 4096;

  int bufsize = DEFAULT_BUFSIZE;
  long duration = DEFAULT_DURATION;
  protected FileTimingReporter fileReporter;
  byte[] buf;
  char[] cbuf;
  String outLabel = "b";

  public void setUp() throws Exception {
    buf = new byte[bufsize];
    cbuf = new char[bufsize];
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

  public void setOutLabel(String label) {
    outLabel = label;
  }

  public void time(File file, String msg, Computation c) throws Exception {
    fileReporter = new FileTimingReporter(file, msg, outLabel);
    time(fileReporter, c);
  }

  public void time(TimingReporter reporter, Computation c) throws Exception {
    long start = System.currentTimeMillis();
    while (true) {
      reporter.startTimer();
      c.execute();
      reporter.stopTimer();
      long now = System.currentTimeMillis();
      if ((now - start) > duration) {
	break;
      }
    }
    reporter.report();
  }

  static class FileTimingReporter extends TimingReporterImpl {
    private File file;
    private String msg;
    private String outLabel;
    private long bytesProcessed = 0;

    FileTimingReporter(File file, String msg, String outLabel) {
      this.file = file;
      this.msg = msg;
      this.outLabel = outLabel;
    }
    
    public void report() {
      NumberFormat nf;
      
      nf = NumberFormat.getInstance();
      
      StringBuffer sb = new StringBuffer();
      sb.append(msg);
      sb.append(":  ");
      long ms = m_sumTime/m_count;
      if (ms >= 10) {
	sb.append(Long.toString(ms));
	sb.append(" ms");
      } else {
	sb.append(Long.toString(m_sumTime * 1000000 /m_count));
	sb.append(" ns");
      }
      sb.append(" ( std. dev. ");
      sb.append(nf.format(stddevTime()));
      sb.append(" )");
      if (file != null) {
	sb.append(",  ");
	sb.append(rateString(file.length() * m_count, m_sumTime));
	sb.append(" b/ms(in)");
      }
      if (bytesProcessed > 0) {
        sb.append(",  ");
        sb.append(rateString(bytesProcessed, m_sumTime));
        sb.append(" ");
        sb.append(outLabel);
        sb.append("/ms(out)");
      }
      System.out.println(sb.toString());
    }

  }

  static final NumberFormat rateFormat = new DecimalFormat("0.00");

  public static String rateString(long dividend, long divisor) {
    long b = dividend / divisor;
    if (b >= 100) {
      return Long.toString(b);
    } else {
      double f = (double)dividend / (double)divisor;
      return rateFormat.format(f);
    }
  }

  public void incrBytesProcessed(long n) {
    fileReporter.bytesProcessed += n;
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

  protected InputStreamReader newInputStreamReader(InputStream is)
      throws UnsupportedEncodingException{
    return new InputStreamReader(is, Constants.DEFAULT_ENCODING);
  }

  public interface Computation {
    public void execute() throws Exception;
  }
}
