/*
 * $Id: TestV3PollerState.java,v 1.1 2005-07-13 07:53:05 smorabito Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;
import org.lockss.app.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;
import org.exolab.castor.mapping.Mapping;

public class TestV3PollerState extends LockssTestCase {

  private File tempDir = null;

  public void setUp() throws Exception {
    super.setUp();
    tempDir = getTempDir();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void testFoo() {
  }

//   public void testMarshallUnmarshall() throws Throwable {
//     V3PollerState state =
//       new V3PollerState("foobarbaz", ByteArray.makeRandomBytes(20),
// 			"SHA-1", 1116885592875L);
//     state.addVoterState(new V3VoterState("127.0.0.1:4000"));
//     state.addVoterState(new V3VoterState("192.168.0.1:4000"));
//     state.addVoterState(new V3VoterState("220.2.32.7"));
//     V3VoterState v = new V3VoterState("64.128.22.31:20");
//     v.setStatus(V3VoterState.VOTING);
//     state.addVoterState(v);
    
//     File f = new File(tempDir, "v3pollerState.xml");

//     log.debug("State 1: " + state.toString());
        
//     state.store(f);
//     V3PollerState state2 = V3PollerState.load(f);

//     log.debug("State 2: " + state2.toString());
    
//     assertEqualV3PollerStates(state, state2);
//   }

//   public void assertEqualV3PollerStates(V3PollerState a, V3PollerState b) {
//     assertEquals(a.getPollKey(), b.getPollKey());
//     assertEquals(a.getDeadline(), b.getDeadline());
//     assertEquals(a.getChallenge(), b.getChallenge());
//     assertEquals(a.getHashAlgorithm(), b.getHashAlgorithm());
//     assertIsomorphicVoterStates(a.getVoterStates(), b.getVoterStates());
//   }

//   public void assertEqualV3VoterStates(V3VoterState a, V3VoterState b) {
//     assertEquals(a.getVoterIdString(), b.getVoterIdString());
//     assertEquals(a.getPollerIdString(), b.getPollerIdString());
//     assertEquals(a.getStatus(), b.getStatus());
//   }

//   public void assertIsomorphicVoterStates(List a, List b) {
//     int len = a.size();
//     if (b.size() != len) {
//       fail("List sizes do not match");
//     }
//     for (int i = 0; i < len; i++) {
//       V3VoterState state1 = (V3VoterState)a.get(i);
//       V3VoterState state2 = (V3VoterState)b.get(i);
//       assertEqualV3VoterStates(state1, state2);
//     }
//   }

}
