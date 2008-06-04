package org.lockss.protocol;

import java.util.*;
import java.io.*;
import java.nio.*;

import org.lockss.app.LockssApp;
import org.lockss.util.*;

/**
 * This is a VoteBlocks data structure backed by a disk file.
 * 
 * @author sethm
 * 
 */
public class DiskVoteBlocks extends BaseVoteBlocks {

  private String m_filePath;
  private transient File m_file;
  private int m_size = 0;

  private static final Logger log = Logger.getLogger("DiskVoteBlocks");

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
    FileOutputStream fos = new FileOutputStream(m_file);
    // Just copy to the output stream.
    StreamUtil.copy(from, fos);
    // Close
    fos.close();
    this.m_size = blocksToRead;
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

  public synchronized void addVoteBlock(VoteBlock b) throws IOException {
    // Append to the end of the file.
    FileOutputStream fos = null;
    fos = new FileOutputStream(m_file, true);
    DataOutputStream dos = new DataOutputStream(fos);
    byte[] encodedBlock = b.getEncoded();
    dos.writeShort(encodedBlock.length);
    dos.write(encodedBlock);
    this.m_size++;
    dos.close();
  }
  
  protected synchronized VoteBlock getVoteBlock(int i) throws IOException {
    throw new IOException("DiskVateBlocks.getVoteBlock(int) is no longer available.  Please use getIterator().");
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
    try {
      for (VoteBlocksIterator it = iterator(); it.hasNext(); ) {
        VoteBlock vb = (VoteBlock)it.next();
        if (url.equals(vb.getUrl())) {
          return vb;
        }
      }
      return null;
    } catch (IOException ex) {
      log.error("IOException while searching for VoteBlock " + url, ex);
      return null;
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
  
  class Iterator extends BaseVoteBlocks.Iterator implements VoteBlocksIterator {
    // Note: I wanted to use the (simpler) DataInputStream instead of the RandomAccessFile,
    // but I could not find an easy way to code "hasNext" unless the FileInputStream
    // had implemented the mark() method or some peek() method.
    private int m_countDiskReads;
    private RandomAccessFile m_raf;
    private VoteBlock m_nextVB;  // Read ahead, so we can close the stream correctly.
    
    public Iterator() throws FileNotFoundException {
      resetReadCount(); 
      
      m_raf = new RandomAccessFile(m_file, "r"); 
      readVB();
    }
    
    /**
     * Return true if the iteration has more VoteBlock objects.
     * 
     * @return true if the iteration has more elements.
     */
    public boolean hasNext() throws IOException {
      return m_raf.getFD().valid();
    }
    
    /**
     * Returns the next element in the iteration.
     * 
     * @return The next element in the iteration.
     * @throws IOException
     */
    public VoteBlock next() throws IOException {
        VoteBlock current = m_nextVB;
        readVB();
        
        if (current != null) {
          return current;
        } else {
          throw new NoSuchElementException();
        }
    }
    
    /**
     * Returns the next element in the iteration, but does not move the iterator
     * cursor forward. This method is idempotent.
     * 
     * @return The next element in the iteration.
     */
    public VoteBlock peek() {
      if (m_nextVB != null) {
        return m_nextVB;
      } else {
        throw new NoSuchElementException();
      }
    }
    
    /**
     * These methods count the number of times that the disk has been read.  
     * Note: We cannot easily subclass this class, because it depends on
     * being part of DiskVoteBlocks.  
     */
    protected void resetReadCount() {
      m_countDiskReads = 0;
    }
    
    protected void incrementReadCount() {
      m_countDiskReads++;
    }
    
    public int getReadCount() {
      return m_countDiskReads;
    }

    /* This method automatically closes m_fis when it reaches the end of a file. */
    
    private void readVB() {
      byte[] encodedBlock;
      short nextLen;
      
      incrementReadCount();
      
      try {
        nextLen = m_raf.readShort();
        encodedBlock = new byte[nextLen];
        m_raf.readFully(encodedBlock);
        m_nextVB = new VoteBlock(encodedBlock);
      } catch (IOException e) {
        m_nextVB = null;
        
        try {
          m_raf.close();
        } catch (IOException e2) {
          // Do nothing.
        }
      }
    }
  }

}
