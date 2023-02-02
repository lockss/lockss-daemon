/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.psychiatryonline;

import org.lockss.plugin.UrlNormalizer;
import org.lockss.test.LockssTestCase;

public class TestPsychiatryOnlineUrlNormalizer extends LockssTestCase {

  protected UrlNormalizer urlNormalizer;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    urlNormalizer = new PsychiatryOnlineUrlNormalizer();
  }

  public void testNormalization() throws Exception {
    assertEquals("http://www.example.com/foo.html?param1=value1&param2=value2",
                 urlNormalizer.normalizeUrl("http://www.example.com/foo.html?param1=value1&param2=value2", null));
    assertEquals("http://www.example.com/foo.html?param1=value1&param2=value2",
                 urlNormalizer.normalizeUrl("   \t\thttp://\t\t\twww.example.com/\n\rfoo.html\t\t\t?\t\t\tparam1=value1\r\r\r&\n\n\nparam2=value2\n\n\t\t   ", null));
  }

}
