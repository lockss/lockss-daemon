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
import org.lockss.daemon.*;

public class SyslogTarget implements LogTarget{
  
  //FIXME get from props after TAL's commit
  private static final String DEFAULT_HOST = "127.0.0.1";
  private static final String DEFAULT_PORT = "514";

  static final int FACILITY = 8; //user level facility
  static final String PREFIX = Logger.PREFIX+"syslog.";

  private int port;
  private String host;
  private String threshold;
  private Logger logger;

  public SyslogTarget(){
    getProps();
  }

  public void handleMessage (Logger logger, int msgLevel, String message) {
    this.logger = logger;
    try{
      DatagramSocket socket = 
	new DatagramSocket();
      handleMessage(socket, msgLevel, message);
    }
    catch(IOException ioe){
      ioe.printStackTrace();
      //XXX how should we handle errors here?
    }
  }

  protected void handleMessage(DatagramSocket socket, int msgLevel, String message) {
    int syslogSev = loggerSeverityToSyslogSeverity(msgLevel);
    String msg = "<"+syslogSev+">"+"LOCKSS: "+message;
    
    try {
      DatagramPacket packet =
	new DatagramPacket(msg.getBytes(),
			   msg.length(),
			   InetAddress.getByName(host),
			   port);
      if (socket != null) {
	socket.send(packet);
      } 
    }catch (IOException ioe) {
      ioe.printStackTrace();
      // No action intended
    }
  }
 

  protected static int loggerSeverityToSyslogSeverity(int severity){
    switch (severity){
    case Logger.LEVEL_CRITICAL:
      return FACILITY + 2;
    case Logger.LEVEL_ERROR:
      return FACILITY + 3;
    case Logger.LEVEL_WARNING:
      return FACILITY + 4;
    case Logger.LEVEL_INFO:
      return FACILITY + 6;
    case Logger.LEVEL_DEBUG:
      return FACILITY + 7;
    default:
      return -1;
    }
  }


  private void getProps(){
    String portStr = Configuration.getParam(PREFIX+"port", DEFAULT_PORT);
    port = Integer.parseInt(portStr);
    host = Configuration.getParam(PREFIX+"host", DEFAULT_HOST);
  }
   
  public static void main(String[] args){
    SyslogTarget st = new SyslogTarget();
    st.handleMessage((Logger)null, Logger.LEVEL_CRITICAL, "Message");
  }
  
}
