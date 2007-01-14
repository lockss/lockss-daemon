/*
 * $Id: SmtpClient.java,v 1.9 2007-01-14 08:15:00 tlipkis Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.IOException;
import java.net.UnknownHostException;

import org.lockss.config.*;
import org.lockss.util.*;

import sun.net.TransferProtocolClient;

/**
 * Simple SMTP client.
 */
public class SmtpClient extends TransferProtocolClient  {
  protected static Logger log = Logger.getLogger("SmtpClient");

  static final String TIMEOUT_PREFIX =
    Configuration.PREFIX + "mail.smtp.timeout.";
  static final String PARAM_SMTP_DATA_TIMEOUT = TIMEOUT_PREFIX + "data";
  static long DEFAULT_SMTP_DATA_TIMEOUT = Constants.HOUR;

  // Result codes returned by sendMsg()
  /** The mail was sent */
  public static final int RESULT_OK = 0;
  /** The mail was not sent, retry may succeed */
  public static final int RESULT_RETRY = 1;
  /** The mail was not sent, retry is unlikely to succeed */
  public static final int RESULT_FAIL = 2;

  // SMTP reply codes
  static final int RESP_SYSTEM_STATUS = 211;
  static final int RESP_HELP_MESSAGE = 214;
  static final int RESP_SERVICE_READY = 220;
  static final int RESP_SERVICE_CLOSING_TRANSMISSION_CHANNEL = 221;
  static final int RESP_ACTION_OK = 250;
  static final int RESP_USER_NOT_LOCAL_WILL_FORWARD = 251;
  static final int RESP_CANNOT_VRFY_WILL_ATTEMPT_DELIVERY = 252;
  static final int RESP_START_MAIL_INPUT = 354;
  static final int RESP_SERVICE_NOT_AVAILABLE = 421;
  static final int RESP_ACTION_NOT_TAKEN = 450;
  static final int RESP_ACTION_ABORTED = 451;
  static final int RESP_INSUFFICIENT_STORAGE = 452;
  static final int RESP_UNRECOGNIZED_COMMAND = 500;
  static final int RESP_SYNTAX_ERROR_IN_ARGUMENTS = 501;
  static final int RESP_COMMAND_NOT_IMPLEMENTED = 502;
  static final int RESP_BAD_COMMAND_SEQUENCE = 503;
  static final int RESP_COMMAND_NOT_IMPLEMENTED_FOR_PARAMETER = 504;
  static final int RESP_MAILBOX_UNAVAILABLE = 550;
  static final int RESP_USER_NOT_LOCAL = 551;
  static final int RESP_STORAGE_ALLOCATION_EXCEEDED = 552;
  static final int RESP_MAILBOX_NAME_NOT_ALLOWED = 553;
  static final int RESP_TRANSACTION_FAILED = 554;

  private String smtpHost;
  private int smtpPort;
  private String heloName;

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
    serverSocket.setSoTimeout(getSoTimeout());
  }

  public int getSoTimeout() {
    long time = CurrentConfig.getTimeIntervalParam(PARAM_SMTP_DATA_TIMEOUT,
                                                   DEFAULT_SMTP_DATA_TIMEOUT);
    return time >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)time;
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
    String host = PlatformUtil.getLocalHostname();
    if (host == null) {
      log.warning("Couldn't determine HELO name, using \"LOCKSS cache\"");
      host = "LOCKSS cache";
    }
    return host;
  }

  /**
   * Send a single message.
   * @param sender the smtp sender
   * @param recipient the smtp recipient
   * @param msg MailMessage with headers and body filled in
   * @return a RESULT_XXX code
   */
  public int sendMsg(String sender, String recipient, MailMessage msg) {
    int result = -1;

    if (recipient == null) {
      throw new RuntimeException("No recipient supplied");
    }
    try {
      int resp = resp();
      if (resp != RESP_SERVICE_READY) return RESULT_RETRY;

      resp = sendResp("HELO " + getHeloName());
      if (resp != RESP_ACTION_OK) return RESULT_RETRY;

      resp = sendResp("MAIL FROM: <" + sender + ">");
      if (resp != RESP_ACTION_OK) return getErrResult(resp);

      resp = sendResp("RCPT TO: <" + recipient + ">");
      if (!(resp == RESP_ACTION_OK ||
	    resp == RESP_USER_NOT_LOCAL_WILL_FORWARD ||
	    resp == RESP_CANNOT_VRFY_WILL_ATTEMPT_DELIVERY)) {
	return getErrResult(resp);
      }
      resp = sendResp("DATA");
      if (resp != RESP_START_MAIL_INPUT) return getErrResult(resp);

      sendData(msg);
      resp = resp();
      if (resp != RESP_ACTION_OK) return getErrResult(resp);
      result = RESULT_OK;
    } catch (IOException e) {
      log.warning("Sending SMTP message", e);
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
    return resp;
  }

  int getErrResult(int resp) {
    if (log.isDebug3()) {
      log.debug3("getErrResult(" + resp + ")");
    }
    switch (resp / 100) {
    case 4: return RESULT_RETRY;
    case 5: return RESULT_FAIL;
    default:
      log.warning("Unexpected SMTP error reply: " + resp);
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

  private void sendData(MailMessage msg) throws IOException {
    SmtpOutputStream sout = new SmtpOutputStream(serverOutput);
    msg.writeData(sout);
    sout.flushSmtpData();
    serverOutput.flush();
  }

}
