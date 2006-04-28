package org.lockss.protocol;

import java.util.*;
import java.io.*;

/**
 * Provide an Iterator with a <tt>peek</tt> method.
 */
public interface VoteBlocksIterator {

  /**
   * Return true if the iteration has more VoteBlock objects.
   *  
   * @return true if the iteration has more elements.
   */
  public boolean hasNext();
  
  /**
   * Returns the next element in the iteration.
   * 
   * @return The next element in the iteration.
   * @throws IOException
   */
  public VoteBlock next() throws IOException;
  
  /**
   * Returns the next element in the iteration, but does not move the iterator
   * cursor forward. This method is idempotent.
   * 
   * @return The next element in the iteration.
   */
  public VoteBlock peek() throws IOException;

}
