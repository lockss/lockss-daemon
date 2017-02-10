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


public class TestVoteBlocksCoordinator extends LockssTestCase {

  private String tempDirPath;
  private File tempDir;
  private List<VoteBlocksIterator> iterators;

  private static final String BASE_URL = "http://www.test.org/";

  public void setUp() throws Exception {
    super.setUp();
    tempDir = getTempDir();
    tempDirPath = tempDir.getAbsolutePath();
    System.setProperty("java.io.tmpdir", tempDirPath);
  }

  public void tearDown() throws Exception {
    if (iterators != null) {
      for (VoteBlocksIterator iterator: iterators) {
	iterator.release();
      }
    }
    super.tearDown();
  }

  public void testCreate() throws Exception {
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

    iterators = makeIterators(voteBlocks);
    new VoteBlocksCoordinator(iterators);
  }

  public void testNoIterators() throws Exception {
    // The List is allowed to be empty.
    VoteBlock[][] voteBlocks = {};
    iterators = Collections.EMPTY_LIST;
    VoteBlocksCoordinator coordinator = 
      new VoteBlocksCoordinator(iterators);
 
    assertNull(coordinator.peekUrl());
  }

  public void testNullIterator() throws Exception {
    // A null VoteBlocksIterator instance means the votes are spoiled.
    VoteBlocksCoordinator coordinator = 
      new VoteBlocksCoordinator(Arrays.asList((VoteBlocksIterator)null));
    assertTrue(coordinator.isSpoiled(0));
  }

  public void testCreateFail() throws Exception {
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

    try {
      new VoteBlocksCoordinator(null);
      fail("Expected exception not thrown.");
    } catch (NullPointerException e) {
      // expected
    }

    try {
      new VoteBlocksCoordinator(Collections.EMPTY_LIST);
    } catch (IllegalArgumentException e) {
      // expected: Thrown by java.util.PriorityQueue.<init>
    }
  }

  public void testVote() throws Exception {
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

    iterators = makeIterators(voteBlocks);
    VoteBlocksCoordinator coordinator = new VoteBlocksCoordinator(iterators);

    for (int i = 1; i <= 4; i++) {
      String url = "http://test.com/foo" + i;
      assertEquals(coordinator.peekUrl(), url);
      for (int iteratorIndex = 0;
	   iteratorIndex < voteBlocks.length; iteratorIndex++) {
	VoteBlock voteBlock = coordinator.getVoteBlock(url, iteratorIndex);
	assertEquals(findVoteBlock(url, voteBlocks[iteratorIndex]), voteBlock);
      }
    }
    assertNull(coordinator.peekUrl());
  }

  public void testUnconsumed() throws Exception {
    VoteBlock[][] voteBlocks = {
      {
	makeVoteBlock("http://test.com/foo1", "content for foo1"),
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
	makeVoteBlock("http://test.com/foo3", "content for foo3")
      },
      {
	makeVoteBlock("http://test.com/foo1", "content for foo1"),
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
	makeVoteBlock("http://test.com/foo3", "content for foo3")
      }};

    iterators = makeIterators(voteBlocks);
    VoteBlocksCoordinator coordinator = new VoteBlocksCoordinator(iterators);
    
    coordinator.getVoteBlock("http://test.com/foo1", 0);
    try {
      // Try to advance when there is still an unconsumed VoteBlock for foo1.
      coordinator.getVoteBlock("http://test.com/foo2", 0);
      fail("Expected exception not thrown.");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  public void testOutOfOrderCall() throws Exception {
    VoteBlock[][] voteBlocks = {
      {
	makeVoteBlock("http://test.com/foo1", "content for foo1"),
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
	makeVoteBlock("http://test.com/foo3", "content for foo3")
      }};

    iterators = makeIterators(voteBlocks);
    VoteBlocksCoordinator coordinator = new VoteBlocksCoordinator(iterators);
    
    coordinator.getVoteBlock("http://test.com/foo1", 0);
    coordinator.getVoteBlock("http://test.com/foo2", 0);
    try {
      // Try to call with a URL before the previous one.
      coordinator.getVoteBlock("http://test.com/foo1a", 0);
      fail("Expected exception not thrown.");
    } catch (IllegalArgumentException e) {
      // expected
    }
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

    iterators = makeIterators(voteBlocks);
    VoteBlocksCoordinator coordinator = new VoteBlocksCoordinator(iterators);

    assertEquals("http://test.com/foo1", coordinator.peekUrl());
    coordinator.seek("http://test.com/foo1");
    // seek to where we already are
    assertEquals("http://test.com/foo1", coordinator.peekUrl());

    // A url in order, but not known to any voter.
    coordinator.seek("http://test.com/foo1a");
    assertEquals("http://test.com/foo2", coordinator.peekUrl());

    coordinator.seek("http://test.com/foo2");
    assertEquals("http://test.com/foo2", coordinator.peekUrl());

    try {
      coordinator.seek(null);
      fail("Expected Exception was not thrown.");
    } catch (IllegalArgumentException e) {
      assertEquals("url is null.", e.getMessage());
    }

    // skip foo3
    coordinator.seek("http://test.com/foo4");
    assertEquals("http://test.com/foo4", coordinator.peekUrl());

    try {
      // try seeking backwards
      coordinator.seek("http://test.com/foo2");
      fail("Expected IllegalArgumentException was not thrown.");
    } catch (IllegalArgumentException e) {
      assertEquals("Current URL is http://test.com/foo4, "+
		   "past http://test.com/foo2", e.getMessage());
    }

    // And stepping off the end.
    coordinator.seek("http://test.com/foo5");
    assertEquals(null, coordinator.peekUrl());
  }

  public void testOutOfOrderUrl() throws Exception {
    VoteBlock[][] voteBlocks = {
      {
	makeVoteBlock("http://test.com/foo1", "content for foo1"),
	makeVoteBlock("http://test.com/foo3", "content for foo3"),
	makeVoteBlock("http://test.com/foo4", "content for foo4")
      },
      {
	// NOTE: these URLs are not in the canonical order.
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
	makeVoteBlock("http://test.com/foo1", "content for foo1"),
	makeVoteBlock("http://test.com/foo3", "content for foo3"),
	makeVoteBlock("http://test.com/foo4", "content for foo4")
      },
      {
	makeVoteBlock("http://test.com/foo2", "content for foo1"),
	makeVoteBlock("http://test.com/foo3", "content for foo3"),
	makeVoteBlock("http://test.com/foo4", "content for foo4")
      }
    };

    iterators = makeIterators(voteBlocks);
    VoteBlocksCoordinator coordinator = new VoteBlocksCoordinator(iterators);

    for (int i = 1; i <= 4; i++) {
      String url = "http://test.com/foo" + i;
      assertEquals(coordinator.peekUrl(), url);
      for (int iteratorIndex = 0;
	   iteratorIndex < voteBlocks.length; iteratorIndex++) {
	if (iteratorIndex != 1) {
	  VoteBlock voteBlock = coordinator.getVoteBlock(url, iteratorIndex);
	  assertEquals(findVoteBlock(url, voteBlocks[iteratorIndex]),
		       voteBlock);
	} else {
	  VoteBlock voteBlock;
	  switch(i) {
	  case 1:
	    // It has foo1, but out of order so not yet seen.
	    voteBlock = coordinator.getVoteBlock(url, iteratorIndex);
	    assertEquals(null, voteBlock);
	    break;
	  case 2:
	    // It has foo2
	    voteBlock = coordinator.getVoteBlock(url, iteratorIndex);
	    assertEquals(findVoteBlock(url, voteBlocks[iteratorIndex]),
			 voteBlock);
	    break;
	  default:
	    assertTrue(coordinator.isSpoiled(iteratorIndex));
	    try {
	      coordinator.getVoteBlock(url, iteratorIndex);
	      fail("Expected Exception not thrown.");
	    } catch (IllegalArgumentException e) {
	      // expected
	    }
	  }
	}
      }
    }
    assertNull(coordinator.peekUrl());
  }

  public void testPeek() throws Exception {
    VoteBlock[][] voteBlocks = {
      {
	makeVoteBlock("http://test.com/foo1", "content for foo1"),
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
      }};

    iterators = makeIterators(voteBlocks);
    VoteBlocksCoordinator coordinator = new VoteBlocksCoordinator(iterators);

    assertEquals("http://test.com/foo1", coordinator.peekUrl());
    coordinator.getVoteBlock("http://test.com/foo1", 0);
    assertEquals("http://test.com/foo2", coordinator.peekUrl());
    try {
      coordinator.getVoteBlock("http://test.com/goo", 0);
      fail("Expected exception not thrown.");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  public void testIteratorIOException() throws Exception {

    final class IOExceptionVoteBlocksIterator implements VoteBlocksIterator {
      final VoteBlocksIterator iterator;
      final String failAt;

      boolean threw = false;
      boolean released = false;

      IOExceptionVoteBlocksIterator(VoteBlocksIterator iterator,
				    String failAt) {
	this.iterator = iterator;
	this.failAt = failAt;
      }

      void checkFail() throws IOException {
	if (failAt.equals(iterator.peek().getUrl())) {
	  // We only get here once.
	  assertFalse(threw);
	  threw = true;
	  throw new IOException("Expected exception.");
	}
      }
	    
      @Override public boolean hasNext() throws IOException {
	checkFail();
	return iterator.hasNext();
      }
      @Override public VoteBlock next() throws IOException {
	checkFail();
	return iterator.next();
      }
      @Override public VoteBlock peek() throws IOException {
	checkFail();
	return iterator.peek();
      }
      @Override public void release() {
	// We only get here once.
	assertFalse(released);
	released = true;
	releaseForReal();
      }

      void releaseForReal() {
	iterator.release();
      }
    }
    
    // A DiskVoteBlocks that has an iterator that throws IOException
    // when it reaches the given URL.
    final class IOExceptionDiskVoteBlocks extends DiskVoteBlocks {
      final String failAt;

      IOExceptionDiskVoteBlocks(File toDir,
				VoteBlock[] voteBlocks, String failAt)
	  throws IOException {
	super(toDir);
	for (int i = 0; i < voteBlocks.length; i++) {
	  addVoteBlock(voteBlocks[i]);
	}
	this.failAt = failAt;
      }
      
      @Override public IOExceptionVoteBlocksIterator iterator()
	  throws FileNotFoundException {
	return new IOExceptionVoteBlocksIterator(super.iterator(), failAt);
      }
    };

    VoteBlock[][] voteBlocks = {
      {
	makeVoteBlock("http://test.com/foo1", "content for foo1"),
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
	makeVoteBlock("http://test.com/foo3", "content for foo3")
      },
      {
	makeVoteBlock("http://test.com/foo1", "content for foo1"),
	makeVoteBlock("http://test.com/foo2", "content for foo2"),
	makeVoteBlock("http://test.com/foo3", "content for foo3")
      }
    };
    
    iterators = makeIterators(voteBlocks);
    // Add a throwing iterator to the collection
    IOExceptionDiskVoteBlocks ioeVoteBlocks = 
      new IOExceptionDiskVoteBlocks(tempDir,
				    voteBlocks[0], "http://test.com/foo2");
    IOExceptionVoteBlocksIterator ioeIterator = ioeVoteBlocks.iterator();
    iterators.add(ioeIterator);
    VoteBlocksCoordinator coordinator = new VoteBlocksCoordinator(iterators);

    try {
      assertEquals("http://test.com/foo1", coordinator.peekUrl());
      assertFalse(coordinator.isSpoiled(2));
      assertFalse(ioeIterator.threw);
      assertFalse(ioeIterator.released);
      coordinator.seek("http://test.com/foo2");
      assertTrue(ioeIterator.threw);
      assertTrue(ioeIterator.released);
      assertTrue(coordinator.isSpoiled(2));
      // peekUrl() doesn't throw anything, but there's no URL since the
      // iterator threw IOException.
      assertEquals("http://test.com/foo2", coordinator.peekUrl());
    } finally {
      // Remove iterator so tearDown won't release it, which might throw.
      // Release the DiskVoteBlocks directly.
      iterators.remove(ioeIterator);
      ioeIterator.releaseForReal();
    }
  }

  // Make a collection of iterators, each with the given VoteBlock instances.
  private List<VoteBlocksIterator>
    makeIterators(VoteBlock[][] voteBlockArrays) throws Exception {
    List<VoteBlocksIterator> iterators = new ArrayList<VoteBlocksIterator>();
    for (VoteBlock[] voteBlockArray: voteBlockArrays) {
      iterators.add(makeDiskVoteBlocks(voteBlockArray).iterator());
    }
    return iterators;
  }

  // Find and return the VoteBlock with the given URL.  Note: this
  // will return the URL even if the blocks are not in canonical
  // order/.
  private VoteBlock findVoteBlock(String url, VoteBlock[] voteBlocks) {
    for (int i = 0; i < voteBlocks.length; i++) {
      if (voteBlocks[i].getUrl().equals(url)) {
	return voteBlocks[i];
      }
    }
    return null;
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
  
  private DiskVoteBlocks makeDiskVoteBlocks(VoteBlock [] votes)
      throws Exception {
    DiskVoteBlocks vb = new DiskVoteBlocks(tempDir);
    for (int i = 0; i < votes.length; i++) {
      vb.addVoteBlock(votes[i]);
    }
    return vb;
  }
}
