/*
 * $Id: V3Serializer.java,v 1.11 2006-04-10 05:31:01 smorabito Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

public abstract class V3Serializer {

  private static String PREFIX = Configuration.PREFIX + "poll.v3.";

  /** Location of the V3 state directory.  Should be an absolute path. */
  public static String PARAM_V3_STATE_LOCATION = PREFIX + "stateDir";
  public static String DEFAULT_V3_STATE_LOCATION = "v3state";

  protected File pollDir;
  protected ObjectSerializer xstr;

  static final Logger log = Logger.getLogger("V3Serializer");

  public V3Serializer(LockssDaemon daemon) throws PollSerializerException {
    this(daemon, null);
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
    this.xstr = new XStreamSerializer(daemon);
    Configuration config = CurrentConfig.getCurrentConfig();
    String relStateDir = config.get(PARAM_V3_STATE_LOCATION,
                                    DEFAULT_V3_STATE_LOCATION);
    if (dir == null) {
      File stateDir =
        ConfigManager.getConfigManager().getPlatformDir(relStateDir);
      if (!stateDir.exists() && !stateDir.mkdirs()) {
        throw new PollSerializerException("Could not create state directory "
                                          + stateDir);
      }
      try {
	this.pollDir = FileUtil.createTempDir("pollstate-", "", stateDir);
      } catch (IOException ex) {
	throw new PollSerializerException("Cannot create state directory "
					  + stateDir, ex);
      }
    } else {
      this.pollDir = dir;
      if (!pollDir.exists()) {
	throw new IllegalArgumentException("Poll directories passed as"
					   + "arguments must already exist");
      }
    }
  }

  /**
   * Clean up all resources used by this poll. Removes the poll directory.
   */
  public void closePoll() {
    if (!FileUtil.delTree(pollDir))
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
