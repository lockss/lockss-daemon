/*
 * $Id: MailTarget.java,v 1.6 2003-05-06 00:58:26 aalto Exp $
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
import java.util.*;
import org.lockss.protocol.IdentityManager;
import org.lockss.daemon.Configuration;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Log target for email logging.  Utilizes SmtpMailer.
 */
public class MailTarget {
  static final String MAIL_PREFIX = Configuration.PREFIX +
      "log.target.MailTarget.";
  /**
   * Configuration parameter for the smtp host.
   */
  public static final String PARAM_SMTPHOST = MAIL_PREFIX + "smtphost";
  /**
   * Configuration parameter for the smtp port.  Defaults to 25 if not provided.
   */
  public static final String PARAM_SMTPPORT = MAIL_PREFIX + "smtpport";

  /**
   * Configuration parameter for the log email 'to' address.
   */
  public static final String PARAM_EMAIL_TO = MAIL_PREFIX + "email.to";

  /**
   * Configuration parameter for the log email 'from' address.
   */
  public static final String PARAM_EMAIL_FROM = MAIL_PREFIX + "email.from";

  /**
   * Configuration parameter to enable email logging.
   */
  public static final String PARAM_EMAIL_ENABLED = MAIL_PREFIX +
      "email.enabled";

  static final int DEFAULT_SMTPPORT = 25;
  static final boolean DEFAULT_LOG_EMAIL_ENABLED = true;
  static final String ERROR_SUBJECT = "Error on machine ";
  static final DateFormat df = new SimpleDateFormat("HH:mm:ss.SSS");

  static String smtpHost = null;
  static int smtpPort;
  static String localHostName;
  static String toAddr;
  static String fromAddr;
  static boolean emailEnabled;
  protected static Logger logger = Logger.getLogger("MailTarget");

  public void init() {
    Configuration.registerConfigurationCallback(new Configuration.Callback() {
        public void configurationChanged(Configuration newConfig,
					   Configuration oldConfig,
					   Set changedKeys) {
          loadConfiguration();
        }});
    localHostName = IdentityManager.getLocalHostName();

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
    String levelStr = Logger.nameOf(level) + ": ";
    String subject = levelStr + ERROR_SUBJECT + localHostName;
    String body = df.format(new Date()) + ":\n" + levelStr + message;
    if (!emailEnabled) {
      this.logger.warning("Email logging not enabled; message discarded: " +
                          levelStr + message);
      return;
    }
    try {
      SmtpMailer.sendMail(toAddr, fromAddr, subject, body, smtpHost,
                          smtpPort, localHostName);
    } catch (IOException ioe) {
      this.logger.error("Couldn't send mail correctly.", ioe);
    }
  }

  /**
   * Load the configuration parameters.
   * If set in props use that, else load from ssmtp config file.
   */
  private void loadConfiguration() {
    smtpHost = Configuration.getParam(PARAM_SMTPHOST);
    smtpPort = Configuration.getIntParam(PARAM_SMTPPORT, DEFAULT_SMTPPORT);
    toAddr = Configuration.getParam(PARAM_EMAIL_TO);
    fromAddr = Configuration.getParam(PARAM_EMAIL_FROM);
    emailEnabled = Configuration.getBooleanParam(PARAM_EMAIL_ENABLED,
        DEFAULT_LOG_EMAIL_ENABLED);
    if ((smtpHost==null) || (toAddr==null) || (fromAddr==null)) {
      String parameter = PARAM_SMTPHOST;
      if (toAddr==null) {
        parameter = PARAM_EMAIL_TO;
      } else if (fromAddr==null) {
        parameter = PARAM_EMAIL_FROM;
      }
      logger.error("Couldn't determine "+parameter+
                   " from Configuration.  Disabling email logging...");
      emailEnabled = false;

    }
  }
}
