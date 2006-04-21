package org.lockss.protocol;

import java.util.*;
import java.io.*;
import org.lockss.util.*;

/**
 * This is a VoteBlocks data structure backed by a disk file.
 * 
 * @author sethm
 *
 */
public class DiskVoteBlocks implements VoteBlocks {

  private String filePath;
  private Object fileLock;
  private int size = 0;

  private static final Logger log = Logger.getLogger("DiskVoteBlocks");

  public DiskVoteBlocks(int blocksToRead, InputStream from, File toDir)
      throws IOException {
    this(toDir);
    FileOutputStream fos = new FileOutputStream(filePath);
    // Just copy to the output stream.
    StreamUtil.copy(from, fos);
    // Close
    IOUtil.safeClose(fos);
    this.size = blocksToRead;
  }
  
  public DiskVoteBlocks(File toDir) throws IOException {
    fileLock = new Object();
    File f = FileUtil.createTempFile("voteblocks-", ".bin", toDir);
    filePath = f.getAbsolutePath();
  }

  public void addVoteBlock(VoteBlock b) {
    // Append to the end of the file.
    synchronized(fileLock) {
      File f = new File(filePath);
      FileOutputStream fos = null;
      try {
        fos = new FileOutputStream(filePath, true);
        DataOutputStream dos = new DataOutputStream(fos);
        byte[] encodedBlock = b.getEncoded();
        dos.writeShort(encodedBlock.length);
        dos.write(encodedBlock);
        this.size++;
      } catch (IOException ex) {
        throw new RuntimeException("IOException while adding VoteBlock", ex);
      } finally {
        IOUtil.safeClose(fos);
      }
    }
  }

  public ListIterator listIterator() {
    return new DiskVoteBlocksIterator();
  }

  public VoteBlock getVoteBlock(int i) {
    // Read from the file until we reach VoteBlock i, or run out of blocks.
    synchronized(fileLock) {
      File f = new File(filePath);
      FileInputStream fis = null;
      DataInputStream dis = null;
      try {
        fis = new FileInputStream(f);
        dis = new DataInputStream(fis);
      } catch (IOException ex) {
        // This would be bad.  For 1.16, just log and throw RuntimeException.
        // XXX: Implement better exception handling.  Unfortunately has to be 
        // a runtime exception to work with the ListIterator interface below.
        log.error("IOException trying to open VoteBlock file " +
                  filePath, ex);
        throw new RuntimeException("Unable to open VoteBlock file" +
                                   filePath);
      }
      
      try {
        // Seek until we find block i.
        for (int idx = 0; idx < i; idx++) {
          short len = dis.readShort();
          dis.skip(len);
        }
        // Should be there!
        short len = dis.readShort();
        byte[] encodedBlock = new byte[len];
        dis.read(encodedBlock);
        return new VoteBlock(encodedBlock);
      } catch (IOException ex) {
        // This probably means that we've run out of blocks, so we should
        // return null.
        log.warning("Unable to find block " + i + " while seeking " +
                    "DiskVoteBlocks file " + filePath);
        return null;
      } finally {
        IOUtil.safeClose(fis);
      }
    }
  }

  public int size() {
    return size;
  }
  
  public void delete() {
    synchronized(fileLock) {
      File f = new File(filePath);
      f.delete();
    }
  }
  
  public InputStream getInputStream() throws IOException {
    synchronized(fileLock) {
      return new FileInputStream(new File(filePath));
    }
  }

  private class DiskVoteBlocksIterator implements ListIterator {
    private int cursor = 0;
    
    public boolean hasNext() {
      return cursor < size();
    }

    public Object next() {
      if (hasNext()) {
        return getVoteBlock(cursor++);
      }
      return null;
    }

    public boolean hasPrevious() {
      return cursor > 0;
    }

    public Object previous() {
      if (hasPrevious()) {
        return getVoteBlock(--cursor);
      }
      return null;
    }

    public int nextIndex() {
      return cursor;
    }

    public int previousIndex() {
      return cursor - 1;
    }

    public void remove() {
      throw new UnsupportedOperationException("Not implemented.");
    }

    public void set(Object o) {
      throw new UnsupportedOperationException("Not implemented.");
    }

    public void add(Object o) {
      throw new UnsupportedOperationException("Not implemented.");
    }
    
  }
}