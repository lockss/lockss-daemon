/*
 * $Id: CharRing.java,v 1.10 2006-11-15 21:17:53 troberts Exp $
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

public class CharRing {
  private static Logger logger = Logger.getLogger("CharRing");

  protected char chars[];
  protected int head = 0;
  protected int tail = 0;
  protected int size = 0;
  protected int capacity = 0;
  protected boolean isTrace = logger.isDebug2();
  char[] preBuffer;

  public CharRing(int capacity) {
    if (capacity <= 0) {
      throw new IllegalArgumentException("Bad capacity");
    }
    this.capacity = capacity;
    chars = new char[capacity];
  }

  /**
   * returns the number of chars in the ring
   * @return number of chars in the ring
   */
  public int size() {
    return size;
  }

  /**
   * returns the number of chars this ring can hold
   * @return the number of chars this ring can hold
   */
  public int capacity() {
    return capacity;
  }

  /**
   * returns the nth char from this ring
   * @param n index of the char to return
   * @return nth char from this ring
   */
  public char get(int n) {
    if (n >= size) {
      throw new IndexOutOfBoundsException("Tried to get the "+n
					  +" element in a ring with "
					  +size+" elements");
    }
    return chars[(head + n) % capacity];
  }

  /**
   * add kar to the end of this ring
   * @param kar char to add to ring
   */
  public void add(char kar) throws RingFullException {
    if (size == capacity) {
      throw new RingFullException("Array is full");
    }
    if (isTrace) {
      logger.debug2("Adding "+kar+" to "+toString());
    }
    chars[tail] = kar;
    tail = (tail + 1) % capacity;
    size++;
  }

  /**
   * add all chars in newChars to this ring
   * @param newChars array of chars to add to this ring
   * @throws RingFullException if the chars in newChars will exceed
   * the ring's capacity
   */
  public void add(char newChars[]) throws RingFullException {
    add(newChars, 0, newChars.length);
  }

  /**
   * add all chars in newChars to this ring
   * @param newChars array of chars to add to this ring
   * @param pos position to begin read from newChars
   * @param length number of chars to read from newChars
   * @throws IndexOutOfBoundsException if pos or length specify an area
   * outside the array bounds
   * @throws RingFullException if the chars being added will exceed
   * the ring's capacity
   */
  public void add(char newChars[], int pos, int length)
      throws RingFullException {
    if (length + size > capacity) {
      throw new RingFullException("Array is full");
    }
    //number of chars to add to the end of array
    int addToEnd = length < (capacity - tail) ? length : (capacity - tail);

    //number of chars to add to the beginning of array
    int addToStart = length - addToEnd;

    if (addToEnd != 0) {
      System.arraycopy(newChars, pos, chars, tail, addToEnd);
    }
    if (addToStart != 0) {
      System.arraycopy(newChars, pos+addToEnd, chars, 0, addToStart);
    }
    tail = (tail + length) % capacity;
    size += length;
  }

  /**
   * remove the next char from the ring and return it
   * @return next char from the ring
   */
  public char remove() {
    if (size == 0) {
      throw new IndexOutOfBoundsException("remove() called on empty CharRing");
    }
    if (isTrace) {
      logger.debug2("Removing head from "+toString());
    }
    char returnKar = chars[head];
    head = (head + 1) % capacity;
    size--;

    if (isTrace) {
      logger.debug2("Returning "+returnKar);
    }

    return returnKar;
  }

  /**
   * remove the next returnChars.length chars from the ring
   * @param returnChars array to write the removed chars from
   * @return number of chars removed from the ring
   */
  public int remove(char returnChars[]) {
    return remove(returnChars, 0, returnChars.length);
  }

  /**
   * remove the next len chars from the ring into an array
   * @param returnChars array to write the removed chars to
   * @param pos position to begin writing into returnChars
   * @param len max number of chars to remove
   * @return number of chars removed from the ring
   */
  public int remove(char returnChars[], int pos, int len) {

    int numToReturn = len < size ? len : size;
    if (numToReturn == 0) {
      return 0;
    }

    //number of chars to remove from end of array
    int chunk1 =
      numToReturn < (capacity - head) ? numToReturn : (capacity - head);

    //number of chars to remove from start of array
    int chunk2 = numToReturn - chunk1;

    if (chunk1 != 0) {
      System.arraycopy(chars, head, returnChars, pos, chunk1);
    }
    if (chunk2 != 0) {
      System.arraycopy(chars, 0, returnChars, pos + chunk1, chunk2);
    }
    head = (head + numToReturn) % capacity;
    size -= numToReturn;
    return numToReturn;
  }

  /**
   * remove the next len chars from the ring into a StringBuffer.
   * @param sb StringBuffer to append to
   * @return number of chars removed from the ring
   */
  public int remove(StringBuffer sb, int len) {
    int numToReturn = len < size ? len : size;
    if (numToReturn == 0) {
      return 0;
    }
    //number of chars to remove from end of array
    int chunk1 =
      numToReturn < (capacity - head) ? numToReturn : (capacity - head);

    //number of chars to remove from start of array
    int chunk2 = numToReturn - chunk1;

    if (chunk1 != 0) {
      sb.append(chars, head, chunk1);
    }
    if (chunk2 != 0) {
      sb.append(chars, 0, chunk2);
    }
    head = (head + numToReturn) % capacity;
    size -= numToReturn;
    return numToReturn;
  }

  /**
   * Clear the ring.
   */
  public void clear() {
    skip(size);
  }

  /**
   * Skip over the next num chars from the ring.
   * @param num number of chars to skip
   */
  public void skip(int num) {
    if (num < 0) {
      throw new IndexOutOfBoundsException("Tried to clear a negative "
					  +"number: "+num);
    }
    if (num > size) {
      throw new IndexOutOfBoundsException("Tried to clear "+num
					  +" chars, but we only have "+size);
    }
    head = (head + num) % capacity;
    size -= num;
  }

  // This routine accounts for a substantial amount of the time spent in
  // LOCKSS filtered readers, so is worth optimizing for the common cases.
  private int indexOf(char ch, int startIdx, int lastIdx, boolean ignoreCase) {
    if (ignoreCase) {
      // do slow loop only if this char has different upper and lower case
      char uch = Character.toUpperCase(ch);
      if (ch != uch || ch != Character.toLowerCase(ch)) {
	for (int ix = startIdx; ix <= lastIdx; ix++) {
	  char rch = chars[(head + ix) % capacity];
	  if (Character.toUpperCase(rch) == uch) {
	    return ix;
	  }
	}
	return -1;
      }
    }
    if (startIdx > lastIdx) return -1;
    int startPos = (head + startIdx) % capacity;
    int endPos = (head + lastIdx) % capacity;
    if (startPos > endPos) {
      for (int ix = startPos; ix < capacity; ix++) {
	if (chars[ix] == ch) {
	  return ix + startIdx - startPos;
	}
      }
      for (int ix = 0; ix <= endPos; ix++) {
	if (chars[ix] == ch) {
	  return (ix + (capacity - head)) % capacity;
	}
      }
    } else {
      for (int ix = startPos; ix <= endPos; ix++) {
	if (chars[ix] == ch) {
	  return ix + startIdx - startPos;
	}
      }
    }
    return -1;

    // The code above should be equivalent to this
    //     for (int ix = startIdx; ix <= lastIdx; ix++) {
    //       char rch = chars[(head + ix) % capacity];
    //       if (rch == ch) {
    // 	       return ix;
    //       }
    //     }
  }

  /** Search for string in ring.
   * @param str string to search for
   * @param lastIdx last index at which to search for string
   * @param ignoreCase
   * @return index of string if found, else -1
   */
  public int indexOf(String str, int lastIdx, boolean ignoreCase) {
    int strlen = str.length();
    int lastPossible = size - strlen;
    if (lastPossible < 0) return -1;
    lastIdx = (lastIdx < 0 || lastIdx > lastPossible) ? lastPossible : lastIdx;
    int pos = 0;
    // find position of first char of string
    l1:
    while ((pos = indexOf(str.charAt(0), pos, lastIdx, ignoreCase)) >= 0) {
      int ringpos = head + pos;
      for (int ix = 1; ix < strlen; ix++) {
	char ch = str.charAt(ix);
	char rch = chars[(ringpos + ix) % capacity];
	if (ignoreCase) {
	  ch = Character.toUpperCase(ch);
	  rch = Character.toUpperCase(rch);
	}
	if (ch != rch) {
	  pos++;
	  continue l1;
	}
      }
      return pos;
    }
    return -1;
  }

  /**
   * Refill the buffer from the specified reader
   * @param reader reader from which to refill the charBuffer
   * @return true if the reader has reached eof
   * @throws IllegalArgumentException if called with a null reader
   */
  public boolean refillBuffer(Reader reader) throws IOException {
    if (reader == null) {
      throw new IllegalArgumentException("Called with null reader");
    }
    if (isTrace) {
      logger.debug3("Refilling buffer");
    }
    int maxRead;
    while ((maxRead = capacity - size) > 0) {
      // max chars to add to the end of array
      int maxEnd = (maxRead <= (capacity - tail)
		    ? maxRead : (capacity - tail));
      // max chars to add to the beginning of array
      int maxStart = maxRead - maxEnd;

      if (maxStart > 0) {
	// We have room at the beginning and end.  Using a temporary array
	// seems to be cheaper than calling read() twice
	if (preBuffer == null) {
	  preBuffer = new char[capacity];
	}
	int charsRead = reader.read(preBuffer, 0, maxRead);
	if (charsRead == -1) {
	  return true;
	}
	try {
	  add(preBuffer, 0, charsRead);
	} catch (CharRing.RingFullException e) {
	  logger.error("Overfilled a CharRing", e);
	  throw new IOException("Overfilled a CharRing");
	}
      } else {
	// Adding only to the middle or end, read directly into char buffer
	int charsRead = reader.read(chars, tail, maxEnd);
	if (charsRead == -1) {
	  return true;
	}
	tail = (tail + charsRead) % capacity;
	size += charsRead;
	if (charsRead < maxEnd) {
	  continue;
	}
      }
    }
    return false;
  }

  /**
   * Skips any leading whitespace and return true if any was found.
   * Note that this does not refill the buffer, so callers should do something
   * like this:
   * while(ring.skipLeadingWhiteSpace()) {
   * 	refill ring;
   * }
   * @return true if any leading whitespace was found, false otherwise
   */
  public boolean skipLeadingWhiteSpace() {
    if (Character.isWhitespace(get(0))) {
      int idx = 0;
      do {
	idx++;
      } while (idx < size() && Character.isWhitespace(get(idx)));
      skip(idx);
      return true;
    }
    return false;
  }

  /**
   * Return true if the ring starts with the specified string (ignoring case)
   * @param str String to check for
   * @return true if the ring starts with the specified string (ignoring case)
   */
  public boolean startsWithIgnoreCase(String str) {
    return startsWithIgnoreCase(str, 0);
  }

  /**
   * Return true if the ring starts with the specified string (ignoring case)
   * @param str String to check for
   * @param startIdx index to begin searching at
   * @return true if the ring starts with the specified string (ignoring case)
   */
  public boolean startsWithIgnoreCase(String str, int startIdx) {
    if (str.length()+startIdx > size()) {
      return false;
    }
    for (int ix=0; ix < str.length(); ix++) {
      if (!StringUtil.equalsIgnoreCase(get(ix+startIdx), str.charAt(ix))) {
	return false;
      }
    }
    return true;
  }


  public void add0(char newChars[], int pos, int length)
      throws RingFullException {

    tail = (tail + length) % capacity;
    size += length;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer(size);
    for (int ix=0; ix<size; ix++) {
      sb.append(chars[(head + ix) % capacity]);
    }
    return sb.toString();
  }

  public static class RingFullException extends Exception {
    public RingFullException(String msg) {
      super(msg);
    }
  }
}
