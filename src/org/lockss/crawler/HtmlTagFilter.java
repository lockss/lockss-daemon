/*
 * $Id: HtmlTagFilter.java,v 1.5 2003-06-02 23:48:08 troberts Exp $
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
//   List charBuffer = null;
  Buffer charBuffer = null;
  int bufferCapacity = 0;
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
    bufferCapacity = pair.getMaxTagLength();
    charBuffer = new BoundedFifoBuffer(bufferCapacity);
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
    bufferCapacity = pair.getMaxTagLength();
    charBuffer = new BoundedFifoBuffer(bufferCapacity);
    if (pairs.size() >= 2) {
      this.reader = new HtmlTagFilter(reader, 
				      pairs.subList(0,pairs.size()-1));
    }
  }

  /**
   * Reads the next character.
   * @return next character or -1 if there is nothing left
   * @throws IOException if the reader it's constructed with throws
   */
  public int read() throws IOException {
    //read until the buffer is at capacity
    //or there's nothing more in the reader

    //XXX do this more efficiently
    while (charBuffer.size() < bufferCapacity && !streamDone) {
      addCharToBuffer(charBuffer, reader);
    }
    if (charBuffer.size() < 1) {
      logger.debug3("Read Returning -1, spot a");
      return -1;
    }

    if (startsWithTag(charBuffer, pair.getStart())) {
      readThroughTag(charBuffer, reader, pair);
    }
    if (charBuffer.size() == 0) {
      logger.debug3("Read Returning -1, spot b");
      return -1;
    }
    char returnChar = ((Character)charBuffer.remove()).charValue();
    if (logger.isDebug3()) {
      logger.debug3("Read returning "+returnChar);
    }
    return returnChar;
  }

 /**
   * Pull chars off the reader and put them into the char buffer until
   * the reader has no more or we're at bufferCapacity.
   * @param charBuffer a List of Character objects
   * @param reader a Reader to pull chars from
   * @return false if there were not enough chars on the reader to do this
   * @throws IOException if the reader it's constructed with throws
   */
  private boolean addCharToBuffer(Buffer charBuffer, Reader reader)
      throws IOException {
    int curKar;
    if ((curKar = reader.read()) == -1) {
      streamDone = true;
      return false;
    } else {
      if (logger.isDebug3()) {
	logger.debug3("Adding "+(char)curKar+" to charBuffer");
      }
      charBuffer.add(new Character((char)curKar));
    }
    return true;
  }


  private boolean startsWithTag(Buffer charBuffer, String tag) {
    //XXX reimplement with DNA searching algorithm

    if (logger.isDebug3()) {
      logger.debug3("checking if "+charBuffer+" starts with "+tag);
    }
    //less common case than first char not match, but we have to check for 
    //size before that anyway
    if (charBuffer.size() < tag.length()) {
      return false;
    }
    char firstChar = ((Character)charBuffer.get()).charValue();
    if (!charEqualsIgnoreCase(firstChar, tag.charAt(0))) {
      return false;
    }
    //XXX don't use an iterator, slow
    Iterator it = charBuffer.iterator();
    int charPos = 0;
    while (it.hasNext() && charPos < tag.length()) {
      char curChar = ((Character)it.next()).charValue();
      if (!charEqualsIgnoreCase(curChar, tag.charAt(charPos))) {
	logger.debug3("It doesn't");
	return false;
      }
      charPos++;
    }
    logger.debug3("It does");
    return true;
  }

  private boolean charEqualsIgnoreCase(char kar1, char kar2) {
    return (Character.toUpperCase(kar1) == Character.toUpperCase(kar2));
  }


  private void readThroughTag(Buffer charBuffer, Reader reader,
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
	if (!removeAndReplaceChars(charBuffer,
				   reader, pair.getStart().length())) {
	  return;
	}
	tagNesting++;
      } else if (startsWithTag(charBuffer, pair.getEnd())) {
	if (!removeAndReplaceChars(charBuffer,
				   reader, pair.getEnd().length())) {
	  return;
	}
	tagNesting--;
      } else {
	if (!removeAndReplaceChars(charBuffer,
				   reader, 1)) {
	  return;
	}
      }
    } while (tagNesting > 0);
  }

  private boolean removeAndReplaceChars(Buffer charBuffer,
					Reader reader, int numChars)
  throws IOException {
    for (int ix=0; ix<numChars; ix++) {
      if (logger.isDebug3()) {
	logger.debug3("One loop: "+charBuffer);
      }
      if (charBuffer.size() == 0) {
	logger.debug3("removeAndReplaceChars ran out of chars, returning");
	return false;
      }
      charBuffer.remove();
      addCharToBuffer(charBuffer, reader);
    }
    return true;
  }

  public void mark(int readAheadLimit) {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public boolean markSupported() {
    throw new UnsupportedOperationException("Not Implemented");
  }

  public int read(char[] outputBuf) throws IOException {
    return read(outputBuf, 0, outputBuf.length);
  }
  
  public int read(char[] outputBuf, int off, int len) throws IOException {
    logger.debug3("Read array called");
    for (int ix=0; ix<off; ix++) {
      if (read() == -1) {
 	return -1;
      }
    }
    int size = 0;
    int curKar;
    int charsToRead = len < outputBuf.length ? len : outputBuf.length;
    while (size < charsToRead && (curKar = read()) != -1) {
      outputBuf[size] = (char)curKar;
      size++;
    }
    if (logger.isDebug3()) {
      logger.debug3("Read array returning "+size);
    }
    return size == 0 ? -1 : size;
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
