/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.projmuse;

import java.util.Arrays;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.test.*;


public class TestProjectMuseUrlNormalizer extends LockssTestCase {

  protected UrlNormalizer norm;
  
  protected MockArchivalUnit mauHttp;
  
  protected MockArchivalUnit mauHttps;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    norm = new ProjectMuseUrlNormalizer();
    String[] stems = {"http://www.example.com","https://www.example.com/"};
    mauHttp = new MockArchivalUnit();
    mauHttp.setUrlStems(Arrays.asList(stems));
    mauHttp.setConfiguration(ConfigurationUtil.fromArgs(ConfigParamDescr.BASE_URL.getKey(),
                                                        "http://www.example.com/"));
    mauHttp.setUrlStems(Arrays.asList(stems));
    mauHttps = new MockArchivalUnit();
    mauHttps.setUrlStems(Arrays.asList(stems));
    mauHttps.setConfiguration(ConfigurationUtil.fromArgs(ConfigParamDescr.BASE_URL.getKey(),
                                                         "https://www.example.com/"));
  }

  public void testBaseUrl() throws Exception {
    // Null
    assertNull(norm.normalizeUrl(null, mauHttp));
    assertNull(norm.normalizeUrl(null, mauHttps));
    // Off-site URL stays the same
    assertEquals("http://other.example.com/favicon.ico",
                 norm.normalizeUrl("http://other.example.com/favicon.ico", mauHttp));
    assertEquals("https://other.example.com/favicon.ico",
                 norm.normalizeUrl("https://other.example.com/favicon.ico", mauHttps));
    // On-site HTTP URL stays the same
    assertEquals("http://www.example.com/favicon.ico",
                 norm.normalizeUrl("http://www.example.com/favicon.ico", mauHttp));
    // On-site HTTPS URL stays the same
    assertEquals("https://www.example.com/favicon.ico",
                 norm.normalizeUrl("https://www.example.com/favicon.ico", mauHttps));
    // Normalized to HTTP/S if base URL protocol does not match
    assertEquals("http://www.example.com/favicon.ico",
                 norm.normalizeUrl("https://www.example.com/favicon.ico", mauHttp));
    assertEquals("https://www.example.com/favicon.ico",
                 norm.normalizeUrl("http://www.example.com/favicon.ico", mauHttps));
  }
  
  public void testSuffix() throws Exception {
    // HTTP base URL: trim suffix and normalize HTTPS to HTTP
    assertEquals("http://www.example.com/foo.css",
                 norm.normalizeUrl("http://www.example.com/foo.css?v=1.1", mauHttp));
    assertEquals("http://www.example.com/foo.css",
                 norm.normalizeUrl("https://www.example.com/foo.css?v=1.1", mauHttp));
    // HTTPS base URL: trim suffix and normalize HTTP to HTTPS
    assertEquals("https://www.example.com/foo.css",
                 norm.normalizeUrl("http://www.example.com/foo.css?v=1.1", mauHttps));
    assertEquals("https://www.example.com/foo.css",
                 norm.normalizeUrl("https://www.example.com/foo.css?v=1.1", mauHttps));
  }

}
