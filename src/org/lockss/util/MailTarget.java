/*
 * $Id: MailTarget.java,v 1.3 2003-01-07 20:23:24 aalto Exp $
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
  public static final String PARAM_LOG_EMAIL_TO = Configuration.PREFIX +
      "log.email.to";

  /**
   * Configuration parameter for the log email 'from' address.
   */
  public static final String PARAM_LOG_EMAIL_FROM = Configuration.PREFIX +
      "log.email.from";

  /**
   * Configuration parameter to enable email logging.
   */
  public static final String PARAM_LOG_EMAIL_ENABLED = Configuration.PREFIX +
      "log.email.enabled";

  static final int DEFAULT_SMTPPORT = 25;
  static final boolean DEFAULT_ENABLING = true;
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
        public void configurationChanged(Configuration oldConfig,
                                         Configuration newConfig,
                                         Set changedKeys) {
          loadConfiguration();
        }});
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
    toAddr = Configuration.getParam(PARAM_LOG_EMAIL_TO);
    fromAddr = Configuration.getParam(PARAM_LOG_EMAIL_FROM);
    emailEnabled = Configuration.getBooleanParam(PARAM_LOG_EMAIL_ENABLED,
        DEFAULT_ENABLING);
    if ((smtpHost==null) || (toAddr==null) || (fromAddr==null)) {
      String parameter = PARAM_SMTPHOST;
      if (toAddr==null) {
        parameter = PARAM_LOG_EMAIL_TO;
      } else if (fromAddr==null) {
        parameter = PARAM_LOG_EMAIL_FROM;
      }
      logger.error("Couldn't determine "+parameter+
                   " from Configuration.  Disabling email logging...");
      emailEnabled = false;

    }
  }
}
