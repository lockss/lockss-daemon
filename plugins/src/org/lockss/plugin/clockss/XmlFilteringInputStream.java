/*
 * $Id$
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
 * 
 * NOTE: Should we need to expand this to handle other charsets, 
 * enhance the filter to create a reader with the initial charset, 
 * read the first line and match against a regex to extract the charset, 
 * reset and create the correct reader, then filter disallowed chars < 32.  
 * This is much easier than html because the xml declaration must be on the 
 * first line.
 */
public class XmlFilteringInputStream extends FilterInputStream {
  static Logger log = Logger.getLogger(XmlFilteringInputStream.class);

  private static int READ_AHEAD = 2; // for mark/reset
  // make a quick way to assert that a character is < 32 but not 9, 10, or 13
  // in Java an integer is always 32 bits
  private static int INVALID_LOW_CHAR_MASK = (~((1<<9) | (1 <<10) | (1 << 13) )); 

  public XmlFilteringInputStream(InputStream in) {
    super(in);
  }

  /* ISO-8859-1 is a single byte charset and this depends on that*/

  int AMPERSAND_INT = '&'; //38
  int WS_INT = ' '; //32
  int QUEST_INT = '?'; //63
  public int read() throws IOException {

    int b = in.read();
    if (b == -1) return b;
    // check for unprotected & (followed by a space) useful
    // This is a bit of a hack, but works for our limited purposes
    if (b == AMPERSAND_INT) { 
      in.mark(READ_AHEAD);
      int nextb = in.read();
      in.reset();
      // If & followed by end of file, WS or a legal WS ascii char
      if (nextb < 0 || nextb == WS_INT ||
          legal_lowspace_char(nextb)) { 
        return QUEST_INT;  //instead of unprotected &
      }
    }
    // all other printing, ASCII characters
    if (b >= 32) return b;
    // carriage return, linefeed, tab, and end of file are allowed
    else if (legal_lowspace_char(b)) {
      return b;
    }
    // non-printing characters
    return QUEST_INT;
  }

  // tab, line feed, carriage return
  // this also returns FALSE if >= 32, so use carefully
  private boolean legal_lowspace_char(int b) {
    return (b < WS_INT &&
        (b == 9 || b == 10 || b == 13));
  }

  /*
   * (non-Javadoc)
   * @see java.io.FilterInputStream#read(byte[], int, int)
   * 
   */
  public int read(byte[] data, int offset, int length) throws IOException {

    int result = in.read(data, offset, length);
    // result = -1 drops past for loop and returns directly

    // loop over returned results changing invalid chars to ?
    for (int i = offset; i < offset+result; i++) {
      // if low, check validity
      if (data[i] < WS_INT) {
        if (data[i] > 0 && !(legal_lowspace_char(data[i]))) {
          data[i] = (byte)QUEST_INT;
        }
      } else {
        //in this range we only handle '&'
        if (data[i] == AMPERSAND_INT) {
          //perform lookahead
          int nextb;
          if (i+1 < offset+result) {
            // we're in luck and still have data!
            nextb = data[i+1];
          } else {
            // drat. we'll have to read one more byte
            in.mark(READ_AHEAD);
            nextb = in.read();
            in.reset();
          }
          // If & followed by end of file, WS or a legal WS ascii char
          if (nextb < 0 || nextb == WS_INT || legal_lowspace_char(nextb)) {
            data[i] = (byte)QUEST_INT;
          }
        }
      }
    }
    return result;
  }

}
