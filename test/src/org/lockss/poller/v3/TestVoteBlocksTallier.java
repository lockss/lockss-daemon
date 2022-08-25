/*
 * $Id$
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
import org.lockss.protocol.*;


public class TestVoteBlocksTallier extends LockssTestCase {
  List<VoteBlock> sharedVoteBlocks = new ArrayList<VoteBlock>();
  MyV3Voter voter;


  public void setUp() throws Exception {
    super.setUp();
    for (int i = 0 ; i < 20 ; i++ ) {
      sharedVoteBlocks.add(makeVoteBlock(i));
    }
  }

  /**
   * Test the two VoteBlocks against each other, and verify that the
   * tallies are as expected.
   */
  private void doTest(VoteBlocks vBlocks, VoteBlocks pBlocks,
		      int agree, int disagree, int vOnly, int pOnly)
      throws Exception {
    doTest(vBlocks, pBlocks, agree, disagree, vOnly, pOnly,
	   -1.0f, -1.0f);
  }

  private void doTest(VoteBlocks vBlocks, VoteBlocks pBlocks,
		      int agree, int disagree, int vOnly, int pOnly,
		      double pAgree, double wAgree)
      throws Exception {
    voter = new MyV3Voter();
    if (wAgree >= 0.0) {
      voter.setUrlPsllResultMap(PatternFloatMap.fromSpec("01,0.5"));
    }
    VoteBlocksTallier vbt = VoteBlocksTallier.make(voter);
    vbt.tallyVoteBlocks(vBlocks, pBlocks);
    assertEquals("AGREE",
		 agree, vbt.getCount(VoteBlocksTallier.Category.AGREE));
    assertEquals("DISAGREE",
		 disagree, vbt.getCount(VoteBlocksTallier.Category.DISAGREE));
    assertEquals("VOTER_ONLY",
		 vOnly, vbt.getCount(VoteBlocksTallier.Category.VOTER_ONLY));
    assertEquals("POLLER_ONLY",
		 pOnly, vbt.getCount(VoteBlocksTallier.Category.POLLER_ONLY));
    if (pAgree >= 0.0) {
      assertEquals("AGREEMENT", pAgree, (double)vbt.percentAgreement(),
		   0.0000001);
    }
    if (wAgree >= 0.0) {
      assertEquals("WEIGHTED AGREEMENT",
		   wAgree, (double)vbt.weightedPercentAgreement(),
		   0.0000001);
    }
  }

  /**
   * Test all URLs agree.
   */
  public void testAllAgree() throws Exception {
    VoteBlocksBuilder builder;
    builder = new VoteBlocksBuilder();
    builder.addShared(0, 0);
    doTest(builder.build(), builder.build(), 0, 0, 0, 0, 0.0, 0.0);
    for (int size = 1; size < 10; size++) {
      builder = new VoteBlocksBuilder();
      builder.addShared(0, size);
      doTest(builder.build(), builder.build(), size, 0, 0, 0, 1.0, 1.0);
    }
  }
  
  double expWAgree(int size, int loc) {
    return (loc == 1) ? 1.0 - 0.5 / (size - 0.5) : 1.0 - 1.0 / (size - 0.5);
  }

  /**
   * Test same size, but one disagrees
   */
  public void testOneDisagree() throws Exception {
    for (int size = 1; size <= 4; size++) {
      for (int loc = 0; loc <= size-1; loc++) {
	VoteBlocksBuilder builder = new VoteBlocksBuilder();
	builder.addShared(0, size);
	VoteBlocks vBlocks = builder.build();
	builder.addUnshared(loc);
	VoteBlocks pBlocks = builder.build();
	doTest(vBlocks, pBlocks, size-1, 1, 0, 0,
	       1.0 - 1.0 / size,
	       expWAgree(size, loc));
      }
    }
  }
  
  /**
   * Test one missing at poller.
   */
  public void testPollerMissingOne() throws Exception {
    for (int size = 1; size < 5; size++) {
      for (int loc = 0; loc < size-1; loc++) {
	VoteBlocksBuilder builder = new VoteBlocksBuilder();
	builder.addShared(0, size);
	VoteBlocks vBlocks = builder.build();
	builder.remove(loc);
	VoteBlocks pBlocks = builder.build();
	doTest(vBlocks, pBlocks, size-1, 0, 1, 0,
	       1.0 - 1.0 / size,
	       expWAgree(size, loc));
      }
    }
  }
  
  /**
   * Test one missing at voter.
   */
  public void testVoterMissingOne() throws Exception {
    for (int size = 1; size < 5; size++) {
      for (int loc = 0; loc < size-1; loc++) {
	VoteBlocksBuilder builder = new VoteBlocksBuilder();
	builder.addShared(0, size);
	VoteBlocks pBlocks = builder.build();
	builder.remove(loc);
	VoteBlocks vBlocks = builder.build();
	doTest(vBlocks, pBlocks, size-1, 0, 0, 1,
	       1.0 - 1.0 / size,
	       expWAgree(size, loc));
      }
    }
  }
  
  /**
   * Test empty poller.
   */
  public void testEmptyPoller() throws Exception {
    for (int size = 1; size < 5; size++) {
      VoteBlocksBuilder builder = new VoteBlocksBuilder();
      VoteBlocks pBlocks = builder.build();
      builder.addShared(0, size);
      VoteBlocks vBlocks = builder.build();
      doTest(vBlocks, pBlocks, 0, 0, size, 0, 0.0, 0.0);
    }
  }
  
  /**
   * Test empty voter.
   */
  public void testEmptyVoter() throws Exception {
    for (int size = 1; size < 5; size++) {
      VoteBlocksBuilder builder = new VoteBlocksBuilder();
      VoteBlocks vBlocks = builder.build();
      builder.addShared(0, size);
      VoteBlocks pBlocks = builder.build();
      doTest(vBlocks, pBlocks, 0, 0, 0, size, 0.0, 0.0);
    }
  }

  /**
   * Test when one is missing from the voter and one disagrees.
   */
  public void testVoterMissingOneDisagree() throws Exception {
    final int size = 5;
    for (int diff = 0; diff < size; diff++) {
      for (int miss = 0; miss < size; miss++) {
	if (miss != diff) {
	  VoteBlocksBuilder builder = new VoteBlocksBuilder();
	  builder.addShared(0, size);
	  VoteBlocks pBlocks = builder.build();
	  builder.remove(miss).addUnshared(diff);
	  VoteBlocks vBlocks = builder.build();
	  doTest(vBlocks, pBlocks, size-2, 1, 0, 1, 1 - 2.0 / 5.0, -1.0);
	}
      }
    }
  }

  /**
   * Test when one is missing from the poller and one disagrees.
   */
  public void testPollerMissingOneDisagree() throws Exception {
    final int size = 5;
    for (int diff = 0; diff < size; diff++) {
      for (int miss = 0; miss < size; miss++) {
	if (miss != diff) {
	  VoteBlocksBuilder builder = new VoteBlocksBuilder();
	  builder.addShared(0, size);
	  VoteBlocks vBlocks = builder.build();
	  builder.remove(miss).addUnshared(diff);
	  VoteBlocks pBlocks = builder.build();
	  doTest(vBlocks, pBlocks, size-2, 1, 1, 0, 0.6, -1.0);
	}
      }
    }
  }
  
  /**
   * Test when one is missing from each of the voter and poller, and
   * there is also a disagreement.
   */
  public void testBothMissingDisagree() throws Exception {
    final int size = 5;
    for (int diff = 0; diff < size; diff++) {
      for (int pMiss = 0; pMiss < size; pMiss++) {
	for (int vMiss = 0; vMiss < size; vMiss++) {
	  if (vMiss != pMiss &&
	      pMiss != diff &&
	      diff != vMiss) {
	    VoteBlocksBuilder builder = new VoteBlocksBuilder();
	    builder.addShared(0, size).remove(vMiss);
	    VoteBlocks vBlocks = builder.build();
	    builder = new VoteBlocksBuilder();
	    builder.addShared(0, size).remove(pMiss).addUnshared(diff);
	    VoteBlocks pBlocks = builder.build();
	    doTest(vBlocks, pBlocks, size-3, 1, 1, 1, 0.4, -1.0);
	  }
	}
      }
    }
  }

  // The VoteBlocksTallier uses VoteBlockVoteBlockComparerFactory and
  // VoteBlockComparer to deal with versions. More extensive testing
  // of matching with versions is in testing for that class.

  /**
   * Test a single URL with multiple versions, none shared.
   */
  public void testNoMatchVersions() throws Exception {
    int size = 5;
    String url = "http://www.example.com/test-00.html";
    VoteBlock vVoteBlock = new VoteBlock(url);
    VoteBlock pVoteBlock = new VoteBlock(url);
    for (int i = 0; i < size; i++) {
      vVoteBlock.addVersion(0, 1000, // filtered offset/length
			    0, 1000, // unfiltered offset/length
			    ByteArray.makeRandomBytes(20), // Plain hash
			    ByteArray.makeRandomBytes(20), // Nonced hash
			    false); // Hash error
      pVoteBlock.addVersion(0, 1000, // filtered offset/length
			    0, 1000, // unfiltered offset/length
			    ByteArray.makeRandomBytes(20), // Plain hash
			    ByteArray.makeRandomBytes(20), // Nonced hash
			    false); // Hash error
    }
    MyVoteBlocks vBlocks = new MyVoteBlocks(ListUtil.list(vVoteBlock));
    MyVoteBlocks pBlocks = new MyVoteBlocks(ListUtil.list(pVoteBlock));
    doTest(vBlocks, pBlocks, 0, 1, 0, 0, 0.0, 0.0);
  }

  /**
   * Test a single URL with multiple versions, one shared.
   */
  public void testMatchOneVersion() throws Exception {
    int size = 5;
    String url = "http://www.example.com/test-00.html";
    VoteBlock vVoteBlock = new VoteBlock(url);
    VoteBlock pVoteBlock = new VoteBlock(url);

    for (int agree = 0; agree < size; agree++) {
      for (int i = 0; i < size; i++) {
	byte[] plainHash = ByteArray.makeRandomBytes(20);
	byte[] noncedHash = ByteArray.makeRandomBytes(20);
      
	vVoteBlock.addVersion(0, 1000, // filtered offset/length
			      0, 1000, // unfiltered offset/length
			      plainHash,
			      noncedHash,
			      false); // Hash error
	// At every version except "agree", generate new hash
	// values. Version number "agree" is shared between poller and
	// voter.
	if (i != agree) {
	  plainHash = ByteArray.makeRandomBytes(20);
	  noncedHash = ByteArray.makeRandomBytes(20);
	}
	pVoteBlock.addVersion(0, 1000, // filtered offset/length
			      0, 1000, // unfiltered offset/length
			      plainHash,
			      noncedHash,
			      false); // Hash error
      }
      MyVoteBlocks vBlocks = new MyVoteBlocks(ListUtil.list(vVoteBlock));
      MyVoteBlocks pBlocks = new MyVoteBlocks(ListUtil.list(pVoteBlock));
      doTest(vBlocks, pBlocks, 1, 0, 0, 0, 1.0, 1.0);
    }
  }

  /**
   * Construct a VoteBlock for the given index, with precisely one
   * version.
   */
  private VoteBlock makeVoteBlock(int i) {
    // Format to make sure the URLs in numerical order are also in
    // canonical order.
    if (i > 99 || i < 0) {
      fail("Unacceptable value for i: "+i);
    }
    String url = String.format("http://www.example.com/test-%02d.html", i);
    VoteBlock vb = new VoteBlock(url);
    vb.addVersion(0, 1000, // filtered offset/length
		  0, 1000, // unfiltered offset/length
		  ByteArray.makeRandomBytes(20), // Plain hash
		  ByteArray.makeRandomBytes(20), // Nonced hash
		  false); // Hash error
    return vb;
  }

  /**
   * A helper class to create {@link VoteBlock}s instances. Shared
   * {@link VoteBlock} instances will be drawn from {@link
   * sharedVoteBlocks}. Unshared {@link VoteBlock} instances will have
   * random byte arrays for their hashes.
   */
  private class VoteBlocksBuilder {
    /** A sorted set of versions to include in the {@link VoteBlocks}. */
    private final TreeSet<Integer> included = new TreeSet<Integer>();
    /** A subset of {@link #included} which should not be pulled from
     * {@link sharedVoteBlocks}. */
    private final Set<Integer> unshared = new HashSet<Integer>();

    /**
     * Return the {@link MyVoteBlocks} instance as specified.
     */
    public VoteBlocks build() {
      ArrayList<VoteBlock> blocks = new ArrayList<VoteBlock>();
      for (int i: included) {
	// Note: If some test asks for a block out of range, throw and
	// go fix the test.
	VoteBlock voteBlock = unshared.contains(i)
	  ? makeVoteBlock(i)
	  : sharedVoteBlocks.get(i);
	blocks.add(voteBlock);
      }
      return new MyVoteBlocks(blocks);
    }

    private void checkArgs(int start, int end) {
      assertTrue("start must be non-negative", 0 <= start);
      assertTrue("end must be less than VoteBlocks.size()",
		 end < sharedVoteBlocks.size());
      assertTrue("start must be less than or equal to end", start <= end);
    }

    private void checkArgs(int index) {
      assertTrue("index must be non-negative", 0 <= index);
      assertTrue("index must be less than VoteBlocks.size()",
		 index < sharedVoteBlocks.size());
    }

    /**
     * Add {@link VoteBlock}s from {@link sharedVoteBlocks} for the
     * URLs from {@code start} (inclusive) to {@code end} (exclusive).
     */
    public VoteBlocksBuilder addShared(int start, int end) {
      checkArgs(start, end);
      for (int i = start; i < end; i++) {
	included.add(i);
	unshared.remove(i);
      }
      return this;
    }

    /**
     * Add the {@link VoteBlock} from {@link sharedVoteBlocks} for URL
     * {@code index}.
     */
    public VoteBlocksBuilder addShared(int index) {
      checkArgs(index);
      return addShared(index, index+1);
    }

    /**
     * Add unique {@link VoteBlock}s for the URLs from {@code start}
     * (inclusive) to {@code end} (exclusive).
     */
    public VoteBlocksBuilder addUnshared(int start, int end) {
      checkArgs(start, end);
      for (int i = start; i < end; i++) {
	included.add(i);
	unshared.add(i);
      }
      return this;
    }

    /**
     * Add a unique {@link VoteBlock} for URL {@code index}.
     */
    public VoteBlocksBuilder addUnshared(int index) {
      checkArgs(index);
      return addUnshared(index, index+1);
    }

    /**
     * Remove {@link VoteBlock}s for the URLs from {@code start}
     * (inclusive) to {@code end} (exclusive).
     */
    public VoteBlocksBuilder remove(int start, int end) {
      checkArgs(start, end);
      for (int i = start; i < end; i++) {
	included.remove(i);
	unshared.remove(i);
      }
      return this;
    }

    /**
     * Remove the {@link VoteBlock} for URL {@code index}.
     */
    public VoteBlocksBuilder remove(int index) {
      checkArgs(index);
      return remove(index, index+1);
    }

  }

  //      resultWeightMap = getAu().makeUrlPollResultWeightMap();


  private class MyV3Voter extends V3Voter {
    void setUrlPsllResultMap(PatternFloatMap pfm) {
      resultWeightMap = pfm;
    }

  }

  /**
   * Implement {@link VoteBlocks}. The only method used is {@code
   * #iterator}.
   */
  private class MyVoteBlocks implements VoteBlocks {
    final List<VoteBlock> blocks;
    
    MyVoteBlocks(List<VoteBlock> blocks) {
      this.blocks = blocks;
    }
 
    public VoteBlocksIterator iterator() {
      return new MyVoteBlocksIterator(blocks);
    }

    public void addVoteBlock(VoteBlock vb) {
      fail("addVoteBlock is not implemented");
    }

    public InputStream getInputStream() {
      fail("getInputStream is not implemented");
      return null;
    }

    public VoteBlock getVoteBlock(String url) {
      fail("getVoteBlock is not implemented");
      return null;
    }

    public int size() {
      fail("size is not implemented");
      return -1;
    }

    public long getEstimatedEncodedLength() {
      fail("getEstimatedEncodedLength is not implemented");
      return -1L;
    }

    public void release() {
      fail("release is not implemented");
    }
  }
    
  class MyVoteBlocksIterator implements VoteBlocksIterator {
    List<VoteBlock> list;
    int index;

    MyVoteBlocksIterator(List<VoteBlock> list) {
      this.list = list;
      index = 0;
    }

    public boolean hasNext() {
      return (index < list.size());
    }
  
    public VoteBlock next() {
      if (hasNext()) {
	return list.get(index++);
      }
      throw new NoSuchElementException();
    }
  
    public VoteBlock peek() {
      if (hasNext()) {
	return list.get(index);
      }
      return null;
    }

    public void release() {
    }
  }
}
