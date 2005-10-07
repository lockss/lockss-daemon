package org.lockss.protocol;

import java.util.*;

import org.lockss.util.*;

/**
 * Maintain an ordered list of VoteBlocks.  Concrete implementations offer
 * memory based or disk-based access, depending on the size.
 */

public interface VoteBlocks extends LockssSerializable {

  public void addVoteBlock(VoteBlock b);

  public VoteBlock getVoteBlock(int i) throws NoSuchBlockException;

  public Iterator iterator();

  public int size();
  
  public static class NoSuchBlockException extends Exception {
    NoSuchBlockException() { super(); }
    NoSuchBlockException(String msg) { super(msg); }
    NoSuchBlockException(Throwable t) { super(t); }
    NoSuchBlockException(String msg, Throwable t) { super(msg, t); }
  }
}