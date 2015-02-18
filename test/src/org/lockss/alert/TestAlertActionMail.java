/*
 * $Id$
 */

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

package org.lockss.alert;

import java.io.*;
import java.util.*;
import java.net.*;
import java.text.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.mail.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/**
 * This is the test class for org.lockss.alert.AlertActionMail
 */
public class TestAlertActionMail extends LockssTestCase {
  private static final Logger log = Logger.getLogger(TestAlertActionMail.class);

  MockMailService mgr;

  public void setUp() throws Exception {
    super.setUp();
    mgr = new MockMailService();
    getMockLockssDaemon().setMailService(mgr);
  }


  public void testSimple() {
    AlertActionMail a1 = new AlertActionMail("to@here");
    assertTrue(a1.isGroupable());
    assertTrue(a1.getMaxPendTime() > Constants.MINUTE);
  }

  public void testToString() {
    AlertActionMail a1 = new AlertActionMail("to@here");
    a1.toString();
  }

  public void testEquals() {
    AlertActionMail a1 = new AlertActionMail("to@here");
    AlertActionMail a2 = new AlertActionMail("to@here");
    AlertActionMail a3 = new AlertActionMail("to@there");
    assertEquals(a1, a2);
    assertEquals(a2, a1);
    assertNotEquals(a1, a3);
    assertNotEquals(a3, a1);
    assertNotEquals("aa", a1);
    assertNotEquals(a1, "aa");
  }

  public void testHash() {
    AlertActionMail a1 = new AlertActionMail("to@here");
    AlertActionMail a2 = new AlertActionMail("to@here");
    AlertActionMail a3 = new AlertActionMail("to@there");
    assertEquals(a1.hashCode(), a2.hashCode());
  }

  public void testRecips() {
    AlertActionMail act1 = new AlertActionMail("to@here");
    AlertActionMail act2 = new AlertActionMail();
    assertEquals("to@here", act1.getRecipients(null));
    try {
      assertEquals(null, act2.getRecipients(null));
      fail("Should throw NPE");
    } catch (NullPointerException e) {
    }
//     act1 = new AlertActionMail(ListUtil.list("to@here", "and@there"));
//     assertEquals("to@here, and@there", act1.getRecipients());

    Alert a1 = new Alert("Foo").setAttribute(Alert.ATTR_EMAIL_TO, "from@there");
    assertEquals("to@here", act1.getRecipients(a1));
    assertEquals("from@there", act2.getRecipients(a1));
  }

  public void testSenderDefault() {
    Properties p = new Properties();
    p.put(ConfigManager.PARAM_PLATFORM_ADMIN_EMAIL, "admin@there");
    p.put("org.lockss.alert.action.mail.enabled", "true");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    Alert a1 = new Alert("AName");
    a1.setAttribute(Alert.ATTR_CACHE, "cachename");
    a1.setAttribute(Alert.ATTR_SEVERITY, Alert.SEVERITY_WARNING);
    AlertActionMail act1 = new AlertActionMail("recipient");
    act1.record(getMockLockssDaemon(), a1);
    MockMailService.Rec rec = mgr.getRec(0);
    assertEquals("admin@there", rec.getSender());
  }

  public void testRecordOne() {
    Properties p = new Properties();
    p.put("org.lockss.alert.action.mail.sender", "sender");
    p.put("org.lockss.alert.action.mail.enabled", "true");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    Alert a1 = new Alert("AName");
    a1.setAttribute(Alert.ATTR_CACHE, "cachename");
    a1.setAttribute(Alert.ATTR_SEVERITY, Alert.SEVERITY_WARNING);
    AlertActionMail act1 = new AlertActionMail("recipient");
    act1.record(getMockLockssDaemon(), a1);
    MockMailService.Rec rec = mgr.getRec(0);
    assertEquals("sender", rec.getSender());
    assertEquals("recipient", rec.getRecipient());
    String[] body =
      (String[])StringUtil.breakAt(toString(rec.getMsg()),
				   "\n").toArray(new String[0]);
    int line = 0;
    assertEquals("From: LOCKSS box cachename <sender>", body[line++]);
    assertEquals("To: recipient", body[line++]);
    String date = body[line++];
    assertTrue(date.startsWith("Date: "));
    assertEquals("Subject: LOCKSS box warning: AName", body[line++]);
    assertEquals("X-Mailer: " + getXMailer(), body[line++]);
    assertEquals("", body[line++]);
  }

  public void testRecordList() {
    Properties p = new Properties();
    p.put("org.lockss.alert.action.mail.sender", "sender");
    p.put("org.lockss.alert.action.mail.enabled", "true");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    Alert a1 = new Alert("AName");
    a1.setAttribute(Alert.ATTR_CACHE, "cachename");
    a1.setAttribute(Alert.ATTR_SEVERITY, Alert.SEVERITY_WARNING);
    Alert a2 = new Alert("A Nother Name");
    a2.setAttribute(Alert.ATTR_CACHE, "cachename");
    a2.setAttribute(Alert.ATTR_SEVERITY, Alert.SEVERITY_ERROR);
    AlertActionMail act1 = new AlertActionMail("recipient");
    act1.record(getMockLockssDaemon(), ListUtil.list(a1, a2));
    MockMailService.Rec rec = mgr.getRec(0);
    assertEquals("sender", rec.getSender());
    assertEquals("recipient", rec.getRecipient());
    String[] body =
      (String[])StringUtil.breakAt(toString(rec.getMsg()),
				   "\n").toArray(new String[0]);
    int line = 0;
    assertEquals("From: LOCKSS box cachename <sender>", body[line++]);
    assertEquals("To: recipient", body[line++]);
    String date = body[line++];
    assertTrue(date.startsWith("Date: "));
    assertEquals("Subject: LOCKSS box warning: AName (multiple)",
		 body[line++]);
    assertEquals("X-Mailer: " + getXMailer(), body[line++]);
    assertEquals("", body[line++]);
  }

  String getXMailer() {
    String release = BuildInfo.getBuildProperty(BuildInfo.BUILD_RELEASENAME);
    if (release != null) {
      return "LOCKSS daemon " + release;
    }
    return "LOCKSS daemon";
  }

  String toString(MailMessage msg) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      msg.writeData(baos);
      return baos.toString();
    } catch (IOException e) {
      throw new RuntimeException("Error converting MimeMessage to string", e);
    }
  }

}
