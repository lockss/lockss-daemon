/*
 * $Id: TestAuAgreements.java,v 1.1 2013-08-19 22:33:22 barry409 Exp $
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

import org.lockss.test.*;
import org.lockss.util.*;

import java.util.*;


public class TestAuAgreements extends LockssTestCase {

  List<MockPeerIdentity> peerIdentityList;
  MockIdentityManager idMgr;
  MockHistoryRepository hRep;

  public void setUp() {
    peerIdentityList = ListUtil.list(new MockPeerIdentity("id1"),
				     new MockPeerIdentity("id2"),
				     new MockPeerIdentity("id3"));
    idMgr = new MockIdentityManager();
    for (int i = 0; i < peerIdentityList.size(); i++) {
      PeerIdentity pid = peerIdentityList.get(i);
      idMgr.addPeerIdentity(pid.getIdString(), pid);
    }

    hRep = new MockHistoryRepository();
  }

  public void testCreateInitial() throws Exception {
    AuAgreements auAgreements;

    // hRep.loadIdentityAgreements will return null.
    hRep.setLoadedIdentityAgreement((AuAgreements)null);
    auAgreements = AuAgreements.makeUnloaded();
    synchronized (auAgreements.getLoadLock()) {
      assertTrue(auAgreements.isLoadNeeded());
      auAgreements.load(hRep, idMgr);
    }

    hRep.setLoadedIdentityAgreement(Collections.EMPTY_LIST);
    auAgreements = AuAgreements.makeUnloaded();
    synchronized (auAgreements.getLoadLock()) {
      assertTrue(auAgreements.isLoadNeeded());
      auAgreements.load(hRep, idMgr);
    }
  }

  public void testSignalPartialAgreement() {
    AuAgreements auAgreements;
    PeerIdentity pid = peerIdentityList.get(0);
    auAgreements = AuAgreements.makeUnloaded();

    int i = 0;
    for (AgreementType type: AgreementType.values()) {
      auAgreements.signalPartialAgreement(pid, type, (50.0f+i)/100, 100+i);
      auAgreements.signalPartialAgreement(pid, type, (40.0f+i)/100, 200+i);
      i++;
    }

    i = 0;
    for (AgreementType type: AgreementType.values()) {
      PeerAgreement agreement = auAgreements.findPeerAgreement(pid, type);
      assertEquals((40.0f+i)/100, agreement.getPercentAgreement());
      assertEquals(200+i, agreement.getPercentAgreementTime());
      assertEquals((50.0f+i)/100, agreement.getHighestPercentAgreement());
      assertEquals(100+i, agreement.getHighestPercentAgreementTime());
      i++;
    }
  }

  public void testLoadWithList() throws Exception {
    // Create pre-1.62 List agreements.
    List<IdentityManager.IdentityAgreement> identityAgreementList =
      new ArrayList();
    for (int i = 0; i < peerIdentityList.size(); i++) {
      PeerIdentity pid = peerIdentityList.get(i);

      IdentityManager.IdentityAgreement idAgreement =
	new IdentityManager.IdentityAgreement(pid.getIdString());
      idAgreement.setLastAgree(1000+i);
      idAgreement.setLastDisagree(2000+i);
      // call twice with a lower value than the first time, to make
      // the highest and the most recent different.
      idAgreement.setPercentAgreement((50.0f+i)/100);
      idAgreement.setPercentAgreement((40.0f+i)/100);
      idAgreement.setPercentAgreementHint((30.0f+i)/100);
      idAgreement.setPercentAgreementHint((20.0f+i)/100);
      identityAgreementList.add(idAgreement);
    }

    // Tell hRep to use the old-style list.
    hRep.setLoadedIdentityAgreement(identityAgreementList);

    // Load the List
    AuAgreements auAgreements = AuAgreements.makeUnloaded();
    synchronized (auAgreements.getLoadLock()) {
      assertTrue(auAgreements.isLoadNeeded());
      auAgreements.load(hRep, idMgr);
    }

    for (int i = 0; i < peerIdentityList.size(); i++) {
      PeerIdentity pid = peerIdentityList.get(i);
      IdentityManager.IdentityAgreement expected = identityAgreementList.get(i);
      PeerAgreement porAgreement = 
	auAgreements.findPeerAgreement(pid, AgreementType.POR);
      PeerAgreement hintAgreement = 
	auAgreements.findPeerAgreement(pid, AgreementType.POR_HINT);
      assertEquals(expected.getPercentAgreement(),
		   porAgreement.getPercentAgreement());
      assertEquals(expected.getHighestPercentAgreement(),
		   porAgreement.getHighestPercentAgreement());
      assertEquals(expected.getPercentAgreementHint(),
		   hintAgreement.getPercentAgreement());
      assertEquals(expected.getHighestPercentAgreementHint(),
		   hintAgreement.getHighestPercentAgreement());
    }
  }

  public void testLoadWithListV1() throws Exception {
    // Create pre-1.62 List agreements, but make the 
    peerIdentityList = ListUtil.list(new MockPeerIdentity("id1"),
				     new MockPeerIdentity("id2") {
	@Override public boolean isV3() {return false;}
      },
				     new MockPeerIdentity("id3"));
    for (int i = 0; i < peerIdentityList.size(); i++) {
      PeerIdentity pid = peerIdentityList.get(i);
      idMgr.addPeerIdentity(pid.getIdString(), pid);
    }

    List<IdentityManager.IdentityAgreement> identityAgreementList =
      new ArrayList();
    for (int i = 0; i < peerIdentityList.size(); i++) {
      PeerIdentity pid = peerIdentityList.get(i);

      IdentityManager.IdentityAgreement idAgreement =
	new IdentityManager.IdentityAgreement(pid.getIdString());
      idAgreement.setLastAgree(1000+i);
      idAgreement.setLastDisagree(2000+i);
      // call twice with a lower value than the first time, to make
      // the highest and the most recent different.
      idAgreement.setPercentAgreement((50.0f+i)/100);
      idAgreement.setPercentAgreement((40.0f+i)/100);
      idAgreement.setPercentAgreementHint((30.0f+i)/100);
      idAgreement.setPercentAgreementHint((20.0f+i)/100);
      identityAgreementList.add(idAgreement);
    }

    // Tell hRep to use the old-style list.
    hRep.setLoadedIdentityAgreement(identityAgreementList);

    // Load the List
    AuAgreements auAgreements = AuAgreements.makeUnloaded();
    synchronized (auAgreements.getLoadLock()) {
      assertTrue(auAgreements.isLoadNeeded());
      auAgreements.load(hRep, idMgr);
    }

    for (int i = 0; i < peerIdentityList.size(); i++) {
      if (i == 1) {
	// The V1 identity didn't get loaded.
	PeerIdentity pid = peerIdentityList.get(i);
	PeerAgreement porAgreement = 
	  auAgreements.findPeerAgreement(pid, AgreementType.POR);
	assertEquals(PeerAgreement.NO_AGREEMENT, porAgreement);
	PeerAgreement hintAgreement = 
	  auAgreements.findPeerAgreement(pid, AgreementType.POR_HINT);
	assertEquals(PeerAgreement.NO_AGREEMENT, hintAgreement);
      } else {
	PeerIdentity pid = peerIdentityList.get(i);
	IdentityManager.IdentityAgreement expected =
	  identityAgreementList.get(i);
	PeerAgreement porAgreement = 
	  auAgreements.findPeerAgreement(pid, AgreementType.POR);
	PeerAgreement hintAgreement = 
	  auAgreements.findPeerAgreement(pid, AgreementType.POR_HINT);
	assertEquals(expected.getPercentAgreement(),
		     porAgreement.getPercentAgreement());
	assertEquals(expected.getHighestPercentAgreement(),
		     porAgreement.getHighestPercentAgreement());
	assertEquals(expected.getPercentAgreementHint(),
		     hintAgreement.getPercentAgreement());
	assertEquals(expected.getHighestPercentAgreementHint(),
		     hintAgreement.getHighestPercentAgreement());
      }
    }
  }

  public void testLoadWithAuAgreements() throws Exception {
    // Create AuAgreements object and put some agreements in it.
    AuAgreements auAgreements = AuAgreements.makeUnloaded();
    signalPartialAgreements(auAgreements, AgreementType.POR,
			    50.0f, 100);
    signalPartialAgreements(auAgreements, AgreementType.POR,
			    40.0f, 200);

    checkPercentAgreements(auAgreements, AgreementType.POR,
			   40.0f, 200);
    checkHighestPercentAgreements(auAgreements, AgreementType.POR,
				  50.0f, 100);

    // Make auAgreements store.
    auAgreements.store(hRep);
    // Tell hRep to use the stored instance to load.
    hRep.setLoadedIdentityAgreement(
      (AuAgreements)hRep.getStoredIdentityAgreement());

    // Load the saved AuAgreements into a new instance.
    auAgreements = AuAgreements.makeUnloaded();
    synchronized (auAgreements.getLoadLock()) {
      assertTrue(auAgreements.isLoadNeeded());
      auAgreements.load(hRep, idMgr);
    }

    // Check that they are as expected.
    checkPercentAgreements(auAgreements, AgreementType.POR,
			   40.0f, 200);
    checkHighestPercentAgreements(auAgreements, AgreementType.POR,
				  50.0f, 100);
  }

  // Merging when we have no agreements should indeed merge.
  public void testMergeWithAuAgreementsEmpty() throws Exception {
    // Create AuAgreements object and put some agreements in it.
    AuAgreements auAgreements = AuAgreements.makeUnloaded();
    signalPartialAgreements(auAgreements, AgreementType.POR,
			    50.0f, 100);
    signalPartialAgreements(auAgreements, AgreementType.POR,
			    40.0f, 200);

    checkPercentAgreements(auAgreements, AgreementType.POR,
			   40.0f, 200);
    checkHighestPercentAgreements(auAgreements, AgreementType.POR,
				  50.0f, 100);

    // Make auAgreements store.
    auAgreements.store(hRep);
    // Tell hRep to use the stored instance to load.
    hRep.setLoadedIdentityAgreement(
      (AuAgreements)hRep.getStoredIdentityAgreement());

    // Merge the saved AuAgreements into a new instance.
    auAgreements = AuAgreements.makeUnloaded();
    synchronized (auAgreements.getLoadLock()) {
      assertTrue(auAgreements.isLoadNeeded());
      auAgreements.loadAndMerge(hRep, idMgr);
    }

    checkPercentAgreements(auAgreements, AgreementType.POR,
			   40.0f, 200);
    checkHighestPercentAgreements(auAgreements, AgreementType.POR,
				  50.0f, 100);
  }

  // Merging when we have agreements should not merge.
  public void testMergeWithAuAgreementsNonempty() throws Exception {
    MockHistoryRepository hRep = new MockHistoryRepository();

    // Create AuAgreements object and signal some POR agreements
    AuAgreements auAgreementsPOR = AuAgreements.makeUnloaded();
    signalPartialAgreements(auAgreementsPOR, AgreementType.POR,
			    50.0f, 100);

    // Make a AuAgreements with POR_HINT values
    AuAgreements peerAgreementHINT = AuAgreements.makeUnloaded();

    signalPartialAgreements(peerAgreementHINT, AgreementType.POR_HINT,
			    10.0f, 300);

    // Make auAgreementsPOR store.
    auAgreementsPOR.store(hRep);
    // Tell hRep to use the stored instance to load.
    hRep.setLoadedIdentityAgreement(
      (AuAgreements)hRep.getStoredIdentityAgreement());

    // Merge the saved AuAgreements into a new instance.
    peerAgreementHINT.forceReload();
    synchronized (peerAgreementHINT.getLoadLock()) {
      assertTrue(peerAgreementHINT.isLoadNeeded());
      peerAgreementHINT.loadAndMerge(hRep, idMgr);
    }

    checkPercentAgreements(peerAgreementHINT, AgreementType.POR_HINT,
			   10.0f, 300);
    // And nothing recorded for any POR, because the file was ignored.
    checkAgreementsMissing(peerAgreementHINT, AgreementType.POR);
  }

  public void testHasAgreed() {
    // Create AuAgreements object empty.
    AuAgreements auAgreements = AuAgreements.makeUnloaded();
    PeerIdentity pid = peerIdentityList.get(0);
    assertFalse(auAgreements.hasAgreed(pid, 0.0f));
    // in fact, its value is -1.0f
    assertFalse(auAgreements.hasAgreed(pid, -0.5f));

    // signal 0.0, and anything at or under is true, above false.
    auAgreements.signalPartialAgreement(pid, AgreementType.POR,
					  0.0f, 10);
    assertTrue(auAgreements.hasAgreed(pid, 0.0f));
    assertFalse(auAgreements.hasAgreed(pid, 0.00001f));

    // signal 0.5, and anything at or under is true, above false.
    auAgreements.signalPartialAgreement(pid, AgreementType.POR,
					  0.5f, 10);
    assertTrue(auAgreements.hasAgreed(pid, 0.3f));
    assertTrue(auAgreements.hasAgreed(pid, 0.5f));
    assertFalse(auAgreements.hasAgreed(pid, 0.500001f));
  }

  // Signal a sterotyped set of partial agreements
  private void signalPartialAgreements(AuAgreements auAgreements,
				       AgreementType type,
				       float percent, long time) {

    for (int i = 0; i < peerIdentityList.size(); i++) {
      PeerIdentity pid = peerIdentityList.get(i);
      auAgreements.
	signalPartialAgreement(pid, type, (percent+i)/100, time+i);
    }
  }
  
  // Check the sterotyped set of percent agreements
  private void checkPercentAgreements(AuAgreements auAgreements,
				      AgreementType type,
				      float percent, long time) {
    for (int i = 0; i < peerIdentityList.size(); i++) {
      PeerIdentity pid = peerIdentityList.get(i);
      PeerAgreement peerAgreement = 
	auAgreements.findPeerAgreement(pid, type);
      assertEquals((percent+i)/100, peerAgreement.getPercentAgreement());
      assertEquals(time+i, peerAgreement.getPercentAgreementTime());
    }
  }
  
  // Check the sterotyped set of highest percent agreements
  private void checkHighestPercentAgreements(AuAgreements auAgreements,
					     AgreementType type,
					     float percent, long time) {
    for (int i = 0; i < peerIdentityList.size(); i++) {
      PeerIdentity pid = peerIdentityList.get(i);
      PeerAgreement peerAgreement = 
	auAgreements.findPeerAgreement(pid, type);
      assertEquals((percent+i)/100, peerAgreement.getHighestPercentAgreement());
      assertEquals(time+i, peerAgreement.getHighestPercentAgreementTime());
    }
  }

  // Check that there are no agreements of the given type
  private void checkAgreementsMissing(AuAgreements auAgreements,
				      AgreementType type) {
    for (int i = 0; i < peerIdentityList.size(); i++) {
      PeerIdentity pid = peerIdentityList.get(i);
      PeerAgreement peerAgreement = 
	auAgreements.findPeerAgreement(pid, type);
      assertEquals(PeerAgreement.NO_AGREEMENT, peerAgreement);
    }
  }
}
