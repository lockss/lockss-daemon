package org.lockss.protocol;

import java.io.*;
import java.util.*;

import org.lockss.util.*;

/**
 * <p>
 * Maintain an ordered list of vote blocks in a memory-efficient way.
 * </p>
 */

public class MemoryVoteBlocks extends BaseVoteBlocks {

  // ArrayList<VoteBlock>
  private ArrayList voteBlocks;

  private static final Logger log = Logger.getLogger("MemoryVoteBlocks");

  public MemoryVoteBlocks() {
    voteBlocks = new ArrayList();
  }

  public MemoryVoteBlocks(int initialSize) {
    voteBlocks = new ArrayList(initialSize);
  }

  /**
   * Construct this VoteBlocks object from an input stream. Do not close the
   * input stream when reading is complete. It is assumed that for in-memory
   * VoteBlocks objects, bytesToRead will fit into an int. If the stream ends
   * before bytesToRead have been consumed, raise an exception.
   *
   */
  public MemoryVoteBlocks(int blocksToRead, InputStream from)
      throws IOException {
    this(blocksToRead);
    DataInputStream dis = new DataInputStream(from);
    for (int blocksRead = 0; blocksRead < blocksToRead; blocksRead++) {
      short len = dis.readShort();
      byte[] encodedVB = new byte[len];
      dis.read(encodedVB);
      VoteBlock vb = new VoteBlock(encodedVB);
      addVoteBlock(vb);
    }
  }

  public void addVoteBlock(VoteBlock b) {
    voteBlocks.add(b);
  }

  public VoteBlock getVoteBlock(int i) {
    return (VoteBlock)voteBlocks.get(i);
  }

  public int size() {
    return voteBlocks.size();
  }
  
  public void release() {
    ; // do nothing
  }
  
  public InputStream getInputStream() throws IOException {
    // Memory VoteBlocks by definition are allowed to exist entirely
    // in memory.
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(bos);
    
    for (VoteBlocksIterator iter = iterator(); iter.hasNext(); ) {
      VoteBlock vb = (VoteBlock)iter.next();
      byte[] encoded = vb.getEncoded();
      dos.writeShort((short)encoded.length);
      dos.write(encoded);
    }
    
    return new ByteArrayInputStream(bos.toByteArray());
  }
}
