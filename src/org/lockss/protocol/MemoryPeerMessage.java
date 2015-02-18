/*
 * $Id$
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.*;
import org.lockss.util.*;

/** Implementation of PeerMessage that stores its data in an array
 */
class MemoryPeerMessage extends PeerMessage {

  private byte[] payload = null;

  /** Create a MemoryPeerMessage
   */
  public MemoryPeerMessage() {
    super();
  }

  /** Return an InputStream on the payload.
   * @throws IllegalStateException if message data not stored yet
   */
  public synchronized InputStream getInputStream()
      throws IllegalStateException {
    checkHasData();
    return new ByteArrayInputStream(payload);
  }

  /** Return an OutputStream to which to write the payload.  May only be
   * called once.
   * @throws IllegalStateException if called a second time
   */
  public synchronized OutputStream getOutputStream()
      throws IllegalStateException {
    if (hasData() || isOutputOpen) {
      throw new IllegalStateException("PeerMessage already open for output");
    }
    isOutputOpen = true;
    return new MsgOutputStream();
  }

  public void delete() {
    payload = null;
  }

  public boolean hasData() {
    return payload != null;
  }

  /** Return the size of the data
   * @throw IllegalStateException if message data not stored yet
   */
  public long getDataSize() {
    checkHasData();
    return payload.length;
  }

  public String toString() {
    return this.toString("Memory");
  }

  private class MsgOutputStream extends ByteArrayOutputStream {

    public synchronized void close() throws IOException {
      if (isOutputOpen) {
	super.close();
	payload = toByteArray();
	isOutputOpen = false;
      }
    }
  }
}
