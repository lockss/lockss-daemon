/*
 * $Id$
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;

import org.apache.commons.io.FileUtils;

/**
 * Wrapper InputStream that calls a user-supplied callback when closed.
 * Useful for deleting temporary files
 */
public class CloseCallbackInputStream extends InputStream {
  private static final Logger log =
    Logger.getLogger("CloseCallbackInputStream");

  private InputStream in;
  private Callback cb;
  private Object cookie;

  public CloseCallbackInputStream(InputStream in, Callback cb, Object cookie) {
    this.in = in;
    this.cb = cb;
    this.cookie = cookie;
  }
  public int read() throws IOException {
    return in.read();
  }
  public int read(byte b[]) throws IOException {
    return read(b, 0, b.length);
  }
  public int read(byte b[], int off, int len) throws IOException {
    return in.read(b, off, len);
  }
  public long skip(long n) throws IOException {
    return in.skip(n);
  }
  public int available() throws IOException {
    return in.available();
  }
  public void close() throws IOException {
    try {
      in.close();
    } finally {
      try {
	cb.streamClosed(cookie);
      } catch (Exception e ) {
	log.warning("Error in streamClosed callback", e);
      }
    }
  }
  public void mark(int readlimit) {
    in.mark(readlimit);
  }
  public void reset() throws IOException {
    in.reset();
  }
  public boolean markSupported() {
    return in.markSupported();
  }

  public interface Callback {
    void streamClosed(Object cookie);
  }
  
  /**
   * <p>The most common use for {@link CloseCallbackInputStream} is
   * to read a temporary file once and delete it when the stream is
   * closed. This class implements this behavior.</p>
   */
  public static class DeleteFileOnCloseInputStream extends CloseCallbackInputStream {

    /**
     * <p>Creates a new {@link InputStream} that reads from the
     * specified file, and deletes that file when {@link #close()}
     * is called.</p>
     * <p>Uses {@link FileInputStream} (not buffered) and
     * {@link FileUtils#deleteQuietly(File)}.</p>
     * @param file An underlying file, that will get deleted when
     *             {@link #close()} is called.
     * @throws FileNotFoundException if the underlying file cannot be
     *                               found.
     */
    public DeleteFileOnCloseInputStream(File file) throws FileNotFoundException {
      super(new FileInputStream(file),
            new Callback() {
              @Override
              public void streamClosed(Object cookie) {
                FileUtils.deleteQuietly(((File)cookie));
              }        
            },
            file);
    }
    
  }
  
}
