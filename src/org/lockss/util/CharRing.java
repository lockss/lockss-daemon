/*
 * $Id: CharRing.java,v 1.2 2003-06-12 00:55:51 troberts Exp $
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

  public int size() {
    return size;
  }

  public int capacity() {
    return chars.length;
  }

  public char getNthChar(int n) {
    if (n >= chars.length) {
      throw new BadIndexException("Tried to get the "+n
				  +" element in a ring of length "
				  +chars.length);
    } else if (n >= size) {
      throw new BadIndexException("Tried to get the "+n
				  +" element in a ring with "
				  +chars.length+" elements");
    }
    return chars[incrementIndex(head, n)];
  }

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


  public void add(char newChars[]) throws RingFullException {
    add(newChars, 0, newChars.length);
  }
  
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


//   public char getHead() {
//     if (logger.isDebug2()) {
//       logger.debug2("Getting head from "+toString());
//     }

//     if (logger.isDebug2()) {
//       logger.debug2("Returning "+chars[head]);
//     }
//     return chars[head];
//   }

//   public char getTail() {
//     if (logger.isDebug2()) {
//       logger.debug2("Getting tail from "+toString());
//     }
//     return chars[tail];
//   }

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

  public int remove(char returnChars[]) {
    return remove(returnChars, 0, returnChars.length);
  }

  public int remove(char returnChars[], int pos, int len) {
    int numToReturn = Math.min(len, size);
    if (numToReturn == 0) {
      return 0;
    }
    
    if (tail < head) {
      int chunk1 = Math.min(numToReturn, chars.length-head);
      int chunk2 = numToReturn - chunk1;
      System.arraycopy(chars, head, returnChars, pos, chunk1);
      System.arraycopy(chars, 0, returnChars, pos + chunk1, chunk2);
    } else {
      System.arraycopy(chars, head, returnChars, pos, numToReturn);
    }

    incrementHead(numToReturn);
    size -= numToReturn;
    return numToReturn;
  }

//   private int incrementIndex(int idx) {
//     return incrementIndex(idx, 1);
// //     int nextIdx = idx+1;
// //     return nextIdx == chars.length ? 0 : nextIdx;
//   }

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

//   public int getHeadIndex() {
//     return head;
//   }

//   public int getTailIndex() {
//     return tail;
//   }

//   public char getChar(int idx) {
//     return chars[idx];
//   }


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
