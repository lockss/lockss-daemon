/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.test.*;

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

  private static final int k_randomBytesSize = 100;
}
