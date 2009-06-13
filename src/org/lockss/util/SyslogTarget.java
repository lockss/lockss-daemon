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
import java.io.IOException;
import java.net.*;

import org.lockss.config.*;

public class SyslogTarget implements LogTarget{

  static final String PREFIX = Logger.PREFIX+"syslog.";
  static final String PARAM_PORT = PREFIX + "port";
  static final String PARAM_HOST = PREFIX + "host";

  private static final String DEFAULT_HOST = "127.0.0.1";
  private static final int DEFAULT_PORT = 514;

  static final int FACILITY = 8; //user level facility

  static final int SYSLOG_EMERG = 0;	// system is unusable
  static final int SYSLOG_ALERT = 1;	// action must be taken immediately
  static final int SYSLOG_CRIT = 2;	// critical conditions
  static final int SYSLOG_ERR = 3;	// error conditions
  static final int SYSLOG_WARNING = 4;	// warning conditions
  static final int SYSLOG_NOTICE = 5;   // normal but signification condition
  static final int SYSLOG_INFO = 6;	// informational
  static final int SYSLOG_DEBUG = 7;	// debug-level messages

  private int port;
  private String hostname = null;
  private InetAddress host = null;
  private DatagramSocket socket = null;

  public SyslogTarget(){
  }

  /** Initialize this log target */
  public void init() {
    ConfigManager.getConfigManager().registerConfigurationCallback(new Configuration.Callback() {
	public void configurationChanged(Configuration newConfig,
					 Configuration oldConfig,
					 Configuration.Differences changedKeys) {
	  setConfig(changedKeys);
	}});
    try {
      socket = new DatagramSocket();
    } catch(IOException ioe) {
      ioe.printStackTrace();
      //XXX how should we handle errors here?
    }
  }

  private void setConfig(Configuration.Differences changedKeys) {
    port = CurrentConfig.getIntParam(PARAM_PORT, DEFAULT_PORT);
    if (changedKeys.contains(PARAM_HOST)) {
      hostname = CurrentConfig.getParam(PARAM_HOST, DEFAULT_HOST);
      host = null;			// force new name lookup
    }
  }

  private InetAddress getHost() {
    // don't depend on name resolving the first time we try
    try {
      if (hostname != null && host == null) {
	host = InetAddress.getByName(hostname);
      }
    } catch (UnknownHostException e) {
      // no action
    }
    return host;
  }

  public void handleMessage(Logger logger, int level, String message) {
    if (socket != null) {
      handleMessage(socket, level, message);
    }
  }

  public void handleMessage(DatagramSocket socket, int level, String message) {
    int syslogSev = loggerSeverityToSyslogSeverity(level);
    String msg = "<"+syslogSev+">"+"LOCKSS: "+message;
    InetAddress hst = getHost();
    if (hst != null) {
      try {
	DatagramPacket packet =
	  new DatagramPacket(msg.getBytes(),
			     msg.length(),
			     hst,
			     port);
	socket.send(packet);
      } catch (IOException ioe) {
	ioe.printStackTrace();
	// No action intended
      }
    }
  }

  protected static int loggerSeverityToSyslogSeverity(int severity){
    switch (severity){
    case Logger.LEVEL_CRITICAL:
      return FACILITY + SYSLOG_EMERG;
    default:
    case Logger.LEVEL_ERROR:
      return FACILITY + SYSLOG_CRIT;
    case Logger.LEVEL_WARNING:
      return FACILITY + SYSLOG_WARNING;
    case Logger.LEVEL_INFO:
      return FACILITY + SYSLOG_NOTICE;
    case Logger.LEVEL_DEBUG:
      return FACILITY + SYSLOG_INFO;
    case Logger.LEVEL_DEBUG2:
    case Logger.LEVEL_DEBUG3:
      return FACILITY + SYSLOG_DEBUG;
    }
  }
}
