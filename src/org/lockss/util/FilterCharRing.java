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

/**
 * CharRing with some additions used by the filtering code
 */

public class FilterCharRing extends CharRing {
  private static Logger logger = Logger.getLogger("FilterCharRing");

  char[] preBuffer;

  public FilterCharRing(int capacity) {
    super(capacity);
    preBuffer = new char[capacity];
  }

  /**
   * @return true if the two chars are equal ignoring case
   */
  public static boolean charEqualsIgnoreCase(char kar1, char kar2) {
    return (Character.toUpperCase(kar1) == Character.toUpperCase(kar2));
  }

  /**
   * @param tag tag to check
   * @return true if the char buffer begins with this tag, false otherwise
   * @throws RuntimeException if tag.length() > size() of charBuffer
   */
  public boolean startsWith(String tag) {
    return startsWith(0, tag, false);
  }

  /**
   * @param idx index at which to begin checking the char buffer
   * @param tag tag to check
   * @param ignoreCase whether to ignore case
   * @return true if the char buffer contains the tag beginning at index idx
   * @throws RuntimeException if idx + tag.length() > size() of charBuffer
   */
  public boolean startsWith(int idx, String tag, boolean ignoreCase) {
    //XXX reimplement with DNA searching algorithm
    int taglen = tag.length();
    int ringsize = size();
    if ((idx + taglen) > ringsize) {
      throw new RuntimeException("idx("+idx+") plus the length of the tag("
				 +tag+") is greater than the remaining size "
				 +"of the charBuffer("+ringsize+")");
    }

    if (logger.isDebug3()) {
      logger.debug3("checking if \""+this+"\" has \"" +tag+"\" at index "+idx);
    }
    //less common case than first char not match, but we have to check for
    //size before that anyway
    if (ringsize < taglen + idx) {
      return false;
    }

    for (int ix=0; ix < taglen && ix + idx < ringsize; ix ++) {
      char curChar = get(ix + idx);
      if (ignoreCase) {
	if (!charEqualsIgnoreCase(curChar, tag.charAt(ix))) {
	  logger.debug3("It doesn't");
	  return false;
	}
      } else {
	if (curChar != tag.charAt(ix)) {
	  logger.debug3("It doesn't");
	  return false;
	}
      }
    }
    logger.debug3("It does");
    return true;
  }

  /**
   * Refill the buffer from the specified reader
   * @param reader reader from which to refill the charBuffer
   * @return true if we've read through the reader, false otherwise
   * @throws IllegalArgumentException if called with a null reader
   */
  public boolean refillBuffer(Reader reader)
      throws IOException {
    if (reader == null) {
      throw new IllegalArgumentException("Called with null reader");
    }
    logger.debug3("Refilling buffer");
    int curKar;

    int charsNeeded = capacity() - size();
    while (charsNeeded > 0) {
      int charsRead = reader.read(preBuffer, 0, charsNeeded);
      if (charsRead == -1) {
	return true;
      }
      try{
	add(preBuffer, 0, charsRead);
      } catch (CharRing.RingFullException e) {
	logger.error("Overfilled a CharRing", e);
	throw new IOException("Overfilled a CharRing");
      }
      charsNeeded = capacity() - size();
    }
    return false;
  }
}
