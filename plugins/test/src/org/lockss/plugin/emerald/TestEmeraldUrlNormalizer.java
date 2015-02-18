/*
 * $Id$
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.emerald;

import org.lockss.plugin.UrlNormalizer;
import org.lockss.test.LockssTestCase;

public class TestEmeraldUrlNormalizer extends LockssTestCase {

  protected UrlNormalizer normalizer;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    normalizer = new EmeraldUrlNormalizer();
  }

  public void testNormalizeUrl() throws Exception {
    assertEquals("http://www.example.com/page.do?foo=bar",
                 normalizer.normalizeUrl("http://www.example.com/page.do?foo=bar",
                                         null));
    assertEquals("http://www.example.com/page.do?foo=bar",
                 normalizer.normalizeUrl("http://www.example.com/page.do;jsessionid=123456789ABCDEF0123456789ABCDEF0?foo=bar",
                                         null));
    assertEquals("http://www.example.com/page.do",
                 normalizer.normalizeUrl("http://www.example.com/page.do;jsessionid=123456789ABCDEF0123456789ABCDEF0",
                                         null));
    assertEquals("http://www.example.com/page.do;jsessionid=0FEDCBA9876543210FEDCBA987654321?foo=bar",
                 normalizer.normalizeUrl("http://www.example.com/page.do;jsessionid=123456789ABCDEF0123456789ABCDEF0;jsessionid=0FEDCBA9876543210FEDCBA987654321?foo=bar",
                                         null));
    assertEquals("http://www.emeraldinsight.com/journals.htm?issn=1757-0972&volume=4&issue=3",
            normalizer.normalizeUrl("http://www.emeraldinsight.com/journals.htm?issn=1757-0972&volume=4&issue=3&PHPSESSID=4nf97ud2ko8qoj6caeerpa2ek6",
                                    null));
	
  }

}
