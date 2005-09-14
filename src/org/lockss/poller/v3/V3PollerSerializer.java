/*
 * $Id: V3PollerSerializer.java,v 1.2 2005-09-14 23:57:49 smorabito Exp $
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

import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;
import org.lockss.util.*;
import org.lockss.util.ObjectSerializer.*;

public class V3PollerSerializer extends V3Serializer {
  private static final String POLLER_STATE_BEAN = "poller_state_bean.xml";
  private static final String POLLER_USER_DATA_PREFIX = "poller_user_data_";
  private static final String POLLER_USER_DATA_SUFFIX = ".xml";
  private static final String POLLER_STATE_TABLE_PREFIX = "poller_state_table_";
  private static final String POLLER_STATE_TABLE_SUFFIX = ".xml";
  private static final String PEER_MAPPING_FILE = "peer_mapping.xml";

  private File pollerStateBeanFile;
  private File peerMappingFile;
  private LinkedHashMap peerMapping;

  public V3PollerSerializer() throws PollSerializerException {
    super(null);
    this.pollerStateBeanFile = new File(pollDir, POLLER_STATE_BEAN);
    this.peerMappingFile = new File(pollDir, PEER_MAPPING_FILE);
    this.peerMapping = new LinkedHashMap();
  }

  public V3PollerSerializer(String dir) throws PollSerializerException {
    super(dir);
    this.pollerStateBeanFile = new File(pollDir, POLLER_STATE_BEAN);
    this.peerMappingFile = new File(pollDir, PEER_MAPPING_FILE);
    if (peerMappingFile.exists()) {
      log.debug2("Loading peer mapping from file " + peerMappingFile);
      try {
        this.peerMapping = this.loadPeerMapping();
      } catch (Exception ex) {
        throw new PollSerializerException("Exception while loading peer mapping",
                                          ex);
      }
    } else {
      log.debug2("Mapping file " + peerMappingFile + " does not exist.");
      this.peerMapping = new LinkedHashMap();
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
      PollerStateBean bean = (PollerStateBean) xstr
          .deserialize(new FileReader(pollerStateBeanFile));
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
  public PollerUserData loadPollerUserData(PeerIdentity peerId)
      throws PollSerializerException {
    try {
      PeerMapping mapping = getPeerMapping(peerId.getIdString());
      File in = null;
      if (mapping == null || (in = mapping.getUserDataFile()) == null) {
        throw new PollSerializerException("No serialized state for voter " +
                                          peerId);
      }
      return (PollerUserData) xstr.deserialize(in);
    } catch (Exception ex) {
      throw new PollSerializerException("Unable to restore VoterUserData", ex);
    }
  }

  /**
   * Save a PsmInterpStateBean
   */
  public void savePollerInterpState(PeerIdentity id, PsmInterpStateBean bean)
      throws PollSerializerException {
    log.debug2("Saving PsmInterp state for peer " + id);
    try {
      File out = getInterpStateFile(id.getIdString());
      xstr.serialize(out, bean);
    } catch (Exception ex) {
      throw new PollSerializerException("Unable to save PsmInterpStateBean",
                                        ex);
    }
  }

  /**
   * Restore a PsmInterpStateBean for a V3Poller's participant.
   * 
   * @return The saved state bean for a poll.
   */
  public PsmInterpStateBean loadPollerInterpState(PeerIdentity peerId)
      throws PollSerializerException {
    try {
      PeerMapping mapping = getPeerMapping(peerId.getIdString());
      File in = null;
      if (mapping == null || (in = mapping.getInterpStateFile()) == null) {
        throw new PollSerializerException("No serialized state for voter " +
                                          peerId);
      }
      return (PsmInterpStateBean) xstr.deserialize(in);
    } catch (Exception ex) {
      throw new PollSerializerException("Unable to restore PsmInterpStateBean",
                                        ex);
    }
  }

  /**
   * Return a list of all VoterStateBeans for this poll.
   */
  public List loadInnerCircleStates() throws PollSerializerException {
    List innerCircleStates = new ArrayList(peerMapping.size());
    try {
      for (Iterator iter = peerMapping.values().iterator(); iter.hasNext(); ) {
        PeerMapping mapping = (PeerMapping)iter.next();
        File mappingFile = mapping.getUserDataFile();
        if (mappingFile == null) {
          // This should never be null.
          throw new PollSerializerException("Peer has null UserData file.");
        }
        PollerUserData voterState = 
          (PollerUserData) xstr.deserialize(mappingFile);
        innerCircleStates.add(voterState);
      }
    } catch (IOException ex) {
      throw new PollSerializerException("Cannot load inner circle states", ex);
    } catch (ObjectSerializer.SerializationException ex) {
      throw new PollSerializerException("Cannot load inner circle states", ex);
    }
    return innerCircleStates;
  }

  private static final FileFilter voterFilter = new FileFilter() {
    public boolean accept(File pathName) {
      return pathName.getName().startsWith(POLLER_USER_DATA_PREFIX);
    }
  };

  /**
   * Obtain a peer mapping, creating it if necessary.
   */
  
  public synchronized PeerMapping getPeerMapping(String id) {
    PeerMapping map = (PeerMapping)peerMapping.get(id);
    if (map == null) {
      map = new PeerMapping();
      peerMapping.put(id, map);
    }
    return map;
  }
  
  /**
   * Obtain a file for the InterpStateBean.
   */
  private synchronized File getInterpStateFile(String id)
      throws IOException, SerializationException {
    PeerMapping map = getPeerMapping(id);
    File f = map.getInterpStateFile();
    if (f == null) {
      f = FileUtil.createTempFile(POLLER_STATE_TABLE_PREFIX,
                                  POLLER_STATE_TABLE_SUFFIX, pollDir);
      map.setInterpStateFile(f);
      savePeerMapping();
    }
    return f;
  }
  
  /**
   * Obtain a file for the PollerUserData.
   */
  private synchronized File getPollerUserDataFile(PeerIdentity id)
      throws IOException, SerializationException {
    PeerMapping mapping = getPeerMapping(id.getIdString());
    File f = mapping.getUserDataFile();
    if (f == null) {
      f = FileUtil.createTempFile(POLLER_USER_DATA_PREFIX,
                                  POLLER_USER_DATA_SUFFIX, pollDir);
      mapping.setUserDataFile(f);
      savePeerMapping();
    }
    return f;
  }
  
  /**
   * Store the mapping of peer IDs to files.
   * @throws IOException 
   * @throws SerializationException 
   */
  private synchronized void savePeerMapping()
      throws IOException, SerializationException {
    xstr.serialize(peerMappingFile, peerMapping);
  }
  
  private synchronized LinkedHashMap loadPeerMapping() 
      throws IOException, SerializationException {
    return (LinkedHashMap)xstr.deserialize(peerMappingFile);
  }
  
  /**
   * Maintain a mapping of peer IDs to poller state files. 
   * 
   * Note:  IDs are stored as strings because the daemon depends
   * on instance equality for PeerIdentities.  After deserialization,
   * a stored PeerIdentity would by definition be a different instance. 
   */
  private static class PeerMapping implements Serializable {
    private File interpStateFile;
    private File userDataFile;
    
    public PeerMapping() {
    }
    
    public PeerMapping(File interpStateFile, File userDataFile) {
      this.interpStateFile = interpStateFile;
      this.userDataFile = userDataFile;
    }


    public File getInterpStateFile() {
      return interpStateFile;
    }

    public void setInterpStateFile(File interpStateFile) {
      this.interpStateFile = interpStateFile;
    }

    public File getUserDataFile() {
      return userDataFile;
    }

    public void setUserDataFile(File userDataFile) {
      this.userDataFile = userDataFile;
    }
    
    public String toString() {
      StringBuffer buf = new StringBuffer("[PeerMapping: ");
      buf.append("interp=" + (interpStateFile == null ? "null" : interpStateFile.toString()));
      buf.append(", ud=" + (userDataFile == null ? "null" : userDataFile.toString()));
      buf.append("]");
      return buf.toString();
    }
  }
}
