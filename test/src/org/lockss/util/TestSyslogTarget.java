/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

public class TestSyslogTarget extends TestCase{
  private static final int syslogPort = 9999;
  private static final String syslogHost = "127.0.0.1";
  //  private DatagramSocketListener dsl;  
  private static final String SYSLOG_HOST_PROPERTY = "org.lockss.log.syslogHost";
  private static final String SYSLOG_PORT_PROPERTY = "org.lockss.log.syslogPort";

  public TestSyslogTarget(String msg){
    super(msg);
  }
  
  public void setUp() throws Exception{

//     dsl = new DatagramSocketListener(syslogPort);
//     new Thread(dsl).start();
  }



  public void testSeverityToFacility(){
    assertEquals(10, 
		 SyslogTarget.loggerSeverityToSyslogSeverity(Logger.LEVEL_CRITICAL));
    assertEquals(11, 
		 SyslogTarget.loggerSeverityToSyslogSeverity(Logger.LEVEL_ERROR));
    assertEquals(12, 
		 SyslogTarget.loggerSeverityToSyslogSeverity(Logger.LEVEL_WARNING));
    assertEquals(14, 
		 SyslogTarget.loggerSeverityToSyslogSeverity(Logger.LEVEL_INFO));
    assertEquals(15, 
		 SyslogTarget.loggerSeverityToSyslogSeverity(Logger.LEVEL_DEBUG));
  }

  public void testHandleMessageGetsMessageRight() throws Exception{
    int expectedPort = 1234;
    String expectedHost = "199.99.9.99";
    String conf1 = 
      "org.lockss.log.syslog.port="+expectedPort+"\n"+
      "org.lockss.log.syslog.host="+expectedHost+"\n";
    List list = ListUtil.list(FileUtil.urlOfString(conf1));
    TestConfiguration.setCurrentConfigFromUrlList(list);

    String expectedDataStr = 
      "<"+SyslogTarget.loggerSeverityToSyslogSeverity(Logger.LEVEL_ERROR)+">"+
      "LOCKSS: TestMessage";
    byte[] expectedData = expectedDataStr.getBytes();

    SyslogTarget target = new SyslogTarget();
    MockDatagramSocket ds = new MockDatagramSocket();
    target.handleMessage(ds, Logger.LEVEL_ERROR, "TestMessage");
    Vector packets = ds.getSentPackets();
    assertEquals(1, packets.size());
    DatagramPacket packet = (DatagramPacket)packets.elementAt(0);
    //FIXME after props are done
    assertEquals(expectedPort, packet.getPort());
    assertEquals(expectedHost, packet.getAddress().getHostAddress());
    
    byte[] data = packet.getData();
    assertEquals(expectedData.length, packet.getLength());
    assertEquals(new String(expectedData), new String(data));
  }

  public void testActualDatagramComm() throws Exception {
    String conf1 = 
      "org.lockss.log.syslog.port=9091\n"+
      "org.lockss.log.syslog.host=127.0.0.1\n";
    List list = ListUtil.list(FileUtil.urlOfString(conf1));
    TestConfiguration.setCurrentConfigFromUrlList(list);
    int port = 9091;
    DatagramSocketListener dsl = new DatagramSocketListener(port, 1);
    try{
      dsl.beginListening();
    }
    catch (BindException be){
      fail("Could not bind to the port: "+port);
    }
    String expectedMsg = 
      "<"+SyslogTarget.loggerSeverityToSyslogSeverity(Logger.LEVEL_ERROR)+">"+
      "LOCKSS: TestMessage";

    byte[] msgBytes = expectedMsg.getBytes();
    
    SyslogTarget target = new SyslogTarget();
    target.handleMessage((Logger)null, Logger.LEVEL_ERROR, "TestMessage");
    
    DatagramPacket recPacket = dsl.getPacket();
    assertTrue(recPacket != null);
    
    byte[] recData = recPacket.getData();

    assertEquals(msgBytes.length, recPacket.getLength());

    for (int ix=0; ix<msgBytes.length; ix++){
      assertEquals(msgBytes[ix], recData[ix]);
    }
  }

//   public void testNoSyslogHostSpecified(){
//     SyslogTarget target = new SyslogTarget();
//     target.handleMessage("blah", "blah", "blah");
//     assertTrue(dsl.getPacket() == null);
//   }    

//   public void testHostNotPortSpecified(){
//     Properties props = System.getProperties();
//     props.setProperty(SYSLOG_HOST_PROPERTY, syslogHost);
//     SyslogTarget target = new SyslogTarget();
//     target.handleMessage("blah", "blah", "blah");
//     assertTrue(dsl.getPacket() == null);
//   }    
  
//   public void testBasicMessage(){
//     SyslogTarget target = new SyslogTarget();
//     String callerId = "testCaller";
//     String errorMessage = "testErrorMessage";
//     String expectedMessage = "LCAP: "+callerId+": "+errorMessage;
//     byte[] msgBytes = expectedMessage.getBytes();
//     DatagramPacket packet = dsl.getPacket();
//     assertTrue(packet != null);
//     int dataLength = packet.getLength();
//     byte[] data = packet.getData();
//     for (int ix=0; ix < dataLength; ix++){
//       assertEquals(msgBytes[ix], data[ix]);
//     }
//   }
}
