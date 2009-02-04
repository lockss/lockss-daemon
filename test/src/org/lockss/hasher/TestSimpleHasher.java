/*
 * $Id: TestSimpleHasher.java,v 1.1.2.1 2009-02-04 08:30:36 tlipkis Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.hasher;

import java.util.*;
import java.io.*;
import java.security.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.plugin.*;

public class TestSimpleHasher extends LockssTestCase {
  static final String HASH_ALG = "SHA-1";

  static final String BASE_URL = "http://www.test.com/blah/";

  static String[] urls = {
    BASE_URL,
    BASE_URL + "x.html",
    BASE_URL + "foo/",
    BASE_URL + "foo/1",
    BASE_URL + "foo/2",
    BASE_URL + "foo/2/a.txt",
    BASE_URL + "foo/2/b.txt",
    BASE_URL + "foo/2/c.txt",
    BASE_URL + "foo/2/d.txt",
    BASE_URL + "foo/3",
    BASE_URL + "foo/3/a.html",
    BASE_URL + "foo/3/b.html",
  };

  byte[] challenge = null;
  byte[] verifier = null;

  MockArchivalUnit setupContentTree() {
    return setupContentTree(null);
  }

  MockArchivalUnit setupContentTree(MockArchivalUnit mau) {
    if (mau == null) {
      mau = new MockArchivalUnit();
    }
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    List files = new ArrayList();
    for (int ix = 0; ix < urls.length; ix++) {
      String url = urls[ix];
      CachedUrl cu = mau.addUrl(url, false, true);
      files.add(cu);
      addContent(mau, url, org.apache.commons.lang.StringUtils.repeat(url, ix));
    }
    cus.setHashItSource(files);
    return mau;
  }

  void addContent(MockArchivalUnit mau, String url, String content) {
    MockCachedUrl cu = (MockCachedUrl)mau.makeCachedUrl(url);
    cu.setContent(content);
  }
  
  public MessageDigest getMessageDigest(String alg) throws Exception {
    return MessageDigest.getInstance(alg);
  }

  String exp1 =
    "# comment 17\n" +
    "2jmj7l5rSw0yVb/vlWAYkK/YBwk=   http://www.test.com/blah/\n" +
    "ykSi7nDIcbc9qrGr0jzo7fbNujM=   http://www.test.com/blah/x.html\n" +
    "EBghXMXYtgSzI48eCBQRRdU2vfw=   http://www.test.com/blah/foo/\n" +
    "34i4ig3MHL5BV3kOJOkD5lMQX08=   http://www.test.com/blah/foo/1\n" +
    "YhpGRl6vHtNdy4jNj27TlHGiEPc=   http://www.test.com/blah/foo/2\n" +
    "Vy80nUejxnKUsL77iAFBdydXIOM=   http://www.test.com/blah/foo/2/a.txt\n" +
    "WWCWnu24b8Q8euilqaoASbSvIWw=   http://www.test.com/blah/foo/2/b.txt\n" +
    "ASTBn7fw7P61crAO48BvRT90z6Q=   http://www.test.com/blah/foo/2/c.txt\n" +
    "V73T8Z0QqJoWgsX++DGDRcpE0qA=   http://www.test.com/blah/foo/2/d.txt\n" +
    "oBTGK4E/0nq3aECsA3IxDF5eWhM=   http://www.test.com/blah/foo/3\n" +
    "x017Anoxzp+IWsrRBtQyBDiytps=   http://www.test.com/blah/foo/3/a.html\n" +
    "QJNf5ePFKC9tBrWs7b2QmxLeP8A=   http://www.test.com/blah/foo/3/b.html\n";

  public void testV3() throws Exception {
    MockArchivalUnit mau = setupContentTree();
    SimpleHasher hasher = new SimpleHasher(getMessageDigest(HASH_ALG),
					   challenge, verifier);
    File blockFile = FileTestUtil.tempFile("hashtest", ".tmp");
    hasher.doV3Hash(mau.getAuCachedUrlSet(), blockFile, "# comment 17");
    assertEquals(2282, hasher.getBytesHashed());
    assertEquals(12, hasher.getFilesHashed());
    assertEquals(exp1, StringUtil.fromFile(blockFile));
  }
  
}
