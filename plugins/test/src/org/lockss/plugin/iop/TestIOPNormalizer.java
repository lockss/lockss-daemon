/*
 * $Id$
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
