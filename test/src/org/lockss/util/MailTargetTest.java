/*
 * $Id: MailTargetTest.java,v 1.1 2003-02-26 21:36:37 aalto Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.test.*;
import org.lockss.daemon.TestConfiguration;

/**
 * Test class for functional tests on the mail target.
 */
public class MailTargetTest extends LockssTestCase {
  private MailTarget target;
  private static final String emailTo = "lime@leland.stanford.edu";
  private static final String emailFrom = "aalto@cs.stanford.edu";
  protected static Logger logger = Logger.getLogger("MailTarget");

  public MailTargetTest(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    String s = MailTarget.PARAM_SMTPHOST + "=smtp.stanford.edu";
    String s2 = MailTarget.PARAM_LOG_EMAIL_TO + "=" + emailTo;
    String s3 = MailTarget.PARAM_LOG_EMAIL_FROM + "=" + emailFrom;
    TestConfiguration.setCurrentConfigFromUrlList(ListUtil.list(
        FileUtil.urlOfString(s), FileUtil.urlOfString(s2),
        FileUtil.urlOfString(s3)));
    target = new MailTarget();
    target.init();
  }

  public void testEmailLogging() throws Exception {
    target.handleMessage(null, Logger.LEVEL_INFO, "Email test 1");
    target.emailEnabled = false;
    target.handleMessage(null, Logger.LEVEL_INFO,
                         "Email test 2: should not be received");
  }
}
