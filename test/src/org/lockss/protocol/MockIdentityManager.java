/*
* $Id: MockIdentityManager.java,v 1.1 2004-12-07 08:46:05 tlipkis Exp $
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
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.plugin.*;

/**
 * Mock override of IdentityManager.
 */
public class MockIdentityManager extends IdentityManager {
  public HashMap idMap = null;

  public Map agreeMap = new HashMap();

  public MockIdentityManager() {
    super();
  }

  public void initService(LockssDaemon daemon) throws LockssAppException {
    log.debug("MockIdentityManager: initService");
    super.initService(daemon);
  }

  protected String getLocalIpParam(Configuration config) {
    String res = config.get(PARAM_LOCAL_IP);
    if (res == null) {
      res = "127.7.7.7";
    }
    return res;
  }

  public void startService() {
    log.debug("MockIdentityManager: startService");
    super.startService();
    idMap = new HashMap();
  }
  public void stopService() {
    log.debug("MockIdentityManager: stopService");
    super.stopService();
    idMap = null;
  }

  public void changeReputation(PeerIdentity id, int changeKind) {
    idMap.put(id, new Integer(changeKind));
  }

  public int lastChange(PeerIdentity id) {
    Integer change = (Integer)idMap.get(id);
    if (change==null) {
      return -1;
    }
    return change.intValue();
  }

  public void signalAgreed(PeerIdentity id, ArchivalUnit au) {
    throw new UnsupportedOperationException("not implemented");
  }

  public void signalDisagreed(PeerIdentity id, ArchivalUnit au) {
    throw new UnsupportedOperationException("not implemented");
  }

  public Map getAgreed(ArchivalUnit au) {
    return (Map)agreeMap.get(au);
  }
  
  /**
   * getReputation returns the reputation of the peer
   * @param id the PeerIdentity
   * @return the reputation
   */
  public void setReputation(PeerIdentity peer, int reputation) {
    try {
      LcapIdentity lid = findLcapIdentity(peer, peer.getIdString());
      lid.changeReputation(reputation - lid.getReputation());
    } catch (IdentityManager.MalformedIdentityKeyException e) {
      throw new RuntimeException(e.toString());
    }
  }

  public void setAgeedForAu(ArchivalUnit au, Map map) {
    agreeMap.put(au, map);
  }

  

}
