package org.lockss.protocol;

import java.util.*;

/**
 * <p>
 * Maintain an ordered list of vote blocks in a memory-efficient way.
 * </p>
 */

/*
 * At the moment, this class is purely memory-based. In the long run, it will
 * need to be refactored so that it is partially memory-based, and partially
 * disk based.
 */
public class MemoryVoteBlocks implements VoteBlocks {

  // ArrayList<VoteBlock>
  private ArrayList voteBlocks;

  public MemoryVoteBlocks() {
    voteBlocks = new ArrayList();
  }

  public MemoryVoteBlocks(int initialSize) {
    voteBlocks = new ArrayList(initialSize);
  }

  public void addVoteBlock(VoteBlock b) {
    voteBlocks.add(b);
  }

  public VoteBlock getVoteBlock(int i) {
    if (i < voteBlocks.size())
      return (VoteBlock)voteBlocks.get(i);
    else
      return null;
  }

  public ListIterator listIterator() {
    return voteBlocks.listIterator();
  }

  public int size() {
    return voteBlocks.size();
  }

}
