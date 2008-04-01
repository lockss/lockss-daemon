/*
 * $Id: MockHistoryRepository.java,v 1.17 2008-04-01 08:03:49 tlipkis Exp $
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


package org.lockss.test;

import java.io.*;
import java.util.*;
import org.lockss.app.*;
import org.lockss.state.*;
import org.lockss.plugin.*;
import org.lockss.config.Configuration;

/**
 * MockHistoryRepository is a mock implementation of the HistoryRepository.
 */
public class MockHistoryRepository implements HistoryRepository {
  public HashMap storedHistories = new HashMap();
  public AuState theAuState;
  public DamagedNodeSet theDamagedNodeSet;
  public HashMap storedNodes = new HashMap();
  private File auStateFile = null;

  private List storedIdentityAgreement = null;
  private List loadedIdentityAgreement = null;

  private int timesStoreDamagedNodeSetCalled = 0;

  public MockHistoryRepository() { }

  public void initService(LockssApp app) throws LockssAppException { }
  public void startService() { }
  public void stopService() {
    storedHistories = new HashMap();
    theAuState = null;
    theDamagedNodeSet = null;
    storedNodes = new HashMap();
  }
  public LockssApp getApp() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void setAuConfig(Configuration auConfig) {
  }

  public void storePollHistories(NodeState nodeState) {
    storedHistories.put(nodeState.getCachedUrlSet(),
                        nodeState.getPollHistories());
  }

  /**
   * Doesn't function.
   * @param nodeState the NodeState
   */
  public void loadPollHistories(NodeState nodeState) {
  }

  public void storeAuState(AuState auState) {
    theAuState = auState;
  }

  public AuState loadAuState() {
    return theAuState;
  }

  public void storeDamagedNodeSet(DamagedNodeSet nodeSet) {
    timesStoreDamagedNodeSetCalled++;
    theDamagedNodeSet = nodeSet;
  }

  public int timesStoreDamagedNodeSetCalled() {
    return timesStoreDamagedNodeSetCalled;
  }

  public DamagedNodeSet loadDamagedNodeSet() {
    return theDamagedNodeSet;
  }


  public void storeNodeState(NodeState nodeState) {
    storedNodes.put(nodeState.getCachedUrlSet(), nodeState);
  }

  public NodeState loadNodeState(CachedUrlSet cus) {
    return (NodeState)storedNodes.get(cus);
  }

  public File getIdentityAgreementFile() {
    throw new UnsupportedOperationException();
  }

  public File getAuStateFile() {
    return auStateFile;
  }

  public long getAuCreationTime() {
    return -1;
  }

  public void setAuStateFile(File file) {
    auStateFile = file;
  }

  public void storeIdentityAgreements(List list) {
    this.storedIdentityAgreement = list;
  }

  public List loadIdentityAgreements() {
    return loadedIdentityAgreement;
  }

  public void setLoadedIdentityAgreement(List list) {
    this.loadedIdentityAgreement = list;
  }

  public List getStoredIdentityAgreement() {
    return storedIdentityAgreement;
  }

}
