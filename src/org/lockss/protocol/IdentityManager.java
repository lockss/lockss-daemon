/*
 * $Id: IdentityManager.java,v 1.73 2006-01-12 03:13:30 smorabito Exp $
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

package org.lockss.protocol;

import java.io.*;
import java.util.*;

import org.lockss.app.*;
import org.lockss.config.Configuration;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;

/**
 * <p>Abstraction for identity of a LOCKSS cache. Currently wraps an
 * IP address.<p>
 * @author Claire Griffin
 * @version 1.0
 */
public interface IdentityManager extends LockssManager {
  /**
   * <p>A prefix common to all parameters defined by this class.</p>
   */
  public static final String PREFIX = Configuration.PREFIX + "id.";

  /**
   * <p>The LOCAL_IP parameter.</p>
   */
  public static final String PARAM_LOCAL_IP =
    Configuration.PREFIX + "localIPAddress";

  /**
   * <p>The TCP port for the local V3 identity
   * (at org.lockss.localIPAddress). Can be overridden by
   * org.lockss.platform.v3.port.</p>
   */
  public static final String PARAM_LOCAL_V3_PORT =
    Configuration.PREFIX + "localV3Port";

  /**
   * <p>Local V3 identity string. If this is set it will take
   * precedence over org.lockss.platform.v3.identity.</p> */
  public static final String PARAM_LOCAL_V3_IDENTITY =
    Configuration.PREFIX + "localV3Identity";

  /**
   * <p>If true, restored agreement maps will be merged with any
   * already-loaded map
   */
  public static final String PARAM_MERGE_RESTORED_AGREE_MAP =
    Configuration.PREFIX + "id.mergeAgreeMap";

  /**
   * <p>The default value for the MERGE_RESTORED_AGREE_MAP
   * parameter.</p>
   */
  public static final boolean DEFAULT_MERGE_RESTORED_AGREE_MAP = true;

  /**
   * <p>The IDDB_DIR parameter.</p>
   */
  public static final String PARAM_IDDB_DIR = PREFIX + "database.dir";

  /**
   * <p>The name of the IDDB file.</p>
   */
  public static final String IDDB_FILENAME = "iddb.xml";

  /**
   * <p>The mapping file for this class.</p>
   */
  public static final String MAPPING_FILE_NAME =
    "/org/lockss/protocol/idmapping.xml";
  // CASTOR: Remove the field above when Castor is phased out.

  /**
   * <p>The MAX_DELTA reputation constant.</p>
   */
  public static final int MAX_DELTA = 0;

  /**
   * <p>The MAX_DELTA reputation constant.</p>
   */
  public static final int AGREE_VOTE = 1;

  /**
   * <p>The DISAGREE_VOTE reputation constant.</p>
   */
  public static final int DISAGREE_VOTE = 2;

  /**
   * <p>The CALL_INTERNAL reputation constant.</p>
   */
  public static final int CALL_INTERNAL = 3;

  /**
   * <p>The SPOOF_DETECTED reputation constant.</p>
   */
  public static final int SPOOF_DETECTED = 4;

  /**
   * <p>The REPLAY_DETECTED reputation constant.</p>
   */
  public static final int REPLAY_DETECTED = 5;

  /**
   * <p>The ATTACK_DETECTED reputation constant.</p>
   */
  public static final int ATTACK_DETECTED = 6;

  /**
   * <p>The VOTE_NOTVERIFIED reputation constant.</p>
   */
  public static final int VOTE_NOTVERIFIED = 7;

  /**
   * <p>The VOTE_VERIFIED reputation constant.</p>
   */
  public static final int VOTE_VERIFIED = 8;

  /**
   * <p>The VOTE_DISOWNED reputation constant.</p>
   */
  public static final int VOTE_DISOWNED = 9;

  /**
   * <p>Currently the only allowed V3 protocol.</p>
   */
  public static final String V3_ID_PROTOCOL_TCP = "TCP";

  /**
   * <p>The V3 protocol separator.</p>
   */
  public static final String V3_ID_PROTOCOL_SUFFIX = ":";

  /**
   * <p>The V3 TCP IP addr prefix.</p>
   */
  public static final String V3_ID_TCP_ADDR_PREFIX = "[";

  /**
   * <p>The V3 TCP IP addr suffix.</p>
   */
  public static final String V3_ID_TCP_ADDR_SUFFIX = "]";

  /**
   * <p>The V3 TCP IP / port separator.</p>
   */
  public static final String V3_ID_TCP_IP_PORT_SEPARATOR = ":";

  /**
   * <p>The initial reputation value.</p>
   */
  public static final int INITIAL_REPUTATION = 500;

  /**
   * <p>Finds or creates unique instances of PeerIdentity.</p>
   */
  public PeerIdentity findPeerIdentity(String key);

  /**
   * <p>Finds or creates unique instances of LcapIdentity.</p>
   */
  public LcapIdentity findLcapIdentity(PeerIdentity pid, String key)
      throws MalformedIdentityKeyException;

  /**
   * <p>Returns the peer identity matching the IP address and port;
   * An instance is created if necesary.</p>
   * <p>Used only by LcapDatagramRouter (and soon by its stream
   * analog).</p>
   * @param addr The IPAddr of the peer, null for the local peer.
   * @param port The port of the peer.
   * @return The PeerIdentity representing the peer.
   */
  public PeerIdentity ipAddrToPeerIdentity(IPAddr addr, int port);

  public PeerIdentity ipAddrToPeerIdentity(IPAddr addr);


  /**
   * <p>Returns the peer identity matching the String IP address and
   * port. An instance is created if necesary. Used only by
   * LcapMessage (and soon by its stream analog).
   * @param idKey the ip addr and port of the peer, null for the local
   *              peer.
   * @return The PeerIdentity representing the peer.
   */
  public PeerIdentity stringToPeerIdentity(String idKey)
      throws IdentityManager.MalformedIdentityKeyException;

  public IPAddr identityToIPAddr(PeerIdentity pid);

  /**
   * <p>Rturns the local peer identity.</p>
   * @param pollVersion The poll protocol version.
   * @return The local peer identity associated with the poll version.
   * @throws IllegalArgumentException if the pollVersion is not
   *                                  configured or is outside the
   *                                  legal range.
   */
  public PeerIdentity getLocalPeerIdentity(int pollVersion);

  /**
   * <p>Returns the IPAddr of the local peer.</p>
   * @return The IPAddr of the local peer.
   */
  public IPAddr getLocalIPAddr();

  /**
   * <p>Determines if this PeerIdentity is the same as the local
   * host.</p>
   * @param id The PeerIdentity.
   * @return true if is the local identity, false otherwise.
   */
  public boolean isLocalIdentity(PeerIdentity id);

  /**
   * <p>Determines if this PeerIdentity is the same as the local
   * host.</p>
   * @param idStr The string representation of the voter's
   *        PeerIdentity.
   * @return true if is the local identity, false otherwise.
   */
  public boolean isLocalIdentity(String idStr);

  /**
   * <p>Associates the event with the peer identity.</p>
   * @param id    The PeerIdentity.
   * @param event The event code.
   * @param msg   The LcapMessage involved.
   */
  public void rememberEvent(PeerIdentity id, int event, LcapMessage msg);

  /**
   * <p>Returns the max value of an Identity's reputation.</p>
   * @return The int value of max reputation.
   */
  public int getMaxReputation();

  /**
   * <p>Returns the reputation of the peer.</p>
   * @param id The PeerIdentity.
   * @return The peer's reputation.
   */
  public int getReputation(PeerIdentity id);

  /**
   * <p>Makes the change to the reputation of the peer "id" matching
   * the event "changeKind".
   * @param id         The PeerIdentity of the peer to affect.
   * @param changeKind The type of event that is being reflected.
   */
  public void changeReputation(PeerIdentity id, int changeKind);

  /**
   * <p>Used by the PollManager to record the result of tallying a
   * poll.</p>
   * @see #storeIdentities(ObjectSerializer)
   */
  public void storeIdentities() throws ProtocolException;

  /**
   * <p>Records the result of tallying a poll using the given
   * serializer.</p>
   */
  public void storeIdentities(ObjectSerializer serializer)
      throws ProtocolException;

  /**
   * <p>Copies the identity database file to the stream.</p>
   * @param out An OutputStream instance.
   */
  public void writeIdentityDbTo(OutputStream out) throws IOException;

  /**
   * <p>A Castor helper method to convert an identity map into a
   * serializable bean.</p>
   * @return An IdentityListBean corresponding to the identity map.
   */
  public IdentityListBean getIdentityListBean();

  /**
   * Return a list of all known UDP (suitable for V1) peer identities.
   */
  public Collection getUdpPeerIdentities();

  /**
   * Return a list of all known TCP (suitable for V1 or V3), peer identities.
   */
  public Collection getTcpPeerIdentities();

  /**
   * <p>Signals that we've agreed with pid on a top level poll on
   * au.</p>
   * <p>Only called if we're both on the winning side.</p>
   * @param pid The PeerIdentity of the agreeing peer.
   * @param au  The {@link ArchivalUnit}.
   */
  public void signalAgreed(PeerIdentity pid, ArchivalUnit au);

  /**
   * <p>Signals that we've disagreed with pid on any level poll on
   * au.</p>
   * <p>Only called if we're on the winning side.</p>
   * @param pid The PeerIdentity of the disagreeing peer.
   * @param au  The {@link ArchivalUnit}.
   */
  public void signalDisagreed(PeerIdentity pid, ArchivalUnit au);

  /**
   * <p>Peers with whom we have had any disagreement since the last
   * toplevel agreement are placed at the end of the list.</p>
   * @param au ArchivalUnit to look up PeerIdentities for.
   * @return List of peers from which to try to fetch repairs for the
   *         AU.
   */
  public List getCachesToRepairFrom(ArchivalUnit au);

  public boolean hasAgreed(String ip, ArchivalUnit au)
      throws MalformedIdentityKeyException;

  public boolean hasAgreed(PeerIdentity pid, ArchivalUnit au);

  /**
   * <p>Returns a collection of IdentityManager.IdentityAgreement for
   * each peer that we have a record of agreeing or disagreeing with
   * us.
   */
  public Collection getIdentityAgreements(ArchivalUnit au);

  /**
   * <p>Return map peer -> last agree time. Used for logging and
   * debugging.</p>
   */
  public Map getAgreed(ArchivalUnit au);

  /**
   * <p>Returns map peer -> last disagree time. Used for logging and
   * debugging</p>.
   */
  public Map getDisagreed(ArchivalUnit au);

  public boolean hasAgreeMap(ArchivalUnit au);

  /**
   * <p>Copies the identity agreement file for the AU to the given
   * stream.</p>
   * @param au  An archival unit.
   * @param out An output stream.
   * @throws IOException if input or output fails.
   */
  public void writeIdentityAgreementTo(ArchivalUnit au, OutputStream out)
      throws IOException;

  /**
   * <p>Installs the contents of the stream as the identity agreement
   * file for the AU.</p>
   * @param au An archival unit.
   * @param in An input stream to read from.
   */
  public void readIdentityAgreementFrom(ArchivalUnit au, InputStream in)
      throws IOException;

  public static class IdentityAgreement implements LockssSerializable {
    private long lastAgree = 0;
    private long lastDisagree = 0;
    private String id = null;

    public IdentityAgreement(PeerIdentity pid) {
      this.id = pid.getIdString();
    }

    // needed for marshalling
    public IdentityAgreement() {}

    public long getLastAgree() {
      return lastAgree;
    }

    public void setLastAgree(long lastAgree) {
      this.lastAgree = lastAgree;
    }

    public long getLastDisagree() {
      return lastDisagree;
    }

    public void setLastDisagree(long lastDisagree) {
      this.lastDisagree = lastDisagree;
    }

    public String getId() {
      return id;
    }

    public boolean hasAgreed() {
      return lastAgree != 0;
    }

    public void setId(String id) {
      this.id = id;
    }

    public void mergeFrom(IdentityAgreement ida) {
      long ag = ida.getLastAgree();
      if (ag > getLastAgree()) {
        setLastAgree(ag);
      }
      long dis = ida.getLastDisagree();
      if (dis > getLastDisagree()) {
        setLastDisagree(dis);
      }
    }

    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("[IdentityAgreement: ");
      sb.append("id=");
      sb.append(id);
      sb.append(", ");
      sb.append("lastAgree=");
      sb.append(lastAgree);
      sb.append(", ");
      sb.append("lastDisagree=");
      sb.append(lastDisagree);
      sb.append("]");
      return sb.toString();
    }

    public boolean equals(Object obj) {
      if (obj instanceof IdentityAgreement) {
        IdentityAgreement ida = (IdentityAgreement)obj;
        return (id.equals(ida.getId())
            && ida.getLastDisagree() == lastDisagree
            && ida.getLastAgree() == lastAgree);
      }
      return false;
    }

    public int hashCode() {
      return 7 * id.hashCode() + 3 * (int)(getLastDisagree() + getLastAgree());
    }
  }

  /**
   * <p>Exception thrown for illegal identity keys.</p>
   */
  public static class MalformedIdentityKeyException extends IOException {
    public MalformedIdentityKeyException(String message) {
      super(message);
    }
  }
}
