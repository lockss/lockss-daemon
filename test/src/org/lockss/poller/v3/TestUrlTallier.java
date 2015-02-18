/*
 * $Id$
 */

/*

 Copyright (c) 2011-2012 Board of Trustees of Leland Stanford Jr. University,
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
import java.security.*;
import org.lockss.app.*;
import org.lockss.config.ConfigManager;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.protocol.IdentityManager.IdentityAgreement;
import org.lockss.protocol.psm.*;
import org.lockss.util.*;
import org.lockss.poller.*;
import org.lockss.poller.v3.FuncV3Poller.MyV3Poller;
import org.lockss.poller.v3.V3Serializer.*;
import org.lockss.test.*;
import org.lockss.hasher.*;
import org.lockss.repository.LockssRepositoryImpl;
import org.mortbay.util.B64Code;

import static org.lockss.util.Constants.*;


public class TestUrlTallier extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  private PeerIdentity pollerId;

  private String tempDirPath;
  private ArchivalUnit testau;

  private PeerIdentity[] voters;
  List<ParticipantUserData> participants;
  
  private String localPeerKey = "TCP:[127.0.0.1]:9729";
  
  private File tempDir;

  private static final String BASE_URL = "http://www.test.org/";
  

  // The number of random bytes in a pretend nonce
  private static final int NONCE_LENGTH = 20;

  private List initialPeers =
    ListUtil.list("TCP:[10.1.0.1]:9729", "TCP:[10.1.0.2]:9729",
                  "TCP:[10.1.0.3]:9729", "TCP:[10.1.0.4]:9729",
                  "TCP:[10.1.0.5]:9729", "TCP:[10.1.0.6]:9729");

  private static String[] urls = {
    "lockssau:",
    BASE_URL,
    BASE_URL + "index.html",
    BASE_URL + "file1.html",
    BASE_URL + "file2.html",
    BASE_URL + "branch1/",
    BASE_URL + "branch1/index.html",
    BASE_URL + "branch1/file1.html",
    BASE_URL + "branch1/file2.html",
    BASE_URL + "branch2/",
    BASE_URL + "branch2/index.html",
    BASE_URL + "branch2/file1.html",
    BASE_URL + "branch2/file2.html",
  };

  private static List voteBlocks;
  static {
    voteBlocks = new ArrayList();
    for (int ix = 0; ix < urls.length; ix++) {
      VoteBlock vb = V3TestUtils.makeVoteBlock(urls[ix]); 
      voteBlocks.add(vb);
    }
  }

  public void setUp() throws Exception {
    super.setUp();
    theDaemon = getMockLockssDaemon();
    this.tempDir = getTempDir();
    this.testau = setupAu();
    initRequiredServices();
    setupRepo(testau);
    this.pollerId = findPeerIdentity(localPeerKey);
    this.voters = makeVoters(initialPeers);
  }

  private MockArchivalUnit setupAu() {
    MockArchivalUnit mau = new MockArchivalUnit();
    mau.setAuId("mock");
    MockNodeManager nodeMgr = new MockNodeManager();
    theDaemon.setNodeManager(nodeMgr, mau);
    MockPlugin plug = new MockPlugin(theDaemon);
    mau.setPlugin(plug);
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.setEstimatedHashDuration(1000);
    List files = new ArrayList();
    for (int ix = 0; ix < urls.length; ix++) {
      MockCachedUrl cu = (MockCachedUrl)mau.addUrl(urls[ix], false, true);
      // Add mock file content.
      cu.setContent("This is content for CUS file " + ix);
      files.add(cu);
    }
    cus.setHashItSource(files);
    return mau;
  }
  
  private void setupRepo(ArchivalUnit au) throws Exception {
    MockLockssRepository repo = new MockLockssRepository("/foo", au);
    for (int ix =  0; ix < urls.length; ix++) {
      repo.createNewNode(urls[ix]);
    }
    ((MockLockssDaemon)theDaemon).setLockssRepository(repo, au);
  }

  PeerIdentity findPeerIdentity(String key) throws Exception {
    return V3TestUtils.findPeerIdentity(theDaemon, key);
  }

  private PeerIdentity[] makeVoters(List keys) throws Exception {
    PeerIdentity[] ids = new PeerIdentity[keys.size()];
    int idIndex = 0;
    for (Iterator it = keys.iterator(); it.hasNext(); ) {
      PeerIdentity pid = findPeerIdentity((String)it.next());
      ids[idIndex++] = pid;
    }
    return ids;
  }

  public void tearDown() throws Exception {
    theDaemon.getLockssRepository(testau).stopService();
    super.tearDown();
  }

  private void makeParticipants(VoteBlock[][] voteBlocks) throws Exception {
    V3Poller v3Poller = makeV3Poller("testing poll key");

    participants =
      new ArrayList<ParticipantUserData>();
    for (int i = 0; i < voteBlocks.length; i++) {
      VoteBlock[] voteBlockArray = voteBlocks[i];
      PeerIdentity pid = findPeerIdentity("TCP:[127.0.0.1]:899"+i);
      participants.add(makeParticipant(pid, v3Poller, voteBlockArray));
    }
  }

  private UrlTallier makeUrlTallier(VoteBlock[][] voteBlocks) throws Exception {
    makeParticipants(voteBlocks);
    return new UrlTallier(participants);
  }

  public void testVoteAllParticipants() throws Exception {
    VoteBlock[][] voteBlocks = {
      {
	makeVoteBlock("http://test.com/foo1", "content for foo1"),
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
	makeVoteBlock("http://test.com/foo3", "content for foo3")
      },
      {
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
	makeVoteBlock("http://test.com/foo3", "content for foo3")
      },
      {
	makeVoteBlock("http://test.com/foo3", "content for foo3"),
	makeVoteBlock("http://test.com/foo4", "content for foo4")
      }};
    UrlTallier urlTallier = makeUrlTallier(voteBlocks);
    CheckingCallback checkingCallback;

    assertEquals("http://test.com/foo1", urlTallier.peekUrl());
    checkingCallback = 
      new CheckingCallback(new VoteBlock[]{voteBlocks[0][0], 
					   missingBlock,
					   missingBlock});
    urlTallier.voteAllParticipants(urlTallier.peekUrl(), checkingCallback);
    checkingCallback.check();

    assertEquals("http://test.com/foo2", urlTallier.peekUrl());
    checkingCallback = 
      new CheckingCallback(new VoteBlock[]{voteBlocks[0][1], 
					   voteBlocks[1][0],
					   missingBlock});
    urlTallier.voteAllParticipants(urlTallier.peekUrl(), checkingCallback);
    checkingCallback.check();

    assertEquals("http://test.com/foo3", urlTallier.peekUrl());
    checkingCallback = 
      new CheckingCallback(new VoteBlock[]{voteBlocks[0][2], 
					   voteBlocks[1][1], 
					   voteBlocks[2][0]});
    urlTallier.voteAllParticipants(urlTallier.peekUrl(), checkingCallback);
    checkingCallback.check();

    assertEquals("http://test.com/foo4", urlTallier.peekUrl());
    checkingCallback = 
      new CheckingCallback(new VoteBlock[]{missingBlock,
					   missingBlock,
					   voteBlocks[2][1]});
    urlTallier.voteAllParticipants(urlTallier.peekUrl(), checkingCallback);
    checkingCallback.check();

    assertEquals(null, urlTallier.peekUrl());
    checkingCallback = 
      new CheckingCallback(new VoteBlock[]{});
    try {
      urlTallier.voteAllParticipants(urlTallier.peekUrl(), checkingCallback);
      fail("Expected exception not thrown");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  public void testVoteNoParticipants() throws Exception {
    VoteBlock[][] voteBlocks = {
      {
	makeVoteBlock("http://test.com/foo1", "content for foo1"),
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
	makeVoteBlock("http://test.com/foo3", "content for foo3")
      },
      {
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
	makeVoteBlock("http://test.com/foo3", "content for foo3")
      },
      {
	makeVoteBlock("http://test.com/foo3", "content for foo3"),
	makeVoteBlock("http://test.com/foo4", "content for foo4")
      }};
    UrlTallier urlTallier = makeUrlTallier(voteBlocks);

    assertEquals("http://test.com/foo1", urlTallier.peekUrl());
    urlTallier.voteNoParticipants(urlTallier.peekUrl());
    assertEquals("http://test.com/foo2", urlTallier.peekUrl());
    urlTallier.voteNoParticipants(urlTallier.peekUrl());
    assertEquals("http://test.com/foo3", urlTallier.peekUrl());
    urlTallier.voteNoParticipants(urlTallier.peekUrl());
    assertEquals("http://test.com/foo4", urlTallier.peekUrl());
    urlTallier.voteNoParticipants(urlTallier.peekUrl());
    assertEquals(null, urlTallier.peekUrl());
  }

  public void testIOException() throws Exception {
    VoteBlock[][] voteBlocks = {
      {
	makeVoteBlock("http://test.com/foo4", "content for foo4")
      }};
    
    makeParticipants(voteBlocks);

    PeerIdentity pid =
      findPeerIdentity("TCP:[127.0.0.1]:899"+voteBlocks.length);
    V3Poller v3Poller = participants.get(0).getPoller();
    VoteBlock[] voteBlockArray = {
	makeVoteBlock("http://test.com/foo1", "content for foo1"),
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
	makeVoteBlock("http://test.com/foo3", "content for foo3")
    };
    ParticipantUserData participant =
      makeParticipant(pid, v3Poller, voteBlockArray);

    final VoteBlocks vb = participant.getVoteBlocks();
    VoteBlocks thrower = new VoteBlocks() {
	public void addVoteBlock(VoteBlock b) throws IOException {
	  vb.addVoteBlock(b);
	}
	public InputStream getInputStream() throws IOException {
	  return vb.getInputStream();
	}
	public VoteBlock getVoteBlock(String url) {
	  return vb.getVoteBlock(url);
	}
	public VoteBlocksIterator iterator() throws FileNotFoundException {
	  return new VoteBlocksIterator() {
	    final VoteBlocksIterator iterator = vb.iterator();
	    boolean spoiled = false;
	    void checkSpoiled() throws IOException {
	      if ("http://test.com/foo2".equals(iterator.peek().getUrl())) {
		spoiled = true;
	      }
	      if (spoiled) {
		throw new IOException("Unit test says Spoiled!");
	      }
	    }
	    public boolean hasNext() throws IOException { 
	      checkSpoiled();
	      return iterator.hasNext(); }
	    public VoteBlock next() throws IOException {
	      checkSpoiled();
	      return iterator.next(); }
	    public VoteBlock peek() throws IOException {
	      checkSpoiled();
	      return iterator.peek(); }
	    public void release() { iterator.release(); }
	  };
	}
	public int size() {
	  return vb.size();
	}
	public long getEstimatedEncodedLength() {
	  return vb.getEstimatedEncodedLength();
	}
	public void release() {
	  vb.release();
	}
      };
    participant.setVoteBlocks(thrower);
    participants.add(participant);

    UrlTallier urlTallier = new UrlTallier(participants);
    assertEquals("http://test.com/foo1", urlTallier.peekUrl());
    urlTallier.voteNoParticipants(urlTallier.peekUrl());
    // foo2 fails, and the urlTallier jumps right to foo4
    assertEquals("http://test.com/foo4", urlTallier.peekUrl());

    CheckingCallback checkingCallback = 
      new CheckingCallback(new VoteBlock[]{voteBlocks[0][0], 
					   spoiledBlock});
    urlTallier.voteAllParticipants(urlTallier.peekUrl(), checkingCallback);
    checkingCallback.check();

    assertEquals(null, urlTallier.peekUrl());
  }

  public void testSeek() throws Exception {
    VoteBlock[][] voteBlocks = {
      {
	makeVoteBlock("http://test.com/foo1", "content for foo1"),
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
	makeVoteBlock("http://test.com/foo3", "content for foo3")
      },
      {
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
	makeVoteBlock("http://test.com/foo3", "content for foo3")
      },
      {
	makeVoteBlock("http://test.com/foo3", "content for foo3"),
	makeVoteBlock("http://test.com/foo4", "content for foo4")
      }};
    UrlTallier urlTallier = makeUrlTallier(voteBlocks);

    assertEquals("http://test.com/foo1", urlTallier.peekUrl());
    urlTallier.seek("http://test.com/foo1");
    assertEquals("http://test.com/foo1", urlTallier.peekUrl());

    // A url in order, but not known to any voter.
    urlTallier.seek("http://test.com/foo1a");
    assertEquals("http://test.com/foo2", urlTallier.peekUrl());

    urlTallier.seek("http://test.com/foo2");
    assertEquals("http://test.com/foo2", urlTallier.peekUrl());

    try {
      urlTallier.seek(null);
      fail("Expected Exception was not thrown.");
    } catch (IllegalArgumentException e) {
      assertEquals("url is null.", e.getMessage());
    }

    // skip foo3
    urlTallier.seek("http://test.com/foo4");
    assertEquals("http://test.com/foo4", urlTallier.peekUrl());

    try {
      urlTallier.seek("http://test.com/foo2");
      fail("Expected ShouldNotHappenException was not thrown.");
    } catch (IllegalArgumentException e) {
      assertEquals("Current URL is http://test.com/foo4, "+
		   "past http://test.com/foo2", e.getMessage());
    }

    // And stepping off the end.
    urlTallier.seek("http://test.com/foo5");
    assertEquals(null, urlTallier.peekUrl());
  }

  private VoteBlock makeVoteBlock(String url) {
    VoteBlock vb = new VoteBlock(url);
    return vb;
  }
  
  private VoteBlock makeVoteBlock(String url, String content)
      throws Exception {
    VoteBlock vb = new VoteBlock(url);
    addVersion(vb, content);
    return vb;
  }
  
  private void addVersion(VoteBlock block, String content) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA1");
    md.update(content.getBytes());
    byte[] hash = md.digest();
    block.addVersion(0, content.length(), 
                     0, content.length(),
                     hash, hash, false);
  }
  
  private ParticipantUserData makeParticipant(PeerIdentity id,
                                              V3Poller poller,
                                              VoteBlock [] votes) 
      throws Exception {
    byte[] pollerNonce = ByteArray.makeRandomBytes(NONCE_LENGTH);
    ParticipantUserData ud = new ParticipantUserData(id, poller, tempDir);
    ud.setPollerNonce(pollerNonce);
    VoteBlocks vb = new DiskVoteBlocks(tempDir);
    for (int i = 0; i < votes.length; i++) {
      vb.addVoteBlock(votes[i]);
    }
    ud.setVoteBlocks(vb);
    return ud;
  }

  private MyV3Poller makeV3Poller(String key) throws Exception {
    PollSpec ps = new MockPollSpec(testau.getAuCachedUrlSet(), null, null,
                                   Poll.V3_POLL);
    MyV3Poller poller = 
      new MyV3Poller(ps, theDaemon, pollerId, key, 20000, "SHA-1");
    return poller;
  }
  
  private class MyV3Poller extends V3Poller {
    MyV3Poller(PollSpec spec, LockssDaemon daemon, PeerIdentity id,
	       String pollkey, long duration, String hashAlg)
        throws PollSerializerException {
      super(spec, daemon, id, pollkey, duration, hashAlg);
    }
  }
  
  final VoteBlock missingBlock = makeVoteBlock("***Missing");
  final VoteBlock spoiledBlock = makeVoteBlock("***Spoiled");

  class CheckingCallback implements UrlTallier.VoteCallback {
    final Set<Integer> voted = new HashSet<Integer>();

    final VoteBlock[] expected;
    CheckingCallback(VoteBlock[] expected) {
      this.expected = expected;
    }

    @Override public void vote(VoteBlock voteBlock, ParticipantUserData id,
			       int participantIndex) {
      assertIndex(id, participantIndex);
      VoteBlock expected = findExpected(participantIndex);
      assertEquals(expected, voteBlock);
    }
    @Override public void voteMissing(ParticipantUserData id) {
      VoteBlock expected = findExpected(id);
      assertEquals(expected, missingBlock);
    }
    @Override public void voteSpoiled(ParticipantUserData id) {
      VoteBlock expected = findExpected(id);
      assertEquals(expected, spoiledBlock);
    }

    VoteBlock findExpected(ParticipantUserData id) {
      return findExpected(participants.indexOf(id));
    }

    VoteBlock findExpected(int participantIndex) {
      assertFalse("participant "+participantIndex+" voted more than once.",
		  voted.contains(participantIndex));
      voted.add(participantIndex);
      return expected[participantIndex];
    }

    public void assertIndex(ParticipantUserData id, int participantIndex) {
      assertEquals(participants.get(participantIndex), id);
    }

    public void check() {
      for (int i = 0; i < expected.length; i++) {
	assertTrue(voted.contains(i));
      }
    }
  }

  private void initRequiredServices() throws Exception {
    tempDir = getTempDir();
    tempDirPath = tempDir.getAbsolutePath();
    System.setProperty("java.io.tmpdir", tempDirPath);

    Properties p = new Properties();
    p.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    p.setProperty(V3Poller.PARAM_STATE_PATH, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(p);
  }
}
