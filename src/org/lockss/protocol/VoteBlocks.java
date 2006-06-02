package org.lockss.protocol;

import java.util.*;
import java.io.*;

import org.lockss.util.*;

/**
 * Maintain an ordered list of VoteBlocks.  Concrete implementations offer
 * memory based or disk-based access, depending on the size.
 */

public interface VoteBlocks extends LockssSerializable {

  /**
   * Add a VoteBlock to this collection.
   * 
   * @param b VoteBlock to add.
   */
  public void addVoteBlock(VoteBlock b) throws IOException;
  
  /**
   * <p>Get a representation of this collection suitable for streaming
   * to a V3LcapMessage at encode time.</p>
   * 
   * <p>To be considered compatible with the encoded form of a V3LcapMessage,
   * this method <b>must</b> guarantee that it returns a stream consisting
   * of the following byte sequence, one per VoteBlock object:</p>
   * 
   * <ul>
   *   <li><b>One Byte</b>: The length of the encoded VoteBlock</li>
   *   <li><b><i>len</i> Bytes</b>: The result of calling <tt>getEncoded()</tt>
   *   on the VoteBlock.</li>
   * </ul>
   * 
   * @return An InputStream from which to read the encoded form of
   *         this VoteBlock collection.
   *
   * @throws IOException  If an error occurs while opening the stream.
   */
  public InputStream getInputStream() throws IOException;

  /**
   * Obtain an iterator over the VoteBlocks collection.
   * 
   * @return An iterator representing the VoteBlocks.
   */
  public VoteBlocksIterator iterator();

  /**
   * Return the number of VoteBlock objects contained in this collection.
   *
   * @return The size of the VoteBlocks.
   */
  public int size();
  
  /**
   * Ask the object to release resources it is holding.
   */
  public void release();
}