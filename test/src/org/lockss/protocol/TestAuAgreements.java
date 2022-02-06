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

import org.lockss.test.*;
import org.lockss.util.*;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.*;


public class TestAuAgreements extends LockssTestCase {

  List<MockPeerIdentity> peerIdentityList;
  MockIdentityManager idMgr;
  MockHistoryRepository hRep;

  public void setUp() {
    peerIdentityList = ListUtil.list(new MockPeerIdentity("id0"),
				     new MockPeerIdentity("id1"),
				     new MockPeerIdentity("id2"));
    idMgr = new MockIdentityManager();
    for (int i = 0; i < peerIdentityList.size(); i++) {
      PeerIdentity pid = peerIdentityList.get(i);
      idMgr.addPeerIdentity(pid.getIdString(), pid);
    }

    hRep = new MockHistoryRepository();
  }

  public void testCreateEmpty() throws Exception {
    AuAgreements auAgreements;

    // hRep.loadIdentityAgreements will return null.
    hRep.setLoadedIdentityAgreement((AuAgreements)null);
    auAgreements = AuAgreements.make(hRep, idMgr);
    assertFalse(auAgreements.haveAgreements());

    hRep.setLoadedIdentityAgreement(Collections.EMPTY_LIST);
    auAgreements = AuAgreements.make(hRep, idMgr);
    assertFalse(auAgreements.haveAgreements());

    hRep = new MockHistoryRepository() {
	@Override public Object loadIdentityAgreements() {
	  return "should not work";
	}
      };
    try {
      AuAgreements.make(hRep, idMgr);
      fail();
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  public void testMakeWithAuAgreements() throws Exception {
    // Create an AuAgreements object and put some agreements in it.
    AuAgreements auAgreements = AuAgreements.make(hRep, idMgr);
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
    AuAgreements auAgreementsLoad = AuAgreements.make(hRep, idMgr);
    // Note: AuAgreements does not override equals(), so this is
    // Object.equals()
    // The instance is different
    assertNotEquals(auAgreements, auAgreementsLoad);

    assertTrue(auAgreements.haveAgreements());
    // Check that they are as expected.
    checkPercentAgreements(auAgreements, AgreementType.POR,
			   40.0f, 200);
    checkHighestPercentAgreements(auAgreements, AgreementType.POR,
				  50.0f, 100);
  }

  public void testMakeWithList() throws Exception {
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

    // make using the List
    AuAgreements auAgreements = AuAgreements.make(hRep, idMgr);

    // Check the values
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

  public void testMakeWithListV1() throws Exception {
    // Create pre-1.62 List agreements, but make one of the
    // PeerIdentities not-V3
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

    // make using the List
    AuAgreements auAgreements = AuAgreements.make(hRep, idMgr);

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

  public void testHaveAgreements() {
    AuAgreements auAgreements = AuAgreements.make(hRep, idMgr);

    assertFalse(auAgreements.haveAgreements());

    PeerIdentity pid = peerIdentityList.get(0);
    auAgreements.signalPartialAgreement(pid, AgreementType.POR_HINT, 0.5f, 100);

    assertTrue(auAgreements.haveAgreements());
  }

  public void testSignalPartialAgreement() {
    AuAgreements auAgreements;
    PeerIdentity pid = peerIdentityList.get(0);
    auAgreements = AuAgreements.make(hRep, idMgr);

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

  public void testStore() throws Exception {
    // Create an AuAgreements object and put some agreements in it.
    AuAgreements auAgreements = AuAgreements.make(hRep, idMgr);
    signalPartialAgreements(auAgreements, AgreementType.POR,
			    50.0f, 100);
    signalPartialAgreements(auAgreements, AgreementType.POR,
			    40.0f, 200);

    checkPercentAgreements(auAgreements, AgreementType.POR,
			   40.0f, 200);
    checkHighestPercentAgreements(auAgreements, AgreementType.POR,
				  50.0f, 100);

    assertEquals(null, hRep.getStoredIdentityAgreement());
    // Make auAgreements store into the Mock.
    auAgreements.store(hRep);
    assertEquals(auAgreements, hRep.getStoredIdentityAgreement());
  }

  public void testWriteTo() throws Exception {
    String content = "This is some content.";
    final File historyFile =
      FileTestUtil.writeTempFile("#id_agreement", "xml", content);
    MockHistoryRepository hRep = new MockHistoryRepository() {
	@Override public File getIdentityAgreementFile() {
	  return historyFile;
	}
      };

    AuAgreements auAgreements = AuAgreements.make(hRep, idMgr);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    auAgreements.writeTo(hRep, out);

    // The content was copied into the OutputStream.
    assertEquals(content, out.toString("UTF-8"));
  }

  public void testReadFrom() throws Exception {
    // NOTE: The mock doesn't actually deserialize the file content to
    // restore the agreements.
    String agreement = "this is an AuAgreements";
    final File historyFile = FileTestUtil.tempFile("#id_agreement", "xml");

    MockHistoryRepository hRep = new MockHistoryRepository() {
	@Override public File getIdentityAgreementFile() {
	  return historyFile;
	}
      };

    AuAgreements auAgreementsVal = AuAgreements.make(hRep, idMgr);
    signalPartialAgreements(auAgreementsVal, AgreementType.SYMMETRIC_POP,
			    10.0f, 300);
    
    // Make auAgreements before the History knows anything to load.
    AuAgreements auAgreements = AuAgreements.make(hRep, idMgr);
    checkAgreementsMissing(auAgreements, AgreementType.SYMMETRIC_POP);

    // Tell the History to use auAgreementsVal.
    auAgreementsVal.store(hRep);
    hRep.setLoadedIdentityAgreement(
      (AuAgreements)hRep.getStoredIdentityAgreement());

    // Do the readFrom
    auAgreements.readFrom(hRep, idMgr,
			  new ByteArrayInputStream(agreement.getBytes()));

    // The agreement was restored
    checkPercentAgreements(auAgreements, AgreementType.SYMMETRIC_POP,
			   10.0f, 300);
    // The file was copied.
    assertEquals(agreement.getBytes(),
		 FileUtils.readFileToByteArray(historyFile));
  }

  public void testReadFromNonempty() throws Exception {
    // NOTE: The mock doesn't actually deserialize the file content to
    // restore the agreements.
    String previousAgreement = "previous agreement";
    String agreement = "this is an AuAgreements";
    final File historyFile = FileTestUtil.writeTempFile("#id_agreement", "xml",
							previousAgreement);

    MockHistoryRepository hRep = new MockHistoryRepository() {
	@Override public File getIdentityAgreementFile() {
	  return historyFile;
	}

	@Override public MockAuState loadAuState() {
	  return new MockAuState(new MockArchivalUnit("Mock AU"));
	}
      };

    AuAgreements auAgreementsVal = AuAgreements.make(hRep, idMgr);
    signalPartialAgreements(auAgreementsVal, AgreementType.SYMMETRIC_POP,
			    10.0f, 300);
    
    // Make auAgreements before the History knows anything to load.
    AuAgreements auAgreements = AuAgreements.make(hRep, idMgr);
    checkAgreementsMissing(auAgreements, AgreementType.SYMMETRIC_POP);

    // Tell the History to use auAgreementsVal.
    auAgreementsVal.store(hRep);
    hRep.setLoadedIdentityAgreement(
      (AuAgreements)hRep.getStoredIdentityAgreement());

    // Put some content in auAgreements, then try the read.
    auAgreements.signalPartialAgreement(
      peerIdentityList.get(1), AgreementType.POR,
      0.5f, 100);
    auAgreements.readFrom(hRep, idMgr,
			  new ByteArrayInputStream(agreement.getBytes()));

    // The agreement was not restored
    checkAgreementsMissing(auAgreements, AgreementType.SYMMETRIC_POP);

    // The file was not copied.
    assertEquals(previousAgreement.getBytes(),
		 FileUtils.readFileToByteArray(historyFile));
  }

  public void testFindPeerAgreement() {
    AuAgreements auAgreements = AuAgreements.make(hRep, idMgr);
    PeerIdentity pid = peerIdentityList.get(0);

    PeerAgreement peerAgreement = 
      auAgreements.findPeerAgreement(pid, AgreementType.POR_HINT);
    assertEquals(PeerAgreement.NO_AGREEMENT, peerAgreement);

    auAgreements.signalPartialAgreement(pid, AgreementType.POR_HINT, 0.5f, 100);

    peerAgreement = auAgreements.findPeerAgreement(pid, AgreementType.POR_HINT);
    assertEquals(100, peerAgreement.getPercentAgreementTime());
  }

  public void testHasAgreed() {
    // Create AuAgreements object empty.
    AuAgreements auAgreements = AuAgreements.make(hRep, idMgr);
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

  public void testGetAgreements() {
    AuAgreements auAgreements = AuAgreements.make(hRep, idMgr);
    auAgreements.signalPartialAgreement(peerIdentityList.get(1),
					AgreementType.POR_HINT, 0.1f, 101);
    auAgreements.signalPartialAgreement(peerIdentityList.get(0),
					AgreementType.POR_HINT, 0.0f, 100);
    auAgreements.signalPartialAgreement(peerIdentityList.get(2),
					AgreementType.POR_HINT, 0.2f, 102);

    auAgreements.signalPartialAgreement(peerIdentityList.get(1),
					AgreementType.POR, 0.1f, 201);
    auAgreements.signalPartialAgreement(peerIdentityList.get(0),
					AgreementType.POR, 0.0f, 200);

    Map<PeerIdentity, PeerAgreement> map;
    map = auAgreements.getAgreements(AgreementType.POR_HINT);
    assertEquals(3, map.size());
    for (int i = 0; i < 3; i++) {
      PeerIdentity pid = peerIdentityList.get(i);
      PeerAgreement peerAgreement = map.get(pid);
      assertNotNull(peerAgreement);
      assertEquals(100+i, peerAgreement.getPercentAgreementTime());
    }
    assertEquals(3, auAgreements.countAgreements(AgreementType.POR_HINT, 0.0f));
    assertEquals(2, auAgreements.countAgreements(AgreementType.POR_HINT, 0.1f));
    assertEquals(1, auAgreements.countAgreements(AgreementType.POR_HINT, 0.2f));
    assertEquals(0, auAgreements.countAgreements(AgreementType.POR_HINT, 0.3f));

    map = auAgreements.getAgreements(AgreementType.POR);
    assertEquals(2, map.size());
    for (int i = 0; i < 2; i++) {
      PeerIdentity pid = peerIdentityList.get(i);
      PeerAgreement peerAgreement = map.get(pid);
      assertNotNull(peerAgreement);
      assertEquals(200+i, peerAgreement.getPercentAgreementTime());
    }
    assertEquals(2, auAgreements.countAgreements(AgreementType.POR, 0.0f));
    assertEquals(1, auAgreements.countAgreements(AgreementType.POR, 0.1f));
    assertEquals(0, auAgreements.countAgreements(AgreementType.POR, 0.2f));

    map = auAgreements.getAgreements(AgreementType.SYMMETRIC_POR);
    assertTrue(map.isEmpty());
  }

  public void testGetBean() {
    AuAgreements auAgreements = AuAgreements.make(hRep, idMgr);
    auAgreements.signalPartialAgreement(peerIdentityList.get(1),
					AgreementType.POR_HINT, 0.1f, 101);
    auAgreements.signalPartialAgreement(peerIdentityList.get(0),
					AgreementType.POR_HINT, 0.0f, 100);
    auAgreements.signalPartialAgreement(peerIdentityList.get(2),
					AgreementType.POR_HINT, 0.2f, 102);

    auAgreements.signalPartialAgreement(peerIdentityList.get(1),
					AgreementType.POR, 0.0f, 201);
    auAgreements.signalPartialAgreement(peerIdentityList.get(0),
					AgreementType.POR, 0.0f, 200);

    AuAgreementsBean bean = auAgreements.getBean("ididid");
    Map<String, PeerAgreements> beanMap = bean.getRawMap();

    assertEquals(3, beanMap.size());
    assertEquals(auAgreements.findPeerAgreement(peerIdentityList.get(0),
                                                AgreementType.POR_HINT),
                 beanMap.get("id0").getPeerAgreement(AgreementType.POR_HINT));
    assertEquals(auAgreements.findPeerAgreement(peerIdentityList.get(0),
                                                AgreementType.POR),
                 beanMap.get("id0").getPeerAgreement(AgreementType.POR));

    assertEquals(auAgreements.findPeerAgreement(peerIdentityList.get(1),
                                                AgreementType.POR_HINT),
                 beanMap.get("id1").getPeerAgreement(AgreementType.POR_HINT));
    assertEquals(auAgreements.findPeerAgreement(peerIdentityList.get(1),
                                                AgreementType.POR),
                 beanMap.get("id1").getPeerAgreement(AgreementType.POR));

    assertEquals(auAgreements.findPeerAgreement(peerIdentityList.get(2),
                                                AgreementType.POR),
                 beanMap.get("id2").getPeerAgreement(AgreementType.POR));
  }

  public void testGetPrunedBean() {
    AuAgreements auAgreements = AuAgreements.make(hRep, idMgr);
    auAgreements.signalPartialAgreement(peerIdentityList.get(1),
					AgreementType.POR_HINT, 0.1f, 101);
    auAgreements.signalPartialAgreement(peerIdentityList.get(0),
					AgreementType.POR_HINT, 0.0f, 100);
    auAgreements.signalPartialAgreement(peerIdentityList.get(2),
					AgreementType.POR_HINT, 0.2f, 102);

    auAgreements.signalPartialAgreement(peerIdentityList.get(1),
					AgreementType.POR, 0.0f, 201);
    auAgreements.signalPartialAgreement(peerIdentityList.get(0),
					AgreementType.POR, 0.0f, 200);

    AuAgreementsBean bean = auAgreements.getPrunedBean("ididid");
    Map<String, PeerAgreements> beanMap = bean.getRawMap();

    assertEquals(2, beanMap.size());
    assertNull(beanMap.get("id0"));
    assertEquals(auAgreements.findPeerAgreement(peerIdentityList.get(1),
                                                AgreementType.POR_HINT),
                 beanMap.get("id1").getPeerAgreement(AgreementType.POR_HINT));
    assertSame(PeerAgreement.NO_AGREEMENT,
               beanMap.get("id1").getPeerAgreement(AgreementType.POR));

    assertEquals(auAgreements.findPeerAgreement(peerIdentityList.get(2),
                                                AgreementType.POR),
                 beanMap.get("id2").getPeerAgreement(AgreementType.POR));
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
