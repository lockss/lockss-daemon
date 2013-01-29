/*
 * $Id: TestVoteBlocksTallier.java,v 1.1.2.3 2013-01-29 16:55:42 dshr Exp $
 */

/*

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.protocol.*;
import org.lockss.poller.*;


public class TestVoteBlocksTallier extends LockssTestCase {
  MyContent[] content = null;
  MockLockssDaemon daemon;
  private File tempDir;
  String tempDirPath;

  public void setUp() throws Exception {
    super.setUp();
    daemon = getMockLockssDaemon();
    tempDir = getTempDir();
    tempDirPath = tempDir.getAbsolutePath();
    Properties p = new Properties();
    p.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    p.setProperty(V3Poller.PARAM_STATE_PATH, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(p);
    IdentityManager idMgr = new V3TestUtils.NoStoreIdentityManager();
    daemon.setIdentityManager(idMgr);
    idMgr.initService(daemon);
    content = new MyContent[20];
    for (int i = 0 ; i < content.length ; i++ ) {
      content[i] = new MyContent("http://www.example.com/test-" + i + ".html");
    }
  }

  public void testNoneAgree() throws Exception {
    doTest(-1, -1, 0, 0, -1, -1, 0, 0, 100);
  }
  
  public void testOneAgree() throws Exception {
    doTest(-1, -1, 0, 1, -1, -1, 0, 1, 100);
  }
  
  public void testTwoAgree() throws Exception {
    doTest(-1, -1, 0, 2, -1, -1, 0, 2, 100);
  }
  
  public void testTenAgree() throws Exception {
    doTest(-1, -1, 0, 10, -1, -1, 0, 10, 100);
  }
  
  public void testThreeOneDisagree() throws Exception {
    doTest(-1, -1, 0, 3, -1, -1, 0, 3, 1);
  }
  
  public void testVThreePFour() throws Exception {
    doTest(-1, -1, 0, 3, -1, -1, 0, 4, 100);
  }
  
  public void testVFourPThree() throws Exception {
    doTest(-1, -1, 0, 4, -1, -1, 0, 3, 100);
  }
  
  public void testVFourPThreeThreeVersion() throws Exception {
    doTest(-1, -1, 0, 4, -1, -1, 0, 3, 100, 3, 3);
  }
  
  public void testVFourPThreeThreeVsFiveVersion() throws Exception {
    doTest(-1, -1, 0, 4, -1, -1, 0, 3, 100, 3, 5);
  }
  
  public void testVFourPThreeFiveVsThreeVersion() throws Exception {
    doTest(-1, -1, 0, 4, -1, -1, 0, 3, 100, 5, 3);
  }
  
  public void testVFourPThreeMinusThreeVsFiveVersion() throws Exception {
    doTest(-1, -1, 0, 4, -1, -1, 0, 3, 100, -3, 5);
  }
  
  public void testVFourPThreeFiveVsMinusThreeVersion() throws Exception {
    doTest(-1, -1, 0, 4, -1, -1, 0, 3, 100, 5, -3);
  }
  
  public void testVFourPThreeDisagreeTwoMinusThreeVsFiveVersion() throws Exception {
    doTest(-1, -1, 0, 4, -1, -1, 0, 3, 2, -3, 5);
  }
  
  public void testVFourPThreeDisagreeTwoFiveVsMinusThreeVersion() throws Exception {
    doTest(-1, -1, 0, 4, -1, -1, 0, 3, 2, 5, -3);
  }
  
  public void testVFourPThreeThreeVsMinusFiveVersion() throws Exception {
    doTest(-1, -1, 0, 4, -1, -1, 0, 3, 100, 3, -5);
  }
  
  public void testVFourPThreeMinusFiveVsThreeVersion() throws Exception {
    doTest(-1, -1, 0, 4, -1, -1, 0, 3, 100, -5, 3);
  }
  
  public void testVFourPThreeMinusThreeVsMinusFiveVersion() throws Exception {
    doTest(-1, -1, 0, 4, -1, -1, 0, 3, 100, -3, -5);
  }
  
  public void testVFourPThreeMinusFiveVsMinusThreeVersion() throws Exception {
    doTest(-1, -1, 0, 4, -1, -1, 0, 3, 100, -5, -3);
  }
  
  private int min(int a, int b) {
    return ( a < b ? a : b );
  }

  private int max(int a, int b) {
    return ( a > b ? a : b );
  }

  private int atLeastZero(int a) {
    return ( a > 0 ? a : 0 );
  }

  private int abs(int a) {
    return ( a > 0 ? a : -a );
  }

  /**
   * Execute a test in which the poller has URLs:
   * content[p1..p2-1] and content[p3..p4-1]
   * and the voter has URLs:
   * content[v1..v2-1] and content[v3..v4-1]
   * and they disagree about URL: content[d]
   */
  private void doTest(int v1, int v2, int v3, int v4,
		      int p1, int p2, int p3, int p4, int d) {
    doTest(v1, v2, v3, v4, p1, p2, p3, p4, d, 1, 1);
  }

  /**
   * Execute a test in which the poller has URLs:
   * content[p1..p2-1] and content[p3..p4-1]
   * and the voter has URLs:
   * content[v1..v2-1] and content[v3..v4-1]
   * The voter has vVer versions and the poller has pVer versions,
   * and they disagree about URL: content[d]
   */
  private void doTest(int v1, int v2, int v3, int v4,
		      int p1, int p2, int p3, int p4,
		      int d, int vVer, int pVer) {
    VoteBlocks vBlocks = new MyVoteBlocks(v1, v2, v3, v4, d, vVer);
    assertNotNull(vBlocks);
    assert(vBlocks.size() >= 0);
    VoteBlocks pBlocks = new MyVoteBlocks(p1, p2, p3, p4, 100, pVer);
    assertNotNull(pBlocks);
    assert(pBlocks.size() >= 0);
    VoteBlocksTallier vbt = new VoteBlocksTallier(vBlocks, pBlocks);
    int numAgree = vbt.countAgreeUrl();
    int shouldAgree = 0;
    if (min(v1,v2) >= 0 && min(p1,p2) >= 0) {
      shouldAgree += atLeastZero(min(v2,p2) - max(v1,p1));
    }
    if (min(v3,v4) >= 0 && min(p3,p4) >= 0) {
      shouldAgree += atLeastZero(min(v4,p4) - max(v3,p3));
    }
    int numDisagree = vbt.countDisagreeUrl();
    int shouldDisagree = 0;
    if (max(v1,p1) <= d && d <= min(v2,p2)) {
      // The URL they disagree on is in the first range they both have.
      shouldAgree--;
      shouldDisagree++;
    } else if (max(v3,p3) <= d && d <= min(v4,p4)) {
      // The URL they disagree on is in the second range they both have.
      shouldAgree--;
      shouldDisagree++;
    }
    int numVoterOnly = vbt.countVoterOnlyUrl();
    int shouldVoterOnly = atLeastZero(p1 - v1) + atLeastZero(v2 - p2) +
      atLeastZero(p3 - v3) + atLeastZero(v4 - p4);
    int numPollerOnly = vbt.countPollerOnlyUrl();
    int shouldPollerOnly = atLeastZero(v1 - p1) + atLeastZero(p2 - v2) +
      atLeastZero(v3 - p3) + atLeastZero(p4 - v4);
    assertEquals(shouldAgree, numAgree);
    assertEquals(shouldDisagree, numDisagree);
    assertEquals(shouldPollerOnly, numPollerOnly);
    assertEquals(shouldVoterOnly, numVoterOnly);
  }

  private class MyContent {
    String url;
    byte[] plainHash;
    byte[] noncedHash;

    MyContent(String url) {
      this.url = url;
      this.plainHash = ByteArray.makeRandomBytes(20);
      this.noncedHash = ByteArray.makeRandomBytes(20);
    }
  }

  public class MyVoteBlocks implements VoteBlocks {
    ArrayList<VoteBlock> blocks = new ArrayList<VoteBlock>();
    String url;
    
    MyVoteBlocks(int b1, int b2, int b3, int b4, int d, int numVer) {
      if (b1 >= 0) {
	for (int i = b1; i < b2; i++) {
	  VoteBlock vb = new VoteBlock(content[i].url);
	  for (int j = 0; j < (numVer-1); j++) {
	    vb.addVersion(0, 1000, // filtered offset/length
			  0, 1000, // unfiltered offset/length
			  ByteArray.makeRandomBytes(20),
			  ByteArray.makeRandomBytes(20),
			  false); // Hash error
	  }
	  if (i != d) {
	    vb.addVersion(0, 1000, // filtered offset/length
			  0, 1000, // unfiltered offset/length
			  content[i].plainHash,
			  content[i].noncedHash,
			  false); // Hash error
	  } else {
	    // Voter & poller disagree on content[d]
	    vb.addVersion(0, 1000, // filtered offset/length
			  0, 1000, // unfiltered offset/length
			  ByteArray.makeRandomBytes(20),
			  ByteArray.makeRandomBytes(20),
			  false); // Hash error
	  }
	  if (numVer < 0) {
	    numVer = -numVer;
	  }
	  for (int j = 0; j < (numVer-1); j++) {
	    vb.addVersion(0, 1000, // filtered offset/length
			  0, 1000, // unfiltered offset/length
			  ByteArray.makeRandomBytes(20),
			  ByteArray.makeRandomBytes(20),
			  false); // Hash error
	  }
	  blocks.add(vb);
	}
      }
      if (b3 >= 0) {
	for (int i = b3; i < b4; i++) {
	  VoteBlock vb = new VoteBlock(content[i].url);
	  if (i != d) {
	    vb.addVersion(0, 1000, // filtered offset/length
			  0, 1000, // unfiltered offset/length
			  content[i].plainHash,
			  content[i].noncedHash,
			  false); // Hash error
	  } else {
	    // Voter & poller disagree on content[d]
	    vb.addVersion(0, 1000, // filtered offset/length
			  0, 1000, // unfiltered offset/length
			  ByteArray.makeRandomBytes(20),
			  ByteArray.makeRandomBytes(20),
			  false); // Hash error
	  }
	  blocks.add(vb);
	}
      }
    }

    public void addVoteBlock(VoteBlock vb) {
      blocks.add(vb);
    }

    public InputStream getInputStream() throws IOException {
      throw new IOException("not implemented");
    }

    public VoteBlock getVoteBlock(String url) {
      for (VoteBlock vb : blocks) {
	if (vb.getUrl().equals(url)) {
	  return vb;
	}
      }
      return null;
    }
 
    public VoteBlocksIterator iterator() throws FileNotFoundException {
      return new MyVoteBlocksIterator(blocks);
    }

    public int size() {
      return blocks.size();
    }

    public long getEstimatedEncodedLength() {
      return 1000;
    }

    public void release() {
    }
  }
    
  class MyVoteBlocksIterator implements VoteBlocksIterator {
    ArrayList<VoteBlock> list;
    int index;

    MyVoteBlocksIterator(ArrayList<VoteBlock> list) {
      this.list = list;
      index = 0;
    }

    public boolean hasNext() {
      return (index < list.size());
    }
  
    public VoteBlock next() {
      if (index < list.size()) {
	return list.get(index++);
      }
      throw new NoSuchElementException();
    }
  
    public VoteBlock peek() {
      if (index < list.size()) {
	return list.get(index);
      }
      return null;
    }

    public void release() {
    }
  }

}
