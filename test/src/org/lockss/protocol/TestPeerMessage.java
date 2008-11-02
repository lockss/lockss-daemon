/*
 * $Id: TestPeerMessage.java,v 1.7 2008-11-02 21:13:48 tlipkis Exp $
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

import java.util.*;
import java.io.*;
import java.net.*;
import junit.framework.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;


/**
 * Test class for org.lockss.protocol.PeerMessage
 */
public class TestPeerMessage extends LockssTestCase {
  static Logger log = Logger.getLogger("TestPeerMessage");

  String testStr;
  byte[] testData;
  PeerIdentity pid;
  PeerMessage.Factory msgFactory;

  TestPeerMessage(String name) {
    super(name);
  }

  public void setUp(PeerMessage.Factory fact) throws Exception {
    super.setUp();
    msgFactory = fact;
    testStr = "This is test data";
    testData = testStr.getBytes();
    pid = new MyPeerIdentity();
  }

  public void assertReadAccessorsThrow(PeerMessage pm) throws Exception {
    try {
      pm.getInputStream();
      fail("getInputStream() should throw when no data");
    } catch (IllegalStateException e) {
    }
    try {
      pm.getDataSize();
      fail("getDataSize() should throw when no data");
    } catch (IllegalStateException e) {
    }
  }

  PeerMessage makePeerMessage(int proto) throws IOException {
    PeerMessage pm = msgFactory.newPeerMessage();
    pm.setProtocol(proto);
    return pm;
  }

  PeerMessage makePeerMessage(int proto, String data) throws IOException {
    return makePeerMessage(proto, data.getBytes());
  }

  PeerMessage makePeerMessage(int proto, byte[] data) throws IOException {
    PeerMessage pm = makePeerMessage(proto);
    OutputStream os = pm.getOutputStream();
    os.write(data);
    os.close();
    return pm;
  }

  public void testNoData() throws Exception {
    PeerMessage pm = makePeerMessage(4);
    assertReadAccessorsThrow(pm);
    assertEquals(4, pm.getProtocol());
    OutputStream os = pm.getOutputStream();
    // should still throw until output stream closed
    assertReadAccessorsThrow(pm);
    StreamUtil.copy(new ByteArrayInputStream(testData), os);
    assertReadAccessorsThrow(pm);
    try {
      pm.getOutputStream();
      fail("getOutputStream() should throw when called twice");
    } catch (IllegalStateException e) {
    }
  }

  public void testData() throws Exception {
    PeerMessage pm = makePeerMessage(4);
    OutputStream os = pm.getOutputStream();
    StreamUtil.copy(new ByteArrayInputStream(testData), os);
    assertReadAccessorsThrow(pm);
    os.close();
    try {
      pm.getOutputStream();
      fail("getOutputStream() should throw when msg already has data");
    } catch (IllegalStateException e) {
    }
    assertEquals(4, pm.getProtocol());
    assertEquals(testData.length, pm.getDataSize());
    InputStream is = pm.getInputStream();
    assertEquals(testStr, StringUtil.fromInputStream(is));
    is.close();
  }

  public void testZeroData() throws Exception {
    PeerMessage pm = makePeerMessage(4);
    OutputStream os = pm.getOutputStream();
    assertReadAccessorsThrow(pm);
    os.close();
    try {
      pm.getOutputStream();
      fail("getOutputStream() should throw when msg already has data");
    } catch (IllegalStateException e) {
    }
    assertEquals(4, pm.getProtocol());
    assertEquals(0, pm.getDataSize());
    InputStream is = pm.getInputStream();
    assertEquals("", StringUtil.fromInputStream(is));
    is.close();
  }

  public void testSender() throws Exception {
    PeerMessage pm = makePeerMessage(4);
    assertNull(pm.getSender());
    pm.setSender(pid);
    assertEquals(pid, pm.getSender());
  }

  public void testEquals() throws Exception {
    String s1 = "01\0003456789abcdefghijklmnopq";
    PeerMessage pm1 = makePeerMessage(1);
    PeerMessage pm2 = makePeerMessage(1);
    assertEqualsNotSame(pm1, pm2);
    pm2.setSender(pid);
    assertNotEquals(pm1, pm2);
    assertNotEquals(pm1, makePeerMessage(2));
    assertNotEquals(pm1, makePeerMessage(1, s1));

    assertEqualsNotSame(makePeerMessage(1, ""),
			makePeerMessage(1, ""));
    assertEqualsNotSame(makePeerMessage(1, s1),
			makePeerMessage(1, new String(s1)));
    assertNotEquals(makePeerMessage(1, s1),
		    makePeerMessage(0, s1));
    assertNotEquals(makePeerMessage(1, s1),
		    makePeerMessage(1, s1 + "A"));
    assertNotEquals(makePeerMessage(1, s1 + "A"),
		    makePeerMessage(1, s1 + "B"));
  }

  public void testEqualsButSender() throws Exception {
    String s1 = "01\0003456789abcdefghijklmnopq";
    PeerMessage pm1 = makePeerMessage(1);
    PeerMessage pm2 = makePeerMessage(1);
    assertTrue(pm1.equalsButSender(pm2));
    pm2.setSender(pid);
    assertTrue(pm1.equalsButSender(pm2));
    assertFalse(pm1.equalsButSender(makePeerMessage(2)));
    assertFalse(pm1.equalsButSender(makePeerMessage(1, s1)));

    assertEqualsNotSame(makePeerMessage(1, ""), makePeerMessage(1, ""));
    assertEqualsNotSame(makePeerMessage(1, s1), makePeerMessage(1, s1));
    PeerMessage pm = makePeerMessage(1, s1);
    assertTrue(pm.equalsButSender(makePeerMessage(1, s1)));
    assertFalse(pm.equalsButSender(makePeerMessage(0, s1)));
    assertFalse(pm.equalsButSender(makePeerMessage(1, s1+"A")));
  }

  public void testDelete() throws Exception {
    String s1 = "01\0003456789abcdefghijklmnopq";
    PeerMessage pm1 = makePeerMessage(1, s1);
    InputStream ins = pm1.getInputStream();
    ins.close();
    ins = pm1.getInputStream();
    ins.close();
    pm1.delete();
    try {
      ins = pm1.getInputStream();
      fail("Shouldn't be able to  get input stream after message deleted");
    } catch (IllegalStateException e) {
    }
  }


  class MyPeerIdentity extends PeerIdentity {
    MyPeerIdentity()
	throws IdentityManager.MalformedIdentityKeyException {
      super("tcp:[1.2.3.4]:42");
    }
  }

  // Harness to run all tests above with two different configurations:
  // MemoryPeerMessage and DiskPeerMessage.

  public static class Memory extends TestPeerMessage {
    public Memory(String name) {
      super(name);
    }
    public void setUp() throws Exception {
      setUp(new PeerMessage.Factory() {
	  public PeerMessage newPeerMessage() {
	    return new MemoryPeerMessage();
	  }
	  public PeerMessage newPeerMessage(long size) {
	    return new MemoryPeerMessage();
	  }});
    }
  }

  public static class FileMsg extends TestPeerMessage {
    private List msgs = new ArrayList();

    public FileMsg(String name) {
      super(name);
    }
    public void setUp() throws Exception {
      final File tmpdir = getTempDir();
      setUp(new PeerMessage.Factory() {
	  public PeerMessage newPeerMessage() {
	    FilePeerMessage msg = new FilePeerMessage(tmpdir);
	    msgs.add(msg);
	    return msg;
	  }
	  public PeerMessage newPeerMessage(long size) {
	    return newPeerMessage();
	  }});
    }
    public void tearDown() throws Exception {
      for (Iterator iter = msgs.iterator(); iter.hasNext(); ) {
	((FilePeerMessage)iter.next()).delete();
      }
      super.tearDown();
    }

    public void testDeleteFile() throws Exception {
      String s1 = "01\0003456789abcdefghijklmnopq";
      FilePeerMessage pm1 = (FilePeerMessage)makePeerMessage(1, s1);
      File file = pm1.getDataFile();
      assertTrue(file.exists());
      InputStream ins = pm1.getInputStream();
      ins.close();
      ins = pm1.getInputStream();
      ins.close();
      assertTrue(file.exists());
      pm1.delete();
      assertFalse(file.exists());
      try {
	ins = pm1.getInputStream();
	fail("Shouldn't be able to  get input stream after message deleted");
      } catch (IllegalStateException e) {
      }
    }
  }


  public static Test suite() {
    return variantSuites(new Class[] {Memory.class, FileMsg.class});
  }

}
