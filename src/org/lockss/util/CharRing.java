/*
 * $Id: CharRing.java,v 1.3 2003-06-13 00:34:28 troberts Exp $
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

package org.lockss.util;

public class CharRing {
  private static Logger logger = Logger.getLogger("CharRing");

  char chars[];
  int idx = -1;
  int head = 0;
  int tail = -1;
  int size = 0;


  public CharRing(int size) {
    if (size <= 0) {
      throw new IllegalArgumentException("Bad size");
    }
    chars = new char[size];
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
    return chars.length;
  }

  /**
   * returns the nth char from this ring
   * @param n index of the char to return
   * @return nth char from this ring
   */
  public char get(int n) {
    if (n >= chars.length) {
      throw new BadIndexException("Tried to get the "+n
				  +" element in a ring of length "
				  +chars.length);
    } else if (n >= size) {
      throw new BadIndexException("Tried to get the "+n
				  +" element in a ring with "
				  +size+" elements");
    }
    return chars[incrementIndex(head, n)];
  }

  /**
   * add kar to the end of this ring
   * @param kar char to add to ring
   */
  public void add(char kar) throws RingFullException {
    if (size == chars.length) {
      throw new RingFullException("Array is full");
    }
    incrementTail(1);
    if (logger.isDebug2()) {
      logger.debug2("Adding "+kar+" to "+toString());
    }
    chars[tail] = kar;
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
   * @throws RingFullException if the chars being added will exceed
   * the ring's capacity
   */
  public void add(char newChars[], int pos, int length)
      throws RingFullException {
    if (length + size > chars.length) {
      throw new RingFullException("Array is full");
    }
    incrementTail(1);
    int addToEnd = Math.min(length, chars.length - tail);
    int addToStart = length - addToEnd;

    System.arraycopy(newChars, pos, chars, tail, addToEnd);
    System.arraycopy(newChars, pos+addToEnd, chars, 0, addToStart);

    incrementTail(newChars.length-1);
    size += length;
  }

  /**
   * remove the next char from the ring and return it
   * @return next char from the ring
   */
  public char remove() {
    if (size == 0) {
      throw new BadIndexException("remove() called on empty CharRing");
    }
    if (logger.isDebug2()) {
      logger.debug2("Removing head from "+toString());
    }
    char returnKar = chars[head];
    incrementHead(1);
    size--;

    if (logger.isDebug2()) {
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
   * remove the next len chars from the ring
   * @param returnChars array to write the removed chars from
   * @param pos position to begin writting into array
   * @param len number of chars to remove
   * @return number of chars removed from the ring
   */ 
  public int remove(char returnChars[], int pos, int len) {
  //XXX should throw if trying to remove too many?
    int numToReturn = Math.min(len, size);
    if (numToReturn == 0) {
      return 0;
    }
    
    int chunk1 = Math.min(numToReturn, chars.length-head);
    int chunk2 = numToReturn - chunk1;
    System.arraycopy(chars, head, returnChars, pos, chunk1);
    System.arraycopy(chars, 0, returnChars, pos + chunk1, chunk2);

    incrementHead(numToReturn);
    size -= numToReturn;
    return numToReturn;
  }

  private int incrementIndex(int idx, int amt) {
    idx += amt;
    return idx % chars.length;
  }

  private void incrementHead(int amt) {
    head = incrementIndex(head, amt);
  }

  private void incrementTail(int amt) {
    tail = incrementIndex(tail, amt);
  }


  /**
   * Clear the next num chars from the ring.  Should be quicker than remove
   * @param num number of chars to clear
   */
  public void clear(int num) {
    if (num > size) {
      throw new BadIndexException("Tried to clear "+num
				  +" chars, but we only have "+size);
    }
    incrementHead(num);
    size -= num;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer(size);
    int curIdx = head;
    for (int ix=0; ix<size; ix++) {
      sb.append(chars[curIdx]);
      curIdx = incrementIndex(curIdx, 1);
    } 
    return sb.toString();
  }

  public static class RingFullException extends Exception {
    public RingFullException(String msg) {
      super(msg);
    }
  }

  public static class BadIndexException extends RuntimeException {
    public BadIndexException(String msg) {
      super(msg);
    }
  }

}
