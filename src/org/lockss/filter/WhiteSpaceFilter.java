/*
 * $Id: WhiteSpaceFilter.java,v 1.5 2004-09-27 22:39:14 smorabito Exp $
 */

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

package org.lockss.filter;
import java.util.*;
import java.io.*;
import org.lockss.util.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;

/** A FilterInputStream that canonicalizes white space.
 */

public class WhiteSpaceFilter extends Reader {
  public static final int DEFAULT_BUFFER_CAPACITY = 4096;
  public static final String PARAM_BUFFER_CAPACITY =
    Configuration.PREFIX + "filter.buffer_capacity";

  static final byte asciiSpace = 32;

  boolean inWhiteSpace;
  boolean hitEOF;
  Reader reader;
  int bufsize;
  byte[] buf;
  int bufrem = 0;
  int bufptr = 0;

  // Create filtered stream, initialize state.
  public WhiteSpaceFilter(Reader reader) {
    this(reader, DEFAULT_BUFFER_CAPACITY);
  }

  // Create filtered stream, initialize state.
  public WhiteSpaceFilter(Reader reader, int bufsize) {
    if (reader == null) {
      throw new IllegalArgumentException("Called with a null reader");
    }
    this.reader = reader;
    if (bufsize < 0) {
      bufsize = Configuration.getIntParam(PARAM_BUFFER_CAPACITY,
                                          DEFAULT_BUFFER_CAPACITY);
    }
    this.bufsize = bufsize;

    inWhiteSpace = false;
    hitEOF = false;
    this.bufsize = bufsize;
    buf = new byte[bufsize];
  }

  // Read one byte
  public int read() throws IOException {
    char[] buf = new char[1];
    int ret = read(buf, 0, 1);
    if (ret >= 0)
      return ((int)buf[0]);
    return ret;
  }

  // Read bytes into array
  public int read(char[] buf) throws IOException {
    return (read(buf, 0, buf.length));
  }
  public boolean ready() {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public void reset() {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public long skip(long n) {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public void close() throws IOException {
    reader.close();
  }

  public int read(char[] buf, int off, int len) throws IOException {
    if (hitEOF)
      return -1;
    int endoff = off + len;
    int ptr = off;
    int rem = len;
    while (ptr < endoff) {
      if (bufptr >= bufrem) {
	bufrem = reader.read(buf, 0, len);
	if (bufrem == -1) {
	  hitEOF = true;
	  return (ptr == off) ? -1 : ptr - off;
	}
	bufptr = 0;
      }
      while (ptr < endoff && bufptr < bufrem) {
	int next = buf[bufptr++];
	// handle whitespace - collapse multiple whitespace to single space
	if ((next >= 0 && next <= asciiSpace) || next == 127) {
	  if (inWhiteSpace) {
	    continue;
	  } else {
	    inWhiteSpace = true;
	    next = asciiSpace;
	  }
	} else {
	  if (inWhiteSpace)
	    inWhiteSpace = false;
	}
	// output non-white or first white character
	buf[ptr++] = (char)next;
      }
    }
    return (ptr - off);
  }

  private static boolean isWhiteSpace(int b) {
    return ((b >= 0 && b <= asciiSpace) || b == 127);
  }


}
