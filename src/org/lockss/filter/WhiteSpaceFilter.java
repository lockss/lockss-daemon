/*
 * $Id: WhiteSpaceFilter.java,v 1.1 2003-09-02 20:12:11 troberts Exp $
 */

package org.lockss.filter;
import java.util.*;
import java.io.*;

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

// A FilterInputStream that removes HTML markup.  Idea is to remove all
// variable content (e.g., advertisement URLs), leaving only journal text
// to feed to hasher.
// XXX Curently too simplistic and easily fooled, but works well enough
// XXX for beta test.
// XXX Need to generalize and make part of journal definition API.

public class WhiteSpaceFilter extends FilterInputStream {
  boolean inWhiteSpace;
  boolean hitEOF;
  static final byte asciiSpace = 32;

  // Create filtered stream, initialize state.
  public WhiteSpaceFilter(InputStream fis) {
    super(fis);
    inWhiteSpace = false;
    hitEOF = false;
  }

  // Read one byte
  public int read() throws IOException {
    byte[] buf = new byte[1];
    int ret = read(buf, 0, 1);
    if (ret >= 0)
      return ((int)buf[0]);
    else
      return (ret);
  }

  // Read bytes into array
  public int read(byte[] b) throws IOException {
    return (read(b, 0, b.length));
  }

  // Read bytes into array.  Remove everything between < and >, and
  // collapse all whitespace to a single space
  public int read(byte[] b, int off, int len) throws IOException {
    if (hitEOF)
      return -1;
    int ptr = off;
    while (ptr < len) {
      // we still haven't found enough bytes
      int next = super.read();
      if (next == -1) {
	// we just hit EOF
	hitEOF = true;
	break;
      }

      // handle whitespace - collapse multiple whitespace to single space
      if (inWhiteSpace) {
	if (isWhiteSpace(next))
	  continue;
	inWhiteSpace = false;
      } else {
	if (isWhiteSpace(next)) {
	  inWhiteSpace = true;
	  next = asciiSpace;
	}
      }
      // not whitespace - output the character
      b[ptr++] = (byte) next;
    }
    return (ptr - off);
  }

  private static boolean isWhiteSpace(int b) {
    return ((b >= 0 && b <= asciiSpace) || b == 127);
  }

}
