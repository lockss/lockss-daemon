/*
 * $Id: AlertActionSyslog.java,v 1.1 2009-11-04 03:13:19 dshr Exp $
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

package org.lockss.alert;

import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.config.*;
import java.io.IOException;
import java.net.*;

/** An AlertAction that records the Alert by sending it to syslog */
public class AlertActionSyslog extends AbstractAlertAction {
  private static Logger log = Logger.getLogger("AlertActionSyslog");
  /** If specified, the sender address on alert emails.  If not specified,
   * uses the admin email adress */
  static final String PARAM_HOST = PREFIX + "syslog.host";
  static final String PARAM_PORT = PREFIX + "syslog.port";

  public static final String PARAM_ENABLED = PREFIX + "syslog.enabled";
  static final boolean DEFAULT_ENABLED = false;

  private static final String DEFAULT_HOST = "127.0.0.1";
  private static final int DEFAULT_PORT = 514;

  static final int SYSLOG_EMERG = 0;	// system is unusable
  static final int SYSLOG_ALERT = 1;	// action must be taken immediately
  static final int SYSLOG_CRIT = 2;	// critical conditions
  static final int SYSLOG_ERR = 3;	// error conditions
  static final int SYSLOG_WARNING = 4;	// warning conditions
  static final int SYSLOG_NOTICE = 5;   // normal but significant condition
  static final int SYSLOG_INFO = 6;	// informational
  static final int SYSLOG_DEBUG = 7;	// debug-level messages

  public static final String PARAM_LEVEL = PREFIX + "syslog.level";
  static final int DEFAULT_LEVEL = SYSLOG_NOTICE;
  int fixedLevel = -1;
  public static final String PARAM_FACILITY = PREFIX + "syslog.facility";
  static final int DEFAULT_FACILITY = 8;

  // XXX need test/src/org/lockss/alert/TestAlertActionSyslog.java

  public AlertActionSyslog() {
  }

  public AlertActionSyslog(int level) {
    fixedLevel = level;
  }

  private String makeMessage(Alert alert) {
    String ret = null;
    if (alert != null) {
      ret = alert.getString(Alert.ATTR_TEXT);
    } else {
      ret = "null alert";
    }
    return ret;
  }

  public void record(LockssDaemon daemon, Alert alert) {
    InetAddress host = null;
    DatagramSocket socket = null;
    int port;
    String hostname;
    int level;
    int facility;
    Configuration config = CurrentConfig.getCurrentConfig();
    log.debug("record: " + makeMessage(alert));
    if (!config.getBoolean(PARAM_ENABLED, DEFAULT_ENABLED)) {
      return;
    }
    hostname = config.get(PARAM_HOST, DEFAULT_HOST);
    port = config.getInt(PARAM_PORT, DEFAULT_PORT);
    level = (fixedLevel < 0 ? config.getInt(PARAM_LEVEL, DEFAULT_LEVEL) :
	     fixedLevel);
    facility = config.getInt(PARAM_FACILITY, DEFAULT_FACILITY);
    try {
      socket = new DatagramSocket();
    } catch(IOException ioe) {
      log.error("Can't get socket: " + ioe);
      return;
    }
    // don't depend on name resolving the first time we try
    for (int i = 0; i < 3; i++) {
      try {
	if (hostname != null && host == null) {
	  host = InetAddress.getByName(hostname);
	}
      } catch (UnknownHostException e) {
	// no action
      }
    }
    if (host == null) {
      log.error("Can't resolve: " + hostname);
      return;
    }
    int indicator = (facility + level);
    String msg = "<"+ indicator +">" + "LOCKSS: " + makeMessage(alert);
    if (host != null) {
      try {
	DatagramPacket packet =
	  new DatagramPacket(msg.getBytes(),
			     msg.length(),
			     host,
			     port);
	socket.send(packet);
      } catch (IOException ioe) {
	log.error("Can't send to " + hostname + ":" + port + " " + ioe);
	ioe.printStackTrace();
      }
    }
  }

  public boolean isGroupable() {
    return false;
  }

  public String toString() {
    Configuration config = CurrentConfig.getCurrentConfig();
    int level = config.getInt(PARAM_LEVEL, DEFAULT_LEVEL);
    int facility = config.getInt(PARAM_FACILITY, DEFAULT_FACILITY);
    return "[AlertActions.Syslog: " + facility + ":" + level + "]";
  }
}
