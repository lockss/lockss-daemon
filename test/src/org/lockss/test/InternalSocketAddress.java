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

/*
 * Adapted from InProcSocketAddress, written by Dawid Kurzyniec and released
 * under Creative Commans (http://creativecommons.org/licenses/publicdomain)
 */

package org.lockss.test;

import java.io.*;
import java.net.*;

/**
 * Represents an address of internal socket. Consists of a logical port
 * number.
 *
 * @author Dawid Kurzyniec
 * @version 1.0
 */
public class InternalSocketAddress extends SocketAddress {

  final int port;

  /**
   * Creates new internal socket address representing specified logical port.
   * @param port the logical port
   */
  public InternalSocketAddress(int port) {
    if (port < 0) throw new IllegalArgumentException("Port number must be non-negative");
    this.port = port;
  }

  /**
   * Returns the logical port number represented by this internal socket
   * address.
   *
   * @return logical port number
   */
  public int getPort() {
    return port;
  }

  public int hashCode() {
    return port;
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof InternalSocketAddress)) return false;
    InternalSocketAddress that = (InternalSocketAddress)obj;
    return this.port == that.port;
  }
}
