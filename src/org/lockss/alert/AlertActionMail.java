/*
 * $Id: AlertActionMail.java,v 1.4 2004-08-09 02:54:59 tlipkis Exp $
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

import java.util.*;
import java.text.*;

import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.mail.*;
import org.lockss.util.*;

/** An AlertAction that records the Alert by emailing it */
public class AlertActionMail extends AbstractAlertAction {
  private static Logger log = Logger.getLogger("AlertActionMail");

  /** If sepcified, the sender address on alert emails.  If not specific,
   * uses the admin email adress */
  static final String PARAM_EMAIL_SENDER = PREFIX + "mail.sender";
  /** Default sender, if PARAM_EMAIL_SENDER not present */
  static final String PARAM_DEFAULT_EMAIL_SENDER =
    ConfigManager.PARAM_PLATFORM_ADMIN_EMAIL;
  // Make default param name show up as default in param listing.
  static final String DEFAULT_EMAIL_SENDER = PARAM_DEFAULT_EMAIL_SENDER;

  /** printf string applied to cache-name, email-sender */
  static final String PARAM_EMAIL_FROM = PREFIX + "mail.from";
  static final String DEFAULT_EMAIL_FROM = "LOCKSS cache %s <%s>";

  static final String PARAM_ENABLED = PREFIX + "mail.enabled";
  static final boolean DEFAULT_ENABLED = false;

  DateFormat headerDf = new SimpleDateFormat("EEE dd MMM yyyy HH:mm:ss zzz"
					     /*, Locale.US */);
//   headerDf.setTimeZone(TimeZone.getTimeZone("GMT"));

  private String recipients;

  public AlertActionMail() {
  }

  /** Create an action that mails to a single addresse */
  public AlertActionMail(String to) {
    this.recipients = to;
  }

//   /** Create an action that mails to a list of recipients */
//   public AlertActionMail(List recipients) {
//     this(StringUtil.separatedString(recipients, ", "));
//   }

  /** Return the recipients */
  public String getRecipients() {
    return recipients;
  }

  /** Set the recipients */
  public void setRecipients(String recipients) {
    this.recipients = recipients;
  }

  /** Create and send a message */
  public void record(LockssDaemon daemon, Alert alert) {
    Configuration config = Configuration.getCurrentConfig();
    if (config.getBoolean(PARAM_ENABLED, DEFAULT_ENABLED)) {
      MailService mailSvc = daemon.getMailService();
      String sender = Configuration.getParam(PARAM_EMAIL_FROM);
      MailMessage msg = new MailMessage();
      msg.addHeader("From", getFrom(alert, config));
      msg.addHeader("To", recipients);
      msg.addHeader("Date", headerDf.format(TimeBase.nowDate()));
      msg.addHeader("Subject", alert.getMailSubject());
      msg.addHeader("X-Mailer", getXMailer());
      msg.setText(alert.getMailBody());
      mailSvc.sendMail(getSender(config), recipients, msg.getBody());
    }
  }

  String getXMailer() {
    String release = BuildInfo.getBuildProperty(BuildInfo.BUILD_RELEASENAME);
    if (release != null) {
      return "LOCKSS daemon " + release;
    }
    return "LOCKSS daemon";
  }

  /** Create and send a single message for a list of alerts */
  public void record(LockssDaemon daemon, List alerts) {
    if (log.isDebug2()) {
      log.debug2("Recording " + alerts.size() + " actions");
      log.debug3(alerts.toString());
    }
    if (alerts == null || alerts.isEmpty()) return;
    Alert firstAlert = (Alert)alerts.get(0);
    Configuration config = Configuration.getCurrentConfig();
    if (config.getBoolean(PARAM_ENABLED, DEFAULT_ENABLED)) {
      MailService mailSvc = daemon.getMailService();
      String sender = Configuration.getParam(PARAM_EMAIL_FROM);
      MailMessage msg = new MailMessage();
      msg.addHeader("From", getFrom(firstAlert, config));
      msg.addHeader("To", recipients);
      msg.addHeader("Date", headerDf.format(TimeBase.nowDate()));
      msg.addHeader("Subject", firstAlert.getMailSubject() + " (multiple)");
      msg.addHeader("X-Mailer", getXMailer());
      StringBuffer sb = new StringBuffer();
      for (Iterator iter = alerts.iterator(); iter.hasNext(); ) {
	Alert alert = (Alert)iter.next();
	sb.append(alert.getMailBody());
	if (iter.hasNext()) {
	  sb.append("\n==========================================================================\n");
	}
      }
      msg.setText(sb.toString());
      mailSvc.sendMail(getSender(config), recipients, msg.getBody());
    }
  }

  public boolean isGroupable() {
    return true;
  }

  /** Return the maximum time an alert should remain pending before it it
   * reported in a group */
  public long getMaxPendTime() {
    return Constants.DAY;
  }

  private String getSender(Configuration config) {
    String defaultSender = config.get(PARAM_DEFAULT_EMAIL_SENDER);
    return config.get(PARAM_EMAIL_SENDER, defaultSender);
  }

  private String getFrom(Alert alert, Configuration config) {
    try {
      String fmt = config.get(PARAM_EMAIL_FROM, DEFAULT_EMAIL_FROM);
      PrintfFormat pf = new PrintfFormat(fmt);
      // if not locally generated alert, should use our name instead
      String cacheName = alert.getString(Alert.ATTR_CACHE);
      String[] args = { cacheName, getSender(config) };
      return pf.sprintf(args);
    } catch (Exception e) {
      log.warning("getFrom()", e);
      return "LOCKSS cache";
    }
  }

  public boolean equals(Object obj) {
    if (obj instanceof AlertActionMail ) {
      return recipients.equals(((AlertActionMail)obj).recipients);
    }
    return false;
  }

  public int hashCode() {
    return recipients.hashCode();
  }

  public String toString() {
    return "[AlertActions.Mail: " + recipients + "]";
  }
}
