/*
 * $Id: ReaderInputStream.java,v 1.4 2003-06-02 21:41:10 troberts Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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
 * Wrapper to turn a Reader into an InputStream
 */

public class ReaderInputStream extends InputStream {
  Reader reader = null;
  protected static Logger logger = Logger.getLogger("ReaderInputStream");

  public int read() throws IOException {
    logger.debug3("Calling read");
    int kar = reader.read();
    logger.debug3("Read called");
    if (kar == -1) {
      logger.debug3("Reader is done");
      return kar;
    }
    return charToByte((char)kar);
  }

  private byte charToByte(char kar) {
    return charToByte2(kar);
  }

  private byte charToByte1(char kar) {
    //XXX hack, make better
    if (logger.isDebug3()) {
      logger.debug3("Converting "+kar);
    }
    byte bytes[] = String.valueOf(kar).getBytes();
    return bytes[0];
  }

  private byte charToByte2(char kar) {
    return (byte)kar;
  }

  public ReaderInputStream(Reader reader) {
    this.reader = reader;//new BufferedReader(reader);
  }

}
