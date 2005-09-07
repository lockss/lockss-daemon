/*
 * $Id: V3PollerSerializer.java,v 1.1 2005-09-07 03:06:29 smorabito Exp $
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

public class V3PollerSerializer extends V3Serializer {

  private File pollerStateBeanFile;
  private Map pollerUserDataFiles;
  private Map pollerStateTableFiles;
  
  private int pollerUserDataFileNum = 0;
  private int pollerStateTableFileNum = 0;

  public V3PollerSerializer() throws PollSerializerException {
    this(null);
  }

  public V3PollerSerializer(String dir) throws PollSerializerException {
    super(dir);
    this.pollerUserDataFiles = new HashMap();
    this.pollerStateTableFiles = new HashMap();
    this.pollerStateBeanFile = new File(pollDir, POLLER_STATE_BEAN);
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
      throw new PollSerializerException("Unable to save state for poll");
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
      PollerStateBean bean =
        (PollerStateBean) xstr.deserialize(new FileReader(pollerStateBeanFile));
      bean.setSerializer(this);
      return bean; 
    } catch (Exception ex) {
      throw new PollSerializerException("Unable to restore poll state", ex);
    }
  }

  /**
   * Store a PollerUserData.
   * 
   * @throws PollSerializerException if the user data cannot be saved.
   */
  public void savePollerUserData(PollerUserData state)
      throws PollSerializerException {
    PeerIdentity peerId = state.getVoterId();
    log.debug2("Saving voter state for participant " + state.getVoterId());
    File out = null;
    synchronized(pollerUserDataFiles) {
      out = (File) pollerUserDataFiles.get(peerId);
      if (out == null) {
        out = new File(pollDir, POLLER_USER_DATA_PREFIX + (pollerUserDataFileNum++)
                       + POLLER_USER_DATA_SUFFIX);
        pollerUserDataFiles.put(peerId, out);
      }
    }
    try {
      xstr.serialize(out, state);
    } catch (Exception ex) {
      throw new PollSerializerException("Unable to save voter state", ex);
    }
  }

  /**
   * Restore a PollerUserData.
   * 
   * @throws PollSerializerException if the PollerUserData cannot be loaded.
   */
  public PollerUserData loadPollerUserData(String peerId)
      throws PollSerializerException {
    File in = null;  
    synchronized(pollerUserDataFiles) {
      in = (File) pollerUserDataFiles.get(peerId);
    }
    if (in == null)
      throw new PollSerializerException("No serialized state for voter "
          + peerId);
    try {
      return (PollerUserData) xstr.deserialize(new FileReader(in));
    } catch (Exception ex) {
      throw new PollSerializerException("Unable to restore VoterUserData", ex);
    }
  }
  
  /**
   * Save a PsmInterpStateBean
   */
  public void savePollerInterpState(PeerIdentity id, PsmInterpStateBean bean)
      throws PollSerializerException {
    File out = null;
    synchronized(pollerStateTableFiles) {
      out = (File) pollerStateTableFiles.get(id);
      if (out == null) {
        out = new File(pollDir, POLLER_STATE_TABLE_PREFIX + (pollerStateTableFileNum++)
                       + POLLER_STATE_TABLE_SUFFIX);
        pollerStateTableFiles.put(id, out);
      }
    }
    try {
      xstr.serialize(out, bean);
    } catch (Exception ex) {
      throw new PollSerializerException("Unable to save PsmInterpStateBean", ex);
    }
  }

  /**
   * Restore a PsmInterpStateBean for a V3Poller's participant.
   * 
   * @return The saved state bean for a poll.
   */
  public PsmInterpStateBean loadPollerInterpState(PeerIdentity id) 
      throws PollSerializerException {
    File in = null;
    synchronized(pollerStateTableFiles) {
      in = (File) pollerStateTableFiles.get(id);
    }
    if (in == null) {
      throw new PollSerializerException("No serialized state for voter "
                                        + id);
    }
    try {
      return (PsmInterpStateBean) xstr.deserialize(in);
    } catch (Exception ex) {
      throw new PollSerializerException("Unable to restore PsmInterpStateBean",
                                        ex);
    }
  }
  

  /**
   * <p>
   * Return a list of all VoverStateBeans for this poll.
   * </p>
   */
  // XXX: This MUST be able to load voters in the same order that they were
  // saved. This will require vigorous unit tests!
  // (i.e., probably cannot rely on order from listFiles()!)
  public List loadInnerCircleStates() throws PollSerializerException {
    File[] files = pollDir.listFiles(voterFilter);
    List innerCircleStates = new ArrayList(files.length);
    try {
      for (int i = 0; i < files.length; i++) {
        PollerUserData voterState = (PollerUserData) xstr
            .deserialize(new FileReader(files[i]));
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
