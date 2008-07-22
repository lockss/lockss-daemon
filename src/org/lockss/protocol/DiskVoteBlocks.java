/*
 * $Id: DiskVoteBlocks.java,v 1.17.2.2 2008-07-22 06:47:03 tlipkis Exp $
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
import java.nio.*;

import org.lockss.app.LockssApp;
import org.lockss.util.*;

/**
 * A VoteBlocks data structure backed by a disk file.  This implementation
 * is not thread-safe.
 * 
 * @author sethm
 */
public class DiskVoteBlocks implements VoteBlocks {
  private static final Logger log = Logger.getLogger("DiskVoteBlocks");

  private String m_filePath;
  private transient File m_file;
  private int m_size = 0;

  /**
   * <p>
   * Decode a DiskVoteBlocks object from the supplied inputstream, to be stored
   * in the supplied directory.
   * </p>
   * 
   * <p>
   * This method is used when decoding V3LcapMessages.
   * </p>
   * 
   * @param blocksToRead Number of blocks to read from the InputStream.
   * @param from Input stream from which to read.
   * @param toDir Directory to use as temporary storage.
   * @throws IOException
   */
  public DiskVoteBlocks(int blocksToRead, InputStream from, File toDir)
      throws IOException {
    this(toDir);
    
    OutputStream os = new BufferedOutputStream(new FileOutputStream(m_file));
    try {
      // Just copy to the output stream.
      StreamUtil.copy(from, os);
      // Close
      os.close();
      this.m_size = blocksToRead;
    } catch (IOException e) {
      IOUtil.safeClose(os);
      throw e;
    }
  }

  /**
   * Create a new VoteBlocks collection to be backed by a file in the supplied
   * directory.
   * 
   * @param toDir  Directory to use as temporary storage.
   * @throws IOException
   */
  public DiskVoteBlocks(File toDir) throws IOException {
    m_file = FileUtil.createTempFile("voteblocks-", ".bin", toDir);
    m_filePath = m_file.getAbsolutePath();
  }

  /**
   * Automagically restore File object following deserialization.
   */
  protected void postUnmarshal(LockssApp lockssContext) {
    m_file = new File(m_filePath);
  }

  /* Inherit documentation */
  public synchronized void addVoteBlock(VoteBlock b) throws IOException {
    // Append to the end of the file.
    FileOutputStream fos = new FileOutputStream(m_file, true);
    DataOutputStream dos = new DataOutputStream(fos);
    byte[] encodedBlock = b.getEncoded();
    dos.writeShort(encodedBlock.length);
    dos.write(encodedBlock);
    this.m_size++;
    dos.close();
  }
  
  public VoteBlocksIterator iterator() throws FileNotFoundException {
    return new DiskVoteBlocks.Iterator();
  }

  /** Search the collection for the requested VoteBlock.
   * 
   * XXX: This is implemented as a simple linear search, so it is O(n).
   * The disk structure is not terribly easy to seek into because each
   * record is variable length, so I'm not sure it will be easy to implement
   * binary search and improve performance.  Therefore, this method should
   * be used with care, and sparingly.
   */
  public VoteBlock getVoteBlock(String url) {
    VoteBlocksIterator iter = null;
    try {
      iter = iterator();
      while (iter.hasNext()) {
        VoteBlock vb = iter.next();
        if (url.equals(vb.getUrl())) {
          return vb;
        }
      }
      return null;
    } catch (IOException ex) {
      log.error("IOException while searching for VoteBlock " + url, ex);
      return null;
    } finally {
      if (iter != null) {
	iter.release();
      }
    }
  }

  public int size() {
    return m_size;
  }

  public synchronized void release() {
    // The poller should have already cleaned up our directory by now,
    // but just in case, we'll run some cleanup code.
    if (m_file != null && !m_file.delete() && log.isDebug2()) {
      log.debug2("Unable to delete file: " + m_file);
    }
    m_file = null;
  }

  public synchronized InputStream getInputStream() throws IOException {
    return new BufferedInputStream(new FileInputStream(m_file));
  }
  
  class Iterator implements VoteBlocksIterator {
    private RandomAccessFile m_raf;
    private VoteBlock m_nextVB;  // Next block to be returned by next(), peek()
    
    public Iterator() throws FileNotFoundException {
      m_raf = new RandomAccessFile(m_file, "r"); 
    }
    
    /* Inherit documentation */
    public void release() {
      IOUtil.safeClose(m_raf);
    }

    /* Inherit documentation */
    public boolean hasNext() throws IOException {
      ensureVB();
      return m_nextVB != null;
    }
    
    /* Inherit documentation */
    public VoteBlock next() throws IOException {
      VoteBlock res = peek();
      if (res == null) {
	throw new NoSuchElementException();
      }
      m_nextVB = null;
      return res;
    }
    
    /* Inherit documentation */
    public VoteBlock peek() throws IOException {
      ensureVB();
      return m_nextVB;
    }
    
    private void ensureVB() throws IOException {
      if (m_nextVB == null) {
	readVB();
      }
    }

    /* This method automatically closes the file when it reaches the end. */
    protected void readVB() throws IOException {
      m_nextVB = null;
      if (m_raf.getFD().valid()) {
	try {
	  short nextLen = m_raf.readShort();
	  byte[] encodedBlock = new byte[nextLen];
	  m_raf.readFully(encodedBlock);
	  m_nextVB = new VoteBlock(encodedBlock);
	} catch (java.io.EOFException e) {
	  IOUtil.safeClose(m_raf);
	}
      }
    }
  }
}
