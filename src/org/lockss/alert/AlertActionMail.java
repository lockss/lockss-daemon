/*
 * $Id: AlertActionMail.java,v 1.2 2004-07-12 21:09:44 clairegriffin Exp $
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

import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.mail.*;
import org.lockss.util.*;

/** An AlertAction that records the Alert by emailing it */
public class AlertActionMail extends AbstractAlertAction {
  private static Logger log = Logger.getLogger("AlertActionMail");

  static final String PARAM_EMAIL_FROM = PREFIX + "mail.from";
  static final String PARAM_ENABLED = PREFIX + "mail.enabled";
  static final boolean DEFAULT_ENABLED = false;

  private String addressees;

  public AlertActionMail() {
  }

  /** Create an action that mails to a single addresse */
  public AlertActionMail(String to) {
    this.addressees = to;
  }

  /** Create an action that mails to a list of addresses */
  public AlertActionMail(List addressees) {
    this(StringUtil.separatedString(addressees, ", "));
  }

  /** Return the addresses */
  public String getAddresses() {
    return addressees;
  }

  /** Set the addresses */
  public void setAddresses(String addressees) {
    this.addressees = addressees;
  }

  /** Create and send a message */
  public void record(LockssDaemon daemon, Alert alert) {
    Configuration config = Configuration.getCurrentConfig();
    if (config.getBoolean(PARAM_ENABLED, DEFAULT_ENABLED)) {
      MailService mailSvc = daemon.getMailService();
      String fromAddr = Configuration.getParam(PARAM_EMAIL_FROM);
      MailMessage msg = new MailMessage();
      msg.addHeader("From", fromAddr);
      msg.addHeader("To", addressees);
      msg.addHeader("Subject", alert.getMailSubject());
      msg.addHeader("X-Mailer", "LOCKSS cache");
      msg.setText(alert.getMailBody());
      mailSvc.sendMail(addressees, fromAddr, msg.getBody());
    }
  }

  /** Create and send a single message for a list of alerts */
  public void record(LockssDaemon daemon, List alerts) {
    log.info("Recording " + alerts.size() + " actions");
    log.info(alerts.toString());
    if (alerts == null || alerts.isEmpty()) return;
    Alert firstAlert = (Alert)alerts.get(0);
    Configuration config = Configuration.getCurrentConfig();
    if (config.getBoolean(PARAM_ENABLED, DEFAULT_ENABLED)) {
      MailService mailSvc = daemon.getMailService();
      String fromAddr = Configuration.getParam(PARAM_EMAIL_FROM);
      MailMessage msg = new MailMessage();
      msg.addHeader("From", fromAddr);
      msg.addHeader("To", addressees);
      msg.addHeader("Subject", "(multiple) " + firstAlert.getMailSubject());
      msg.addHeader("X-Mailer", "LOCKSS cache");
      StringBuffer sb = new StringBuffer();
      for (Iterator iter = alerts.iterator(); iter.hasNext(); ) {
	Alert alert = (Alert)iter.next();
	sb.append(alert.getMailBody());
	if (iter.hasNext()) {
	  sb.append("\n==========================================================================\n");
	}
      }
      msg.setText(sb.toString());
      mailSvc.sendMail(addressees, fromAddr, msg.getBody());
    }
  }

  public boolean isGroupable() {
    return true;
  }

  /** Return the maximum time an alert should remain pending before it it
   * reported in a group */
  public long getMaxPendTime() {
    return Constants.MINUTE;
  }

  public boolean equals(Object obj) {
    if (obj instanceof AlertActionMail ) {
      return addressees.equals(((AlertActionMail)obj).addressees);
    }
    return false;
  }

  public int hashCode() {
    return addressees.hashCode();
  }

  public String toString() {
    return "[AlertActions.Mail: " + addressees + "]";
  }
}
