/*
 * $Id: TestAlertActionMail.java,v 1.1.2.1 2004-07-19 08:24:33 tlipkis Exp $
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
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/**
 * This is the test class for org.lockss.alert.AlertActionMail
 */
public class TestAlertActionMail extends LockssTestCase {
  private static Logger log = Logger.getLogger("TestAlertActionMail");

  MyMockMailService mgr;

  public void setUp() throws Exception {
    super.setUp();
    mgr = new MyMockMailService();
    getMockLockssDaemon().setMailService(mgr);
  }


  public void testEquals() {
    AlertActionMail a1 = new AlertActionMail("to@here");
    AlertActionMail a2 = new AlertActionMail("to@here");
    AlertActionMail a3 = new AlertActionMail("to@there");
    assertEquals(a1, a2);
    assertEquals(a2, a1);
    assertNotEquals(a1, a3);
    assertNotEquals(a3, a1);
  }

  public void testHash() {
    AlertActionMail a1 = new AlertActionMail("to@here");
    AlertActionMail a2 = new AlertActionMail("to@here");
    AlertActionMail a3 = new AlertActionMail("to@there");
    assertEquals(a1.hashCode(), a2.hashCode());
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
    String[] msg = (String[])mgr.getMsgs().get(0);
    assertEquals("sender", msg[0]);
    assertEquals("recipient", msg[1]);
    String[] body =
      (String[])StringUtil.breakAt(msg[2], '\n').toArray(new String[0]);
    int line = 0;
    assertEquals("From: LOCKSS cache cachename <sender>", body[line++]);
    assertEquals("To: recipient", body[line++]);
    String date = body[line++];
    assertTrue(date.startsWith("Date: "));
    assertEquals("Subject: LOCKSS cache warning: AName", body[line++]);
    assertEquals("X-Mailer: LOCKSS daemon", body[line++]);
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
    String[] msg = (String[])mgr.getMsgs().get(0);
    assertEquals("sender", msg[0]);
    assertEquals("recipient", msg[1]);
    String[] body =
      (String[])StringUtil.breakAt(msg[2], '\n').toArray(new String[0]);
    int line = 0;
    assertEquals("From: LOCKSS cache cachename <sender>", body[line++]);
    assertEquals("To: recipient", body[line++]);
    String date = body[line++];
    assertTrue(date.startsWith("Date: "));
    assertEquals("Subject: LOCKSS cache warning: AName (multiple)",
		 body[line++]);
    assertEquals("X-Mailer: LOCKSS daemon", body[line++]);
    assertEquals("", body[line++]);
  }

  static class MyMockMailService extends MockMailService {
    List msgs = new ArrayList();

    public boolean sendMail(String sender, String recipient, String body) {
      String[] msg = {sender, recipient, body};
      msgs.add(msg);
      return true;
    }

    List getMsgs() {
      return msgs;
    }
  }

}
