/*
 * $Id: VoteBlocksIterator.java,v 1.5.2.1 2008-07-22 06:47:03 tlipkis Exp $
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
 * Provide an Iterator with a <tt>peek</tt> method.
 */
public interface VoteBlocksIterator extends LockssSerializable {
  
  /** An empty, immutable VoteBlock iterator.  Calling next() throws
   * NoSuchElementException.
   */
  public static final VoteBlocksIterator EMPTY_ITERATOR =
    new VoteBlocksIterator() {
      public boolean hasNext() { return false; }
      public VoteBlock next() { throw new NoSuchElementException(); }
      public VoteBlock peek() { return null; }
      public void release() { }
    };

  /**
   * Return true if the iteration has more VoteBlock objects.
   *  
   * @return true if the iteration has more elements.
   */
  public boolean hasNext() throws IOException;
  
  /**
   * Returns the next VoteBlock in the iteration.
   * 
   * @return The next VoteBlock in the iteration
   * VoteBlocks
   * @throws IOException
   * @throws NoSuchElementException
   */
  public VoteBlock next() throws IOException;
  
  /**
   * Returns the VoteBlock that next() would return, or null if next()
   * would throw NoSuchElementException.  Does not advance the iterator
   * cursor forward.
   * 
   * @return The next VoteBlock in the iteration, or null if no more
   * VoteBlocks.
   */
  public VoteBlock peek() throws IOException;

  /** Release any resources held by the iterator */
  public void release();

}
