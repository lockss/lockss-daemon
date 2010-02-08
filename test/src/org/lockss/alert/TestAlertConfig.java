/*
 * $Id: TestAlertConfig.java,v 1.4 2010-02-08 23:00:52 tlipkis Exp $
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

import java.io.*;
import java.util.*;
import java.net.*;
import java.text.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/**
 * This is the test class for org.lockss.alert.AlertConfig
 */
public class TestAlertConfig extends LockssTestCase {

  public void testNew() {
    AlertConfig c1 = new AlertConfig();
    assertEmpty(c1.getFilters());
    List lst = ListUtil.list(new AlertFilter(null, null));
    c1.setFilters(lst);
    assertEquals(lst, c1.getFilters());
  }

  public void testCopy() {
    AlertConfig c1 = new AlertConfig();
    List lst = ListUtil.list(new AlertFilter(null, null));
    c1.setFilters(lst);
    AlertConfig c2 = new AlertConfig(c1);
    assertEquals(lst, c2.getFilters());
  }

  public void testHashCodeEqual() {
    List list = new ArrayList();
    AlertConfig c1 = new AlertConfig(list);
    AlertConfig c2 = new AlertConfig(list);
    assertEquals(c1.hashCode(), c2.hashCode());
  }

  /**
   * While it is allowed for hashCode for different objects to be equal, these
   * tests are meant to catch simple errors which break the hash function by
   * making it insensitive to one of the unique vars
   */
  public void testHashCodeNotEqual() {
    List list1 = new ArrayList();
    list1.add("blah");
    List list2 = new ArrayList();
    list2.add("blah2");
    AlertConfig c1 = new AlertConfig(list1);
    AlertConfig c2 = new AlertConfig(list2);
    AlertConfig c3 = new AlertConfig();
    assertNotEquals(c1.hashCode(), c2.hashCode());
    assertNotEquals(c1.hashCode(), c3.hashCode());
    assertNotEquals(c2.hashCode(), c3.hashCode());
  }

  public void testGen() throws Exception {
    MockLockssDaemon daemon = getMockLockssDaemon();
    AlertManagerImpl mgr = new AlertManagerImpl();
    daemon.setAlertManager(mgr);
    mgr.initService(daemon);
    daemon.setDaemonInited(true);
    mgr.startService();

    List pwdAlertNames = ListUtil.list(Alert.PASSWORD_REMINDER.getName(),
				       Alert.ACCOUNT_DISABLED.getName());
    AlertFilter passwdFilt = 
      new AlertFilter(AlertPatterns.CONTAINS(Alert.ATTR_NAME,
					     pwdAlertNames),
		      new AlertActionMail());
    AlertFilter crawlExclFilt = 
      new AlertFilter(AlertPatterns.EQ(Alert.ATTR_NAME,
				       Alert.CRAWL_EXCLUDED_URL.getName()),
		      new AlertActionMail("exclmail"));
    AlertFilter devFilt = 
      new AlertFilter(AlertPatterns.CONTAINS(Alert.ATTR_NAME,
					     ListUtil.list("devAlert1",
							   "devAlert2")),
		      new AlertActionMail("devmail"));

    AlertConfig conf =
      new AlertConfig(ListUtil.list(passwdFilt, crawlExclFilt, devFilt));

    File file = FileTestUtil.tempFile("alertconf", ".xml");
    File file2 = FileTestUtil.tempFile("alertconf", ".asc");
    mgr.storeAlertConfig(file, conf);

    Reader rdr =
      new org.lockss.filter.WhiteSpaceFilter(new BufferedReader(new FileReader(file)));
    String str = StringUtil.fromReader(rdr);
    String str2 = org.apache.commons.lang.StringEscapeUtils.escapeXml(str);
    FileTestUtil.writeFile(file2, str2);
  }

}
