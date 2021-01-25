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

package org.lockss.plugin.ingenta;

import org.lockss.plugin.*;
import org.lockss.test.*;

public class TestIngentaHttpHttpsUrlNormalizer extends LockssTestCase {

  protected UrlNormalizer normalizer;

  protected MockArchivalUnit au;

  public void setUp() throws Exception {
    au = new MockArchivalUnit();
    au.setConfiguration(ConfigurationUtil.fromArgs("base_url", "http://www.example.com/",
        "api_url", "http://api.example.com/"));
    normalizer = new IngentaHttpHttpsUrlNormalizer();

  }

  public void testHttpsUrls() throws Exception {
    assertEquals("http://api.example.com/content/publi/jour/2005/00000044/00000003/art00001?crawler=true&mimetype=text/html",
        normalizer.normalizeUrl("https://www.example.com/search/download?pub=infobike%3a%2f%2fpubli%2fjour%2f2005%2f00000044%2f00000003%2fart00001&mimetype=text%2fhtml&exitTargetId=1234567890123", au));
    assertEquals("http://api.example.com/content/publi/jour/2002/00000009/00000001/art00003?crawler=true",
        normalizer.normalizeUrl("https://www.example.com/search/download?pub=infobike%3a%2f%2fpubli%2fjour%2f2002%2f00000009%2f00000001%2fart00003&mimetype=application%2fpdf", au));
  }
}
