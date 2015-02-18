/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of his software and associated documentation files (the "Software"), to deal
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

package org.lockss.plugin.usdocspln.gov.gpo.fdsys;

import java.io.*;
import java.util.zip.*;

/*
 * FIXME 1.67: use org.lockss.filter.ZipFilterInputStream.java instead
 */
public abstract class ZipFilterInputStream extends InputStream {

  private ZipInputStream zipInputStream;
  
  private ZipEntry currentZipEntry;
  
  private boolean eof;

  private boolean closed;
  
  public ZipFilterInputStream(InputStream inputStream) {
    this(new ZipInputStream(inputStream));
  }
  
  public ZipFilterInputStream(ZipInputStream zipInputStream) {
    this.zipInputStream = zipInputStream;
    this.currentZipEntry = null;
    this.eof = false;
    this.closed = false;
  }

  @Override
  public int read(byte[] buf, int off, int len) throws IOException {
    internalRead();
    if (eof) {
      return -1;
    }
    
    int charsRequested = Math.min(len, buf.length - off);
    int charsProcessed = 0;
    int charsRead = -1;
    do {
      charsRead = zipInputStream.read(buf, off + charsProcessed, len - charsProcessed);
      if (charsRead == -1) {
        currentZipEntry = null;
      }
      else {
        charsProcessed += charsRead;
      }
    } while (charsRead != -1 && charsProcessed < charsRequested);
    return charsProcessed;
  }
  
  @Override
  public int read() throws IOException {
    while (true) {
      internalRead();
      if (eof) {
        return -1;
      }
      int ret = zipInputStream.read();
      if (ret == -1) {
        currentZipEntry = null;
      }
      else {
        return ret;
      }
    }
  }

  private void internalRead() throws IOException {
    if (closed) {
      throw new IOException("Stream closed");
    }
    while (!eof && currentZipEntry == null) {
      ZipEntry ze = zipInputStream.getNextEntry();
      if (ze == null) {
        eof = true;
        return;
      }
      long size = ze.getSize(); // can be -1L for deflate method
      if (size != 0L && keepZipEntry(ze)) {
        currentZipEntry = ze;
        return;
      }
    }
  }
  
  @Override
  public void close() throws IOException {
    if (closed) {
      throw new IOException("Stream closed");
    }
    closed = true;
    zipInputStream.close();
  }
  
  public abstract boolean keepZipEntry(ZipEntry zipEntry);
  
}
