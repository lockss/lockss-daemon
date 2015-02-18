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

public class TestArchiveFileTypes extends LockssTestCase {

  ArchiveFileTypes aft;

  public void setUp() {
    aft = ArchiveFileTypes.DEFAULT;
  }

  public void testGetFromUrl() throws Exception {
    assertEquals(".zip", aft.getFromUrl("http://example.com/path/foo.zip"));
    assertEquals(".zip", aft.getFromUrl("http://example.com/path/foo.Zip"));
    assertEquals(".zip", aft.getFromUrl("http://example.com/path/foo.zip#ref"));
    assertEquals(".tgz", aft.getFromUrl("http://example.com/path/foo.tgz"));
    assertEquals(".tar.gz", aft.getFromUrl("http://example.com/path/foo.tar.gz"));
    assertEquals(".tar.gz", aft.getFromUrl("http://example.com/path/foo.TAR.GZ"));
  }

  public void testGetFromMime() throws Exception {
    assertEquals(".zip", aft.getFromMime("application/zip"));
    assertEquals(".zip", aft.getFromMime("APPLICATION/zip"));
    assertEquals(".tar", aft.getFromMime("application/x-gtar"));
    assertEquals(".tar", aft.getFromMime("application/x-tar"));
  }

  void setProps(MockCachedUrl mcu, Properties props) {
    mcu.setProperties(CIProperties.fromProperties(props));
  }

  public void testGetFromCu() throws Exception {
    MockCachedUrl mcu = new MockCachedUrl("http://example.com/path/foo.zip");
    assertEquals(".zip", aft.getFromCu(mcu));
    // content type should take precedence
    setProps(mcu, PropUtil.fromArgs(CachedUrl.PROPERTY_CONTENT_TYPE,
				    "application/x-tar"));
    assertEquals(".tar", aft.getFromCu(mcu));

    assertNull(aft.getFromCu(new MockCachedUrl("http://example.com/foo.html")));
  }

  static final Map<String,String> PLUG_MAP =
    MapUtil.fromList(ListUtil.list(".zip", ".zipity",
				   "application/jar", ".jar",
				   "text/gibberish", ".random"));
  

  public void testGetFromAuCu() throws Exception {
    MockArchivalUnit mau = new MockArchivalUnit();
    mau.setArchiveFileTypes(new ArchiveFileTypes(PLUG_MAP));
    MockCachedUrl mcu =
      new MockCachedUrl("http://example.com/path/foo.zip", mau);
    assertEquals(".zipity", ArchiveFileTypes.getArchiveExtension(mcu));
    // content type should take precedence
    setProps(mcu, PropUtil.fromArgs(CachedUrl.PROPERTY_CONTENT_TYPE,
				    "application/jar"));
    assertEquals(".jar", ArchiveFileTypes.getArchiveExtension(mcu));

    MockCachedUrl mcu2 = new MockCachedUrl("http://example.com/foo.html", mau);
    assertNull(ArchiveFileTypes.getArchiveExtension(mcu2));
  }

}
