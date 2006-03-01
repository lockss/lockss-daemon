/*
 * $Id: V3PollerSerializer.java,v 1.8 2006-03-01 02:50:14 smorabito Exp $
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
import java.util.*;

import org.lockss.app.*;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;
import org.lockss.util.*;
import org.lockss.util.ObjectSerializer.*;

public class V3PollerSerializer extends V3Serializer {
  public static final String POLLER_STATE_BEAN = "poller_state_bean.xml";
  public static final String POLLER_USER_DATA_PREFIX = "participant_";
  public static final String POLLER_USER_DATA_SUFFIX = ".xml";

  private File pollerStateBeanFile;

  /** Mapping of peer identity to state file. */
  private HashMap peerMapping;

  public V3PollerSerializer(LockssDaemon daemon) throws PollSerializerException {
    super(daemon, null);
    this.pollerStateBeanFile = new File(pollDir, POLLER_STATE_BEAN);
    this.peerMapping = new HashMap();
  }

  public V3PollerSerializer(LockssDaemon daemon,
                            File dir) throws PollSerializerException {
    super(daemon, dir);
    this.pollerStateBeanFile = new File(pollDir, POLLER_STATE_BEAN);
    this.peerMapping = new HashMap();
    File[] stateFiles = pollDir.listFiles(new PollerUserDataFileFilter());
    try {
      for (int ix = 0; ix < stateFiles.length; ix++) {
        // Pre-load the object to determine its voter PeerIdentity.
        // Somewhat expensive, but should only be done once per restored poll.
        ParticipantUserData ud = (ParticipantUserData)xstr.deserialize(stateFiles[ix]);
        peerMapping.put(ud.getVoterId(), stateFiles[ix]);
      }
    } catch (Exception ex) {
      throw new PollSerializerException("Unable to restore PollerUserData", ex);
    }
  }

  private static final class PollerUserDataFileFilter implements FileFilter {

    public boolean accept(File pathname) {
      return StringUtil.startsWithIgnoreCase(pathname.getName(),
                                             POLLER_USER_DATA_PREFIX);
    }

  }

  /**
   * Store a PollerStateBean.
   *
   * @throws PollSerializerException if the poller state cannot be saved.
   */
  public void savePollerState(PollerStateBean state)
      throws PollSerializerException {
    log.debug2("Saving state for poll");
    try {
      xstr.serialize(pollerStateBeanFile, state);
    } catch (Exception ex) {
      throw new PollSerializerException("Unable to save state for poll", ex);
    }
  }

  /**
   * Load a PollerStateBean.
   *
   * @throws PollSerializerException if the poller state cannot be loaded.
   */
  public PollerStateBean loadPollerState() throws PollSerializerException {
    if (!pollerStateBeanFile.exists()) {
      throw new PollSerializerException("No serialized state for poll");
    }
    log.debug2("Restoring state for poll");
    try {
      return (PollerStateBean)xstr.deserialize(pollerStateBeanFile);
    } catch (Exception ex) {
      throw new PollSerializerException("Unable to restore poll state", ex);
    }
  }

  /**
   * Store a PollerUserData.
   *
   * @throws PollSerializerException if the user data cannot be saved.
   */
  public void savePollerUserData(ParticipantUserData state)
      throws PollSerializerException {
    PeerIdentity peerId = state.getVoterId();
    log.debug2("Saving voter state for participant " + state.getVoterId());
    try {
      File outFile = getPollerUserDataFile(peerId);
      xstr.serialize(outFile, state);
    } catch (Exception ex) {
      throw new PollSerializerException("Unable to save voter state", ex);
    }
  }

  /**
   * Restore a PollerUserData.
   *
   * @throws PollSerializerException if the PollerUserData cannot be loaded.
   */
  public ParticipantUserData loadPollerUserData(PeerIdentity peerId)
      throws PollSerializerException {
    try {
      File in = (File)peerMapping.get(peerId);
      if (in == null) {
        throw new PollSerializerException("No serialized state for voter " +
                                          peerId);
      }
      return (ParticipantUserData) xstr.deserialize(in);
    } catch (Exception ex) {
      throw new PollSerializerException("Unable to restore PollerUserData", ex);
    }
  }

  /**
   * Clean up and remove a peer from the serialized state.  Called by
   * V3Poller when dropping a voter from a poll.
   *
   * @param id The peer identity to remove.
   */
  public void removePollerUserData(PeerIdentity id)
      throws IOException, SerializationException {
    File f = (File)peerMapping.get(id);
    if (f == null || !f.delete())
      throw new SerializationException("Could not remove poller user " +
                                       "data file " + f);
  }

  /**
   * Obtain a file for the PollerUserData.
   *
   * @param id The peer identity for which to find a state file.
   */
  private synchronized File getPollerUserDataFile(PeerIdentity id)
      throws IOException, SerializationException {
    File f = (File)peerMapping.get(id);
    if (f == null) {
      f = FileUtil.createTempFile(POLLER_USER_DATA_PREFIX,
                                  POLLER_USER_DATA_SUFFIX, pollDir);
      peerMapping.put(id, f);
    }
    return f;
  }

  /**
   * Return a collection containing all the poll participants' user data
   * objects.  Used when restoring a poll.  The order loaded is assumed to be
   * unimportant.
   *
   * @return A collection of all the poll participants' user data objects.
   *
   * @throws PollSerializerException
   */
  public Collection loadVoterStates() throws PollSerializerException {
    File[] files = pollDir.listFiles(voterFilter);
    List innerCircleStates = new ArrayList(files.length);
    try {
      for (int i = 0; i < files.length; i++) {
        ParticipantUserData voterState =
          (ParticipantUserData)xstr.deserialize(new FileReader(files[i]));
        innerCircleStates.add(voterState);
      }
    } catch (Exception ex) {
      throw new PollSerializerException("Unable to restore inner circle", ex);
    }
    return innerCircleStates;
  }

  private static final FileFilter voterFilter = new FileFilter() {
    public boolean accept(File pathName) {
      return pathName.getName().startsWith(POLLER_USER_DATA_PREFIX);
    }
  };
}
