/*
 * $Id: HtmlTagFilter.java,v 1.2 2003-12-17 02:09:47 tlipkis Exp $
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

package org.lockss.filter;

import java.io.*;
import java.util.*;
import org.apache.commons.collections.*;
import org.lockss.util.*;
import org.lockss.daemon.Configuration;

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

  public static final int DEFAULT_BUFFER_CAPACITY = 256;
  public static final String PARAM_BUFFER_CAPACITY =
    Configuration.PREFIX + "filter.buffer_capacity";



  Reader reader = null;
  TagPair pair = null;
  FilterCharRing charBuffer = null;
  int minBufferSize = 0;
  int bufferCapacity;
  boolean streamDone = false;

  private static Logger logger = Logger.getLogger("HtmlTagFilter");


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
    } else if (pair.start == "" || pair.end == "") {
      throw new IllegalArgumentException("Called with a tag pair with an "
					 +"empty string: "+pair);
    }
    this.pair = pair;
    minBufferSize = pair.getMaxTagLength();
    bufferCapacity = Configuration.getIntParam(PARAM_BUFFER_CAPACITY,
					       DEFAULT_BUFFER_CAPACITY);
    bufferCapacity = Math.max(minBufferSize, bufferCapacity);
    charBuffer = new FilterCharRing(bufferCapacity);
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


  public int read() throws IOException {
    char[] buf = new char[1];
    int ret = read(buf, 0, 1);
    if (ret >= 0) {
      return ((int)buf[0]);
    }
    else {
      return (ret);
    }
  }

//   /**
//    * Reads the next character.
//    * @return next character or -1 if there is nothing left
//    * @throws IOException if the reader it's constructed with throws
//    */
//   public int read() throws IOException {
//     if (charBuffer.size() < minBufferSize) {
//       refillBuffer(charBuffer, reader);
//     }
//     if (charBuffer.size() == 0) {
//       logger.debug3("Read Returning -1, spot a");
//       return -1;
//     }
//     if (charBuffer.size() >= pair.start.length()) {
//       while (startsWithTag(charBuffer, 0, pair.start, pair.ignoreCase)) {
// 	readThroughTag(charBuffer, reader, pair);
// 	if (charBuffer.size() < minBufferSize) {
// 	  refillBuffer(charBuffer, reader);
// 	}
//       }
//       if (charBuffer.size() == 0) {
// 	//ran out of stream while trying to read through a tag
// 	logger.debug3("Read Returning -1, spot b");
// 	return -1;
//       }
//     }
//     char returnChar = charBuffer.remove();
//     if (logger.isDebug3()) {
//       logger.debug3("Read returning "+returnChar);
//     }
//     return returnChar;
//   }


  /**
   * Reads through the charBuffer until the end tag is reached, taking
   * nesting into account
   *
   * May leave buffer empty even if there are chars left on the reader
   * @param charBuffer the buffer
   * @param reader the reader
   * @param pair the TagPair
   * @throws IOException
   */
  private void readThroughTag(FilterCharRing charBuffer, Reader reader,
			      TagPair pair)
      throws IOException {
    int idx = pair.start.length();
    int tagNesting = 1;
    int maxTagLength = (pair.start.length() > pair.end.length())
                         ? pair.start.length() : pair.end.length();
    if (logger.isDebug3()) {
      logger.debug3("reading through tag pair "+pair+" in "+charBuffer);
    }
    while (tagNesting > 0) {
      if (logger.isDebug3()) {
	logger.debug3("tagNesting: "+tagNesting);
      }
      if ((idx + maxTagLength) > charBuffer.size()) {
	charBuffer.clear(idx);
	streamDone = charBuffer.refillBuffer(reader);
	idx = 0;
      }
      if (streamDone && charBuffer.size() == 0) {
	return;
      }
      if (charBuffer.size() < pair.end.length()) {
	idx += charBuffer.size();
      } else {
	if (charBuffer.size() >= pair.start.length()
	    && charBuffer.startsWith(idx, pair.start,
				     pair.ignoreCase)) {
	  tagNesting++;
	  idx += pair.start.length();
	} else if (charBuffer.size() >= pair.end.length()
		   && charBuffer.startsWith(idx,
				    pair.end, pair.ignoreCase)) {
	  tagNesting--;
	  idx += pair.end.length();
	} else {
	  idx++;
	}
      }
    }
    charBuffer.clear(idx);
  }

  public void mark(int readAheadLimit) {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public int read(char[] outputBuf, int off, int len) throws IOException {
    if (logger.isDebug3()) {
      logger.debug3("Read array called with: ");
      logger.debug3("off: "+off);
      logger.debug3("len: "+len);
    }

    if ((off < 0) || (len < 0) || ((off + len) > outputBuf.length)) {
      throw new IndexOutOfBoundsException();
    } else if (len == 0) {
      return 0;
    }


    int numLeft = len;
    boolean matchedTag = false;
    while (numLeft > 0
	   && (!streamDone || charBuffer.size() > 0)) {
      if (charBuffer.size() < minBufferSize) {
	streamDone = charBuffer.refillBuffer(reader);
      }
      if (charBuffer.size() < pair.start.length()) {
	logger.debug3("Refill only returned "+charBuffer.size()+" elements, "+
		      "which is less than the start tag length("
		      +pair.start.length()+"), returning");
	int numToReturn =
	  numLeft < charBuffer.size() ? numLeft : charBuffer.size();
	charBuffer.remove(outputBuf, off + (len - numLeft), numToReturn);
	numLeft -= numToReturn;
      } else {
	int idx =0;
	while (!matchedTag && idx < numLeft
	       && idx+pair.start.length() <= charBuffer.size()) {
	  matchedTag = charBuffer.startsWith(idx,
					     pair.start, pair.ignoreCase);
	  if (!matchedTag) {
	    idx++;
	  }
	}
	if (idx > 0) {
	  charBuffer.remove(outputBuf, off + (len - numLeft), idx);
	}
	numLeft -= idx;
	if (matchedTag) {
	  readThroughTag(charBuffer, reader, pair);
	  matchedTag = false;
	}
      }
    }
    int numRead = len - numLeft;
    return numRead == 0 ? -1 : numRead;
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
