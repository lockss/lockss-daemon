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
 * Adapted from InProcServerSocket, written by Dawid Kurzyniec and released
 * under Creative Commans (http://creativecommons.org/licenses/publicdomain)
 */

package org.lockss.test;

import java.io.*;
import java.net.*;
import java.util.*;

import org.lockss.util.*;
import org.lockss.util.Queue;
import org.lockss.util.FifoQueue;

/**
 * Server socket that accepts and maintains "connections" internal to a jvm
 */
public class InternalServerSocket extends ServerSocket {
  static Logger log = Logger.getLogger("InternalServerSocket");

  static final InetAddress internalInetAddr;
  static {
    try {
      internalInetAddr = InetAddress.getByName("127.0.0.129");
    } catch (UnknownHostException e) {
      // can't happen
      throw new RuntimeException("FATAL: " + e.toString());
    }
  }

  final static Map bindings = new HashMap();
  final static Random random = new Random();

  boolean bound;
  volatile boolean closed;
  int localPort;
  int soTimeout = 0;

  Queue connectionQueue;
  int backlog;

  public InternalServerSocket() throws IOException {
    super();
  }

  public InternalServerSocket(int port) throws IOException {
    this(port, 50);
  }

  public InternalServerSocket(int port, int backlog) throws IOException {
    super();
    bind(new InternalSocketAddress(port), backlog);
  }

  public InternalServerSocket(InternalSocketAddress addr) throws IOException {
    this(addr, 50);
  }

  public InternalServerSocket(InternalSocketAddress addr, int backlog) throws IOException {
    super();
    bind(addr, backlog);
  }

  public static int findUnboundPort(int above) {
    synchronized (bindings) {
      for (int port = above; port <= 65535; port++) {
	if (!bindings.containsKey(new Integer(port))) {
	  return port;
	}
      }
      return -1;
    }
  }

  public synchronized void bind(SocketAddress endpoint, int backlog)
      throws IOException {
    ensureNotClosed();
    if (isBound()) throw new SocketException("Already bound");
    if (endpoint == null) endpoint = new InternalSocketAddress(0);
    if (backlog <= 0)
      backlog = 50;
    if (!(endpoint instanceof InternalSocketAddress))
      throw new IllegalArgumentException("Unsupported address type");
    int port = ((InternalSocketAddress)endpoint).port;
    try {
      synchronized (bindings) {
	if (port != 0) {
	  if (bindings.containsKey(new Integer(port))) {
	    throw new BindException("Internal port " + port +
				    " already in use");
	  }
	} else {
	  boolean ok = false;
	  for (int i=0; i<100; i++) {
	    port = random.nextInt() & 0x7FFFFFFF;
	    // try to fit into the 2-byte range
	    if (i<4) port = port & 0x3FFF;
	    else if (i<8) port = port & 0xFFFF;
	    if (port > 1024 && !bindings.containsKey(new Integer(port))) {
	      ok = true;
	      break;
	    }
	  }
	  if (!ok) {
	    throw new BindException("Internal: Could not find available " +
				    "port to listen on");
	  }
	}

	// at this point, we have established an available port number
	bindings.put(new Integer(port), this);
      }
      log.debug("Binding port " + port);
      localPort = port;
      bound = true;
      connectionQueue = new FifoQueue();
      this.backlog = backlog;
    } catch (SecurityException e) {
      bound = false;
      throw e;
    } catch (IOException e) {
      bound = false;
      throw e;
    }
  }

  public boolean isClosed() {
    return closed;
  }

  public boolean isBound() {
    return bound;
  }

  public InetAddress getInetAddress() {
    // not an Inet socket, but for security managers it is better to report
    // that it is a "local" socket
    return InternalSocket.internalInetAddr;
  }

  public int getLocalPort() {
    if (!isBound()) return -1;
    return localPort;
  }

  public SocketAddress getLocalSocketAddress() {
    if (!isBound()) return null;
    return new InternalSocketAddress(getLocalPort());
  }

  public Socket accept() throws IOException {
    ensureNotClosed();
    if (!isBound()) throw new SocketException("Socket is not bound yet");
    int soTimeout = this.soTimeout;
    ConnReq req;
    InternalSocket.Channel ch;
    do {
      try {
	if (soTimeout > 0) {
	  req = (ConnReq)connectionQueue.get(Deadline.in(soTimeout));
	}
	else {
	  req = (ConnReq)connectionQueue.get(Deadline.MAX);
	}
      }
      catch (InterruptedException e) {
	throw new InterruptedIOException(e.toString());
      }
      if (req == null) {
	throw new SocketTimeoutException("Timeout on InternalServerSocket.accept");
      }
      if (req == TERMINATOR) {
	throw new SocketException("Socket closed");
      }
      ch = req.accept();
    }
    while (ch == null);
    return new InternalSocket(ch, localPort);
  }


  private static final ConnReq TERMINATOR = new ConnReq();

  public synchronized void close() throws IOException {
    closed = true;
    while (true) {
      ConnReq connReq = (ConnReq)connectionQueue.peek();
      if (connReq == null) break;
      connReq.refuse();
    }
    // abort possibly blocked accept
    connectionQueue.put(TERMINATOR);
    if (isBound()) {
      synchronized (bindings) {
	bindings.remove(new Integer(localPort));
      }
    }
  }

  public void setSoTimeout(int timeout) throws SocketException {
    if (timeout < 0) throw new IllegalArgumentException("Timeout must be non-negative");
    ensureNotClosed();
    soTimeout = timeout;
  }

  public int getSoTimeout() throws IOException {
    ensureNotClosed();
    return soTimeout;
  }

  public void setReuseAddress(boolean on) throws SocketException {
    ensureNotClosed();
    // no op
  }

  public boolean getReuseAddress() throws SocketException {
    ensureNotClosed();
    return true;
  }

  public String toString() {
    if (!isBound()) return "[InternalServerSocket: unbound]";
    return "[InternalServerSocket: port=" + localPort + "]";
  }

  private void ensureNotClosed() throws SocketException {
    if (isClosed()) throw new SocketException("Socket is closed");
  }

  static InternalSocket.Channel connect(int port, int timeout)
      throws IOException {
    InternalServerSocket srvsock;
    synchronized (bindings) {
      srvsock = (InternalServerSocket)bindings.get(new Integer(port));
    }
    if (srvsock == null) {
      throw new IOException("Connection refused (server not listening)");
    }
    boolean success;
    ConnReq connreq = new ConnReq();
    synchronized (srvsock) {
      if (srvsock.isClosed())
	throw new IOException("Connection refused (server closed)");
      if (srvsock.connectionQueue.size() >= srvsock.backlog) {
	throw new IOException("Connection refused (queue full, try later)");
      }
      srvsock.connectionQueue.put(connreq);
    }
    return connreq.awaitOrCancel(timeout);
  }

  /** Reset all bindings, useful in junit tearDown() */
  public static void resetAllBindings() {
    bindings.clear();
  }



  private static class ConnReq {
    boolean accepted  = false;
    boolean cancelled = false;
    boolean refused = false;
    InternalSocket.Channel srvChannel;
    InternalSocket.Channel cliChannel;

    ConnReq() {}

    public synchronized boolean cancel() {
      if (accepted || refused) return false;
      cancelled = true;
      notifyAll();
      return true;
    }

    public synchronized InternalSocket.Channel accept() throws IOException {
      if (cancelled) return null;
      if (accepted || refused) throw new IllegalStateException("Already responded");
      accepted = true;
      PipedInputStream serverIn = new PipedInputStream();
      PipedInputStream clientIn = new PipedInputStream();
      PipedOutputStream serverOut = new PipedOutputStream(clientIn);
      PipedOutputStream clientOut = new PipedOutputStream(serverIn);

      srvChannel = new InternalSocket.Channel(serverIn, serverOut);
      cliChannel = new InternalSocket.Channel(clientIn, clientOut);
      notifyAll();
      return srvChannel;
    }

    public synchronized void refuse() {
      if (cancelled) return;
      if (accepted || refused) throw new IllegalStateException("Already responded");
      refused = true;
      notifyAll();
    }

    public synchronized InternalSocket.Channel awaitOrCancel(int timeout)
	throws IOException {
      try {
	if (timeout == 0) {
	  while (!accepted && !cancelled && !refused) {
	    wait();
	  }
	} else {
	  long endtime = timeout + System.currentTimeMillis();
	  while (!accepted && !cancelled && !refused) {
	    long todo = endtime - System.currentTimeMillis();
	    if (todo > 0) {
	      wait(todo);
	    } else {
	      cancel();
	      break;
	    }
	  }
	}

	if (cancelled) throw new SocketTimeoutException("Connection timed out");
	if (refused) throw new IOException("Connection refused (server closing)");
	// otherwise must be accepted
	return cliChannel;
      }
      catch (InterruptedException e) {
	if (accepted) return cliChannel;
	cancel();
	throw new InterruptedIOException("Connect interrupted");
      }
    }
  }
}
