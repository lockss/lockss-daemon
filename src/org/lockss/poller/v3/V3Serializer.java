/*
 * $Id: V3Serializer.java,v 1.17 2010-02-11 21:02:16 tlipkis Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.poller.v3;

import java.io.*;

import org.lockss.app.LockssDaemon;
import org.lockss.config.*;
import org.lockss.util.*;
import org.lockss.poller.*;

public abstract class V3Serializer {

  private static final String PREFIX = Configuration.PREFIX + "poll.v3.";

  protected File pollDir;
  protected LockssDaemon daemon;

  static final Logger log = Logger.getLogger("V3Serializer");
  
  public V3Serializer(LockssDaemon daemon) throws PollSerializerException {
    this.daemon = daemon;
    Configuration config = CurrentConfig.getCurrentConfig();
    File stateDir = PollUtil.ensurePollStateRoot();
    if (!FileUtil.ensureDirExists(stateDir)) {
      throw new PollSerializerException("Could not create state directory "
					+ stateDir);
    }
    try {
      this.pollDir = FileUtil.createTempDir("pollstate-", "", stateDir);
    } catch (IOException ex) {
      throw new PollSerializerException("Cannot create temp dir in state directory"
					+ stateDir, ex);
    }
  }

  /**
   * Create a new PollSerializer.  The parameter pollDir is optional.  If it
   * is specified, it must be a poll serialization directory that already
   * exists.  If it is null, a new poll serialization directory will be
   * created.
   *
   * @param dir Optionally, a pre-existing serialization directory to use.
   * @throws PollSerializerException
   */
  public V3Serializer(LockssDaemon daemon, File dir)
      throws PollSerializerException {
    if (dir == null) {
      throw new NullPointerException("Poll serialization directory must not "
                                     + "be null");
    }
    this.daemon = daemon;
    this.pollDir = dir;
    if (!pollDir.exists()) {
      throw new IllegalArgumentException("Poll directories passed as "
                                         + "arguments must already exist");
    }
  }
  
  /** Make an XStreamSerializer */
  protected ObjectSerializer getSerializer() {
    return new XStreamSerializer(daemon);
  }

  /**
   * Clean up all resources used by this poll. Removes the poll directory.
   */
  public void closePoll() {
    if (pollDir != null && pollDir.isDirectory() && !FileUtil.delTree(pollDir))
      log.warning("Unable to delete poll state directory: " + pollDir);
  }

  /**
   * PollSerializerException. Simply exception handling by wrapping IOException
   * and ObjectSerializer.SerializationException.
   */

  public static class PollSerializerException extends Exception {
    public PollSerializerException() {
      super();
    }

    public PollSerializerException(String msg) {
      super(msg);
    }

    public PollSerializerException(String msg, Throwable cause) {
      super(msg, cause);
    }

    public PollSerializerException(Throwable cause) {
      super(cause);
    }
  }
}
