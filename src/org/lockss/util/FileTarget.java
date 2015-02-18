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

import org.lockss.config.*;

/** A <code>LogTarget</code> implementation that outputs to a file, which
 * is closed and reopened periodically to allow for log file rotation.
 */
public class FileTarget extends PrintStreamTarget {
  static final String PREFIX = Logger.TARGET_PREFIX + "FileTarget.";
  static final String PARAM_REOPEN_INTERVAL = PREFIX + "reopenInterval";
  public static final String PARAM_FILE = PREFIX + "file";

  private static final long DEFAULT_REOPEN_INTERVAL = 10 * Constants.MINUTE;

  private String filename;
  private File logfile;

  private long reopenInterval;
  private Deadline nextReopen = Deadline.in(0);
  long maxSize = 0;

  /** Create a FileTarget that will log to the file specified by
   * org.lockss.log.target.FileTarget.file */
  public FileTarget() {
    super(null);
  }

  /** Create a FileTarget that will log to the specified file */
  FileTarget(String file) {
    super(null);
    filename = file;
    logfile = new File(file);
  }

  public void init() {
    super.init();
    Configuration config = CurrentConfig.getCurrentConfig();
    reopenInterval = config.getTimeInterval(PARAM_REOPEN_INTERVAL,
                                            DEFAULT_REOPEN_INTERVAL);
    if (filename == null) {
      filename = config.get(PARAM_FILE);
      if (filename == null) {
	throw new RuntimeException("No log target filename specified");
      }
      logfile = new File(filename);
    }
  }

  /** Write the message to the file, reopening it first if it's time
   */
  public void handleMessage(Logger log, int msgLevel, String message) {
    openOrReopenStream(log);
    super.handleMessage(log, msgLevel, message);
    maxSize = Math.max(maxSize, logfile.length());
  }

  /** Ensure file is open.  If it's been a while since we re-opened the
   * log, close it and reopen it. */
  void openOrReopenStream(Logger log) {
    if (stream != null && nextReopen.expired()) {
      stream.flush();
      stream.close();
      stream = null;
    }
    if (stream == null) {
      try {
        FileOutputStream fos = new FileOutputStream(filename, true);
        stream = new PrintStream(fos);
	nextReopen.expireIn(reopenInterval);
	if (logfile.length() < maxSize) {
	  maxSize = 0;
	  emitTimestamp(log);
	}
      } catch (IOException e) {
	log.error("Can't open " + filename);
        System.err.println("Can't open " + filename);
      }
    }
  }
}
