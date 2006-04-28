package org.lockss.protocol;

import java.io.*;
import java.util.*;

import org.lockss.util.*;

/**
 * Common functionality for all concrete implementations of VoteBlocks.
 * 
 */
public abstract class BaseVoteBlocks implements VoteBlocks {

  public VoteBlocksIterator iterator() {
    return new BaseVoteBlocks.Iterator();
  }
  
  private class Iterator implements VoteBlocksIterator {
    private int cursor = 0;
    /* For efficiency purposes, store the most recently peeked voteblock. */
    private VoteBlock cachedVoteBlock;
    
    public boolean hasNext() {
      return cursor < size();
    }

    public VoteBlock next() throws IOException {
      if (hasNext()) {
        cachedVoteBlock = null; // Release the cache object
        return getVoteBlock(cursor++);
      }
      return null;
    }

    public VoteBlock peek() throws IOException {
      if (hasNext()) {
        if (cachedVoteBlock != null)
          return cachedVoteBlock;
        else
          return getVoteBlock(cursor);
      }
      return null;
    }
  }
}
