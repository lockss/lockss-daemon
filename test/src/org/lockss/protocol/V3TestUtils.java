/*
 * $Id$
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

import java.security.MessageDigest;
import java.util.*;

import org.lockss.app.*;
import org.lockss.protocol.*;
import org.lockss.test.*;
import org.lockss.util.*;

public class V3TestUtils {

  public static class NoStoreIdentityManager extends IdentityManagerImpl {
    @Override
    public void storeIdentities() {
    }

    @Override
    protected void setupLocalIdentities() {
    }
  }

  public static PeerIdentity findPeerIdentity(MockLockssDaemon daemon,
					      String id)
      throws IdentityManager.MalformedIdentityKeyException {
    IdentityManager idMgr;
    if (daemon.hasIdentityManager()) {
      idMgr = daemon.getIdentityManager();
    } else {
      idMgr = new NoStoreIdentityManager();
      daemon.setIdentityManager(idMgr);
      idMgr.initService(daemon);
//       idMgr.startService();
    }
    return idMgr.findPeerIdentity(id);
  }

  public static List<VoteBlock> makeVoteBlockList(int count) {
    ArrayList<VoteBlock> vbList = new ArrayList<VoteBlock>();
    for (int ix = 0; ix < count; ix++) {
      String fileName = "/test-" + ix + ".html";
      // XXX DSHR shouldn't the hash go somewhere?
      V3TestUtils.computeHash(fileName);
      VoteBlock vb = V3TestUtils.makeVoteBlock(fileName);
      vbList.add(vb);
    }
    return vbList;
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
  
  public static VoteBlock makeVoteBlock(String url) {
    return V3TestUtils.makeVoteBlock(url, 1);
  }

  private static final int k_randomBytesSize = 100;
  
  public static VoteBlock makeVoteBlock(String url, int versions) {
    VoteBlock vb = new VoteBlock(url, VoteBlock.CONTENT_VOTE);
    for (int ix = 0; ix < versions; ix++) {
      vb.addVersion(0L, 1000L, 0L, 1000L,
                    ByteArray.makeRandomBytes((int) (Math.random() * k_randomBytesSize)), // URL
                    ByteArray.makeRandomBytes(20), // Hash 
                    false);
    }
    return vb;
  }

}
