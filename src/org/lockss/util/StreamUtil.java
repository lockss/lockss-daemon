/*
 * $Id: StreamUtil.java,v 1.7 2003-07-18 00:37:14 tyronen Exp $
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

/**
 * This is a class to contain generic stream utilities
 *
 * @author  Emil Aalto
 * @version 0.0
 */

public class StreamUtil {

  private static final int BUFFER_SIZE = 256;

  /**
   * This function copies the contents of in InputStream to an Outputstream.
   * It buffers the copying, and closes neither.
   * @param is input
   * @param os output
   * @throws IOException
   */
  public static void copy(InputStream is, OutputStream os) throws IOException {
    if (is == null || os == null) {
      return;
    }
    byte[] bytes = new byte[BUFFER_SIZE];
    int byteCount;
    while ((byteCount = is.read(bytes)) > 0) {
      os.write(bytes, 0, byteCount);
    }
    os.flush();
  }

  /**
   * This function copies the contents of a Reader to a Writer
   * It buffers the copying, and closes neither.
   * @param reader reader
   * @param writer writer
   * @throws IOException
   */
  public static void copy(Reader reader, Writer writer) throws IOException {
    if (reader == null || writer == null) {
      return;
    }
    char[] chars = new char[BUFFER_SIZE];
    int count;
    while ((count = reader.read(chars)) > 0) {
      writer.write(chars, 0, count);
    }
    writer.flush();
  }
}

