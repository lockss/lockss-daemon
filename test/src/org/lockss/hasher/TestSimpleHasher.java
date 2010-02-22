/*
 * $Id: TestSimpleHasher.java,v 1.5.6.1 2010-02-22 06:44:10 tlipkis Exp $
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
import org.lockss.filter.*;
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

  MockArchivalUnit mau = null;

  public void setUp() throws Exception {
    super.setUp();
    mau = new MockArchivalUnit(new MockPlugin());
  }

  MockArchivalUnit setupContentTree() {
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

  String exp =
    "# comment 17\n" +
    "DA39A3EE5E6B4B0D3255BFEF95601890AFD80709   http://www.test.com/blah/\n" +
    "CA44A2EE70C871B73DAAB1ABD23CE8EDF6CDBA33   http://www.test.com/blah/x.html\n" +
    "1018215CC5D8B604B3238F1E08141145D536BDFC   http://www.test.com/blah/foo/\n" +
    "DF88B88A0DCC1CBE4157790E24E903E653105F4F   http://www.test.com/blah/foo/1\n" +
    "621A46465EAF1ED35DCB88CD8F6ED39471A210F7   http://www.test.com/blah/foo/2\n" +
    "572F349D47A3C67294B0BEFB88014177275720E3   http://www.test.com/blah/foo/2/a.txt\n" +
    "5960969EEDB86FC43C7AE8A5A9AA0049B4AF216C   http://www.test.com/blah/foo/2/b.txt\n" +
    "0124C19FB7F0ECFEB572B00EE3C06F453F74CFA4   http://www.test.com/blah/foo/2/c.txt\n" +
    "57BDD3F19D10A89A1682C5FEF8318345CA44D2A0   http://www.test.com/blah/foo/2/d.txt\n" +
    "A014C62B813FD27AB76840AC0372310C5E5E5A13   http://www.test.com/blah/foo/3\n" +
    "C74D7B027A31CE9F885ACAD106D4320438B2B69B   http://www.test.com/blah/foo/3/a.html\n" +
    "40935FE5E3C5282F6D06B5ACEDBD909B12DE3FC0   http://www.test.com/blah/foo/3/b.html\n";

  String exp64 =
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
    mau.setHashFilterFactory(new SimpleFilterFactory());
    SimpleHasher hasher = new SimpleHasher(getMessageDigest(HASH_ALG),
					   challenge, verifier);
    File blockFile = FileTestUtil.tempFile("hashtest", ".tmp");
    hasher.doV3Hash(mau.getAuCachedUrlSet(), blockFile, "# comment 17");
    assertEquals(2282, hasher.getBytesHashed());
    assertEquals(12, hasher.getFilesHashed());
    assertEquals(exp, StringUtil.fromFile(blockFile));
  }

  public void testV364() throws Exception {
    MockArchivalUnit mau = setupContentTree();
    mau.setHashFilterFactory(new SimpleFilterFactory());
    SimpleHasher hasher = new SimpleHasher(getMessageDigest(HASH_ALG),
					   challenge, verifier);
    hasher.setBase64Result(true);
    File blockFile = FileTestUtil.tempFile("hashtest", ".tmp");
    hasher.doV3Hash(mau.getAuCachedUrlSet(), blockFile, "# comment 17");
    assertEquals(2282, hasher.getBytesHashed());
    assertEquals(12, hasher.getFilesHashed());
    assertEquals(exp64, StringUtil.fromFile(blockFile));
  }

  String expFilt =
    "# comment 17\n" +
    "DA39A3EE5E6B4B0D3255BFEF95601890AFD80709   http://www.test.com/blah/\n" +
    "CA44A2EE70C871B73DAAB1ABD23CE8EDF6CDBA33   http://www.test.com/blah/x.html\n" +
    "325A92DEB64D764F4C80B30B6EBCA1DEA68773BB   http://www.test.com/blah/foo/\n" +
    "8F61702A9CDB0CF54C4954C14A455EB147E3D340   http://www.test.com/blah/foo/1\n" +
    "69EB01F0B9C1A6DC58C05ADC62C077985C5A27D7   http://www.test.com/blah/foo/2\n" +
    "136B317433070E1355B77BBF8C0D5BC64E100E43   http://www.test.com/blah/foo/2/a.txt\n" +
    "A89EA485278644EB3E11D3C1F3974DC95E1A2F0F   http://www.test.com/blah/foo/2/b.txt\n" +
    "F69DA3BFB2B49D8B391D76A1793BFC7C15F238C2   http://www.test.com/blah/foo/2/c.txt\n" +
    "F92DBF9FE11A9AB2BC2DD670AA82EC1E75B4B682   http://www.test.com/blah/foo/2/d.txt\n" +
    "442ACC24CEADC9EA9D038B71ABDDFFC50F3C6018   http://www.test.com/blah/foo/3\n" +
    "B39CA4E43E4C2D9624A83D6D07891A18F804AEE0   http://www.test.com/blah/foo/3/a.html\n" +
    "62E6BEFE0C9A37D1124F0D4A5CC64614B0396C1C   http://www.test.com/blah/foo/3/b.html\n";

  String expFilt64 =
    "# comment 17\n" +
    "2jmj7l5rSw0yVb/vlWAYkK/YBwk=   http://www.test.com/blah/\n" +
    "ykSi7nDIcbc9qrGr0jzo7fbNujM=   http://www.test.com/blah/x.html\n" +
    "MlqS3rZNdk9MgLMLbryh3qaHc7s=   http://www.test.com/blah/foo/\n" +
    "j2FwKpzbDPVMSVTBSkVesUfj00A=   http://www.test.com/blah/foo/1\n" +
    "aesB8LnBptxYwFrcYsB3mFxaJ9c=   http://www.test.com/blah/foo/2\n" +
    "E2sxdDMHDhNVt3u/jA1bxk4QDkM=   http://www.test.com/blah/foo/2/a.txt\n" +
    "qJ6khSeGROs+EdPB85dNyV4aLw8=   http://www.test.com/blah/foo/2/b.txt\n" +
    "9p2jv7K0nYs5HXaheTv8fBXyOMI=   http://www.test.com/blah/foo/2/c.txt\n" +
    "+S2/n+EamrK8LdZwqoLsHnW0toI=   http://www.test.com/blah/foo/2/d.txt\n" +
    "RCrMJM6tyeqdA4txq93/xQ88YBg=   http://www.test.com/blah/foo/3\n" +
    "s5yk5D5MLZYkqD1tB4kaGPgEruA=   http://www.test.com/blah/foo/3/a.html\n" +
    "Yua+/gyaN9ESTw1KXMZGFLA5bBw=   http://www.test.com/blah/foo/3/b.html\n";

  public void testV3Filtered() throws Exception {
    MockArchivalUnit mau = setupContentTree();
    mau.setHashFilterFactory(new SimpleFilterFactory());
    SimpleHasher hasher = new SimpleHasher(getMessageDigest(HASH_ALG),
					   challenge, verifier);
    hasher.setFiltered(true);
    File blockFile = FileTestUtil.tempFile("hashtest", ".tmp");
    hasher.doV3Hash(mau.getAuCachedUrlSet(), blockFile, "# comment 17");
    assertEquals(2282, hasher.getBytesHashed());
    assertEquals(12, hasher.getFilesHashed());
    assertEquals(expFilt, StringUtil.fromFile(blockFile));
  }

  public void testV3Filtered64() throws Exception {
    MockArchivalUnit mau = setupContentTree();
    mau.setHashFilterFactory(new SimpleFilterFactory());
    SimpleHasher hasher = new SimpleHasher(getMessageDigest(HASH_ALG),
					   challenge, verifier);
    hasher.setFiltered(true);
    hasher.setBase64Result(true);
    File blockFile = FileTestUtil.tempFile("hashtest", ".tmp");
    hasher.doV3Hash(mau.getAuCachedUrlSet(), blockFile, "# comment 17");
    assertEquals(2282, hasher.getBytesHashed());
    assertEquals(12, hasher.getFilesHashed());
    assertEquals(expFilt64, StringUtil.fromFile(blockFile));
  }

  public class SimpleFilterFactory implements FilterFactory {
    public InputStream createFilteredInputStream(ArchivalUnit au,
						 InputStream in,
						 String encoding) {
      log.info("createFilteredInputStream");
      Reader rdr = FilterUtil.getReader(in, encoding);
      StringFilter filt = new StringFilter(rdr, "foo", "bar");
      filt.setIgnoreCase(true);
      return new ReaderInputStream(filt);
    }
  }
}
