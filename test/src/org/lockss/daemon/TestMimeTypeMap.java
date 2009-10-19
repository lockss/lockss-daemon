/*
 * $Id: TestMimeTypeMap.java,v 1.4 2009-10-19 05:27:00 tlipkis Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon;

import java.util.*;

import org.lockss.app.LockssApp;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.filter.*;
import org.lockss.extractor.*;

/**
 * This is the test class for org.lockss.daemon.MimeTypeMap
 */
public class TestMimeTypeMap extends LockssTestCase {

  MimeTypeMap map;
  MimeTypeInfo.Mutable mti;

  public void setUp() {
    map = new MimeTypeMap();
    mti = new MimeTypeInfo.Impl();
  }

  public void testGetMimeTypeInfo() {
    MimeTypeInfo mt1 = new MimeTypeInfo.Impl();
    MimeTypeInfo mt2 = new MimeTypeInfo.Impl();
    map.putMimeTypeInfo("text/html", mt1);
    map.putMimeTypeInfo("text/css", mt2);
    assertSame(mt1, map.getMimeTypeInfo("text/html"));
    assertSame(mt1, map.getMimeTypeInfo("text/HTML; charset=illegible"));
    assertSame(mt2, map.getMimeTypeInfo("text/CSS; charset=illegible"));
    assertNull(map.getMimeTypeInfo("text/plaim; charset=illegible"));
  }

  public void testInherit() {
    MimeTypeMap child = new MimeTypeMap(map);
    MimeTypeInfo mt1 = new MimeTypeInfo.Impl();
    MimeTypeInfo mt2 = new MimeTypeInfo.Impl();
    MimeTypeInfo mt3 = new MimeTypeInfo.Impl();
    map.putMimeTypeInfo("text/html", mt2);
    map.putMimeTypeInfo("text/css", mt3);
    assertSame(mt2, child.getMimeTypeInfo("text/html"));
    assertSame(mt3, child.getMimeTypeInfo("text/css"));
    child.putMimeTypeInfo("text/html", mt1);
    assertSame(mt1, child.getMimeTypeInfo("text/html"));
    assertSame(mt2, map.getMimeTypeInfo("text/html"));
  }

  public void testDefault() {
    MimeTypeInfo mt1 = MimeTypeMap.DEFAULT.getMimeTypeInfo("text/html");
    assertNull(mt1.getHashFilterFactory());
    assertNull(mt1.getCrawlFilterFactory());
    assertTrue(mt1.getLinkExtractorFactory()
	       instanceof GoslingHtmlLinkExtractor.Factory);

    MimeTypeInfo mt2 = MimeTypeMap.DEFAULT.getMimeTypeInfo("text/css");
    assertNull(mt2.getHashFilterFactory());
    assertNull(mt2.getCrawlFilterFactory());
    assertTrue(mt2.getLinkExtractorFactory()
	       instanceof CssLinkExtractor.Factory);
  }

  public void testModifyMimeTypeInfo() {
    MimeTypeMap DEFAULT = MimeTypeMap.DEFAULT;
    MimeTypeMap child = new MimeTypeMap(DEFAULT);
    MimeTypeInfo.Mutable mt1 = child.modifyMimeTypeInfo("text/html");
    assertTrue(DEFAULT.getMimeTypeInfo("TEXT/html").getLinkExtractorFactory()
	       instanceof GoslingHtmlLinkExtractor.Factory);
    assertTrue(mt1.getLinkExtractorFactory()
	       instanceof GoslingHtmlLinkExtractor.Factory);
    LinkExtractorFactory uf = new MockLinkExtractorFactory();
    mt1.setLinkExtractorFactory(uf);
    assertTrue(DEFAULT.getMimeTypeInfo("TEXT/html").getLinkExtractorFactory()
	       instanceof GoslingHtmlLinkExtractor.Factory);
    assertEquals(uf, mt1.getLinkExtractorFactory());
  }
}
