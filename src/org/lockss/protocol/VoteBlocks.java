/*
 * $Id: VoteBlocks.java,v 1.10.2.1 2008-07-22 06:47:03 tlipkis Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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
   * Return a VoteBlock for the specified URL, or null if no VoteBlock
   * is found in the collection.
   * 
   * @return The requested VoteBlock, or null.
   * @param url The URL to search for.
   */
  public VoteBlock getVoteBlock(String url);
  
  /**
   * Obtain an iterator over the VoteBlocks collection.
   * 
   * @return An iterator representing the VoteBlocks.
   */
  public VoteBlocksIterator iterator() throws FileNotFoundException;

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
