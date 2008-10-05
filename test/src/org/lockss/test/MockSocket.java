/*
 * $Id: MockSocket.java,v 1.2 2005-10-11 05:52:05 tlipkis Exp $
 */

/*

Copyright (c) 2000-2004 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.net.*;

public class MockSocket extends Socket {
  public InetAddress getInetAddress() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public InetAddress getLocalAddress() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public int getPort() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public int getLocalPort() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public InputStream getInputStream() throws IOException {
    throw new UnsupportedOperationException("Not implemented");
  }

  public OutputStream getOutputStream() throws IOException {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void setTcpNoDelay(boolean on) throws SocketException {
    throw new UnsupportedOperationException("Not implemented");
  }

  public boolean getTcpNoDelay() throws SocketException {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void setSoLinger(boolean on, int linger) throws SocketException {
    throw new UnsupportedOperationException("Not implemented");
  }

  public int getSoLinger() throws SocketException {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void setSoTimeout(int timeout) throws SocketException {
    throw new UnsupportedOperationException("Not implemented");
  }

  public int getSoTimeout() throws SocketException {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void setSendBufferSize(int size)
      throws SocketException{
    throw new UnsupportedOperationException("Not implemented");
  }

  public int getSendBufferSize() throws SocketException {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void setReceiveBufferSize(int size) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public int getReceiveBufferSize() throws SocketException {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void setKeepAlive(boolean on) throws SocketException {
    throw new UnsupportedOperationException("Not implemented");
  }

  public boolean getKeepAlive() throws SocketException {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void close() throws IOException {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void shutdownInput() throws IOException {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void shutdownOutput() throws IOException {
    throw new UnsupportedOperationException("Not implemented");
  }

  public String toString() {
    return "[MockSocket]";
  }

}
