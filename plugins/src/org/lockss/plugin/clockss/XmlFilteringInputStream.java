/*
 * $Id: XmlFilteringInputStream.java,v 1.1 2014-01-16 22:17:59 alexandraohlson Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss;

import java.io.*;
import org.lockss.util.Logger;

/**
 * Use of this filter ASSUMES a byte-based character system.
 * such as ISO-8859-1
 * This filters an input stream, replacing any non-XML legal single byte
 * characters with a "?" so while it modifies the stream, it allows for 
 * parsing to continue. 
 * Characters below 32 are illegal except for 9,10, & 13
 * Characters above 32 are legal except that & is special and should be 
 * protected. We do a minimal
 * check on this and if it is followed by any WS char, replace it with ?
 * 
 * XML specification
 *  Unicode character, excluding the surrogate blocks, FFFE, and FFFF. 
 * Char    ::=          #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | 
 * [#x10000-#x10FFFF]  
 */
public class XmlFilteringInputStream extends FilterInputStream {
  static Logger log = Logger.getLogger(XmlFilteringInputStream.class);
  
  private static int READ_AHEAD = 2; // for mark/reset
  // make a quick way to assert that a character is < 32 but not 9, 10, or 13
  // in Java an integer is always 32 bits
  private static int INVALID_LOW_CHAR_MASK = (~((1<<9) | (1 <<10) | (1 << 13) )); 

  protected XmlFilteringInputStream(InputStream in) {
    super(in);
  }
  
  /* ISO-8859-1 is a single byte charset */
  public int read() throws IOException {
    
    int b = in.read();
    if (b == -1) return b;
    // check for unprotected & (followed by a space) useful, but not perfect
    if (b == 38) {
      in.mark(READ_AHEAD);
      int nextb = in.read();
      in.reset();
      if (nextb < 0 || nextb == 32 ||
            // mask to see if a legal WS character
            (nextb < 32 && ((INVALID_LOW_CHAR_MASK & (1 << nextb)) == 0))) {
        return '?'; //instead of unprotected &
      }
    }
    // all other printing, ASCII characters
    if (b >= 32) return b;
    // carriage return, linefeed, tab, and end of file are allowed
    //else if (b == 10 || b == 13 || b == 9 || b == -1) return b;
    else if ( ((INVALID_LOW_CHAR_MASK & (1 << b)) == 0 )) {
      return b;
    }
    // non-printing characters
    return '?';
  }

  /*
   * (non-Javadoc)
   * @see java.io.FilterInputStream#read(byte[], int, int)
   * Implement using the read() method so that the filtering is consolidated
   * in one method
   */
  public int read(byte[] data, int offset, int length) throws IOException {

    if (data == null) {
      throw new NullPointerException();
    } else if (offset < 0 || length < 0 || length > data.length - offset) {
      throw new IndexOutOfBoundsException();
    } else if (length == 0) {
      return 0;
    }

    int c = read(); 
    if (c == -1) {
      return -1;
    }
    data[offset] = (byte)c;

    int i = 1;
    try {
      for (; i < length ; i++) {
        c = read(); 
        if (c == -1) {
          break;
        }
        data[offset + i] = (byte)c;
      }
    } catch (IOException ee) {
    }
    return i;
  }



}
