/*
 * $Id: TestPsychiatryOnlineUrlNormalizer.java,v 1.2 2009-03-04 21:29:55 thib_gc Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.psychiatryonline;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.test.LockssTestCase;

public class TestPsychiatryOnlineUrlNormalizer extends LockssTestCase {

  protected UrlNormalizer urlNormalizer;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    urlNormalizer = new PsychiatryOnlineUrlNormalizer();
  }

  public void testNormalization() throws PluginException {
    assertEquals("http://www.example.com/foo.html?param1=value1&param2=value2",
                 urlNormalizer.normalizeUrl("http://www.example.com/foo.html?param1=value1&param2=value2", null));
    assertEquals("http://www.example.com/foo.html?param1=value1&param2=value2",
                 urlNormalizer.normalizeUrl(" http://\t\t\twww.example.com/\n\rfoo.html\t\t\t?\t\t\tparam1=value1\r\r\r&\n\n\nparam2=value2 ", null));
  }

}
