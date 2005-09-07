/*
 * $Id: V3Serializer.java,v 1.1 2005-09-07 03:06:29 smorabito Exp $
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

import java.util.*;
import java.io.*;

import org.lockss.config.*;
import org.lockss.util.*;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;

public abstract class V3Serializer {
  /*
   * Implementation notes:
   * This is the V3 Polling Serialization framework.  It is used both
   * by V3Poller and V3Voter to serialize their states.
   *
   * The V3 Polling state directory (see "org.lockss.poll.v3.stateDir") is
   * organized as follows:
   *
   * ${stateDir}/${pollDir}/poll.xml Serialized PollerStateBean.
   *
   * ${stateDir}/${pollDir}/state_table.xml Serialized PsmInterpStateBean.
   *
   * ${stateDir}/${pollDir}/voter_${n}.xml Serialized VoterStateBean.
   *
   *
   * At construction time, create the directory ${stateDir}/${pollDir}.
   *
   * Every time the poll state changes, serialize its state bean to:
   * ${stateDir}/${pollDir}/poll.xml
   *
   * Every time a voter's state changes, serialize its state bean to:
   * ${stateDir}/${pollDir}/voter_{n}.xml
   *
   * Every time the state machine transitions, serialize its state bean to:
   * ${stateDir}/${pollDir}/state_table.xml
   *
   */
  public static String PARAM_V3_STATE_LOCATION = Configuration.PREFIX
    + "stateDir";
  public static String DEFAULT_V3_STATE_LOCATION = "v3state";

  static final Logger log = Logger.getLogger("V3Serializer");

  public static final String VOTER_USER_DATA_FILE = "voter_user_data.xml";
  public static final String VOTER_STATE_TABLE_FILE = "voter_state_table.xml";

  public static final String POLLER_STATE_BEAN = "poller_state_bean.xml";
  public static final String POLLER_USER_DATA_PREFIX = "poller_user_data_";
  public static final String POLLER_USER_DATA_SUFFIX = ".xml";
  public static final String POLLER_STATE_TABLE_PREFIX = "poller_state_table_";
  public static final String POLLER_STATE_TABLE_SUFFIX = ".xml";

  protected File pollDir;

  protected ObjectSerializer xstr = new XStreamSerializer();

  public V3Serializer() throws PollSerializerException {
    this(null);
  }

  /**
   * Create a new PollSerializer.  The parameter pollDir is optional.  If it is
   * specified, it must be a poll serialization directory that already exists.
   * If it is null, a new poll serialization directory will be created.
   *
   * @param dir Optionally, a pre-existing serialization directory to use.
   * @throws PollSerializerException
   */
  public V3Serializer(String dir) throws PollSerializerException {
    Configuration config = Configuration.getCurrentConfig();
    String relStateDir = config.get(PARAM_V3_STATE_LOCATION) == null ?
      DEFAULT_V3_STATE_LOCATION : config.get(PARAM_V3_STATE_LOCATION);
    List dSpaceList = config
      .getList(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST);
    if (dSpaceList == null || dSpaceList.size() == 0) {
      String msg = ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST
	+ " not specified, cannot persist V3 state.";
      throw new IllegalArgumentException(msg);
    }
    File stateDisk = new File((String) dSpaceList.get(0));
    File stateDir = new File(stateDisk, relStateDir);
    if (!stateDir.exists()) {
      if (!stateDir.mkdirs()) {
	throw new PollSerializerException("Could not create state directory "
					  + stateDir);
      }
    }
    if (dir == null) {
      try {
	this.pollDir = FileUtil.createTempDir("pollstate-", "", stateDir);
      } catch (IOException ex) {
	throw new PollSerializerException("Cannot create state directory "
					  + stateDir, ex);
      }
    } else {
      this.pollDir = new File(stateDir, dir);
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
    File[] files = pollDir.listFiles();
    for (int ix = 0; ix < files.length; ix++) {
      if (!files[ix].delete())
	log.warning("Unable to delete state file " + files[ix]
		    + ", aborting cleanup");
      return;
    }
    if (!pollDir.delete()) {
      log.warning("Unable to delete state dir " + pollDir);
    }
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
