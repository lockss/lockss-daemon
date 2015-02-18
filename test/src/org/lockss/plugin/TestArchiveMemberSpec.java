/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin;

import java.util.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestArchiveMemberSpec extends LockssTestCase {
  MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    mau = new MockArchivalUnit();
    mau.setArchiveFileTypes(ArchiveFileTypes.DEFAULT);
  }

  ArchiveMemberSpec fromUrl(ArchivalUnit au, String url) {
    return ArchiveMemberSpec.fromUrl(au, url);
  }

  void assertUrlMemb(String expUrl, String expMemb, ArchiveMemberSpec ams) {
    assertEquals(expUrl, ams.getUrl());
    assertEquals(expMemb, ams.getName());
  }

  void testAuConditional(String url, String memb) {
    MockArchivalUnit mmm = new MockArchivalUnit();
    String membUrl = url + "!/" + memb;
    assertNull(ArchiveMemberSpec.fromUrl(mmm, membUrl));
    mmm.setArchiveFileTypes(ArchiveFileTypes.DEFAULT);
    assertNotNull(ArchiveMemberSpec.fromUrl(mmm, membUrl));
  }

  void assertParse(String url, String memb) {
    String membUrl = url + "!/" + memb;
    ArchiveMemberSpec ams1 = ArchiveMemberSpec.fromUrl(mau, membUrl);
    assertEquals(url, ams1.getUrl());
    assertEquals(memb, ams1.getName());
    assertEquals(membUrl, ams1.toUrl());

    CachedUrl cu = new MockCachedUrl(url);
    ArchiveMemberSpec ams2 = ArchiveMemberSpec.fromCu(cu, memb);
    assertEquals(url, ams2.getUrl());
    assertEquals(memb, ams2.getName());
    assertEquals(membUrl, ams2.toUrl());
  }

  // Ensure URL normalization doesn't URL-encode the member separater
  public void testAssumptions() throws Exception {
    String u = "http://foo.bar/name.zip!/path/memb.html";
    assertEquals(u, UrlUtil.normalizeUrl(u));
  }

  public void testParse() throws Exception {
    assertParse("http://foo.bar/name.zip", "path/memb.html");
    assertParse("http://foo.bar:8000/path/name.tgz",
		"dir/bar.zip/zap/memb.html");
  }

}
