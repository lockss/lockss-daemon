/*
 * $Id$
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.protocol;

import java.io.*;
import java.net.*;

import org.lockss.util.*;
import org.lockss.util.Queue;
import org.lockss.daemon.*;

public class ChannelStats {
  Count in = new Count();
  Count out = new Count();

  void add(ChannelStats other) {
    in.add(other.getInCount());
    out.add(other.getOutCount());
  }

  public Count getInCount() {
    return in;
  }

  public Count getOutCount() {
    return out;
  }

  void sentBytes(long n) {
    in.addBytes(n);
  }

  void rcvdBytes(long n) {
    out.addBytes(n);
  }

  void sentMsg() {
    in.addMsg();
  }

  void rcvdMsg() {
    out.addMsg();
  }

  public class Count {
    int msgs = 0;
    long bytes = 0;

    void addBytes(long n) {
      bytes += n;
    }

    void addMsg() {
      msgs++;
    }

    void add(Count other) {
      bytes += other.getBytes();
      msgs += other.getMsgs();
    }

    int getMsgs() {
      return msgs;
    }

    long getBytes() {
      return bytes;
    }
  }
}
