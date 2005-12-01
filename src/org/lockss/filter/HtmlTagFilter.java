/*
 * $Id: HtmlTagFilter.java,v 1.8 2005-12-01 23:28:04 troberts Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.List;

import org.lockss.config.*;
import org.lockss.util.*;

/**
 * This class is used to filter all content from a reader between two string
 * (for instance "<&excl;-- and "-->"
 */
public class HtmlTagFilter extends Reader {
  /**
   * TODO
   * 1)See how Mozilla handles nested comments
   * 2)Use better string searching algorithm
   */

  private static Logger logger = Logger.getLogger("HtmlTagFilter");

  public static final int DEFAULT_BUFFER_CAPACITY = 4096;
  public static final String PARAM_BUFFER_CAPACITY =
    Configuration.PREFIX + "filter.buffer_capacity";

  Reader reader;
  CharRing charBuffer = null;
  int ringSize;

  TagPair pair = null;
  String startTag;
  String endTag;
  boolean ignoreCase;
  int startLen;
  int endLen;
  int maxTagLen;
  int minTagLen;
  int bufferCapacity;
  boolean streamDone = false;
  private boolean isClosed = false;
  private boolean isTrace = logger.isDebug3();


  private HtmlTagFilter(Reader reader) {
    if (reader == null) {
      throw new IllegalArgumentException("Called with a null reader");
    }
    this.reader = reader;
  }

  /**
   * Create a HtmlTagFilter that will skip everything between the two tags of
   * pair, properly handling nested tags
   * @param reader reader to filter from
   * @param pair TagPair representing the pair of strings to filter between
   */
  public HtmlTagFilter(Reader reader, TagPair pair) {
    this(reader);
    if (pair == null) {
      throw new IllegalArgumentException("Called with a null tag pair");
    }
    startTag = pair.start;
    endTag = pair.end;
    if (StringUtil.isNullString(startTag) || StringUtil.isNullString(endTag)) {
      throw new IllegalArgumentException("Called with a tag pair with an "
					 +"empty string: "+pair);
    }
    this.pair = pair;
    ignoreCase = pair.ignoreCase;
    startLen = startTag.length();
    endLen = endTag.length();
    if (startLen > endLen) {
      maxTagLen = startLen;
      minTagLen = endLen;
    } else {
      maxTagLen = endLen;
      minTagLen = startLen;
    }
    bufferCapacity = CurrentConfig.getIntParam(PARAM_BUFFER_CAPACITY,
                                               DEFAULT_BUFFER_CAPACITY);
    if (maxTagLen > bufferCapacity) {
      bufferCapacity = maxTagLen;
    }
    charBuffer = new CharRing(bufferCapacity);
    ringSize = charBuffer.size();
  }

  /**
   * Create a filter with multiple tags.  When filtering with multiple tags
   * it behaves as though everything between each pair is removed sequentially
   * (ie, everything between the first pair is filtered, then the second pair
   * then the third, etc).
   *
   * @param reader reader to filter from
   * @param pairs List of TagPairs to filter between.
   * @return an HtmlTagFilter
   */
  public static HtmlTagFilter makeNestedFilter(Reader reader, List pairs) {
    if (pairs == null) {
      throw new IllegalArgumentException("Called with a null tag pair list");
    }
    if (pairs.size() <= 0) {
      throw new IllegalArgumentException("Called with empty tag pair list");
    }

    Reader curReader = reader;
    for (int ix = 0; ix < pairs.size(); ix++) {
      curReader = new HtmlTagFilter(curReader, (TagPair) pairs.get(ix));
    }
    return (HtmlTagFilter)curReader;
  }

  public int read(char[] outputBuf, int off, int bufSize) throws IOException {
    if (isTrace) logger.debug3("read(buf, " + off + ", " + bufSize + ")");
    if (isClosed) throw new IOException("Read from closed HtmlTagFilter");
    int bufPtrPlusFree = off + bufSize;
    if ((off < 0) || (bufSize < 0) || (bufPtrPlusFree > outputBuf.length)) {
      throw new IndexOutOfBoundsException("char["+outputBuf.length+"], "+off+
					  ", "+bufSize);
    }

    int bufFree = bufSize;
    while (bufFree > 0 && (!streamDone || (ringSize > 0))) {
      if (ringSize < startLen && !streamDone) {
	streamDone = charBuffer.refillBuffer(reader);
	ringSize = charBuffer.size();
      }
      int idx = charBuffer.indexOf(startTag, bufFree, ignoreCase);
      int ncopy;
      if (idx >= 0) {
	ncopy = idx;
      } else {
	int max = streamDone ? ringSize : (ringSize - (startLen - 1));
	ncopy = bufFree < max ? bufFree : max;
      }
      charBuffer.remove(outputBuf, bufPtrPlusFree - bufFree, ncopy);
      bufFree -= ncopy;
      ringSize -= ncopy;
      if (idx >= 0) {
	charBuffer.skip(startLen);
	ringSize -= startLen;
	skipToEndTag();
      }
    }
    int numRead = bufSize - bufFree;
    return numRead == 0 ? -1 : numRead;
  }

  /**
   * Reads through the charBuffer until the end tag is reached, taking
   * nesting into account
   *
   * May leave buffer empty even if there are chars left on the reader
   * @throws IOException
   */
  private void skipToEndTag() throws IOException {
    if (isTrace) {
      logger.debug3("reading through tag pair "+pair+" in "+charBuffer);
    }
    int tagNesting = 1;
    while (tagNesting > 0 && !(streamDone && ringSize == 0)) {
      if (isTrace) logger.debug3("tagNesting: "+tagNesting);
      // refill
      if (!streamDone && ringSize < maxTagLen) {
	streamDone = charBuffer.refillBuffer(reader);
	ringSize = charBuffer.size();
      }
      int endPos = charBuffer.indexOf(endTag, -1, ignoreCase);
      int startPos = charBuffer.indexOf(startTag, endPos, ignoreCase);
      if (isTrace) logger.debug3("start: " + startPos + ", end: " +  endPos);
      int nskip;
      if (endPos >= 0 && (startPos < 0 || endPos < startPos)) {
	// first or only tag is end
	tagNesting--;
	nskip = endPos + endLen;
      }	else if (startPos >= 0) {
	// first or only tag is start
	tagNesting++;
	nskip = startPos + startLen;
      } else {
	// neither tag found
	nskip = streamDone ? ringSize : (ringSize - (maxTagLen - 1));
      }
      charBuffer.skip(nskip);
      ringSize -= nskip;
    }
  }

  public void close() throws IOException {
    isClosed = true;
    reader.close();
  }

  public static class TagPair {
    String start = null;
    String end = null;
    boolean ignoreCase = false;

    public TagPair(String start, String end) {
      if (start == null || end == null) {
	throw new IllegalArgumentException("Called with a null start "+
					 "or end string");
      }
      this.start = start;
      this.end = end;
    }

    public TagPair(String start, String end, boolean ignoreCase) {
      this(start, end);
      this.ignoreCase = ignoreCase;
    }

    int getMaxTagLength() {
      return Math.max(start.length(), end.length());
    }

    int getMinTagLength() {
      return Math.min(start.length(), end.length());
    }

    public String toString() {
      StringBuffer sb = new StringBuffer(start.length()
					 + end.length()
					 + 10);
      sb.append("[TagPair: ");
      sb.append(start);
      sb.append(", ");
      sb.append(end);
      sb.append("]");
      return sb.toString();
    }

    public boolean equals(Object obj) {
      if (obj instanceof TagPair) {
	TagPair pair = (TagPair) obj;
	return (start.equals(pair.start) && end.equals(pair.end));
      }
      return false;
    }

    public int hashCode() {
      return ((3 * start.hashCode()) + (5 * end.hashCode()));
    }
  }
}
