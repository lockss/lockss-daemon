/*
 * $Id$
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

package org.lockss.test;

import java.util.*;
import org.lockss.util.*;

/**
 * Mock implementation of LogTarget
 */

public class MockLogTarget implements LogTarget{
  static Logger log = Logger.getLogger("Mock log target");
  Vector messages;
  int initCount = 0;

  public MockLogTarget(){
    messages = new Vector();
  }

  public void init() {
    initCount++;
  }

  /**
   * Adds the message and severity to a Vector, so they can be retrieved
   * by unit tests
   */
  public void handleMessage(Logger log, int msgLevel, String message) {
    StringBuffer sb = new StringBuffer();
    sb.append(Logger.nameOf(msgLevel));
    sb.append(": ");
    sb.append(message);
    String str = sb.toString();
    messages.add(str);
  }

  public Iterator messageIterator() {
    return messages.iterator();
  }

  public List getMessages() {
    return messages;
  }

  public int messageCount() {
    return messages.size();
  }

  public void resetMessages() {
    messages.clear();
  }

  public int initCount() {
    return initCount;
  }

  public boolean hasMessage(String str) {
    return messages.contains(str);
  }

  public void dumpMessages() {
    Iterator it = messageIterator();
    while (it.hasNext())
    {
      System.out.println((String)it.next());
    }
  }

}
