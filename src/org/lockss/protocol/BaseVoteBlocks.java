package org.lockss.protocol;

import java.io.*;
import java.util.*;

import org.lockss.util.*;

/**
 * Common functionality for all concrete implementations of VoteBlocks.
 * 
 */
public abstract class BaseVoteBlocks implements VoteBlocks {

  public VoteBlocksIterator iterator() throws FileNotFoundException {
    return new BaseVoteBlocks.Iterator();
  }
  
  /**
   * Return the VoteBlock at a given index.  Concrete subclasses must
   * provide an implementation of this.
   *
   * @param i The index of the VoteBlock to return.
   * @return  The VoteBlock at the given index.
   */
  abstract protected VoteBlock getVoteBlock(int i) throws IOException;
  
  protected class Iterator implements VoteBlocksIterator {
    private int cursor = 0;
    /* For efficiency purposes, store the most recently peeked voteblock. */
    private VoteBlock cachedVoteBlock;
    
    public boolean hasNext() throws IOException {
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
