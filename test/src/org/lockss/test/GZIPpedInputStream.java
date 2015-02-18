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

package org.lockss.test;

import java.io.*;
import java.util.zip.*;
//import java.io.ByteArrayInputStream;

/**
 * InputStream that supplies a gzipped version of what it reads.  Currently
 * source must be a string. InputStream source requires a piped copy.
 */
public class GZIPpedInputStream extends InputStream {
  private ByteArrayInputStream bais;
  private byte[] bytes;

  public GZIPpedInputStream(String srcStr) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    OutputStream os = baos;
    os = new GZIPOutputStream(os);
    os.write(srcStr.getBytes());
    os.flush();
    os.close();
    bais = new ByteArrayInputStream(baos.toByteArray());
  }

  public int read() {
    return bais.read();
  }

  /**
   * Returns the string the stream represents.
   * @return the source String
   */
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("[GZIPpedInputStream");
    sb.append("]");
    return sb.toString();
  }

  public boolean markSupported() {
    return false;
  }
}
