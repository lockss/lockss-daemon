/*
 * $Id$
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

/**
 * Tests that the mapping files allow for an object to be marshalled
 * and unmarshalled
 */


package org.lockss.state;

import java.util.*;

public class TestPollMapping extends TestCastor {

  String mapFile = "/org/lockss/state/pollmapping.xml";

  public void testVoteBean() throws Exception {
    for (int ix=0; ix<5; ix++) {
      VoteBean vb1 = makeVoteBean(ix);

      VoteBean vb2 = (VoteBean)marshalAndUnmarshal(vb1, mapFile);
      assertNotSame(vb1, vb2);
      assertEquals(vb1, vb2);
    }
  }
  // methods to make an instance of a specific bean.

  private VoteBean makeVoteBean(int num) {
    VoteBean vb = new VoteBean();
    vb.setId("1234"+num);
    vb.setAgreeState(true);
    vb.setChallengeString("This is a challenge"+num);
    vb.setVerifierString("This is a verifier"+num);
    vb.setHashString("This is a hash"+num);
    return vb;
  }

  private PollStateBean makePollStateBean(int num) {
    PollStateBean psb = new PollStateBean();
    psb.setType(7+num);
    psb.setLwrBound("123"+num);
    psb.setUprBound("890"+num);
    psb.setStatus(8+num);
    psb.setStartTime(100+num);
    psb.setDeadlineTime(150+num);
    psb.setOurPoll(true);
    return psb;
  }

  private NodeStateBean makeNodeStateBean(int num) {
    NodeStateBean nsb = new NodeStateBean();
    nsb.setState(7+num);
    nsb.setAverageHashDuration(890+num);
    nsb.setCrawlStateBean(makeCrawlStateBean(num+5));

    List pbColl = new ArrayList();
    for (int ix=10; ix<(11+num); ix++) {
      PollStateBean pb = makePollStateBean(ix);
      pbColl.add(pb);
    }
    nsb.setPollBeans(pbColl);

    return nsb;
  }

  private PollHistoryBean makePollHistoryBean(int num) {
    PollHistoryBean phb = new PollHistoryBean();
    phb.setType(7+num);
    phb.setLwrBound("123"+num);
    phb.setUprBound("890"+num);
    phb.setStatus(8+num);
    phb.setStartTime(100+num);
    phb.setDuration(150+num);
    phb.setOurPoll(true);

    Collection vbColl = new ArrayList();
    for (int ix=10; ix<(11+num); ix++) {
      VoteBean vb = makeVoteBean(ix);
      vbColl.add(vb);
    }
    phb.setVoteBeans(vbColl);
    return phb;
  }

  private NodeHistoryBean makeNodeHistoryBean(int num) {
    NodeHistoryBean nhb = new NodeHistoryBean();

    List phbColl = new ArrayList();
    for (int ix=10; ix<(11+num); ix++) {
      PollHistoryBean phb = makePollHistoryBean(ix);
      phbColl.add(phb);
    }
    nhb.setHistoryBeans(phbColl);
    return nhb;
  }

  public void testPollHistoryBean() throws Exception {
    for (int ix=0; ix<5; ix++) {
      PollHistoryBean phb1 = makePollHistoryBean(ix);

      PollHistoryBean phb2 =
	(PollHistoryBean)marshalAndUnmarshal(phb1, mapFile);
      assertNotSame(phb1, phb2);
      assertEquals(phb1, phb2);
    }
  }

  public void testPollStateBean() throws Exception {
    for (int ix=0; ix<5; ix++) {
      PollStateBean psb1 = makePollStateBean(ix);

      PollStateBean psb2 =
	(PollStateBean)marshalAndUnmarshal(psb1, mapFile);
      assertNotSame(psb1, psb2);
      assertEquals(psb1, psb2);
    }
  }

  public void testNodeStateBean() throws Exception {
    for (int ix=0; ix<5; ix++) {
      NodeStateBean nsb1 = makeNodeStateBean(ix);

      NodeStateBean nsb2 =
	(NodeStateBean)marshalAndUnmarshal(nsb1, mapFile);
      assertNotSame(nsb1, nsb2);
      assertEquals(nsb1, nsb2);
    }
  }

  public void testNodeHistoryBean() throws Exception {
    for (int ix=0; ix<5; ix++) {
      NodeHistoryBean nhb1 = makeNodeHistoryBean(ix);

      NodeHistoryBean nhb2 =
	(NodeHistoryBean)marshalAndUnmarshal(nhb1, mapFile);
      assertNotSame(nhb1, nhb2);
      assertEquals(nhb1, nhb2);
    }
  }

  public void testAuStateBean() throws Exception {
    AuStateBean ausb1 = makeAuStateBean(1);
    AuStateBean ausb2 = (AuStateBean)marshalAndUnmarshal(ausb1, mapFile);
    assertNotSame(ausb1, ausb2);
    assertEquals(ausb1, ausb2);
  }

  private AuStateBean makeAuStateBean(int num) {
    AuStateBean ausb = new AuStateBean();
    ausb.setLastCrawlTime(100+num);
    ausb.setLastTopLevelPollTime(200+num);

    HashSet urls = new HashSet();
    for (int ix=0; ix<5+num; ix++) {
      urls.add("http://www.example.com/"+num);
    }

    ausb.setCrawlUrls(urls);
    return ausb;
  }

  public void testCrawlStateBean() throws Exception {
    CrawlStateBean bean1 = makeCrawlStateBean(1);
    CrawlStateBean bean2 = (CrawlStateBean)marshalAndUnmarshal(bean1, mapFile);
    assertNotSame(bean1, bean2);
    assertEquals(bean1, bean2);
  }

  private CrawlStateBean makeCrawlStateBean(int num) {
    CrawlStateBean bean = new CrawlStateBean();
    bean.setType(1+num);
    bean.setStatus(2+num);
    bean.setStartTime(200+num);
    return bean;
  }
  //VoteBean, PollHistoryBean, NodeHistoryBean don't have equals() methods

  private void assertEquals(AuStateBean ausb1, AuStateBean ausb2) {
    String errStr =
      "AuStateBean not equal:\n" +
      "\tausb1: "+ausb1+"\n"+
      "\tausb2: "+ausb2+"\n";
    assertEquals(errStr, ausb1.getLastCrawlTime(), ausb2.getLastCrawlTime());
    assertEquals(errStr, ausb1.getLastTopLevelPollTime(),
		 ausb2.getLastTopLevelPollTime());
    assertEquals(errStr, ausb1.getCrawlUrls(), ausb2.getCrawlUrls());
  }

  private void assertEquals(CrawlStateBean bean1, CrawlStateBean bean2) {
    assertEquals(null, bean1, bean2);
  }
  private void assertEquals(String msg,
			    CrawlStateBean bean1, CrawlStateBean bean2) {
    String errStr =
      (msg == null) ? msg :
      "CrawlStateBean not equal:\n" +
      "\tbean1: "+bean1+"\n"+
      "\tbean2: "+bean2+"\n";
    assertEquals(errStr, bean1.getStatus(), bean2.getStatus());
    assertEquals(errStr, bean1.getType(), bean2.getType());
    assertEquals(errStr, bean1.getStartTime(), bean2.getStartTime());
  }

  private void assertEquals(VoteBean vb1, VoteBean vb2) {
    String errStr =
      "VoteBeans not equal:\n" +
      "\tvb1: "+vb1+"\n"+
      "\tvb2: "+vb2+"\n";
    assertEquals(errStr, vb1.getId(), vb2.getId());
    assertEquals(errStr, vb1.getAgreeState(), vb2.getAgreeState());
    assertEquals(errStr, vb1.getChallengeString(), vb2.getChallengeString());
    assertEquals(errStr, vb1.getVerifierString(), vb2.getVerifierString());
    assertEquals(errStr, vb1.getHashString(), vb2.getHashString());
  }

  private void assertEquals(PollHistoryBean phb1, PollHistoryBean phb2) {
    String errStr =
      "PollHistoryBeans not equal:\n" +
      "\tphb1: "+phb1+"\n"+
      "\tphb2: "+phb2+"\n";
    assertEquals(errStr, phb1.getType(), phb2.getType());
    assertEquals(errStr, phb1.getLwrBound(), phb2.getLwrBound());
    assertEquals(errStr, phb1.getUprBound(), phb2.getUprBound());
    assertEquals(errStr, phb1.getStatus(), phb2.getStatus());
    assertEquals(errStr, phb1.getStartTime(), phb2.getStartTime());
    assertEquals(errStr, phb1.getDuration(), phb2.getDuration());
    assertEquals(errStr, phb1.ourPoll, phb2.ourPoll);

    Object[] coll1 = phb1.getVoteBeans().toArray();
    Object[] coll2 = phb2.getVoteBeans().toArray();
    assertEquals(errStr, coll1.length, coll2.length);
    for (int ix=0; ix<coll1.length; ix++) {
      assertEquals((VoteBean)coll1[ix], (VoteBean)coll2[ix]);
    }

  }

  private void assertEquals(NodeHistoryBean nhb1, NodeHistoryBean nhb2) {
    String errStr =
      "NodeHistoryBeans not equal:\n" +
      "\tnhb1: "+nhb1+"\n"+
      "\tnhb2: "+nhb2+"\n";

    Object[] coll1 = nhb1.getHistoryBeans().toArray();
    Object[] coll2 = nhb2.getHistoryBeans().toArray();
    assertEquals(errStr, coll1.length, coll2.length);
    for (int ix=0; ix<coll1.length; ix++) {
      assertEquals((PollHistoryBean)coll1[ix], (PollHistoryBean)coll2[ix]);
    }
  }

  private void assertEquals(NodeStateBean nsb1, NodeStateBean nsb2) {
    String errStr =
      "NodeStateBeans not equal:\n" +
      "\tnsb1: "+nsb1+"\n"+
      "\tnsb2: "+nsb2+"\n";

    assertEquals(errStr, nsb1.getState(), nsb2.getState());
    assertEquals(errStr,
		 nsb1.getAverageHashDuration(), nsb2.getAverageHashDuration());
    assertEquals(errStr, nsb1.getCrawlStateBean(), nsb2.getCrawlStateBean());
    assertEquals(errStr, nsb1.getPollBeans(), nsb2.getPollBeans());
    Object[] coll1 = nsb1.getPollBeans().toArray();
    Object[] coll2 = nsb2.getPollBeans().toArray();
    assertEquals(errStr, coll1.length, coll2.length);
    for (int ix=0; ix<coll1.length; ix++) {
      assertEquals((PollStateBean)coll1[ix], (PollStateBean)coll2[ix]);
    }
  }


  /*
todo
        <class name="org.lockss.state.DamagedNodeSet">
                <map-to xml="DamagedNodeSet"/>
                <field name="damagedNodes"
                       type="java.util.Set"
                       collection="set" lazy="true">
                </field>
                <field name="repairNodeBean"
                       type="org.lockss.util.ExtMapBean">
                        <bind-xml name="RepairMap" node="element"/>
                </field>
        </class>
  */
}
