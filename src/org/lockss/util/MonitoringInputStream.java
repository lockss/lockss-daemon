/*
 * $Id$
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

package org.lockss.util;

import java.io.*;
import java.util.*;

/**
 * Wrapper InputStream that records open() and close() events, to aid in
 * finding unclosed streams.
 */

public class MonitoringInputStream extends InputStream {
  private static final Logger log = Logger.getLogger("MonitoringInputStream");

  private InputStream in;
  private String name;
  private String openTrace;
  private long openTime;;
//   private String closeTrace;
  private String closeAbortTrace;
  private boolean isOpen = true;
  private boolean logClose = false;

  public MonitoringInputStream(InputStream in, String name) {
    this.in = in;
    this.name = name;
    openTrace = StringUtil.stackTraceString(new Exception("Open"));
    openTime = TimeBase.nowMs();
  }

  public MonitoringInputStream(InputStream in, String name, boolean logClose) {
    this(in, name);
    this.logClose = logClose;
  }
  public int read() throws IOException {
    return in.read();
  }
  public int read(byte b[]) throws IOException {
    return read(b, 0, b.length);
  }
  public int read(byte b[], int off, int len) throws IOException {
    return in.read(b, off, len);
  }
  public long skip(long n) throws IOException {
    return in.skip(n);
  }
  public int available() throws IOException {
    return in.available();
  }
  public void close() throws IOException {
//     closeTrace = StringUtil.stackTraceString(new Exception("Close"));
    if (logClose) log.info("close(" + name + ")",
			   new Throwable("Stack trace only"));
    try {
      in.close();
    } catch (IOException e) {
      closeAbortTrace = StringUtil.stackTraceString(e);
    }
    isOpen = false;
  }
  public void mark(int readlimit) {
    in.mark(readlimit);
  }
  public void reset() throws IOException {
    in.reset();
  }
  public boolean markSupported() {
    return in.markSupported();
  }

  protected void finalize() {
    if (isOpen) {
      log.warning("Never closed (" + name + ").  Opened at " +
		  Logger.getTimeStampFormat().format(openTime) +
		  " at " + openTrace);
      if (closeAbortTrace != null) {
	log.warning("Close threw: " + closeAbortTrace);
      }
      IOUtil.safeClose(in);
    }
  }
}
