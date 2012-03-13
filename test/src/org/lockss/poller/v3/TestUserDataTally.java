/*
 * $Id: TestUserDataTally.java,v 1.2 2012-03-13 23:41:01 barry409 Exp $
 */

/*

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.poller.v3;

import org.lockss.test.*;
import java.util.*;

public class TestUserDataTally extends LockssTestCase {
  private String[] testPeers;

  public void setUp() throws Exception {
    super.setUp();
    setupPeers();
  }
  
  private void setupPeers() {
    testPeers = new String[10];
    testPeers[0] = "TCP:[192.168.0.1]:9900";
    testPeers[1] = "TCP:[192.168.0.1]:9901";
    testPeers[2] = "TCP:[192.168.0.1]:9902";
    testPeers[3] = "TCP:[192.168.0.1]:9903";
    testPeers[4] = "TCP:[192.168.0.1]:9904";
  }

  public void testTalliedVoters() {
    UserDataTally tally;
    Collection<String> talliedVoters;
    Collection<String> talliedAgreeVoters;

    tally = new UserDataTally<String>();
    tally.addTalliedAgreeVoter(testPeers[0]);
    tally.addTalliedDisagreeVoter(testPeers[1]);
    talliedVoters = tally.talliedVoters;
    assertEquals(2, talliedVoters.size());
    assertContains(talliedVoters, testPeers[0]);
    assertContains(talliedVoters, testPeers[1]);
    talliedAgreeVoters = tally.talliedAgreeVoters;
    assertEquals(1, talliedAgreeVoters.size());
    assertContains(talliedAgreeVoters, testPeers[0]);
  }
}
