/*
 * $Id: TestFilterUtil.java,v 1.3.36.1 2009-11-03 23:44:55 edwardsb1 Exp $
 */

/*

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

package org.lockss.filter;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.daemon.*;

/**
 * Test class for org.lockss.filter.FilterUtil
 */
public class TestFilterUtil extends LockssTestCase {

  public void testGetReader() {
    InputStream in = new StringInputStream("123");
    InputStreamReader rdr;
    Charset def = Charset.forName(Constants.DEFAULT_ENCODING);
    Charset utf = Charset.forName("UTF-8");
    rdr = (InputStreamReader)FilterUtil.getReader(in, null);
    assertTrue(def.aliases().contains(rdr.getEncoding()));
    rdr = (InputStreamReader)FilterUtil.getReader(in, "ISO-8859-1");
    assertTrue(def.aliases().contains(rdr.getEncoding()));
    rdr = (InputStreamReader)FilterUtil.getReader(in, "utf-8");
    assertTrue(utf.aliases().contains(rdr.getEncoding()));
    rdr = (InputStreamReader)FilterUtil.getReader(in, "NoSuchCharset");
    assertTrue(def.aliases().contains(rdr.getEncoding()));
  }

  public void testGetReaderShortcut() throws Exception {
    StringReader rdr = new StringReader("foo");
    ReaderInputStream in = new ReaderInputStream(rdr);
    Reader r2 = FilterUtil.getReader(in, null);
    assertSame(rdr, r2);
    assertReaderMatchesString("foo", r2);
  }

  public void testGetCrawlFilteredStream() {
    String mime = "text/html";
    MyMockArchivalUnit mau = new MyMockArchivalUnit();
    InputStream in = new MockInputStream();
    assertSame(in, FilterUtil.getCrawlFilteredStream(mau, in, null, mime));
    MyFiltFact fact = new MyFiltFact();
    mau.setCrawlFilterFactory(fact);
    MyFilterInputStream filtIn =
      (MyFilterInputStream)FilterUtil.getCrawlFilteredStream(mau, in,
							     null, mime);
    assertEquals(mau, fact.au);
    assertEquals(mime, mau.crawlFiltMime);
    assertNotSame(filtIn, in);
    assertSame(in, filtIn.in);
  }

  private static class MyFiltFact implements FilterFactory {
    ArchivalUnit au;
    String encoding;
    public InputStream createFilteredInputStream(ArchivalUnit au,
						 InputStream in,
						 String encoding)
	throws PluginException {
      this.au = au;
      this.encoding = encoding;
      return new MyFilterInputStream(in);
    }
  }

  private static class MyFilterInputStream extends FilterInputStream {
    InputStream in;
    MyFilterInputStream(InputStream in) {
      super(in);
      this.in = in;
    }
    InputStream getWrappedStream() {
      return in;
    }
  }

  private static class MyMockArchivalUnit extends MockArchivalUnit {
    String crawlFiltMime = null;

    public FilterFactory getCrawlFilterFactory(String contentType) {
      crawlFiltMime = contentType;
      return super.getCrawlFilterFactory(contentType);
    }
  }

}
