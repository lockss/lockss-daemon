package org.lockss.protocol;

import java.util.*;
import java.io.*;

import org.lockss.app.LockssApp;
import org.lockss.util.*;

/**
 * This is a VoteBlocks data structure backed by a disk file.
 * 
 * @author sethm
 * 
 */
public class DiskVoteBlocks extends BaseVoteBlocks {

  private String filePath;
  private transient File file;
  private int size = 0;

  // Hint to allow seeking to the correct point in the InputStream
  private transient long nextVoteBlockAddress = 0;
  private transient int nextVoteBlockIndex = 0;

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
    FileOutputStream fos = new FileOutputStream(file);
    // Just copy to the output stream.
    StreamUtil.copy(from, fos);
    // Close
    fos.flush();
    fos.close();
    this.size = blocksToRead;
  }

  /**
   * Create a new VoteBlocks collection to be backed by a file in the supplied
   * directory.
   * 
   * @param toDir Directory to use as temporary storage.
   * @throws IOException
   */
  public DiskVoteBlocks(File toDir) throws IOException {
    file = FileUtil.createTempFile("voteblocks-", ".bin", toDir);
    filePath = file.getAbsolutePath();
  }

  /**
   * Automagically restore File object following deserialization.
   */
  protected Object postUnmarshalResolve(LockssApp lockssContext) {
    file = new File(filePath);
    // Sanity Check
    if (!file.exists()) {
      throw new IllegalArgumentException("Unable to restore DiskVoteBlocks, "
                                         + "because target voteblocks file "
                                         + filePath + " does not " + "exist.");
    }
    return this;
  }

  public synchronized void addVoteBlock(VoteBlock b) throws IOException {
    // Append to the end of the file.
    FileOutputStream fos = null;
    fos = new FileOutputStream(file, true);
    DataOutputStream dos = new DataOutputStream(fos);
    byte[] encodedBlock = b.getEncoded();
    dos.writeShort(encodedBlock.length);
    dos.write(encodedBlock);
    this.size++;
    fos.flush();
    fos.close();
  }

  protected synchronized VoteBlock getVoteBlock(int i) throws IOException {
    // Read from the file until we reach VoteBlock i, or run out of blocks.
    FileInputStream fis = null;
    DataInputStream dis = null;
    fis = new FileInputStream(file);
    dis = new DataInputStream(fis);

    try {
      // Shortcut for quickly finding the next iterable block
      if (i == nextVoteBlockIndex) {
        dis.skip(nextVoteBlockAddress);
      } else {
        for (int idx = 0; idx < i; idx++) {
          short len = dis.readShort();
          dis.skip(len);
          nextVoteBlockIndex++;
          nextVoteBlockAddress += len + 2;
        }
      }

      // Should be there!
      short len = dis.readShort();
      byte[] encodedBlock = new byte[len];
      dis.read(encodedBlock);
      nextVoteBlockIndex++;
      nextVoteBlockAddress += len + 2;
      return new VoteBlock(encodedBlock);
    } catch (IOException ex) {
      // This probably means that we've run out of blocks, so we should
      // return null.
      log.warning("Unable to find block " + i + " while seeking "
                  + "DiskVoteBlocks file " + filePath);
      return null;
    } finally {
      fis.close();
    }
  }

  public int size() {
    return size;
  }

  public synchronized void release() {
    if (file != null && !file.delete()) {
      log.warning("Unable to delete file: " + file);
    }
  }

  public synchronized InputStream getInputStream() throws IOException {
    return new BufferedInputStream(new FileInputStream(file));
  }

}