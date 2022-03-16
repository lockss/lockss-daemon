package org.lockss.util;

import java.io.*;
import org.apache.commons.io.input.*;

/** InputStream wrapper that remembers when end-of-file has been reached,
 * i.e., when read() has returned. */
public class EofRememberingInputStream extends ProxyInputStream {
  private boolean atEof = false;

  public EofRememberingInputStream(InputStream in) {
    super(in);
  }

  @Override
  protected void afterRead(int n) {
    if (n < 0) {
      atEof = true;
    }
  }

  /** Return true iff the underlying stream has reached end-of-file */
  public boolean isAtEof() {
    return atEof;
  }

}
