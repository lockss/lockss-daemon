/*
 * $Id: TestUrlTallier.java,v 1.15 2013-05-03 17:30:44 barry409 Exp $
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

  private MyIdentityManager idMgr;
  private MockLockssDaemon theDaemon;

  private PeerIdentity pollerId;

  private String tempDirPath;
  private ArchivalUnit testau;
  private PollManager pollmanager;
  private HashService hashService;
  private PluginManager pluginMgr;

  private PeerIdentity[] voters;
  
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
    TimeBase.setSimulated();
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
      PeerIdentityStatus status = idMgr.getPeerIdentityStatus(pid);
      ids[idIndex++] = pid;
    }
    return ids;
  }

  public void tearDown() throws Exception {
    theDaemon.getLockssRepository(testau).stopService();
    theDaemon.getHashService().stopService();
    theDaemon.getDatagramRouterManager().stopService();
    theDaemon.getRouterManager().stopService();
    theDaemon.getSystemMetrics().stopService();
    theDaemon.getPollManager().stopService();
    TimeBase.setReal();
    super.tearDown();
  }

  public void testSeek() throws Exception {

    V3Poller v3Poller = makeV3Poller("testing poll key", 3);
    
    PeerIdentity id1 = findPeerIdentity("TCP:[127.0.0.1]:8990");
    PeerIdentity id2 = findPeerIdentity("TCP:[127.0.0.1]:8991");
    PeerIdentity id3 = findPeerIdentity("TCP:[127.0.0.1]:8992");

    VoteBlock [] voter1_voteblocks = {
	makeVoteBlock("http://test.com/foo1", "content for foo1"),
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
	makeVoteBlock("http://test.com/foo3", "content for foo3")
      };
    
    VoteBlock [] voter2_voteblocks = {
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
	makeVoteBlock("http://test.com/foo3", "content for foo3")
      };
    
    VoteBlock [] voter3_voteblocks = {
	makeVoteBlock("http://test.com/foo3", "content for foo3"),
	makeVoteBlock("http://test.com/foo4", "content for foo4")
      };

    List<ParticipantUserData> theParticipants =
      new ArrayList<ParticipantUserData>();

    theParticipants.add(makeParticipant(id1, v3Poller,
					voter1_voteblocks));
    theParticipants.add(makeParticipant(id2, v3Poller,
					voter2_voteblocks));
    theParticipants.add(makeParticipant(id3, v3Poller,
					voter3_voteblocks));

    UrlTallier urlTallier = new UrlTallier(theParticipants,
					   v3Poller.getHashIndexer(), 5, 75);

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
      fail("Expected ShouldNotHappenException was not thrown.");
    } catch (ShouldNotHappenException e) {
      assertEquals("url is null.", e.getMessage());
    }

    // skip foo3
    urlTallier.seek("http://test.com/foo4");
    assertEquals("http://test.com/foo4", urlTallier.peekUrl());

    try {
      urlTallier.seek("http://test.com/foo2");
      fail("Expected ShouldNotHappenException was not thrown.");
    } catch (ShouldNotHappenException e) {
      assertEquals("Current URL is http://test.com/foo4, "+
		   "past http://test.com/foo2", e.getMessage());
    }

    // And stepping off the end.
    urlTallier.seek("http://test.com/foo5");
    assertEquals(null, urlTallier.peekUrl());
  }

  public void testOutOfOrderUrl() throws Exception {

    V3Poller v3Poller = makeV3Poller("testing poll key", 3);
    
    PeerIdentity id1 = findPeerIdentity("TCP:[127.0.0.1]:8990");
    PeerIdentity id2 = findPeerIdentity("TCP:[127.0.0.1]:8991");
    PeerIdentity id3 = findPeerIdentity("TCP:[127.0.0.1]:8992");

    VoteBlock [] voter1_voteblocks = {
      // NOTE: these URLs are not in the canonical order.
      makeVoteBlock("http://test.com/foo2", "content for foo2"),
      makeVoteBlock("http://test.com/foo1", "content for foo1"),
      makeVoteBlock("http://test.com/foo3", "content for foo3"),
      makeVoteBlock("http://test.com/foo4", "content for foo4")
    };

    VoteBlock [] voter2_voteblocks = {
      makeVoteBlock("http://test.com/foo1", "content for foo1"),
      makeVoteBlock("http://test.com/foo2", "content for foo2"),
      makeVoteBlock("http://test.com/foo3", "content for foo3"),
      makeVoteBlock("http://test.com/foo4", "content for foo4")
    };

    BlockTally tally;

    List<ParticipantUserData> theParticipants =
      new ArrayList<ParticipantUserData>();

    theParticipants.add(makeParticipant(id1, v3Poller,
					voter1_voteblocks));
    theParticipants.add(makeParticipant(id2, v3Poller,
					voter2_voteblocks));

    List voter2 = ListUtil.list(theParticipants.get(1));

    UrlTallier urlTallier = new UrlTallier(theParticipants,
					   v3Poller.getHashIndexer(), 5, 75);

    // The first URL is seen
    assertEquals("http://test.com/foo1", urlTallier.peekUrl());
    tally = urlTallier.tallyVoterUrl("http://test.com/foo1");
    assertSameElements(voter2, tally.getVoterOnlyBlockVoters());
    assertEquals("http://test.com/foo2", urlTallier.peekUrl());
    tally = urlTallier.tallyVoterUrl("http://test.com/foo2");
    // Both have foo2
    assertSameElements(theParticipants, tally.getVoterOnlyBlockVoters());
    // But at this point the out-of-order URL in voter1 has been seen,
    // and voter1 doesn't show any more votes.
    assertEquals("http://test.com/foo3", urlTallier.peekUrl());
    tally = urlTallier.tallyVoterUrl("http://test.com/foo3");
    assertSameElements(voter2, tally.getVoterOnlyBlockVoters());
    tally = urlTallier.tallyVoterUrl("http://test.com/foo4");
    assertSameElements(voter2, tally.getVoterOnlyBlockVoters());

    assertEquals(null, urlTallier.peekUrl());
  }

  public void testTallyPollerUrl() throws Exception {

    V3Poller v3Poller = makeV3Poller("testing poll key", 4);
    
    PeerIdentity id1 = findPeerIdentity("TCP:[127.0.0.1]:8990");
    PeerIdentity id2 = findPeerIdentity("TCP:[127.0.0.1]:8991");
    PeerIdentity id3 = findPeerIdentity("TCP:[127.0.0.1]:8992");
    PeerIdentity id4 = findPeerIdentity("TCP:[127.0.0.1]:8994");
    
    HashBlock [] hashblocks = {
	makeHashBlock("http://test.com/foo1", "content for foo1"),
	makeHashBlock("http://test.com/foo2", "content for foo2"),
	makeHashBlock("http://test.com/foo3", "content for foo3"),
	makeHashBlock("http://test.com/foo4", "content for foo4")
      };

    VoteBlock [] voter1_voteblocks = {
	makeVoteBlock("http://test.com/foo1", "content for foo1"),
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
	makeVoteBlock("http://test.com/foo3", "content for foo3")
      };
    
    VoteBlock [] voter2_voteblocks = {
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
	makeVoteBlock("http://test.com/foo3", "content for foo3")
      };
    
    VoteBlock [] voter3_voteblocks = {
	makeVoteBlock("http://test.com/foo3", "content for foo3"),
	makeVoteBlock("http://test.com/foo4", "content for foo4")
      };

    List<ParticipantUserData> theParticipants =
      new ArrayList<ParticipantUserData>();

    theParticipants.add(makeParticipant(id1, v3Poller,
					voter1_voteblocks));
    theParticipants.add(makeParticipant(id2, v3Poller,
					voter2_voteblocks));
    theParticipants.add(makeParticipant(id3, v3Poller,
					voter3_voteblocks));
    UrlTallier urlTallier = new UrlTallier(theParticipants,
					   v3Poller.getHashIndexer(), 5, 75);
    assertEquals("http://test.com/foo1", urlTallier.peekUrl());
    urlTallier.tallyPollerUrl("http://test.com/foo1", hashblocks[0]);
    assertEquals("http://test.com/foo2", urlTallier.peekUrl());
    urlTallier.tallyPollerUrl("http://test.com/foo2", hashblocks[1]);
    assertEquals("http://test.com/foo3", urlTallier.peekUrl());
    urlTallier.tallyPollerUrl("http://test.com/foo3", hashblocks[2]);
    assertEquals("http://test.com/foo4", urlTallier.peekUrl());
    urlTallier.tallyPollerUrl("http://test.com/foo4", hashblocks[3]);
    assertEquals(null, urlTallier.peekUrl());
  }

  public void testTallyVoterUrl() throws Exception {

    V3Poller v3Poller = makeV3Poller("testing poll key", 3);
    
    PeerIdentity id1 = findPeerIdentity("TCP:[127.0.0.1]:8990");
    PeerIdentity id2 = findPeerIdentity("TCP:[127.0.0.1]:8991");
    PeerIdentity id3 = findPeerIdentity("TCP:[127.0.0.1]:8992");

    VoteBlock [] voter1_voteblocks = {
	makeVoteBlock("http://test.com/foo1", "content for foo1"),
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
	makeVoteBlock("http://test.com/foo3", "content for foo3")
      };
    
    VoteBlock [] voter2_voteblocks = {
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
	makeVoteBlock("http://test.com/foo3", "content for foo3")
      };
    
    VoteBlock [] voter3_voteblocks = {
	makeVoteBlock("http://test.com/foo3", "content for foo3"),
	makeVoteBlock("http://test.com/foo4", "content for foo4")
      };

    BlockTally tally;
    
    List<ParticipantUserData> theParticipants =
      new ArrayList<ParticipantUserData>();

    theParticipants.add(makeParticipant(id1, v3Poller,
					voter1_voteblocks));
    theParticipants.add(makeParticipant(id2, v3Poller,
					voter2_voteblocks));
    theParticipants.add(makeParticipant(id3, v3Poller,
					voter3_voteblocks));

    UrlTallier urlTallier = new UrlTallier(theParticipants,
					   v3Poller.getHashIndexer(), 5, 75);
    assertEquals("http://test.com/foo1", urlTallier.peekUrl());
    tally = urlTallier.tallyVoterUrl("http://test.com/foo1");
    // todo(bhayes): BlockTally needs to have a better interface, both
    // for testing and for use.
    assertEquals(tally.getVoterOnlyBlockVoters().size(), 1);
    // todo(bhayes): This seems incorrect; foo1 was only present at
    // one voter, but incrementTalliedBlocks will be called for each
    // voter.
    // assertEquals(1, tally.getTalliedVoters().size());
    assertEquals(1, tally.getVoterOnlyBlockVoters().size());
    assertContains(tally.getVoterOnlyBlockVoters(), theParticipants.get(0));
    assertEquals("http://test.com/foo2", urlTallier.peekUrl());

    tally = urlTallier.tallyVoterUrl("http://test.com/foo2");
    assertEquals(tally.getVoterOnlyBlockVoters().size(), 2);
    // assertEquals(2, tally.getTalliedVoters().size());
    assertContains(tally.getVoterOnlyBlockVoters(), theParticipants.get(0));
    assertContains(tally.getVoterOnlyBlockVoters(), theParticipants.get(1));
    assertEquals("http://test.com/foo3", urlTallier.peekUrl());

    tally = urlTallier.tallyVoterUrl("http://test.com/foo3");
    assertEquals(tally.getVoterOnlyBlockVoters().size(), 3);
    // assertEquals(3, tally.getTalliedVoters().size());
    assertContains(tally.getVoterOnlyBlockVoters(), theParticipants.get(0));
    assertContains(tally.getVoterOnlyBlockVoters(), theParticipants.get(1));
    assertContains(tally.getVoterOnlyBlockVoters(), theParticipants.get(2));
    assertEquals("http://test.com/foo4", urlTallier.peekUrl());

    tally = urlTallier.tallyVoterUrl("http://test.com/foo4");
    assertEquals(tally.getVoterOnlyBlockVoters().size(), 1);
    // assertEquals(1, tally.getTalliedVoters().size());
    assertContains(tally.getVoterOnlyBlockVoters(), theParticipants.get(2));
    assertEquals(null, urlTallier.peekUrl());
  }

  public void testTallyVoterUrlNotPeek() throws Exception {

    V3Poller v3Poller = makeV3Poller("testing poll key", 3);
    
    PeerIdentity id1 = findPeerIdentity("TCP:[127.0.0.1]:8990");
    PeerIdentity id2 = findPeerIdentity("TCP:[127.0.0.1]:8991");
    PeerIdentity id3 = findPeerIdentity("TCP:[127.0.0.1]:8992");

    VoteBlock [] voter1_voteblocks = {
	makeVoteBlock("http://test.com/foo1", "content for foo1"),
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
	makeVoteBlock("http://test.com/foo3", "content for foo3")
      };

    BlockTally tally;
    
    List<ParticipantUserData> theParticipants =
      new ArrayList<ParticipantUserData>();

    theParticipants.add(makeParticipant(id1, v3Poller,
					voter1_voteblocks));

    UrlTallier urlTallier = new UrlTallier(theParticipants,
					   v3Poller.getHashIndexer(), 5, 75);
    assertEquals("http://test.com/foo1", urlTallier.peekUrl());
    tally = urlTallier.tallyVoterUrl("http://test.com/foo1");
    assertEquals("http://test.com/foo2", urlTallier.peekUrl());
    try {
      // Call tallyVoterUrl with a url other than the peekUrl
      tally = urlTallier.tallyVoterUrl("http://test.com/bar");
      fail("Expected ShouldNotHappenException was not thrown.");
    } catch (ShouldNotHappenException e) {
      assertEquals("Current URL is http://test.com/foo2 not "+
		   "http://test.com/bar", e.getMessage());
    }
  }

  public void testIteratorFileNotFound() throws Exception {
    // A VoteBlocks which supports nothing except iterator(), and that
    // throws FileNotFound.
    final class FileNotFoundVoteBlocks implements VoteBlocks {
      boolean thrown = false;
      public VoteBlocksIterator iterator() throws FileNotFoundException {
	// The test only calls iterator() once.
	assertFalse(thrown);
	thrown = true;
	throw new FileNotFoundException("Expected exception.");
      }
      public void addVoteBlock(VoteBlock b) throws IOException {
	throw new UnsupportedOperationException();
      }
      public InputStream getInputStream() throws IOException {
	throw new UnsupportedOperationException();
      }
      public VoteBlock getVoteBlock(String url) {
	throw new UnsupportedOperationException();
	}
      public int size() {
	throw new UnsupportedOperationException();
      }
      public long getEstimatedEncodedLength() {
	throw new UnsupportedOperationException();
      }
      public void release() {
	throw new UnsupportedOperationException();
      }
    };

    V3Poller v3Poller = makeV3Poller("testing poll key", 1);

    PeerIdentity id1 = findPeerIdentity("TCP:[127.0.0.1]:8990");
    VoteBlock [] voter1_voteblocks = {};
    
    List<ParticipantUserData> theParticipants =
      new ArrayList<ParticipantUserData>();

    ParticipantUserData participant =
      new ParticipantUserData(id1, v3Poller, null);

    FileNotFoundVoteBlocks vb = new FileNotFoundVoteBlocks();
    participant.setVoteBlocks(vb);

    theParticipants.add(participant);
    assertFalse(vb.thrown);
    UrlTallier urlTallier = new UrlTallier(theParticipants,
					   v3Poller.getHashIndexer(), 5, 75);
    assertTrue(vb.thrown);
    assertTrue(urlTallier.voteSpoiled(participant));
    // peekUrl() doesn't throw anything, but there's no URL since the
    // iterator's file is not found.
    assertEquals(null, urlTallier.peekUrl());
  }

  public void testIteratorIOException() throws Exception {

    // A DiskVoteBlocks that has an iterator that throws IOException
    // when it reaches the given URL.
    final class IOExceptionDiskVoteBlocks extends DiskVoteBlocks {
      final String failAt;
      boolean threw;
      boolean released;
      IOExceptionDiskVoteBlocks(File toDir, String failAt) throws IOException {
	super(toDir);
	this.failAt = failAt;
	threw = false;
	released = false;
      }
      
      public VoteBlocksIterator iterator() throws FileNotFoundException {
	final VoteBlocksIterator iterator = super.iterator();

	return new VoteBlocksIterator() {
	  void checkFail() throws IOException {
	    if (failAt.equals(iterator.peek().getUrl())) {
	      // We only get here once.
	      assertFalse(threw);
	      threw = true;
	      throw new IOException("Expected exception.");
	    }
	  }
	    
	  public boolean hasNext() throws IOException {
	    checkFail();
	    return iterator.hasNext();
	  }
	  public VoteBlock next() throws IOException {
	    checkFail();
	    return iterator.next();
	  }
	  public VoteBlock peek() throws IOException {
	    checkFail();
	    return iterator.peek();
	  }
	  public void release() {
	    // We only get here once.
	    assertFalse(released);
	    released = true;
	    iterator.release();
	  }
	};
      }
    };

    final V3Poller v3Poller = makeV3Poller("testing poll key", 1);

    PeerIdentity id1 = findPeerIdentity("TCP:[127.0.0.1]:8990");
    VoteBlock [] voter1_voteblocks = {
	makeVoteBlock("http://test.com/foo1", "content for foo1"),
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
	makeVoteBlock("http://test.com/foo3", "content for foo3")
      };
    
    List<ParticipantUserData> theParticipants =
      new ArrayList<ParticipantUserData>();

    // Pass in null messageDir; we won't be calling makeMessage.
    ParticipantUserData participant =
      new ParticipantUserData(id1, v3Poller, null);

    IOExceptionDiskVoteBlocks vb = 
      new IOExceptionDiskVoteBlocks(tempDir, "http://test.com/foo2");

    for (int i = 0; i < voter1_voteblocks.length; i++) {
      vb.addVoteBlock(voter1_voteblocks[i]);
    }
    participant.setVoteBlocks(vb);

    theParticipants.add(participant);
    UrlTallier urlTallier = new UrlTallier(theParticipants,
					   v3Poller.getHashIndexer(), 5, 75);

    assertEquals("http://test.com/foo1", urlTallier.peekUrl());
    assertFalse(urlTallier.voteSpoiled(participant));
    assertFalse(vb.threw);
    assertFalse(vb.released);
    urlTallier.seek("http://test.com/foo2");
    assertTrue(vb.threw);
    assertTrue(vb.released);
    assertTrue(urlTallier.voteSpoiled(participant));
    // peekUrl() doesn't throw anything, but there's no URL since the
    // iterator threw IOException.
    assertEquals(null, urlTallier.peekUrl());
  }

  public void testHashStatsTallier() throws Exception {
    V3Poller v3Poller = makeV3Poller("testing poll key", 1);
    PeerIdentity id1 = findPeerIdentity("TCP:[127.0.0.1]:8990");
    ParticipantUserData participant =
      new ParticipantUserData(id1, v3Poller, null);

    VoteBlock vb = new VoteBlock("foo", VoteBlock.CONTENT_VOTE);
    byte[] testBytes = ByteArray.makeRandomBytes(20);
    vb.addVersion(0, 123, 0, 155, testBytes, testBytes, false);

    VoteBlockTallier.VoteCallback callback = UrlTallier.makeHashStatsTallier();
    callback.vote(vb, participant);
    assertEquals(286, participant.getBytesHashed());
    assertEquals(155, participant.getBytesRead());
  }
  
  private HashBlock makeHashBlock(String url) {
    MockCachedUrl cu = new MockCachedUrl(url);
    return new HashBlock(cu);
  }
  
  private HashBlock makeHashBlock(String url, String content)
      throws Exception {
    MockCachedUrl cu = new MockCachedUrl(url);
    HashBlock hb = new HashBlock(cu);
    addVersion(hb, content);
    return hb;
  }

  private static int hbVersionNum = 1;

  private void addVersion(HashBlock block, String content) throws Exception {
    // 1 plain hash, plus 4 voters
    MessageDigest[] digests = new MessageDigest[5];  
    // fake "Plain Hash"
    digests[0] = MessageDigest.getInstance("SHA1");
    digests[0].update(content.getBytes());
    // fake "Nonced Hash" for voter 1
    digests[1] = MessageDigest.getInstance("SHA1");
    digests[1].update(content.getBytes());
    // fake "Nonced Hash" for voter 2
    digests[2] = MessageDigest.getInstance("SHA1");
    digests[2].update(content.getBytes());
    // fake "Nonced Hash" for voter 3
    digests[3] = MessageDigest.getInstance("SHA1");
    digests[3].update(content.getBytes());
    // fake "Nonced Hash" for voter 4
    digests[4] = MessageDigest.getInstance("SHA1");
    digests[4].update(content.getBytes());
    
    block.addVersion(0, content.length(), 
                     0, content.length(),
		     digests.length * content.length(), // total bytes hashed
                     digests, hbVersionNum++, null);    
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

  private MyV3Poller makeV3Poller(String key, int pollSize) throws Exception {
    PollSpec ps = new MockPollSpec(testau.getAuCachedUrlSet(), null, null,
                                   Poll.V3_POLL);
    MyV3Poller poller = 
      new MyV3Poller(ps, theDaemon, pollerId, key, 20000, "SHA-1", pollSize);
    return poller;
  }
  
  private class MyV3Poller extends V3Poller {
    // For testing:  Hashmap of voter IDs to V3LcapMessages.
    private Map sentMsgs = Collections.synchronizedMap(new HashMap());
    private Map semaphores = new HashMap();
    private List<PollerStateBean.Repair> repairs;
    private final int pollSize;

    MyV3Poller(PollSpec spec, LockssDaemon daemon, PeerIdentity id,
	       String pollkey, long duration, String hashAlg, int pollSize)
        throws PollSerializerException {
      super(spec, daemon, id, pollkey, duration, hashAlg);
      this.pollSize = pollSize;
    }
    
    @Override
    public void sendMessageTo(V3LcapMessage msg, PeerIdentity to) {
      fail("");
      sentMsgs.put(to, msg);
      SimpleBinarySemaphore sem = (SimpleBinarySemaphore)semaphores.get(to);
      if (sem == null) {
        sem = new SimpleBinarySemaphore();
        semaphores.put(to, sem);
      }
      sem.give();
    }

    @Override
    public List getCompletedRepairs() {
      if (repairs != null) {
	return repairs;
      }
      return super.getCompletedRepairs();
    }

    @Override
    public int getPollSize() {
      return pollSize;
    }
  }

  private void initRequiredServices() throws Exception {
    pollmanager = theDaemon.getPollManager();
    hashService = theDaemon.getHashService();

    pluginMgr = theDaemon.getPluginManager();

    tempDir = getTempDir();
    tempDirPath = tempDir.getAbsolutePath();
    System.setProperty("java.io.tmpdir", tempDirPath);

    Properties p = new Properties();
    p.setProperty(IdentityManagerImpl.PARAM_ENABLE_V1, "false");
    p.setProperty(LcapDatagramComm.PARAM_ENABLED, "false");

    p.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    p.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    p.setProperty(IdentityManager.PARAM_LOCAL_V3_IDENTITY, localPeerKey);
    p.setProperty(ConfigManager.PARAM_NEW_SCHEDULER, "true");
    p.setProperty(IdentityManagerImpl.PARAM_INITIAL_PEERS,
                  StringUtil.separatedString(initialPeers, ";"));
    p.setProperty(V3Poller.PARAM_QUORUM, "3");
    p.setProperty(V3Poller.PARAM_STATE_PATH, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(p);
    idMgr = new MyIdentityManager();
    theDaemon.setIdentityManager(idMgr);
    idMgr.initService(theDaemon);
    idMgr.startService();
    theDaemon.getSchedService().startService();
    hashService.startService();
    theDaemon.getDatagramRouterManager().startService();
    theDaemon.getRouterManager().startService();
    theDaemon.getSystemMetrics().startService();
    theDaemon.getActivityRegulator(testau).startService();
    theDaemon.setNodeManager(new MockNodeManager(), testau);
    pollmanager.startService();
  }

  static class MyIdentityManager extends IdentityManagerImpl {
    IdentityAgreement findTestIdentityAgreement(PeerIdentity pid,
						ArchivalUnit au) {
      Map map = findAuAgreeMap(au);
      synchronized (map) {
	return findPeerIdentityAgreement(map, pid);
      }
    }

    public void storeIdentities() throws ProtocolException {
    }
  }

}
