/*
 * $Id: HtmlTagFilter.java,v 1.6 2003-06-12 00:55:51 troberts Exp $
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

package org.lockss.crawler;
import java.io.*;
import java.util.*;
import org.apache.commons.collections.*;
import org.lockss.util.*;

/**
 * This class is used to filter all content from a reader between two string
 * (for instance "<!--" and "-->"
 */
public class HtmlTagFilter extends Reader {
  /**
   * TODO
   * 1)Check to see if we can inherit the 2 char array reads from another class
   * 2)See how Mozilla handles nested comments
   * 3)Use better string searching algorithm
   */


  Reader reader = null;
  TagPair pair = null;
  CharRing charBuffer = null;
//   Buffer charBuffer = null;
  int minBufferSize = 0;
  int bufferCapacity = 256;
  char[] preBuffer = new char[bufferCapacity];
  boolean streamDone = false;
  boolean withinIgnoredTag = false;

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
    }
    this.pair = pair;
    minBufferSize = pair.getMaxTagLength();
    logger.debug3("minBufferSize1: "+minBufferSize);
    bufferCapacity = Math.max(minBufferSize, bufferCapacity);
    charBuffer = new CharRing(bufferCapacity);
  }
  /**
   * Create a filter with multiple tags.  When filtering with multiple tags
   * it behaves as though everything between each pair is removed sequentially
   * (ie, everything between the first pair is filtered, then the second pair
   * then the third, etc).
   *
   * @param reader reader to filter from
   * @param pairs List of TagPairs to filter between.
   * @see HtmlTagFilter#read()
   */
  public HtmlTagFilter(Reader reader, List pairs) {
    // XXX add check to make sure no tag is a substring of another
    this(reader);
    if (pairs == null) {
      throw new IllegalArgumentException("Called with a null tag pair list");
    }
    if (pairs.size() < 1) {
      throw new IllegalArgumentException("Called with empty tag pair list");
    }
    this.pair = (TagPair) pairs.get(pairs.size()-1);
    minBufferSize = pair.getMaxTagLength();
    bufferCapacity = Math.max(minBufferSize, bufferCapacity);
    charBuffer = new CharRing(bufferCapacity);
    if (pairs.size() >= 2) {
      this.reader = new HtmlTagFilter(reader, 
				      pairs.subList(0,pairs.size()-1));
    }
  }

//   /**
//    * Reads the next character.
//    * @return next character or -1 if there is nothing left
//    * @throws IOException if the reader it's constructed with throws
//    */
//   public int read() throws IOException {
//     //read until the buffer is at capacity
//     //or there's nothing more in the reader

//     //XXX do this more efficiently
//     refillBuffer(charBuffer, reader);
//     if (charBuffer.size() < 1) {
//       logger.debug3("Read Returning -1, spot a");
//       return -1;
//     }

//     if (startsWithTag(charBuffer, pair.getStart())) {
//       readThroughTag(charBuffer, reader, pair);
//     }
//     if (charBuffer.size() == 0) {
//       logger.debug3("Read Returning -1, spot b");
//       return -1;
//     }
//     char returnChar = charBuffer.remove();
//     if (logger.isDebug3()) {
//       logger.debug3("Read returning "+returnChar);
//     }
//     return returnChar;
//   }


  private void refillBuffer(CharRing charBuffer, Reader reader) 
      throws IOException {
    logger.debug3("Refilling buffer");
    int curKar;

    int charsNeeded = charBuffer.capacity() - charBuffer.size();
    while (charsNeeded > 0) {
      int charsRead = reader.read(preBuffer, 0, charsNeeded);
      if (charsRead == -1) {
	streamDone = true;
	return;
      }
      try{
	charBuffer.add(preBuffer, 0, charsRead);
      } catch (CharRing.RingFullException e) {
	//XXX handle this
	logger.error("Overfilled a CharRing", e);
      }
      charsNeeded = charBuffer.capacity() - charBuffer.size();
    }
  }

  private boolean startsWithTag(CharRing charBuffer, String tag) {
    return startsWithTag(charBuffer, 0, tag);
  }

  private boolean startsWithTag(CharRing charBuffer, int idx, String tag) {
    //XXX reimplement with DNA searching algorithm

    if (logger.isDebug3()) {
      logger.debug3("checking if \""+charBuffer+"\" has \""
		    +tag+"\" at index "+idx);
    }
    //less common case than first char not match, but we have to check for 
    //size before that anyway
    if (charBuffer.size() < tag.length() + idx) {
      return false;
    }

    int ringArrayIdx = idx;
    int charPos = 0;
    while (ringArrayIdx < charBuffer.size()
	   && charPos < tag.length()) {
      char curChar = charBuffer.getNthChar(ringArrayIdx);
      if (!charEqualsIgnoreCase(curChar, tag.charAt(charPos))) {
	logger.debug3("It doesn't");
	return false;
      }
      ringArrayIdx++;
      charPos++;
    }
    logger.debug3("It does");
    return true;
  }

  private boolean charEqualsIgnoreCase(char kar1, char kar2) {
    return (Character.toUpperCase(kar1) == Character.toUpperCase(kar2));
  }


  private void readThroughTag(CharRing charBuffer, Reader reader,
			      TagPair pair)
  throws IOException {
    int tagNesting = 0;
    if (logger.isDebug3()) {
      logger.debug3("reading through tag pair "+pair+" in "+charBuffer);
    }
    do {
      if (logger.isDebug3()) {
	logger.debug3("tagNesting: "+tagNesting);
      }
      if (startsWithTag(charBuffer, pair.getStart())) {
	removeAndReplaceChars(charBuffer, reader, pair.getStart().length());
	tagNesting++;
      } else if (startsWithTag(charBuffer, pair.getEnd())) {
	removeAndReplaceChars(charBuffer, reader, pair.getEnd().length());
	tagNesting--;
      } else {
	removeAndReplaceChars(charBuffer, reader, 1);
      }
      if (streamDone && charBuffer.size() == 0) {
	return;
      }
    } while (tagNesting > 0);
  }
  
  private void removeAndReplaceChars(CharRing charBuffer,
				     Reader reader, int numChars)
      throws IOException {
    charBuffer.clear(numChars);
    if (charBuffer.size() < minBufferSize) {
      refillBuffer(charBuffer, reader);
    } 
  }

  public void mark(int readAheadLimit) {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public boolean markSupported() {
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
    } else if (len ==0) {
      return 0;
    }
    

    int numLeft = len;
    boolean matchedTag = false;
    while (numLeft > 0
	   && (!streamDone || charBuffer.size() > 0)) {
      if (charBuffer.size() < minBufferSize) {
	refillBuffer(charBuffer, reader);
      }
      int idx =0;
      while (!matchedTag && idx < numLeft && idx < charBuffer.size()) {
	matchedTag = startsWithTag(charBuffer, idx, pair.getStart());
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
	System.err.println("rtt: "+charBuffer);
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

    public TagPair(String start, String end) {
      if (start == null || end == null) {
	throw new IllegalArgumentException("Called with a null start "+
					 "or end string");
      }
      this.start = start;
      this.end = end;
    }

    public String getStart() {
      return start;
    }

    public String getEnd() {
      return end;
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
      TagPair pair = (TagPair) obj;
      return (start.equals(pair.getStart()) && end.equals(pair.getEnd()));
    }

    public int hashCode() {
      return (start.hashCode() + end.hashCode());
    }
  }
}
