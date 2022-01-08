/*
 * $Id$
 */

/*

Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.plugin.AuUtil;
import org.lockss.protocol.IdentityManager;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.repository.LockssRepositoryException;
import org.lockss.state.HistoryRepository;
import org.lockss.util.*;


/**
 * The saved information for a single {@link ArchivalUnit} about poll
 * agreements between this cache and all the peers it has agreements
 * with.
 *
 * This class is used by {@link
 * HistoryRepository#storeIdentityAgreements} and {@link
 * HistoryRepository#loadIdentityAgreements}.
 */
public class AuAgreements implements LockssSerializable {

  /**
   * <p>A logger for this class.</p>
   */
  protected static Logger log = Logger.getLogger(IdentityManagerImpl.class);

  // A collection of polling agreements as stored in the
  // HistoryRepository. The content is cached in map, and the two
  // structures are synchronized when needed. See updateListFromMap and
  // updateMapFromList.
  private List<PeerAgreements> list;

  // A quick lookup to avoid traversing list.
  private transient Map<PeerIdentity, PeerAgreements> map;

  private AuAgreements() {
    this.map = new HashMap();
  }

  /**
   * Create a new instance.
   * @param hRep The {@link HistoryRepository} to use.
   * @param idMgr A {@link IdentityManager} to translate {@link
   * String}s to {@link PeerIdentity} instances.
   */
  public static AuAgreements make(HistoryRepository hRep,
				  IdentityManager idMgr) {
    AuAgreements auAgreements = new AuAgreements();
    auAgreements.loadFrom(getRawAgreements(hRep), idMgr);
    return auAgreements;
  }

  /**
   * @param hRep A {@link HistoryRepository} to use.
   * @return The value and type from {@link
   * HistoryRepository#loadIdentityAgreements}, or {@code null} if the
   * no value is available.
   */
  private static Object getRawAgreements(HistoryRepository hRep) {
    Object rawAgreements = null;
    try {
      rawAgreements = hRep.loadIdentityAgreements();
    } catch (LockssRepositoryException e) {
      ArchivalUnit au = hRep.loadAuState().getArchivalUnit();
      log.error("getRawAgreements au="+au, e);
      // Should anything else be done in case of error?
    }
    return rawAgreements;
  }

  @Override
  public String toString() {
    return "AuAgreements[list="+list+
      ", map="+map+
      "]";
  }

  /**
   * @return true iff we have some data.
   */
  public synchronized boolean haveAgreements() {
    return !map.isEmpty();
  }

  /**
   * Store to the {@link HistoryRepository} supplied.
   * @param hRep A {@link HistoryRepository} to use.
   */
  public synchronized void store(HistoryRepository hRep) {
    try {
      updateListFromMap();
      hRep.storeIdentityAgreements(this);
    } catch (LockssRepositoryException e) {
      ArchivalUnit au = hRep.loadAuState().getArchivalUnit();
      log.error("AuAgreements.store("+au+")", e);
      // Should anything else be done in case of error?
    }
  }

  // NOTE: Since this instance is already in the cache and used for
  // initialization synchronization, we change the internal state to
  // be that of the Object supplied -- even if it is an instance of
  // AuAgreements.

  private void loadFrom(Object rawAgreements, IdentityManager idMgr) {
    if (rawAgreements == null) {
      loadInitial();
    } else if (rawAgreements instanceof AuAgreements) {
      loadFromAuAgreements((AuAgreements)rawAgreements);
    } else if (rawAgreements instanceof List) {
      loadFromList((List<IdentityManager.IdentityAgreement>)rawAgreements,
		   idMgr);
    } else {
      throw new IllegalArgumentException("Unexpected class: "+
					 rawAgreements.getClass().getName());
    }
    updateMapFromList(idMgr);
  }

  // Create when the history has nothing
  private void loadInitial() {
    this.list = new ArrayList();
  }

  // The format used in daemon 1.62 and beyond.
  private void loadFromAuAgreements(AuAgreements auAgreements) {
    // Copy over the non-transient instance variables.
    this.list = auAgreements.list;
  }

  // The format used prior to daemon 1.62
  private void loadFromList(List<IdentityManager.IdentityAgreement> list,
			    IdentityManager idMgr) {
    this.list = makePeerAgreementsList(list, idMgr);
  }

  /**
   * Translate a pre-1.62 List<IdentityAgreement> to a List<PeerAgreements>.
   */
  private List<PeerAgreements>
      makePeerAgreementsList(List<IdentityManager.IdentityAgreement> list,
			     IdentityManager idMgr) {
    List<PeerAgreements> result = new ArrayList();
    for (IdentityManager.IdentityAgreement idAgreement: list) {
      try {
	PeerIdentity pid = idMgr.stringToPeerIdentity(idAgreement.getId());
	// Check that it's a V3 pid
	if (! pid.isV3()) {
	  log.debug("Ignoring non-V3 peer: "+pid);
	  continue;
	}
	// Discard local hash data stored by early builds of 1.62
	if (pid.isLocalIdentity()) {
	  log.debug("Ignoring local identify: "+pid);
	  continue;
	}

      } catch (IdentityManager.MalformedIdentityKeyException e) {
	log.warning("Couldn't load agreement for key "+idAgreement.getId(), e);
      }
      PeerAgreements peerAgreements = PeerAgreements.from(idAgreement);
      result.add(peerAgreements);
    }
    return result;
  }


  private void updateListFromMap() {
    list = makeList(map);
  }

  private void updateMapFromList(IdentityManager idMgr) {
    map = makeMap(list, idMgr);
  }

  private List<PeerAgreements> makeList(Map<PeerIdentity, PeerAgreements> map) {
    return new ArrayList<PeerAgreements>(map.values());
  }

  private Map<PeerIdentity, PeerAgreements> makeMap(List<PeerAgreements> list,
						    IdentityManager idMgr) {
    Map<PeerIdentity, PeerAgreements> map = new HashMap();
    for (PeerAgreements peerAgreements: list) {
      String id = peerAgreements.getId();
      try {
	PeerIdentity pid = idMgr.findPeerIdentity(id);
	if (pid != null) {
	  map.put(pid, peerAgreements);
	} else {
	  // This means these agreements will be lost when the
	  // AuAgreements is saved.
	  log.info("Could not find PeerIdentity for "+id);
	}
      } catch (IdentityManager.MalformedIdentityKeyException e) {
	log.info("Ignoring malformed id: "+id);
      }
    }
    return map;
  }

  // NOTE: The calls to HistoryRepository to store and load
  // AuAgreements are serialized by locking the AuAgreements instance
  // for the AU. The writeTo and ReadFrom calls are used from
  // RemoteApi to save and restore back-ups, and need to make sure
  // that the locking of the HistoryRepository is correct, so those
  // calls are sent through the AuAgreements, via the
  // IdentityManagerImpl.

  /**
   * <p>Copies the identity agreement file for the AU to the given
   * stream.</p>
   * @param hRep A {@link HistoryRepository} to use.
   * @param out An output stream.
   * @throws IOException if input or output fails.
   */
  public synchronized void writeTo(HistoryRepository hRep,
				   OutputStream out) throws IOException {
    File file = hRep.getIdentityAgreementFile();
    InputStream in = new BufferedInputStream(new FileInputStream(file));
    try {
      StreamUtil.copy(in, out);
    } finally {
      IOUtil.safeClose(in);
    }
  }

  /**
   * <p>If there are no agreements, deserialize the contents of the
   * stream as the identity agreement file for the AU. Otherwise,
   * ignore the contents of the file.</p>
   * @param hRep A {@link HistoryRepository} to use.
   * @param idMgr A {@link IdentityManager} to translate {@link
   * String}s to {@link PeerIdentity} instances.
   * @param in An input stream to read from.
   */
  public synchronized void readFrom(HistoryRepository hRep,
				    IdentityManager idMgr,
				    InputStream in) throws IOException {
    if (haveAgreements()) {
      ArchivalUnit au = hRep.loadAuState().getArchivalUnit();
      log.debug("Ignoring request to restore "+au);
    } else {
      File file = hRep.getIdentityAgreementFile();
      OutputStream out = new FileOutputStream(file);
      boolean copyFinished = false;
      try  {
	StreamUtil.copy(in, out);
	copyFinished = true;
	// If the copy fails, we remain empty.  The file may be
	// corrupt and unusable, but if the copy failed, we don't
	// detect that here.
	loadFrom(getRawAgreements(hRep), idMgr);
      } finally {
	if (! copyFinished) {
	  ArchivalUnit au = hRep.loadAuState().getArchivalUnit();
	  log.debug("Copy failed restoring "+au);
	}
	IOUtil.safeClose(out);
      }
    }
  }

 public AuAgreementsBean getBean(String auId) {
    HashMap<String, PeerAgreements> rawMap = new HashMap<>();
    for (Map.Entry<PeerIdentity,PeerAgreements> ent : map.entrySet()) {
        rawMap.put(ent.getKey().getIdString(), ent.getValue());
    }
    AuAgreementsBean res = new AuAgreementsBean(auId, rawMap);
    return res;
  }

  /**
   * @param pid a {@link PeerIdentity}.
   * @return The {@link PeerAgreements} or {@code null} if no agreement
   * exists.
   */
  private PeerAgreements getPeerAgreements(PeerIdentity pid) {
    return map.get(pid);
  }

  /**
   * @param pid a {@link PeerIdentity}.
   * @return The {@link PeerAgreements} -- perhaps newly-created -- for the
   * pid.
   */
  private PeerAgreements findPeerAgreements(PeerIdentity pid) {
    PeerAgreements peerAgreements = getPeerAgreements(pid);
    if (peerAgreements == null) {
      peerAgreements = new PeerAgreements(pid);
      map.put(pid, peerAgreements);
    }
    return peerAgreements;
  }

  /**
   * Find or create a PeerAgreement for the {@link PeerIdentity}.
   *
   * @param pid A {@link PeerIdentity}.
   * @param type The {@link AgreementType} to record.
   * @return A {@link PeerAgreement}, either the one already existing
   * or {@link PeerAgreement#NO_AGREEMENT}. Never {@code null}.
   */
  public synchronized PeerAgreement findPeerAgreement(PeerIdentity pid,
						      AgreementType type) {
    return findPeerAgreement0(pid, type);
  }

  /**
   * Find or create a PeerAgreement for the {@link PeerIdentity}.
   * Should only be called with this instance synchronized.
   * @param pid A PeerIdentity.
   * @param type The {@link AgreementType} to record.
   * @return A {@link PeerAgreement}, either the one already existing
   * or {@link PeerAgreement#NO_AGREEMENT}. Never {@code null}.
   */
  private PeerAgreement findPeerAgreement0(PeerIdentity pid,
					   AgreementType type) {
    PeerAgreements peerAgreements = getPeerAgreements(pid);
    if (peerAgreements == null) {
      return PeerAgreement.NO_AGREEMENT;
    }
    return peerAgreements.getPeerAgreement(type);
  }

  /**
   * Record the agreement hint we received from one of our votes in a
   * V3 poll on this AU.
   *
   * @param pid
   * @param type The {@link AgreementType} to record.
   * @param percent
   */
  public synchronized void signalPartialAgreement(PeerIdentity pid,
						  AgreementType type,
						  float percent, long time) {
    PeerAgreements peerAgreements = findPeerAgreements(pid);
    peerAgreements.signalAgreement(type, percent, time);
  }

  /**
   * @param pid A {@link PeerIdentity} instance.
   * @param minPercentPartialAgreement The threshold below which we
   * will return {@code false}.
   * @return {@code true} iff the peer has agreed with us at or above
   * the requested threshold on a POR poll.
   */
  public synchronized boolean hasAgreed(PeerIdentity pid,
					float threshold) {
    PeerAgreements peerAgreements = findPeerAgreements(pid);
    PeerAgreement peerAgreement =
      peerAgreements.getPeerAgreement(AgreementType.POR);
    return peerAgreement.getHighestPercentAgreement() >= threshold;
  }

  /**
   * Return a mapping for each peer for which we have an agreement of
   * the requested type to the {@link PeerAgreement} record for that
   * peer.
   *
   * @param type The {@link AgreementType} to look for.
   * @return A Map mapping each {@link PeerIdentity} which has an
   * agreement of the requested type to the {@link PeerAgreement} for
   * that type. A peer with some agreements but none of the requested
   * type will not have an entry in the returned map.
   */
  public synchronized Map<PeerIdentity, PeerAgreement>
    getAgreements(AgreementType type) {
    Map<PeerIdentity, PeerAgreement> agreements = new HashMap();
    for (Map.Entry<PeerIdentity, PeerAgreements> ent: map.entrySet()) {
      PeerAgreements peerAgreements = ent.getValue();
      PeerAgreement peerAgreement = peerAgreements.getPeerAgreement(type);
      if (peerAgreement != PeerAgreement.NO_AGREEMENT) {
	PeerIdentity pid = ent.getKey();
	agreements.put(pid, peerAgreement);
      }
    }
    return agreements;
  }

  /**
   * Count the number of peers with at least threshold agreement of the
   * specified type.
   * @param type The {@link AgreementType} to look for.
   * @param threshold The minimum agreement value to count
   * @return count of peers with at least specified agreement of specified
   * type.
   */
  public synchronized int countAgreements(AgreementType type,
					  float threshold) {
    int res = 0;
    for (Map.Entry<PeerIdentity, PeerAgreements> ent: map.entrySet()) {
      PeerAgreements peerAgreements = ent.getValue();
      PeerAgreement peerAgreement = peerAgreements.getPeerAgreement(type);
      if (peerAgreement != PeerAgreement.NO_AGREEMENT &&
	  (peerAgreement.getHighestPercentAgreement() >= threshold)) {
	res++;
      }
    }
    return res;
  }
}
