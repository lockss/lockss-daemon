package org.lockss.protocol;

import java.io.*;
import java.util.*;

import org.lockss.test.LockssTestCase;


public class TestDiskVoteBlocks extends LockssTestCase {

  File tempDir;
  
  protected void setUp() throws Exception {
    super.setUp();
    tempDir = this.getTempDir();
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /*
   * Test method for 'org.lockss.protocol.DiskVoteBlocks.DiskVoteBlocks(int, InputStream, File)'
   */
  public void testDiskVoteBlocks() throws Exception {
//  Construct a byte array in memory to read in.
    int blockCount = 20;
    
    List voteBlockList = V3TestUtils.makeVoteBlockList(blockCount);

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(bos);
    
    for (Iterator iter = voteBlockList.iterator(); iter.hasNext(); ) {
      VoteBlock vb = (VoteBlock)iter.next();
      dos.write(vb.getEncoded());
    }
    
    byte[] encodedBlocks = bos.toByteArray();
    ByteArrayInputStream bis = new ByteArrayInputStream(encodedBlocks);

    DiskVoteBlocks dvb = new DiskVoteBlocks(blockCount, bis, tempDir);
    
    assertEquals(dvb.size(), blockCount);
  }
  
  /*
   * Test method for 'org.lockss.protocol.DiskVoteBlocks.addVoteBlock(VoteBlock)'
   */
  public void testAddVoteBlock() throws Exception {
    DiskVoteBlocks dvb = new DiskVoteBlocks(tempDir);
    assertEquals(0, dvb.size());

    List voteBlockList = V3TestUtils.makeVoteBlockList(3);
    
    // Add first block.
    dvb.addVoteBlock((VoteBlock)voteBlockList.get(0));
    assertEquals(1, dvb.size());
    assertEquals((VoteBlock)voteBlockList.get(0), dvb.getVoteBlock(0));
    
    dvb.addVoteBlock((VoteBlock)voteBlockList.get(1));
    assertEquals(2, dvb.size());
    assertEquals((VoteBlock)voteBlockList.get(1), dvb.getVoteBlock(1));
    
    dvb.addVoteBlock((VoteBlock)voteBlockList.get(2));
    assertEquals(3, dvb.size());
    assertEquals((VoteBlock)voteBlockList.get(2), dvb.getVoteBlock(2));
  }

  /*
   * Test method for 'org.lockss.protocol.DiskVoteBlocks.iterator()'
   */
  public void testIterator() throws Exception {
    
    List<VoteBlock> voteBlockList = V3TestUtils.makeVoteBlockList(3);
    
    DiskVoteBlocks dvb = makeDiskVoteBlocks(voteBlockList);
    
    VoteBlocksIterator iter = dvb.iterator();

    // First
    assertTrue(iter.hasNext());
    // Be sure peek works.
    VoteBlock p1 = (VoteBlock)iter.peek();
    VoteBlock p2 = (VoteBlock)iter.peek();
    VoteBlock p3 = (VoteBlock)iter.peek();
    VoteBlock vb0 = (VoteBlock)iter.next();
    assertEquals(p1, vb0);
    assertEquals(p2, vb0);
    assertEquals(p3, vb0);
    assertNotNull(vb0);
    assertEquals(vb0, (VoteBlock)voteBlockList.get(0));
    
    // Second
    assertTrue(iter.hasNext());
    VoteBlock vb1 = (VoteBlock)iter.next();
    assertNotNull(vb1);
    assertEquals(vb1, (VoteBlock)voteBlockList.get(1));
    
    // Third
    assertTrue(iter.hasNext());
    VoteBlock vb2 = (VoteBlock)iter.next();
    assertNotNull(vb2);
    assertEquals(vb2, (VoteBlock)voteBlockList.get(2));
    
    // Shouldn't be a next.
    assertFalse(iter.hasNext());

    // Shouldn't increment
    assertNull(iter.next());
    assertNull(iter.next());
    assertNull(iter.next());
  }
  
  /*
   * This test verifies how many times the iterator reads each item,
   * even when the iterator has to restart several times.
   * 
   * Unlike the previous test, this test sets b0, b1, and b2 from the
   * vote block list directly.
   */
  private static final int k_num_vote_blocks = 5;
  private static final int k_num_random_blocks = 100;
  
  public void testGetVoteBlock2() throws Exception {
    VoteBlock[] arvb = new VoteBlock[k_num_vote_blocks];
    DiskVoteBlocks dvb;
    int i;
    VoteBlocksIterator iter;
    int randomBlock;
    List<VoteBlock> voteBlockList;
    
    // Create the list of vote blocks and the blocks themselves.
    voteBlockList = V3TestUtils.makeVoteBlockList(k_num_vote_blocks);
    for (i = 0; i < k_num_vote_blocks; i++) {
      arvb[i] = voteBlockList.get(i);
    }
    
    // Put them on the disk.
    dvb = makeDiskVoteBlocks(voteBlockList);

    // Test: The old version of the software does NOT reset the list of 
    // votes.  Therefore, the old version would probably fail on this test...
    assertEquals(arvb[1], dvb.getVoteBlock(1));
    assertEquals(arvb[0], dvb.getVoteBlock(0));
    assertEquals(arvb[3], dvb.getVoteBlock(3));  

    // Test: Run the list of vote blocks BACKWARDS, and make sure that we have the right
    // ones...
    for (i = k_num_vote_blocks - 1; i >= 0; i--) {
      assertEquals(arvb[i], dvb.getVoteBlock(i));
    }
    
    // Since I don't know certainly what to test, why not throw a bunch of random tests?
    for (i= 0; i < k_num_random_blocks; i++) {
      randomBlock = (int) (Math.random() * k_num_vote_blocks);
      assertEquals(arvb[randomBlock], dvb.getVoteBlock(randomBlock));
    }
  }

  /*
   * Test method for 'org.lockss.protocol.DiskVoteBlocks.getVoteBlock(int)'
   */
  public void testGetVoteBlock() throws Exception {
    List voteBlockList = V3TestUtils.makeVoteBlockList(10);
    DiskVoteBlocks dvb = makeDiskVoteBlocks(voteBlockList);

    for (int i = 0; i <  voteBlockList.size(); i++) {
      assertEquals((VoteBlock)voteBlockList.get(i), dvb.getVoteBlock(i));
    }
    
  }

  /*
   * Test method for 'org.lockss.protocol.DiskVoteBlocks.size()'
   */
  public void testSize() throws Exception {
    List voteBlockList = V3TestUtils.makeVoteBlockList(10);
    DiskVoteBlocks dvb = makeDiskVoteBlocks(voteBlockList);
    assertEquals(dvb.size(), 10);
  }

  /*
   * Test method for 'org.lockss.protocol.DiskVoteBlocks.delete()'
   */
  public void testDelete() {
    // XXX:  To do.
  }

  /*
   * Test method for 'org.lockss.protocol.DiskVoteBlocks.getInputStream()'
   */
  public void testGetInputStream() throws Exception {
    List voteBlockList = V3TestUtils.makeVoteBlockList(10);
    DiskVoteBlocks dvb = makeDiskVoteBlocks(voteBlockList);
    InputStream is = dvb.getInputStream();
    
    assertNotNull(is);
    assertTrue(is instanceof InputStream);
  }
  
  private DiskVoteBlocks makeDiskVoteBlocks(List voteBlockList)
      throws Exception {
    DiskVoteBlocks dvb = new DiskVoteBlocks(tempDir);
    for (Iterator iter = voteBlockList.iterator(); iter.hasNext(); ) {
      VoteBlock vb = (VoteBlock)iter.next();
      dvb.addVoteBlock(vb);
    }
    return dvb;
  }

}
