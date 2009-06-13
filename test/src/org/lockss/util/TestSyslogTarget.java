/*
 * $Id: TestSyslogTarget.java,v 1.7.106.1 2009-06-13 08:53:34 tlipkis Exp $
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

package org.lockss.util;

import java.net.*;
import java.io.*;
import java.util.*;
import junit.framework.TestCase;
import org.lockss.test.*;
import org.lockss.daemon.*;

public class TestSyslogTarget extends LockssTestCase {
  private static final int syslogPort = 9999;
  private static final String syslogHost = "127.0.0.1";
  private static final String PARAM_HOST = SyslogTarget.PARAM_HOST;
  private static final String PARAM_PORT = SyslogTarget.PARAM_PORT;

  public TestSyslogTarget(String msg){
    super(msg);
  }

  private SyslogTarget newSyslogTarget() {
    SyslogTarget target = new SyslogTarget();
    target.init();
    return target;
  }

  public void testSeverityToFacility(){
    assertEquals(8+0,
		 SyslogTarget.loggerSeverityToSyslogSeverity(Logger.LEVEL_CRITICAL));
    assertEquals(8+2,
		 SyslogTarget.loggerSeverityToSyslogSeverity(Logger.LEVEL_ERROR));
    assertEquals(8+4,
		 SyslogTarget.loggerSeverityToSyslogSeverity(Logger.LEVEL_WARNING));
    assertEquals(8+5,
		 SyslogTarget.loggerSeverityToSyslogSeverity(Logger.LEVEL_INFO));
    assertEquals(8+6,
		 SyslogTarget.loggerSeverityToSyslogSeverity(Logger.LEVEL_DEBUG));
    assertEquals(8+7,
		 SyslogTarget.loggerSeverityToSyslogSeverity(Logger.LEVEL_DEBUG2));
    assertEquals(8+7,
		 SyslogTarget.loggerSeverityToSyslogSeverity(Logger.LEVEL_DEBUG3));
  }

  private void setConfig(String host, int port) throws Exception {
    String conf1 =
      PARAM_HOST + "=" + host + "\n" +
      PARAM_PORT + "=" + port + "\n";
    List list = ListUtil.list(FileTestUtil.urlOfString(conf1));
    ConfigurationUtil.setCurrentConfigFromUrlList(list);
  }

  public void testHandleMessageGetsMessageRight() throws Exception{
    int expectedPort = 1234;
    String expectedHost = "199.99.9.99";
    setConfig(expectedHost, expectedPort);

    String expectedDataStr =
      "<"+SyslogTarget.loggerSeverityToSyslogSeverity(Logger.LEVEL_ERROR)+">"+
      "LOCKSS: TestMessage";
    byte[] expectedData = expectedDataStr.getBytes();

    SyslogTarget target = newSyslogTarget();
    MockDatagramSocket ds = new MockDatagramSocket();
    target.handleMessage(ds, Logger.LEVEL_ERROR, "TestMessage");
    Vector packets = ds.getSentPackets();
    assertEquals(1, packets.size());
    DatagramPacket packet = (DatagramPacket)packets.elementAt(0);
    assertEquals(expectedPort, packet.getPort());
    assertEquals(expectedHost, packet.getAddress().getHostAddress());

    byte[] data = packet.getData();
    assertEquals(expectedData.length, packet.getLength());
    assertEquals(new String(expectedData), new String(data));
  }

  public void testActualDatagramComm() throws Exception {
    int port = 9091;
    setConfig("127.0.0.1", port);
    DatagramSocketListener dsl = new DatagramSocketListener(port, 1);
    try{
      dsl.beginListening();
    }
    catch (BindException e) {
      fail("Could not bind to port " + port + ": " + e);
    }
    String expectedMsg =
      "<"+SyslogTarget.loggerSeverityToSyslogSeverity(Logger.LEVEL_ERROR)+">"+
      "LOCKSS: TestMessage";

    byte[] msgBytes = expectedMsg.getBytes();

    SyslogTarget target = newSyslogTarget();
    target.handleMessage((Logger)null, Logger.LEVEL_ERROR, "TestMessage");

    DatagramPacket recPacket = dsl.getPacket();
    assertTrue(recPacket != null);

    byte[] recData = recPacket.getData();

    assertEquals(msgBytes.length, recPacket.getLength());

    for (int ix=0; ix<msgBytes.length; ix++){
      assertEquals(msgBytes[ix], recData[ix]);
    }
  }
}
