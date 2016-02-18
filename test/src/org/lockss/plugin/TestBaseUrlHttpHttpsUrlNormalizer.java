/*
 * $Id$
 */

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

package org.lockss.plugin;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.test.*;

public class TestBaseUrlHttpHttpsUrlNormalizer extends LockssTestCase {

  public void testBaseUrlHttpHttpsUrlNormalizer() throws Exception {
    final String BASEURLKEY = ConfigParamDescr.BASE_URL.getKey();
    UrlNormalizer norm = new BaseUrlHttpHttpsUrlNormalizer();
    // HTTP AU
    MockArchivalUnit httpau = new MockArchivalUnit();
    httpau.setConfiguration(ConfigurationUtil.fromArgs(BASEURLKEY, "http://www.example.com/"));
    assertEquals("http://www.example.com/favicon.ico",
                 norm.normalizeUrl("http://www.example.com/favicon.ico",
                                   httpau));
    assertEquals("http://www.example.com/favicon.ico",
                 norm.normalizeUrl("https://www.example.com/favicon.ico",
                                   httpau));
    assertEquals("https://www.lockss.org/favicon.ico",
                 norm.normalizeUrl("https://www.lockss.org/favicon.ico",
                                   httpau));
    // HTTPS AU
    MockArchivalUnit httpsau = new MockArchivalUnit();
    httpsau.setConfiguration(ConfigurationUtil.fromArgs(BASEURLKEY, "https://www.example.com/"));
    assertEquals("https://www.example.com/favicon.ico",
                 norm.normalizeUrl("http://www.example.com/favicon.ico",
                                   httpsau));
    assertEquals("https://www.example.com/favicon.ico",
                 norm.normalizeUrl("https://www.example.com/favicon.ico",
                                   httpsau));
    assertEquals("http://www.lockss.org/favicon.ico",
                 norm.normalizeUrl("http://www.lockss.org/favicon.ico",
                                   httpsau));
  }
  
}
