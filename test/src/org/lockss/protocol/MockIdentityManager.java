/*
* $Id: MockIdentityManager.java,v 1.15 2007-10-03 00:35:53 smorabito Exp $
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
package org.lockss.protocol;

import java.util.*;
import java.io.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.protocol.IdentityManager.MalformedIdentityKeyException;

/**
 * Mock override of IdentityManager.
 */

public class MockIdentityManager implements IdentityManager {
  protected static Logger log = Logger.getLogger("MockIdentityManager");

  public HashMap idMap = new HashMap();

  public HashMap piMap = new HashMap();

  public HashMap repMap = new HashMap();

  public Map agreeMap = new HashMap();

  int maxRep = 5000;

  PeerIdentity localId = new MockPeerIdentity("fake peer id");

  public MockIdentityManager() {
    super();
  }

  public void initService(LockssApp daemon) throws LockssAppException {
//     throw new UnsupportedOperationException("not implemented");
  }

  public void startService() {
    throw new UnsupportedOperationException("not implemented");
  }

  public void stopService() {
//     throw new UnsupportedOperationException("not implemented");
  }


  public PeerIdentity findPeerIdentity(String key) {
    return (PeerIdentity)piMap.get(key);
  }
  
  public void removePeer(String key) {
    piMap.remove(key);
  }

  public PeerIdentity ipAddrToPeerIdentity(IPAddr addr, int port) {
    String key = ""+addr+port;
    return new MockPeerIdentity(key);
  }

  public PeerIdentity ipAddrToPeerIdentity(IPAddr addr) {
    throw new UnsupportedOperationException("not implemented");
  }

  public PeerIdentity stringToPeerIdentity(String idKey)
      throws IdentityManager.MalformedIdentityKeyException {
    return (PeerIdentity)piMap.get(idKey);
  }

  public void addPeerIdentity(String idKey, PeerIdentity pi) {
    piMap.put(idKey, pi);
  }

  public IPAddr identityToIPAddr(PeerIdentity pid) {
    throw new UnsupportedOperationException("not implemented");
  }

  public PeerIdentity getLocalPeerIdentity(int pollVersion) {
    return new MockPeerIdentity("127.0.0.1");
  }

  public IPAddr getLocalIPAddr() {
    throw new UnsupportedOperationException("not implemented");
  }

  public boolean isLocalIdentity(PeerIdentity id) {
    log.debug3("Checking if "+id+" is the local identity "+localId);
    return id == localId;
  }

  public void setLocalIdentity(PeerIdentity id) {
    log.debug3("Setting local identity to "+id);
    this.localId = id;
  }

  public boolean isLocalIdentity(String idStr) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void rememberEvent(PeerIdentity id, int event, LcapMessage msg) {
    throw new UnsupportedOperationException("not implemented");
  }

  public int getMaxReputation() {
    return maxRep;
  }
  
  public Map getIdentities() {
    return piMap;
  }

  public void setMaxReputation(int maxRep) {
    this.maxRep = maxRep;
  }

  public int getReputation(PeerIdentity id) {
    Integer rep = (Integer)repMap.get(id);
    if (rep == null) {
      return 0;
    }
    return rep.intValue();
  }

  public void setReputation(PeerIdentity id, int rep) {
    repMap.put(id, new Integer(rep));
  }


  /** @deprecated */
  public IdentityListBean getIdentityListBean() {
    throw new UnsupportedOperationException("not implemented");
  }

  public void signalAgreed(PeerIdentity pid, ArchivalUnit au) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void signalDisagreed(PeerIdentity pid, ArchivalUnit au) {
    throw new UnsupportedOperationException("not implemented");
  }
  
  public void signalPartialAgreement(PeerIdentity pid, ArchivalUnit au,
                                     float percentAgreement) {
    throw new UnsupportedOperationException("not implemented");
  }
  
  public float getPercentAgreement(PeerIdentity pid, ArchivalUnit au) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void setConfig(Configuration config, Configuration oldConfig,
			Configuration.Differences changedKeys) {
    throw new UnsupportedOperationException("not implemented");
  }

//   public void initService(LockssDaemon daemon) throws LockssAppException {
//     log.debug("MockIdentityManager: initService");
//     super.initService(daemon);
//   }

//   protected String getLocalIpParam(Configuration config) {
//     String res = config.get(PARAM_LOCAL_IP);
//     if (res == null) {
//       res = "127.7.7.7";
//     }
//     return res;
//   }

//   public void startService() {
//     log.debug("MockIdentityManager: startService");
//     super.startService();
//     idMap = new HashMap();
//   }
//   public void stopService() {
//     log.debug("MockIdentityManager: stopService");
//     super.stopService();
//     idMap = null;
//   }

  public void changeReputation(PeerIdentity id, int changeKind) {
    throw new UnsupportedOperationException("not implemented");
    //      idMap.put(id, new Integer(changeKind));
  }

  public void storeIdentities() throws ProtocolException {
    throw new UnsupportedOperationException("not implemented");
  }

  public void storeIdentities(ObjectSerializer serializer) {
    throw new UnsupportedOperationException("not implemented");
  }

  public LockssApp getApp() {
    throw new UnsupportedOperationException("not implemented");
  }


  public int lastChange(PeerIdentity id) {
    Integer change = (Integer)idMap.get(id);
    if (change == null) {
      return -1;
    }
    return change.intValue();
  }

//   public void signalAgreed(PeerIdentity id, ArchivalUnit au) {
//     throw new UnsupportedOperationException("not implemented");
//   }

//   public void signalDisagreed(PeerIdentity id, ArchivalUnit au) {
//     throw new UnsupportedOperationException("not implemented");
//   }

  public Map getAgreed(ArchivalUnit au) {
    return (Map)agreeMap.get(au);
  }

  public Map getDisagreed(ArchivalUnit au) {
    throw new UnsupportedOperationException("not implemented");
  }

  public List getCachesToRepairFrom(ArchivalUnit au) {
    Map map = getAgreed(au);
    if (map == null) return Collections.EMPTY_LIST;
    return new ArrayList(map.keySet());
  }

  public boolean hasAgreed(String ip, ArchivalUnit au)
      throws IdentityManager.MalformedIdentityKeyException {
    return hasAgreed(stringToPeerIdentity(ip), au);
  }

  public boolean hasAgreed(PeerIdentity pid, ArchivalUnit au) {
    Map map = getAgreed(au);
    if (map == null) return false;
    return map.containsKey(pid);
  }

  public boolean hasAgreeMap(ArchivalUnit au) {
    throw new UnsupportedOperationException("not implemented");
  }

  public Collection getIdentityAgreements(ArchivalUnit au) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void readIdentityAgreementFrom(ArchivalUnit au, InputStream in)
      throws IOException {
    throw new UnsupportedOperationException("not implemented");
  }

  public void writeIdentityAgreementTo(ArchivalUnit au, OutputStream out)
      throws IOException {
    throw new UnsupportedOperationException("not implemented");
  }

  public void writeIdentityDbTo(OutputStream out) throws IOException {
//     throw new UnsupportedOperationException("not implemented");
  }

   public void setAgeedForAu(ArchivalUnit au, Map map) {
     agreeMap.put(au, map);
   }

  //protected
//   protected IdentityManagerStatus makeStatusAccessor(Map theIdentities) {
//     throw new UnsupportedOperationException("not implemented");
//   }

//   protected LcapIdentity findLcapIdentity(PeerIdentity pid, String key) {
//     throw new UnsupportedOperationException("not implemented");
//   }

  protected LcapIdentity findLcapIdentity(PeerIdentity pid,
					  IPAddr addr, int port) {
    throw new UnsupportedOperationException("not implemented");
  }
  protected int getReputationDelta(int changeKind) {
    throw new UnsupportedOperationException("not implemented");
  }

  public Collection getUdpPeerIdentities() {
    throw new UnsupportedOperationException("not implemented");
  }

  public Collection getTcpPeerIdentities() {
    throw new UnsupportedOperationException("not implemented");
  }

  public LcapIdentity findLcapIdentity(PeerIdentity pid, String key) {
    throw new UnsupportedOperationException("not implemented");
  }

  public PeerIdentityStatus getPeerIdentityStatus(PeerIdentity pid) {
    throw new UnsupportedOperationException("not implemented");
  }

  public PeerIdentityStatus getPeerIdentityStatus(String key) {
    throw new UnsupportedOperationException("not implemented");
  }

  public float getHighestPercentAgreement(PeerIdentity pid, ArchivalUnit au) {
    // TODO Auto-generated method stub
    return 0.0f;
  }

//   protected String getLocalIpParam(Configuration config) {
//     throw new UnsupportedOperationException("not implemented");
//   }


}
