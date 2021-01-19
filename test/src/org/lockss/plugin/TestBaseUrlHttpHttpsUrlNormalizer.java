/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.test.*;

/**
 * 
 * @author thib
 * @deprecated {@link BaseUrlHttpHttpsUrlNormalizer} is deprecated as of 1.75.4
 *             in favor of {@link HttpHttpsParamUrlNormalizer}.
 */
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
