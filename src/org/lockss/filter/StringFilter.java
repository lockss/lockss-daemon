/*
 * $Id: StringFilter.java,v 1.2 2003-12-11 22:36:50 eaalto Exp $
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

public class StringFilter extends Reader {

  public static final int DEFAULT_BUFFER_CAPACITY = 256;
  public static final String PARAM_BUFFER_CAPACITY =
    Configuration.PREFIX + "filter.buffer_capacity";

  private static Logger logger = Logger.getLogger("StringFilter");
  private boolean streamDone = false;

  private int bufferCapacity;

  private FilterCharRing charBuffer = null;
  private Reader reader;
  private String str;
  private String replaceStr = null;
  private boolean ignoreCase = false;
  private int replaceIndex = -1;

  public StringFilter(Reader reader) {
    if (reader == null) {
      throw new IllegalArgumentException("Called with a null reader");
    }
    this.reader = reader;
  }

  public StringFilter(Reader reader, String str) {
    this(reader);
    if (str == null) {
      throw new IllegalArgumentException("Called with a null string");
    }
    this.str = str;
    init();
  }

  public StringFilter(Reader reader, String origStr, String replaceStr) {
    this(reader, origStr);
    this.replaceStr = replaceStr;
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


//   /**
//    * @param reader Reader to filter
//    * @param str String to filter out of reader
//    */
//   public StringFilter(Reader reader, List strs) {
//     this(reader);
//     if (strs == null) {
//       throw new IllegalArgumentException("Called with a null list");
//     } else if (strs.size() == 0) {
//       throw new IllegalArgumentException("Called with a empty list");
//     }
//     this.str = (String)strs.get(0);
//     minBufferSize = getMaxStringLength(strs);
//     init();
//   }

//   private int getMaxStringLength(String[] strs) {
//     int len = 0;
//     for(int ix=0; ix<strs.length; ix++) {
//       if (len < strs[ix].length()) {
// 	len = strs[ix].length();
//       }
//     }
//     return len;
//   }

  private void init() {
    bufferCapacity = Configuration.getIntParam(PARAM_BUFFER_CAPACITY,
					       DEFAULT_BUFFER_CAPACITY);
    bufferCapacity = Math.max(str.length(), bufferCapacity);
    charBuffer = new FilterCharRing(bufferCapacity);
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
    boolean matchedStr = false;
    while (numLeft > 0 &&
           (!streamDone || (charBuffer.size() > 0) || (replaceIndex >=0))) {
      // if we need to insert the replacement string
      if (replaceIndex >= 0) {
        // work through the replace string until either it's done
        // or we've done enough chars
        while (numLeft > 0 && (replaceIndex < replaceStr.length())) {
          outputBuf[off + (len - numLeft)] = replaceStr.charAt(replaceIndex++);
          numLeft--;
        }
        // reset the index if done with the replace string
        if (replaceIndex >= replaceStr.length()) {
          replaceIndex = -1;
        }
      }

      if (charBuffer.size() < str.length()) {
	streamDone = charBuffer.refillBuffer(reader);
      }
      if (charBuffer.size() < str.length()) {
	logger.debug3("Refill only returned "+charBuffer.size()+" elements, "+
		      "which is less than the str length("
		      +str.length()+"), returning");
	int numToReturn =
	  numLeft < charBuffer.size() ? numLeft : charBuffer.size();
	charBuffer.remove(outputBuf, off + (len - numLeft), numToReturn);
	numLeft -= numToReturn;
      } else {
	int idx = 0;
	while (!matchedStr && idx < numLeft
	       && idx+str.length() <= charBuffer.size()) {
	  matchedStr = charBuffer.startsWith(idx, str, ignoreCase);
	  if (!matchedStr) {
	    idx++;
	  }
	}
	if (idx > 0) {
	  charBuffer.remove(outputBuf, off + (len - numLeft), idx);
	}
	numLeft -= idx;
	if (matchedStr) {
	  charBuffer.clear(str.length());
          if (replaceStr!=null) {
            // set the replace index to the start of the string
            replaceIndex = 0;
          }
	  matchedStr = false;
	}
      }
    }

    int numRead = len - numLeft;
    return numRead == 0 ? -1 : numRead;
  }

  public void close() throws IOException {
    throw new UnsupportedOperationException("Not Implemented");
  }
}
