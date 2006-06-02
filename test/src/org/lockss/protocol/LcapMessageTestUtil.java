/*
 * $Id: LcapMessageTestUtil.java,v 1.8 2006-06-02 20:27:16 smorabito Exp $
 *

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

package org.lockss.protocol;

import java.security.*;
import java.io.*;
import java.util.*;
import org.lockss.util.*;

/** Utilities for making test messages.
 * XXX needs to be rationalized with TestV3LcapMessage
 */
public class LcapMessageTestUtil {
  public static Logger log = Logger.getLogger("MsgUtil");

  private static String m_archivalID = "TestAU_1.0";
  private static String m_url = "http://www.example.com";
  private static  byte[] m_testBytes = {
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
    11, 12, 13, 14, 15, 16, 17, 18, 19, 20
  };

  public static V3LcapMessage makeTestVoteMessage(PeerIdentity peer,
                                                  File tempDir) 
      throws IOException {
    return makeTestVoteMessage(peer, null, tempDir);
  }

  public static V3LcapMessage makeTestVoteMessage(PeerIdentity peer,
						  Collection voteBlocks,
                                                  File tempDir)
      throws IOException {
    V3LcapMessage msg = new V3LcapMessage("ArchivalID_2", "key", "Plug42",
                                          ByteArray.makeRandomBytes(20),
                                          ByteArray.makeRandomBytes(20),
                                          V3LcapMessage.MSG_REPAIR_REQ,
                                          987654321, peer, tempDir);

    // Set msg vote blocks.
    if (voteBlocks != null) {
      for (Iterator ix = voteBlocks.iterator(); ix.hasNext(); ) {
	msg.addVoteBlock((VoteBlock)ix.next());
      }
    }
    
    msg.setHashAlgorithm(LcapMessage.getDefaultHashAlgorithm());
    msg.setArchivalId(m_archivalID);
    msg.setPluginVersion("PlugVer42");
    return msg;
  }

  public static byte[] computeHash(String s) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA");
      digest.update(s.getBytes());
      byte[] hashed = digest.digest();
      return hashed;
    } catch (java.security.NoSuchAlgorithmException e) {
      return new byte[0];
    }
  }

}
