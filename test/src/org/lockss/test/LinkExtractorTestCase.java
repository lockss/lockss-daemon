/*
 * $Id: LinkExtractorTestCase.java,v 1.1.8.1 2008-09-09 08:00:57 tlipkis Exp $
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

package org.lockss.test;

import java.io.*;
import java.util.*;
import org.lockss.extractor.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/** Framework for LinkExtractor tests.  Subs must implement only {@link
 * #getFactory()} and {@link #getMimeType()}, and tests which call {@link
 * #extractUrls(String)}. */
public abstract class LinkExtractorTestCase extends LockssTestCase {
  public static String URL = "http://www.example.com/";

  public static String MIME_TYPE_HTML = "text/html";
  public static String MIME_TYPE_RAM = "audio/x-pn-realaudio";

  protected LinkExtractor extractor = null;
  protected MyLinkExtractorCallback cb = null;
  protected String encoding;
  protected MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    mau = new MockArchivalUnit();
    extractor = getFactory().createLinkExtractor(getMimeType());
    cb = new MyLinkExtractorCallback();
    encoding = getEncoding();
  }

  public abstract String getMimeType();
  public abstract LinkExtractorFactory getFactory();

  public String getEncoding() {
    return Constants.DEFAULT_ENCODING;
  }

  public String getUrl() {
    return URL;
  }

  protected Collection extractUrls(String source)
      throws IOException, PluginException {
    return extractUrls(source, cb);
  }

  protected Collection extractUrls(String source,
				   MyLinkExtractorCallback callback)
      throws IOException, PluginException {
    extractUrls(source, (LinkExtractor.Callback)cb);
    return callback.getFoundUrls();
  }

  protected void extractUrls(String source,
			     LinkExtractor.Callback callback)
      throws IOException, PluginException {
    MockCachedUrl mcu = new MockCachedUrl(getUrl());
    mcu.setContent(source);

    String enc = Constants.DEFAULT_ENCODING;

    extractor.extractUrls(new MockArchivalUnit(),
			  mcu.getUnfilteredInputStream(),
			  enc, mcu.getUrl(), callback);
      
  }

  protected void extractUrls(String source, List foundUrls)
      throws IOException, PluginException {
    extractUrls(source, new MyLinkExtractorCallback(foundUrls));
  }      

  public void testEmptyFileReturnsNoLinks() throws Exception {
    assertEquals(SetUtil.set(), extractUrls(""));
  }

  public void testThrows() throws IOException, PluginException {
    try {
      extractor.extractUrls(mau, null, encoding, getUrl(), cb);
      fail("Calling extractUrls with a null InputStream should have thrown");
    } catch (IllegalArgumentException e) {
    } catch (NullPointerException e) {
    }
    try {
      extractor.extractUrls(mau, new StringInputStream("blah"), encoding,
			    getUrl(), null);
      fail("Calling extractUrls with a null callback should have thrown");
    } catch (IllegalArgumentException iae) {
    }
  }

  protected class MyLinkExtractorCallback implements LinkExtractor.Callback {
    Collection foundUrls;

    MyLinkExtractorCallback() {
      foundUrls = new HashSet();
    }

    MyLinkExtractorCallback(Collection foundUrls) {
      this.foundUrls = foundUrls;
    }

    public void foundLink(String url) {
      foundUrls.add(url);
    }

    public Collection getFoundUrls() {
      return foundUrls;
    }
  }
}
