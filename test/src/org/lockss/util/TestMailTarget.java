/*
 * $Id: TestMailTarget.java,v 1.9 2003-09-16 23:28:36 eaalto Exp $
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

package org.lockss.util;

import java.net.*;
import java.io.*;
import java.util.*;
import org.lockss.test.*;
import org.lockss.daemon.*;
import org.lockss.protocol.IdentityManager;

public class TestMailTarget extends LockssTestCase {
  private MailTarget target;

  public TestMailTarget(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    String s = MailTarget.PARAM_SMTPHOST + "=1.2.3.4";
    String s2 = MailTarget.PARAM_EMAIL_TO + "=target@t.com";
    String s3 = MailTarget.PARAM_EMAIL_FROM + "=source@s.com";
    ConfigurationUtil.setCurrentConfigFromUrlList(ListUtil.list(
        FileTestUtil.urlOfString(s), FileTestUtil.urlOfString(s2),
        FileTestUtil.urlOfString(s3)));
    target = new MailTarget();
    System.err.println("Ignore IdentityManager error:");
    target.init();
  }

  public void testConfiguration() throws Exception {
    // testing with defaults
    assertEquals("1.2.3.4", target.smtpHost);
    assertEquals(25, target.smtpPort);
    assertEquals("target@t.com", target.toAddr);
    assertEquals("source@s.com", target.fromAddr);
    assertFalse(target.emailEnabled);

    //retesting without defaults
    String s = MailTarget.PARAM_SMTPHOST + "=1.2.3.5";
    String s2 = MailTarget.PARAM_SMTPPORT + "=24";
    String s3 = MailTarget.PARAM_EMAIL_TO + "=target2@t.com";
    String s4 = MailTarget.PARAM_EMAIL_FROM + "=source2@s.com";
    String s5 = MailTarget.PARAM_EMAIL_ENABLED + "=false";
    ConfigurationUtil.setCurrentConfigFromUrlList(ListUtil.list(
        FileTestUtil.urlOfString(s), FileTestUtil.urlOfString(s2),
        FileTestUtil.urlOfString(s3), FileTestUtil.urlOfString(s4),
        FileTestUtil.urlOfString(s5)));
    target = new MailTarget();
    System.err.println("Ignore IdentityManager error:");
    target.init();
    assertEquals("1.2.3.5", target.smtpHost);
    assertEquals(24, target.smtpPort);
    assertEquals("target2@t.com", target.toAddr);
    assertEquals("source2@s.com", target.fromAddr);
    assertFalse(target.emailEnabled);
  }
}
