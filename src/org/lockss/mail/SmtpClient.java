/*
 * $Id: SmtpClient.java,v 1.1 2004-07-12 06:11:23 tlipkis Exp $
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


package org.lockss.mail;

import java.io.*;
import java.net.*;
import sun.net.TransferProtocolClient;
import sun.net.NetworkClient;
import org.lockss.util.*;
import org.lockss.daemon.*;

/**
 * Simple SMTP client.
 */
public class SmtpClient extends TransferProtocolClient  {
  protected static Logger log = Logger.getLogger("SmtpClient");

  /** The mail was sent */
  public static final int RESULT_OK = 0;
  /** The mail was not sent, retry may succeed */
  public static final int RESULT_RETRY = 1;
  /** The mail was not sent, retry is unlikely to succeed */
  public static final int RESULT_FAIL = 2;

  static final int RESP_POS = 1;
  static final int RESP_INTER = 2;
  static final int RESP_TRANS = 3;
  static final int RESP_PERM = 4;

  private String smtpHost;
  private int smtpPort;
  private String heloName;
  private int respType;

  SmtpClient() {
  }

  /** Create an smtp client to talk to smtpHost on the standard port */
  SmtpClient(String smtpHost) throws IOException {
    this(smtpHost, 25);
  }

  /** Create an smtp client to talk to smtpHost on port smtpPort */
  SmtpClient(String smtpHost, int smtpPort) throws IOException {
    super(smtpHost, smtpPort);
    this.smtpHost = smtpHost;
    this.smtpPort = smtpPort;
  }

  private String getHeloName() {
    if (heloName == null) {
      heloName = makeHeloName();
    }
    if (heloName == null) {
      heloName = "LOCKSS cache";
    }
    return heloName;
  }

  private String makeHeloName() {
    String host = Configuration.getPlatformHostname();
    if (host == null) {
      try {
	host = IPAddr.getLocalHost().getHostName();
      } catch (UnknownHostException ex) {
	log.error("Couldn't determine HELO name, using \"LOCKSS cache\"", ex);
	return null;
      }
    }
    return host;
  }

  /**
   * Send a single message.
   * @param sender the smtp sender
   * @param recipient the smtp recipient
   * @param body smtp message body
   * @return a RESULT_XXX code
   */
  public int sendMsg(String sender, String recipient, String body) {
    int result = -1;

    if (recipient == null) {
      throw new RuntimeException("No recipient supplied");
    }
    try {
      int resp = resp();
      if (resp != 220) return RESULT_RETRY;

      resp = sendResp("HELO " + getHeloName());
      if (resp != 250) return RESULT_RETRY;

      resp = sendResp("MAIL FROM: <" + sender + ">");
      if (resp != 250) return getErrResult();

      resp = sendResp("RCPT TO: <" + recipient + ">");
      if (!(resp == 250 || resp == 251 || resp == 252)) {
	return getErrResult();
      }
      resp = sendResp("DATA");
      if (resp != 354) return getErrResult();

      sendBody(serverOutput, body);
      resp = resp();
      if (resp != 250) return getErrResult();
      result = RESULT_OK;
    } catch (IOException e) {
      if (result < 0) {
	result = RESULT_RETRY;
      }
    } finally {
      if (serverIsOpen()) {
	send("QUIT");
	try {
	  closeServer();
	} catch (IOException e) {
	}
      }
    }
    return result;
  }

  /** Read and return a numeric response code */
  int resp() throws IOException {
    int resp = readServerResponse();
    if (log.isDebug3()) {
      log.debug3("Response: " + getResponseString());
    }
    respType = respType(resp);
    return resp;
  }

  int respType(int resp) {
    switch (resp / 100) {
    case 2: return RESP_POS;
    case 3: return RESP_INTER;
    case 4: return RESP_TRANS;
    case 5: return RESP_PERM;
    default: return RESP_PERM;
    }
  }

  int getErrResult() {
    if (log.isDebug3()) {
      log.debug3("getErrResult(" + respType + ")");
    }
    switch (respType) {
    case RESP_TRANS: return RESULT_RETRY;
    case RESP_PERM: return RESULT_FAIL;
    default:
      return RESULT_FAIL;
    }
  }

  /**
   * Send a line
   */
  private void send(String msg) {
    if (log.isDebug3()) {
      log.debug3("Send: " + msg);
    }
    serverOutput.print(msg);
    serverOutput.print("\r\n");
    serverOutput.flush();
  }

  private int sendResp(String msg) throws IOException {
    send(msg);
    return resp();
  }

  /** Send the body, ensuring proper network end-of-line, quoting any
   * leading dots, and terminating with <nl>,<nl> */
  void sendBody(PrintStream ostrm, String body)
      throws IOException {
    char prev = 0;
    for (int ix = 0; ix < body.length(); ix++) {
      char c = (char)body.charAt(ix);
      // double leading dots
      if (prev == '\n' && c == '.') {
	ostrm.write('.');
      }
      // convert newline to crlf
      if (c == '\n' && prev != '\r') {
	ostrm.write('\r');
      }
      ostrm.write(c);
      prev = c;
    }
    // ensure ending crlf
    if (prev != '\n') {
      ostrm.print("\r\n");
    }
    // final .<crlf>
    ostrm.print(".\r\n");
    log.debug3("Body sent");
  }
}
