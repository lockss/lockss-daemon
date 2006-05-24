/*
 * $Id: StringFilter.java,v 1.11 2006-05-24 23:04:17 tlipkis Exp $
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

/** Filter that removes all instances of a string, or replaces all
 * instances of a string with another string */
public class StringFilter extends Reader {

  public static final int DEFAULT_BUFFER_CAPACITY = 4096;
  public static final String PARAM_BUFFER_CAPACITY =
    Configuration.PREFIX + "filter.buffer_capacity";

  private static Logger logger = Logger.getLogger("StringFilter");
  private boolean streamDone = false;

  private CharRing charBuffer = null;
  private int ringSize;
  private Reader reader;
  private String str;
  private String replaceStr = null;
  private int strlen;
  private int replaceLen;
  private boolean ignoreCase = false;
  private int toReplace = 0;
  private boolean isClosed = false;
  private boolean isTrace = logger.isDebug3();

  public StringFilter(Reader reader, String str) {
    this(reader, -1, str, null);
  }

  public StringFilter(Reader reader, int bufferCapacity, String str) {
    this(reader, bufferCapacity, str, null);
  }

  public StringFilter(Reader reader, String origStr, String replaceStr) {
    this(reader, -1, origStr, replaceStr);
  }

  public StringFilter(Reader reader, int bufferCapacity,
		      String origStr, String replaceStr) {
    if (reader == null) {
      throw new IllegalArgumentException("Called with a null reader");
    }
    this.reader = reader;
    if (origStr == null) {
      throw new IllegalArgumentException("Called with a null string");
    }
    this.str = origStr;
    strlen = str.length();
    if (bufferCapacity < 0) {
      bufferCapacity = CurrentConfig.getIntParam(PARAM_BUFFER_CAPACITY,
                                                 DEFAULT_BUFFER_CAPACITY);
    }
    // Avoid problems caused by buffer smaller than search string
    if (bufferCapacity < strlen) {
      bufferCapacity = strlen;
    }
    this.replaceStr = replaceStr;
    if (replaceStr != null) {
      replaceLen = replaceStr.length();
    }
    charBuffer = new CharRing(bufferCapacity);
    ringSize = charBuffer.size();
  }

  public void setIgnoreCase(boolean ignoreCase) {
    this.ignoreCase = ignoreCase;
  }

  public boolean ignoresCase() {
    return ignoreCase;
  }

  /**
   * Factory method for a series of nested StringFilters, with no replacement.
   * @param reader the source Reader
   * @param strList a List of strings to remove
   * @return the nested StringFilter
   */
  public static StringFilter makeNestedFilter(Reader reader, List strList) {
    if (reader == null) {
      throw new IllegalArgumentException("Called with a null Reader");
    } else if (strList == null) {
      throw new IllegalArgumentException("Called with a null List");
    } else if (strList.size() <= 0) {
      throw new IllegalArgumentException("Called with a empty list");
    }
    Reader curReader = reader;
    for (int ix = 0; ix < strList.size(); ix++) {
      curReader = new StringFilter(curReader, (String)strList.get(ix));
    }
    return (StringFilter)curReader;
  }

  /**
   * Factory method for a series of nested StringFilters, with an array of
   * strings and their replacements (may be null for none).
   * @param reader the source Reader
   * @param strArray 2-dimensional array of strings and replacements
   * @param ignoreCase set for all the filters
   * @return the nested StringFilter
   */
  public static StringFilter makeNestedFilter(Reader reader,
                                              String[][] strArray,
                                              boolean ignoreCase) {
    if (reader == null) {
      throw new IllegalArgumentException("Called with a null Reader");
    } else if (strArray == null) {
      throw new IllegalArgumentException("Called with a null List");
    } else if ((strArray.length <= 0) || (strArray[0].length <= 0)) {
      throw new IllegalArgumentException("Called with a empty array");
    }
    Reader curReader = reader;
    for (int ix = 0; ix < strArray.length; ix++) {
      String srcStr = strArray[ix][0];
      String replaceStr = null;
      if (strArray[ix].length > 1) {
        replaceStr = strArray[ix][1];
      }
      curReader = new StringFilter(curReader, srcStr, replaceStr);
      ((StringFilter)curReader).setIgnoreCase(ignoreCase);
    }
    return (StringFilter)curReader;
  }

  public int read(char[] outputBuf, int off, int bufSize) throws IOException {
    if (isTrace) logger.debug3("read(buf, " + off + ", " + bufSize + ")");
    if (isClosed) throw new IOException("Read from closed StringFilter");
    int bufPtrPlusFree = off + bufSize;
    if ((off < 0) || (bufSize < 0) || (bufPtrPlusFree > outputBuf.length)) {
      throw new IndexOutOfBoundsException();
    }

    int bufFree = bufSize;
    while (bufFree > 0 &&
	   (!streamDone || (ringSize > 0) || (toReplace > 0))) {
      // if we need to insert the replacement string
      if (toReplace > 0) {
	// store all or part of the replace string in the output
	int replaceIndex = replaceLen - toReplace;
	int n = bufFree < toReplace ? bufFree : toReplace;
	replaceStr.getChars(replaceIndex, replaceIndex + n,
			    outputBuf, bufPtrPlusFree - bufFree);
	toReplace -= n;
	bufFree -= n;
	if (bufFree == 0) {
	  return bufSize;
	}
      }
      if (ringSize < strlen && !streamDone) {
	if (!streamDone) {
	  streamDone = charBuffer.refillBuffer(reader);
	  ringSize = charBuffer.size();
	}
      } else {
	int idx = charBuffer.indexOf(str, bufFree, ignoreCase);
	int ncopy;
	if (idx >= 0) {
	  ncopy = idx;
	} else {
	  int max = streamDone ? ringSize : (ringSize - (strlen - 1));
	  ncopy = bufFree < max ? bufFree : max;
	}
	charBuffer.remove(outputBuf, bufPtrPlusFree - bufFree, ncopy);
	ringSize -= ncopy;
	bufFree -= ncopy;
	if (idx >= 0) {
	  charBuffer.skip(strlen);
	  ringSize -= strlen;
          if (replaceStr!=null) {
            // set the replace index to the start of the string
	    toReplace = replaceLen;
          }
	}
      }
    }
    int numRead = bufSize - bufFree;
    return numRead == 0 ? -1 : numRead;
  }

  public void close() throws IOException {
    isClosed = true;
    reader.close();
  }
}
