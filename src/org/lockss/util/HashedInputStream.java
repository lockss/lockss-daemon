/*
 * $Id$
 */

/*

Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;
import java.io.InputStream;
import java.io.IOException;
import java.security.MessageDigest;

/**
 * An InputStream wrapper that feeds all the bytes read to a MessageDigest
 */
public class HashedInputStream extends InputStream {
  protected static Logger logger = Logger.getLogger("HashedInputStream");

  private InputStream is;
  private Hasher hasher;
  private boolean isClosed = false;

  /**
   * Construct a HashedInputStream from an InputStream and a
   * HashedInputStream.Hasher containing a MessageDigest. The data read
   * from the InputStream will be fed to the MessageDigest.  The hasher
   * will be marked valid iff the complete underlying stream is read,
   * @param is the underlying <code>InputStream</code>.
   * @param hasher a HashedInputStream.Hasher containing the MessageDigest
   * to be updated.
   */
  public HashedInputStream(InputStream is, Hasher hasher) {
    if (is == null) {
      throw new IllegalArgumentException("null is invalid");
    }
    if (hasher == null) {
      throw new IllegalArgumentException("null hasher invalid");
    }
    this.is = is;
    this.hasher = hasher;
    logger.debug3("New HashedInputStream");
  }

  /**
   * From <code>InputStream</code>
   */
  public int available() throws IOException {
    return is.available();
  }

  /**
   * From <code>InputStream</code>.  Sets hash valid iff all bytes have
   * been read from the underlying stream.
   */
  public void close() throws IOException {
    if (isClosed) {
      return;
    }
    // If haven't yet determined that all bytes were read...
    if (!hasher.isValid()) {
      // If bytes are known to be available, don't set valid.
      if (available() == 0) {
	// available() == 0 isn't required to mean stream is at EOF; trying
	// to read a byte will set valid if we are at EOF
	read();
      }
    }
    isClosed = true;
    IOUtil.safeClose(is);
  }

  /**
   * From <code>InputStream</code>
   * @param readlimit int limit on number of bytes read ahead of mark
   */
  public void mark(int readlimit) {
    throw new UnsupportedOperationException("mark not supported");
  }

  /**
   * From <code>InputStream</code>
   */
  public boolean markSupported() {
    return false;
  }

  /**
   * Read and hash the next byte from the <code>InputStream</code>
   * @return number of bytes read and hashed
   */
  public int read() throws IOException {
    byte[] buf = new byte[1];
    int ret = read(buf);
    if (ret == 1) {
      return buf[0];
    } else {
      return check(ret);
    }
  }

  /**
   * Read and hash up to <code>buf.length</code> bytes from the
   * <code>InputStream</code>
   * @param buf byte[] of bytes read
   * @return number of bytes read and hashed
   */
  public int read(byte[] buf) throws IOException {
    return check(read(buf, 0, buf.length));
  }

  /**
   * Read and hash up to <code>len</code> bytes to <code>off</code>
   * in the <code>buf</code> array from the <code>InputStream</code>
   * @param buf byte[] of bytes read
   * @param off int offset in <code>buf</buf>
   * @param len int max number of bytes to read and hash
   * @return number of bytes read and hashed
   */
  public int read(byte[] buf, int off, int len) throws IOException {
    int ret = is.read(buf, off, len);
    if (ret > 0) {
      if (logger.isDebug3()) {
	logger.debug3("Hashing " + ret + " bytes from " + off);
      }
      hasher.md.update(buf, off, ret);
    } else {
      if (logger.isDebug3()) {
	logger.debug3("0 or fewer bytes read");
      }
    }
    return check(ret);
  }
  
  /**
   * From <code>InputStream</code>
   */
  public void reset() throws IOException {
    throw new UnsupportedOperationException("reset not supported");
  }

  /**
   * From <code>InputStream</code>
   * @param n long number of bytes to skip
   * @return number of bytes skipped
   */
  public long skip(long n) throws IOException {
    throw new UnsupportedOperationException("skip not supported");
  }

  private int check(int n) {
    if (n < 0) {
      hasher.setValid(true);
    }
    return n;
  }

  private long check(long n) {
    if (n < 0) {
      hasher.setValid(true);
    }
    return n;
  }


  public static class Hasher {
    private boolean valid = false;
    private MessageDigest md;

    public Hasher(MessageDigest md) {
      this.md = md;
    }

    private void setValid(boolean val) {
      valid = val;
    }

    public MessageDigest getDigest() {
      return md;
    }

    public boolean isValid() {
      return valid;
    }
  }

}
