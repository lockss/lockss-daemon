/*
 * $Id: MailTarget.java,v 1.1 2003-01-07 02:06:31 aalto Exp $
 */

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

import java.io.*;
import java.net.*;
import org.lockss.protocol.IdentityManager;
import org.lockss.daemon.Configuration;

/**
 * Simple SMTP client.
 * Reads smtp server name from ssmtp config in /etc/ssmtp/ssmtp.conf
 */
public class MailTarget {
  /**
   * Configuration parameter for the smtp host.
   */
  public static final String PARAM_SMTPHOST = Configuration.PREFIX + "smtphost";
  /**
   * Configuration parameter for the smtp port.  Defaults to 25 if not provided.
   */
  public static final String PARAM_SMTPPORT = Configuration.PREFIX + "smtpport";

  /**
   * Configuration parameter for the log email 'to' address.
   */
  public static final String PARAM_LOG_EMAIL_TARGET = Configuration.PREFIX +
      "log.email.target";

  /**
   * Configuration parameter for the log email 'from' address.
   */
  public static final String PARAM_LOG_EMAIL_SOURCE = Configuration.PREFIX +
      "log.email.source";

  /**
   * Configuration parameter to enable email logging.
   */
  public static final String PARAM_LOG_EMAIL_ACTIVE = Configuration.PREFIX +
      "log.email.active";

  static final int DEFAULT_SMTPPORT = 25;
  static final boolean DEFAULT_ENABLING = true;
  static final String ERROR_SUBJECT = "Error on machine ";

  static String smtpHost = null;
  static int smtpPort;
  static String localHostName;
  static String toAddr;
  static String fromAddr;
  static boolean emailEnabled;
  protected static Logger logger = Logger.getLogger("MailTarget");

  public void init() {
    loadConfiguration();
    localHostName = IdentityManager.getIdentityManager().getLocalHostName();

    if (localHostName == null) {
      logger.error("Couldn't find localhost from IdentityManager; "+
                   "attempting to look up from InetAddress");
      try {
        localHostName = InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException ex) {
        logger.error("Couldn't determine localhost.", ex);
        throw new IllegalStateException("Couldn't determine localhost");
      }
    }
  }

  public void handleMessage(Logger logger, int level, String message) {
    if (!emailEnabled) {
      this.logger.warning("Email logging not enabled; message discarded: "+message);
      return;
    }

    String subject = ERROR_SUBJECT + localHostName;
    try {
      SmtpMailer.sendMail(toAddr, fromAddr, subject, message, smtpHost,
                          smtpPort, localHostName);
    } catch (IOException ioe) {
      this.logger.error("Couldn't send mail correctly.", ioe);
    }
  }

  /**
   * Load the configuration parameters.
   * If set in props use that, else load from ssmtp config file.
   */
  private static void loadConfiguration() {
    smtpHost = Configuration.getParam(PARAM_SMTPHOST);
    smtpPort = Configuration.getIntParam(PARAM_SMTPPORT, DEFAULT_SMTPPORT);
    toAddr = Configuration.getParam(PARAM_LOG_EMAIL_TARGET);
    fromAddr = Configuration.getParam(PARAM_LOG_EMAIL_SOURCE);
    emailEnabled = Configuration.getBooleanParam(PARAM_LOG_EMAIL_ACTIVE,
        DEFAULT_ENABLING);
    if ((smtpHost==null) || (toAddr==null) || (fromAddr==null)) {
      String parameter = PARAM_SMTPHOST;
      if (toAddr==null) {
        parameter = PARAM_LOG_EMAIL_TARGET;
      } else if (fromAddr==null) {
        parameter = PARAM_LOG_EMAIL_SOURCE;
      }
      logger.error("Couldn't determine "+parameter+" from Configuration.");
      throw new IllegalStateException("Couldn't determine "+parameter+" from Configuration");
    }
  }
}
