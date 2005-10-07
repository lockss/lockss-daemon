/*
 * $Id: TestV1PollTally.java,v 1.3 2005-10-07 23:46:45 smorabito Exp $
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

package org.lockss.poller;

import java.io.*;
import java.util.*;
import java.net.*;
import org.lockss.app.*;
import org.lockss.config.ConfigManager;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.repository.LockssRepositoryImpl;

/** JUnitTest case for class: org.lockss.poller.Poll */
public class TestV1PollTally extends LockssTestCase {
  private static Logger log = Logger.getLogger("TestV1PollTally");

  public void testConcatenateNullNameSubPollLists() {
    V1PollTally t1 = new MockV1PollTally();
    V1PollTally t2 = new MockV1PollTally();
    t1.localEntries = null;
    t2.localEntries = ListUtil.list("three", "fore");
    t2.setPreviousNamePollTally(t1);
    V1PollTally concatTally = t2.concatenateNameSubPollLists();
    assertSame(t1, concatTally);
    assertEquals(ListUtil.list("three", "fore"), t1.localEntries);

    t1.localEntries = ListUtil.list("three", "fore");
    t2.localEntries = null;
    t2.setPreviousNamePollTally(t1);
    concatTally = t2.concatenateNameSubPollLists();
    assertSame(t1, concatTally);
    assertEquals(ListUtil.list("three", "fore"), t1.localEntries);
  }

  public void testConcatenateNameSubPollLists() {
    V1PollTally t1 = new MockV1PollTally();
    V1PollTally t2 = new MockV1PollTally();
    V1PollTally t3 = new MockV1PollTally();
    t1.localEntries = ListUtil.list("one", "two");
    t2.localEntries = ListUtil.list("three", "fore");
    t3.localEntries = ListUtil.list("five", "sicks");
    t2.setPreviousNamePollTally(t1);
    t3.setPreviousNamePollTally(t2);
    V1PollTally concatTally = t3.concatenateNameSubPollLists();
    assertSame(t1, concatTally);
    assertEquals(ListUtil.list("one", "two", "three", "fore", "five", "sicks"),
		 t1.localEntries);
  }

  class MockV1PollTally extends V1PollTally {
    MockV1PollTally() {
      super(Poll.V1_NAME_POLL, 0, 4, 0, 0, 0, 0, 3, null);
    }
  }

}
