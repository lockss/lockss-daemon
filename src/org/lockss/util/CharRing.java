/*
 * $Id: CharRing.java,v 1.1 2003-06-05 21:48:59 troberts Exp $
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

  public void add(char kar) {
    if (size == chars.length) {
      throw new UnsupportedOperationException("Array is full");
    }
    tail = incrementIndex(tail);
    if (logger.isDebug2()) {
      logger.debug2("Adding "+kar+" to "+toString());
    }
    chars[tail] = kar;
    size++;
  }

  public char getHead() {
    if (logger.isDebug2()) {
      logger.debug2("Getting head from "+toString());
    }

    if (logger.isDebug2()) {
      logger.debug2("Returning "+chars[head]);
    }
    return chars[head];
  }

  public char getTail() {
    if (logger.isDebug2()) {
      logger.debug2("Getting tail from "+toString());
    }
    return chars[tail];
  }

  public char remove() {
    if (logger.isDebug2()) {
      logger.debug2("Removing head from "+toString());
    }
    char returnKar = getHead();
    head = incrementIndex(head);
    size--;

    if (logger.isDebug2()) {
      logger.debug2("Returning "+returnKar);
    }

    return returnKar;
  }

  public int incrementIndex(int idx) {
    int nextIdx = idx+1;
    return nextIdx == chars.length ? 0 : nextIdx;
  }

  public int getHeadIndex() {
    return head;
  }

  public int getTailIndex() {
    return tail;
  }

  public char getChar(int idx) {
    return chars[idx];
  }

  public String toString() {
    StringBuffer sb = new StringBuffer(size);
    int curIdx = head;
    for (int ix=0; ix<size; ix++) {
      sb.append(chars[curIdx]);
      curIdx = incrementIndex(curIdx);
    } 
    return sb.toString();
  }

}
