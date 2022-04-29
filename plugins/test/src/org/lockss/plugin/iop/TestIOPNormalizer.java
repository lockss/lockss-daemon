/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.iop;

import org.lockss.test.*;

public class TestIOPNormalizer extends LockssTestCase {


  public void testReturnsNullForNullURL() {
    IOPNormalizer norm = new IOPNormalizer();
    assertNull(norm.normalizeUrl(null, null));
  }

  private void assertUrlsNormalize(String unNormUrl, String normUrl) {
    IOPNormalizer norm = new IOPNormalizer();
    assertEquals(normUrl, norm.normalizeUrl(unNormUrl, null));
  }

  public void testNormalizeIssueTOC() {
    assertUrlsNormalize("http://www.iop.org/EJ/S/3/418/SXFqtUP87MBWu6AwxR,8Hw/toc/0266-5611/20/6",
			"http://www.iop.org/EJ/S/3/418/toc/0266-5611/20/6");
  }

  public void testNormalizeArticle() {
    assertUrlsNormalize("http://www.iop.org/EJ/S/3/418/z3ojEA0Y1tlY8SOh74g3mQ/abstract/0266-5611/20/6/E01",
			"http://www.iop.org/EJ/S/3/418/abstract/0266-5611/20/6/E01");
  }

  public void testNormalizeIgnoresOtherHosts() {
    assertUrlsNormalize("http://www.example.com/EJ/S/3/418/z3ojEA0Y1tlY8SOh74g3mQ/abstract/0266-5611/20/6/E01",
			"http://www.example.com/EJ/S/3/418/z3ojEA0Y1tlY8SOh74g3mQ/abstract/0266-5611/20/6/E01");
  }

  public void testNormalizeShortUrls() {
    System.err.println("Stop1");
    assertUrlsNormalize("http://www.iop.org/EJ/",
			"http://www.iop.org/EJ/");
  }


}
