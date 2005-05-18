/*
 * $Id: TestInternalSocket.java,v 1.1 2005-05-18 05:35:52 tlipkis Exp $
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
import java.io.*;
import java.net.*;
import junit.framework.TestCase;
import org.lockss.util.*;
import org.lockss.util.Queue;
import org.lockss.test.*;


/**
 * This is the test class for org.lockss.test.InternalSocket
 */

public class TestInternalSocket extends LockssTestCase {
  static Logger log = Logger.getLogger("TestServerSocket");

  String testStr = "This is test data";
  byte[] testData = testStr.getBytes();
  int testPort = 1234;
  IPAddr testAddr;
  DatagramPacket testPacket;

  public void setUp() throws Exception {
    super.setUp();
  }

  public void tearDown() throws Exception {
    InternalServerSocket.resetAllBindings();
    super.tearDown();
  }


  public void testUnbound() throws Exception {
    ServerSocket serv = new InternalServerSocket();
    assertFalse(serv.isBound());
    assertFalse(serv.isClosed());
  }

  public void testBind() throws Exception {
    ServerSocket serv = new InternalServerSocket(testPort);
    assertTrue(serv.isBound());
    assertFalse(serv.isClosed());
    try {
      new InternalServerSocket(testPort);
      fail("Second listen on port should fail");
    } catch (IOException e) {
    }
    // a different port should work
    new InternalServerSocket(testPort + 1).close();
    serv.close();
    assertTrue(serv.isClosed());
    // should now be able to create another one
    new InternalServerSocket(testPort);
  }

  public void testConnect() throws Exception {
    ServerSocket serv = new InternalServerSocket(testPort);
    assertTrue(serv.isBound());
    SocketAcceptor acpt = new SocketAcceptor(serv);
    acpt.start();
    assertNull(acpt.getSocket());
    Socket client = new InternalSocket(testPort);
    assertTrue(client.isConnected());
    assertFalse(client.isClosed());
    OutputStream clientOs = client.getOutputStream();
    InputStream clientIs = client.getInputStream();
    acpt.join(TIMEOUT_SHOULDNT);
    assertFalse("reader thread didn't terminate", acpt.isAlive());

    Socket servSock = acpt.getSocket();
    assertTrue(servSock.isConnected());
    assertFalse(servSock.isClosed());
    servSock.getInputStream();
    servSock.getOutputStream();
    client.shutdownOutput();
    assertFalse(client.isClosed());
    client.shutdownInput();
    assertTrue(client.isClosed());
  }

  public void testIllConstructor() throws Exception {
    try {
      new InternalSocket(InetAddress.getByName("127.1.2.3"), testPort);
      fail("non-internal InetAddress should throw");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testConnect2() throws Exception {
    ServerSocket serv = new InternalServerSocket(testPort);
    assertTrue(serv.isBound());
    SocketAcceptor acpt = new SocketAcceptor(serv);
    acpt.start();
    assertNull(acpt.getSocket());
    Socket client =
      new InternalSocket(InternalSocket.internalInetAddr, testPort);
    assertTrue(client.isConnected());
    assertFalse(client.isClosed());
    OutputStream clientOs = client.getOutputStream();
    InputStream clientIs = client.getInputStream();
    acpt.join(TIMEOUT_SHOULDNT);
    assertFalse("reader thread didn't terminate", acpt.isAlive());

    Socket servSock = acpt.getSocket();
    assertTrue(servSock.isConnected());
    assertFalse(servSock.isClosed());
    servSock.getInputStream();
    servSock.getOutputStream();
    client.shutdownOutput();
    assertFalse(client.isClosed());
    client.shutdownInput();
    assertTrue(client.isClosed());
  }

  public void testReadWrite() throws Exception {
    SocketReader srvr = new SocketReader(testPort);
    srvr.setEcho(true);
    srvr.start();
    Socket clientSock = new InternalSocket(testPort);
    log.info("client connected");
    assertTrue(clientSock.isConnected());
    assertFalse(clientSock.isClosed());
    SocketReader rdr = new SocketReader(clientSock);
    PrintWriter pw = new PrintWriter(clientSock.getOutputStream());
    rdr.start();
    log.info("print foobar");
    pw.print("foobar");
    pw.flush();
    clientSock.shutdownOutput();
//     pw.close();
    srvr.join(TIMEOUT_SHOULDNT);
    assertFalse("reader thread didn't terminate", srvr.isAlive());
    rdr.join(TIMEOUT_SHOULDNT);
    assertFalse("reader thread didn't terminate", rdr.isAlive());
    assertEquals(null, srvr.getException());
    assertEquals(null, rdr.getException());
    assertEquals("foobar", rdr.getString());
  }

  abstract class ServerThread extends Thread {
    ServerSocket listenSock;
    Socket sock;
    Exception e;

    Socket getSocket() {
      return sock;
    }
    Exception getException() {
      return e;
    }
  }
  class SocketAcceptor extends ServerThread {
    SocketAcceptor(ServerSocket listenSock) {
      this.listenSock = listenSock;
    }
    public void run() {
      try {
	sock = listenSock.accept();
      } catch (IOException e) {
	log.error("accept: " + listenSock, e);
	this.e = e;
      }
    }
  }
  class SocketReader extends ServerThread {
    ByteArrayOutputStream rcvd = new ByteArrayOutputStream();
    InputStream is;
    OutputStream os;
    String str;
    boolean echo = false;

    SocketReader(Socket sock) throws IOException {
      this.sock = sock;
      is = sock.getInputStream();
    }
    SocketReader(ServerSocket listenSock) throws IOException {
      this.listenSock = listenSock;
    }
    SocketReader(int listenPort) throws IOException {
      this.listenSock = new InternalServerSocket(listenPort);
    }
    public void run() {
      if (listenSock != null) {
	try {
	  sock = listenSock.accept();
	  is = sock.getInputStream();
	  log.info("server accepted");
	} catch (IOException e) {
	  log.error("accept: " + listenSock, e);
	  this.e = e;
	  return;
	}
      }
      try {
	OutputStream os;
	if (echo) {
	  os = sock.getOutputStream();
	} else {
	  os = rcvd;
	}
	StreamUtil.copy(is, os);
	os.flush();
	os.close();
	str = rcvd.toString();
      } catch (IOException e) {
	log.error("read/write: " + sock, e);
	this.e = e;
      }
    }
    void setEcho(boolean echo) {
      this.echo = echo;
    }
    String getString() {
      return str;
    }
  }

}
