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

import org.lockss.test.*;

/**
 * <p>
 * Unit tests of {@link HttpHttpsParamUrlNormalizer}.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.75.4
 * @see HttpHttpsParamUrlNormalizer
 */
public class TestHttpHttpsParamUrlNormalizer extends LockssTestCase {

  /**
   * <p>
   * Tests a typical {@link HttpHttpsParamUrlNormalizer}.
   * </p>
   * 
   * @throws Exception if an error occurs.
   */
  public void testHttpHttpsParamUrlNormalizer() throws Exception {
    MockArchivalUnit au = new MockArchivalUnit();
    au.setConfiguration(ConfigurationUtil.fromArgs("foo_url", "http://foo.example.com/",
                                                   "bar_url", "https://bar.example.com/"));
    UrlNormalizer norm = new HttpHttpsParamUrlNormalizer("foo_url", "bar_url");
    // URLs from foo.example.com should all be normalized to http://
    assertEquals("http://foo.example.com/some/path.html",
                 norm.normalizeUrl("http://foo.example.com/some/path.html", au));
    assertEquals("http://foo.example.com/some/path.html",
                 norm.normalizeUrl("https://foo.example.com/some/path.html", au));
    // URLs from bar.example.com should all be normalized to https://
    assertEquals("https://bar.example.com/some/path.html",
                 norm.normalizeUrl("http://bar.example.com/some/path.html", au));
    assertEquals("https://bar.example.com/some/path.html",
                 norm.normalizeUrl("https://bar.example.com/some/path.html", au));
    // URLs from other hosts (e.g. www.example.com) should be unchanged
    assertEquals("http://www.example.com/some/path.html",
                 norm.normalizeUrl("http://www.example.com/some/path.html", au));
    assertEquals("https://www.example.com/some/path.html",
                 norm.normalizeUrl("https://www.example.com/some/path.html", au));
  }

}
