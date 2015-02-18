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
 * Adapted from InProcSocket, written by Dawid Kurzyniec and released
 * under Creative Commans (http://creativecommons.org/licenses/publicdomain)
 */

package org.lockss.test;

import java.io.*;
import java.net.*;
import org.lockss.util.*;

/**
 * Abstraction of a socket accessible only within a process. While this
 * class fully adheres to the socket API, it is a socket that can only
 * connect to an appropriate {@link InternalServerSocket "server socket"}
 * within the same process.
 *
 * @see InternalServerSocket
 *
 * @author Dawid Kurzyniec
 * @version 1.0
 */
public class InternalSocket extends Socket {
  static Logger log = Logger.getLogger("InternalSocket");

  final static InternalSocket CONNECT_CANCELLED = new InternalSocket(false);

  public static final InetAddress internalInetAddr;
  static {
    try {
      internalInetAddr = InetAddress.getByAddress("internal.localhost",
						  new byte[] {0x7f, 0, 0, (byte)0x81});
    }
    catch (UnknownHostException e) {
      // can't happen; see Javadoc
      throw new RuntimeException("FATAL:", e);
    }
  }

  int remotePort;
  int localPort;
  volatile int soTimeout;

  volatile boolean closed;
  volatile boolean connected;
  volatile boolean inputShut;
  volatile boolean outputShut;
  volatile boolean inputClosed;
  volatile boolean outputClosed;

  boolean connecting;

  InputStream in;
  InternalSocketOutputStream out;

  /**
   * Creates new internal socket and connects it to a server socket
   * listening on a specified port.
   *
   * @param address the destination address; must be = internalInetAddr
   * @param port the port to connect to
   * @throws IOException if I/O error occurs
   */
  public InternalSocket(InetAddress address, int port) throws IOException {
    this();
    if (!address.equals(internalInetAddr)) {
      throw new IllegalArgumentException("InternalSocket can only connect to internalInetAddr, not to " + address);
    }
    connect(new InternalSocketAddress(port));
  }

  /**
   * Creates new internal socket and connects it to a server socket
   * listening on a specified port.
   *
   * @param port the port to connect to
   * @throws IOException if I/O error occurs
   */
  public InternalSocket(int port) throws IOException {
    this();
    connect(new InternalSocketAddress(port));
  }

  /**
   * Creates new, unconnected internal socket.
   *
   * @throws SocketException thrown by a superclass constructor
   */
  public InternalSocket() throws SocketException {
    super(new InternalSocketImpl());
  }

  private InternalSocket(boolean dummy) {}

  InternalSocket(Channel channel, int localPort) {
    this.localPort = localPort;
    setChannel(channel);
  }

  private synchronized void setChannel(Channel ch) {
    in = new InternalSocketInputStream(ch.getInput());
    out = new InternalSocketOutputStream(ch.getOutput());
    connected = true;
  }

  /**
   * Connects the socket to the internal server socket. The
   * <code>endpoint</code> must be an instance of
   * {@link InternalSocketAddress}, that is, it must provide an internal port
   * number to connect to.
   *
   * @param endpoint the internal endpoint to connect to
   * @param timeout the timeout to wait for a connection to be established
   *
   * @throws	IOException if an error occurs during the connection
   * @throws	SocketTimeoutException if timeout expires before connecting
   * @throws  IllegalArgumentException if endpoint is not a
   *          InternalSocketAddress instance
   */
  public void connect(SocketAddress endpoint, int timeout) throws IOException {
    if (!(endpoint instanceof InternalSocketAddress)) {
      throw new IllegalArgumentException("connect: The address must be " +
					 "an instance of InternalSocketAddress");
    }
    if (timeout < 0) {
      throw new IllegalArgumentException("connect: timeout can't be negative");
    }
    if (isClosed()) throw new SocketException("Socket is closed");
    InternalSocketAddress addr = (InternalSocketAddress)endpoint;
    /** @todo security check? */
    synchronized (this) {
      if (connecting) {
	throw new IOException("Connect already in progress");
      }
      connecting = true;
    }
    try {
      Channel channel = InternalServerSocket.connect(addr.port, timeout);
      setChannel(channel);
    }
    catch (IOException e) {
      // failed
      close();
    } finally {
      synchronized (this) {
	connecting = false;
      }
    }
  }

  /**
   * Not supported. Internal client sockets do not have meaningful local
   * addresses (port numbers).
   *
   * @param bindpoint
   * @throws IOException
   */
  public void bind(SocketAddress bindpoint) throws IOException {
    ensureNotClosed();
    throw new UnsupportedOperationException();
  }

  /**
   * Returns a dummy "local" IP address of the form 127.0.0.129. It helps
   * security managers to recognize this as a "localhost" socket
   *
   * @return IP address of 127.0.0.129.
   */
  public InetAddress getInetAddress() {
    return internalInetAddr;
  }

  /**
   * Returns a dummy "local" IP address of the form 127.0.0.129. It helps
   * security managers to recognize this as a "localhost" socket
   *
   * @return IP address of 127.0.0.129.
   */
  public InetAddress getLocalAddress() {
    return internalInetAddr;
  }

  public int getPort() {
    if (!isConnected()) return 0;
    return remotePort;
  }

  /**
   * Not meaningful. internal client sockets do not have meaningful local
   * addresses (port numbers).
   *
   * @return meaningless number
   */
  public int getLocalPort() {
    if (!isBound()) return -1;
    return localPort;
  }

  /**
   * Returns server endpoint information as {@link InternalSocketAddress}.
   *
   * @return server endpoint information
   */
  public SocketAddress getRemoteSocketAddress() {
    if (!isConnected()) return null;
    return new InternalSocketAddress(getPort());
  }

  /**
   * Returns an {@link InternalSocketAddress} holding local port number, as
   * obtained via {@link #getLocalPort}.
   *
   * @return client endpoint information
   */
  public SocketAddress getLocalSocketAddress() {
    if (!isBound()) return null;
    return new InternalSocketAddress(getLocalPort());
  }

  public synchronized InputStream getInputStream() throws IOException {
    ensureNotClosed();
    if (!isConnected()) throw new SocketException("Socket is not connected");
    if (isInputShutdown()) throw new SocketException("Socket input is shutdown");
    return in;
  }

  public synchronized OutputStream getOutputStream() throws IOException {
    ensureNotClosed();
    if (!isConnected()) throw new SocketException("Socket is not connected");
    if (isOutputShutdown()) throw new SocketException("Socket output is shutdown");
    return out;
  }

  /**
   * Does nothing.
   */
  public void setTcpNoDelay(boolean on) throws SocketException {
    ensureNotClosed();
    // no op
  }

  /**
   * Returns true.
   */
  public boolean getTcpNoDelay() throws SocketException {
    ensureNotClosed();
    return true;
  }

  /**
   * Does nothing.
   */
  public void setSoLinger(boolean on, int linger) throws SocketException {
    ensureNotClosed();
    // no op
  }

  /**
   * Returns -1.
   */
  public int getSoLinger() throws SocketException {
    return -1;
  }

  /**
   * Unsupported.
   */
  public void sendUrgentData (int data) throws IOException  {
    throw new SocketException ("Urgent data not supported");
  }

  /**
   * Does nothing.
   */
  public void setOOBInline(boolean on) throws SocketException {
    ensureNotClosed();
    // no op
  }

  /**
   * Returns false.
   */
  public boolean getOOBInline() throws SocketException {
    ensureNotClosed();
    return false;
  }

  public synchronized void setSoTimeout(int timeout) throws SocketException {
    ensureNotClosed();
    if (timeout < 0) throw new IllegalArgumentException("timeout can't be negative");
    soTimeout = timeout;
  }

  public synchronized int getSoTimeout() throws SocketException {
    ensureNotClosed();
    return soTimeout;
  }

  public void setSendBufferSize(int size) throws SocketException{
    if (size <= 0) {
      throw new IllegalArgumentException("negative send size");
    }
    ensureNotClosed();
    // for now, ignore
  }

  public int getSendBufferSize() throws SocketException {
    return Integer.MAX_VALUE;
  }

  public void setReceiveBufferSize(int size) throws SocketException{
    if (size <= 0) {
      throw new IllegalArgumentException("negative send size");
    }
    ensureNotClosed();
    // for now, ignore
  }

  public int getReceiveBufferSize() throws SocketException {
    return Integer.MAX_VALUE;
  }

  /**
   * Does nothing.
   */
  public void setKeepAlive(boolean on) throws SocketException {
    ensureNotClosed();
    // no op
  }

  /**
   * Returns true.
   */
  public boolean getKeepAlive() throws SocketException {
    ensureNotClosed();
    return true;
  }

  /**
   * Does nothing.
   */
  public void setTrafficClass(int tc) throws SocketException {
    ensureNotClosed();
    // no op
  }

  /**
   * Returns 0.
   */
  public int getTrafficClass() throws SocketException {
    return 0;
  }

  /**
   * Does nothing.
   */
  public void setReuseAddress(boolean on) throws SocketException {
    ensureNotClosed();
    // no op
  }

  /**
   * Returns true.
   */
  public boolean getReuseAddress() throws SocketException {
    ensureNotClosed();
    return true;
  }

  public void close() throws IOException {
    if (isClosed()) return;
    if (isConnected()) {
      if (!isOutputShutdown()) shutdownOutput();
      if (!isInputShutdown()) shutdownInput();
    }
    closed = true;
  }

  public synchronized void shutdownInput() throws IOException {
    ensureNotClosed();
    if (!isConnected()) throw new SocketException("Socket is not connected");
    if (isInputShutdown()) throw new SocketException("Socket input is already shutdown");
    inputShut = true;
    closeInput();
  }

  private void closeInput() throws IOException {
    inputClosed = true;
    if (outputClosed) close();
  }

  public void shutdownOutput() throws IOException {
    ensureNotClosed();
    if (!isConnected()) throw new SocketException("Socket is not connected");
    if (isOutputShutdown()) throw new SocketException("Socket output is already shutdown");
    outputShut = true;
    try { out.flush(); } catch (IOException e) {}
    // close stream only so other end notices eof, but don't close socket
    try { out.closeStream(); } catch (IOException e) {}
    closeOutput();
  }

  private void closeOutput() throws IOException {
    outputClosed = true;
    if (inputClosed) close();
  }

  public String toString() {
    if (isConnected()) {
      return "[InternalSocket: port=" + getPort() + ", localport=" +
	getLocalPort() + "]";
    }
    else {
      return "[InternalSocket: unconnected]";
    }
  }

  public boolean isConnected() {
    return connected;
  }

  public boolean isBound() {
    return true;
  }

  public boolean isClosed() {
    return closed;
  }

  public boolean isInputShutdown() {
    return inputShut;
  }

  public boolean isOutputShutdown() {
    return outputShut;
  }



  private static class InternalSocketImpl extends SocketImpl {
    InternalSocketImpl() {}

    protected void create(boolean stream) throws SocketException {
      throw new UnsupportedOperationException();
    }

    /** never called; @see Socket#connect() */
    protected void connect(String host, int port) {
      throw new UnsupportedOperationException();
    }

    /** never called; @see Socket#connect() */
    protected void connect(InetAddress address, int port) {
      throw new UnsupportedOperationException();
    }

    protected void connect(SocketAddress address, int timeout) throws IOException {
      throw new UnsupportedOperationException();
    }

    protected void bind(InetAddress host, int port) {
      // this impl is never used by a ServerSocket
      throw new UnsupportedOperationException();
    }

    protected void listen(int backlog) {
      // this impl is never used by a ServerSocket
      throw new UnsupportedOperationException();
    }

    protected void accept(SocketImpl s) {
      // this impl is never used by a ServerSocket
      throw new UnsupportedOperationException();
    }

    protected InputStream getInputStream() throws IOException {
      throw new UnsupportedOperationException();
    }

    protected OutputStream getOutputStream() throws IOException {
      throw new UnsupportedOperationException();
    }

    protected int available() throws IOException {
      throw new UnsupportedOperationException();
    }

    protected void close() throws IOException {
      throw new UnsupportedOperationException();
    }

    protected void shutdownInput() throws IOException {
      throw new UnsupportedOperationException();
    }

    protected void shutdownOutput() throws IOException {
      throw new UnsupportedOperationException();
    }

    protected FileDescriptor getFileDescriptor() {
      // this wrapper is never used by a ServerSocket
      throw new UnsupportedOperationException();
    }

    protected InetAddress getInetAddress() {
      throw new UnsupportedOperationException();
    }

    protected int getPort() {
      throw new UnsupportedOperationException();
    }

    protected boolean supportsUrgentData() {
      throw new UnsupportedOperationException();
    }

    protected void sendUrgentData (int data) throws IOException {
      throw new UnsupportedOperationException();
    }

    protected int getLocalPort() {
      throw new UnsupportedOperationException();
    }

    public Object getOption(int optID) throws SocketException {
      throw new UnsupportedOperationException();
    }

    public void setOption(int optID, Object value) throws SocketException {
      throw new UnsupportedOperationException();
    }
  }

  private void ensureNotClosed() throws SocketException {
    if (isClosed()) throw new SocketException("Socket is closed");
  }

  static class Channel {
    final InputStream is;
    final OutputStream os;
    Channel(InputStream is, OutputStream os) {
      this.is = is;
      this.os = os;
    }
    public InputStream getInput() { return is; }
    public OutputStream getOutput() { return os; }
  }

  private class InternalSocketInputStream extends FilterInputStream {
    final InputStream input;

    InternalSocketInputStream(InputStream input) {
      super(input);
      this.input = input;
    }

    public int read() throws IOException {
      if (inputShut) return -1;
      int timeout = InternalSocket.this.soTimeout;
      int val;
      if (timeout == 0) {
	val = input.read();
      }
      else {
	try {
	  val = input.read();
// 	  val = input.timedRead(timeout);
	}
	catch (IOException e) {
	  throw new SocketTimeoutException(e.getMessage());
	}
      }
      if (val < 0) closeInput();
      return val;
    }

    public int read(byte[] buf) throws IOException {
      return read(buf, 0, buf.length);
    }

    public int read(byte[] buf, int off, int len) throws IOException {
      if (inputShut) return -1;
      int timeout = InternalSocket.this.soTimeout;
      int read;
      if (timeout == 0) {
	read = input.read(buf, off, len);
      }
      else {
	try {
// 	  read = input.timedRead(buf, off, len, timeout);
	  read = input.read(buf, off, len);
	}
	catch (IOException e) {
	  shutdownInput();
	  throw e;
	}
	catch (Exception e) {
	  throw new SocketTimeoutException(e.getMessage());
	}
      }
      if (read < 0) closeInput();
      return read;
    }

    public long skip(long n) throws IOException {
      if (inputShut) return -1;
      return super.skip(n);
    }

    public int available() throws IOException {
      if (inputShut) return 0;
      return super.available();
    }

    public void close() throws IOException {
      try {
	super.close();
      }
      finally {
	InternalSocket.this.close();
      }
    }
  }

  private class InternalSocketOutputStream extends FilterOutputStream {
    InternalSocketOutputStream(OutputStream output) {
      super(output);
    }
    public void write(int b) throws IOException {
      try {
	super.write(b);
      }
      catch (IOException e) {
	closeOutput();
      }
    }
    public void write(byte[] buf, int off, int len) throws IOException {
      try {
	super.write(buf, off, len);
      }
      catch (IOException e) {
	closeOutput();
      }
    }
    public void close() throws IOException {
      try {
	super.close();
      }
      finally {
	InternalSocket.this.close();
      }
    }
    public void closeStream() throws IOException {
      super.close();
    }
  }
}
