package org.lockss.protocol;

import java.util.*;
import java.io.*;

import org.lockss.util.*;

/**
 * Maintain an ordered list of VoteBlocks.  Concrete implementations offer
 * memory based or disk-based access, depending on the size.
 */

public interface VoteBlocks extends LockssSerializable {

  public void addVoteBlock(VoteBlock b);

  public ListIterator listIterator();
  
  public InputStream getInputStream() throws IOException;

  public VoteBlock getVoteBlock(int i);

  public int size();
  
  public void delete();

  public static class NoSuchBlockException extends Exception {
    NoSuchBlockException() { super(); }
    NoSuchBlockException(String msg) { super(msg); }
    NoSuchBlockException(Throwable t) { super(t); }
    NoSuchBlockException(String msg, Throwable t) { super(msg, t); }
  }
}