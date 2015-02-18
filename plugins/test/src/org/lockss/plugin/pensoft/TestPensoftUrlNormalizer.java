/* $Id$

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

package org.lockss.plugin.pensoft;

import org.lockss.plugin.UrlNormalizer;
import org.lockss.test.LockssTestCase;
/*
 * UrlNormalizer removes ?cookieSet=1 suffix
 */
public class TestPensoftUrlNormalizer extends LockssTestCase {
  private static final String urlStr1 = "http://www.pensoft.net/journals/neobiota/article/2060/modelling-the-distribution-of-the-invasive-roesels-bush-cricket-metrioptera-roeselii-in-a-fragmented-landscape";
  private static final String urlStr2 = "http://www.pensoft.net/journals/neobiota/article/1803/plant-pathogens-as-biocontrol-agents-of-cirsium-arvense--an-overestimated-approach";
  private static final String resultStr1 = "http://www.pensoft.net/journals/neobiota/article/2060/modelling-the-distribution-of-the-invasive-roesel%13s-bush-cricket-metrioptera-roeselii-in-a-fragmented-landscape";
  private static final String resultStr2 = "http://www.pensoft.net/journals/neobiota/article/1803/plant-pathogens-as-biocontrol-agents-of-cirsium-arvense-%03-an-overestimated-approach";
  public void testUrlNormalizer() throws Exception { 
    UrlNormalizer normalizer = new PensoftUrlNormalizer();
    assertEquals(resultStr1, normalizer.normalizeUrl(urlStr1, null));
    assertEquals(resultStr2, normalizer.normalizeUrl(urlStr2, null)); 
  }
  
}
