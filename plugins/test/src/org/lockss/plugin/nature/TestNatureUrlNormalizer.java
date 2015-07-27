/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.nature;

import org.lockss.plugin.UrlNormalizer;
import org.lockss.test.LockssTestCase;
/*
 * UrlNormalizer removes "?message=remove"
 * http://www.nature.com/onc/journal/v29/n37/full/onc2010273a.html?message=remove
 */
public class TestNatureUrlNormalizer extends LockssTestCase {

  public void testUrlNormalizerMessageRemoval() throws Exception { 
    UrlNormalizer normalizer = new NaturePublishingGroupUrlNormalizer();
    assertEquals("http://www.nature.com/onc/journal/v29/n37/full/onc2010273a.html",
    			 normalizer.normalizeUrl("http://www.nature.com/onc/journal/v29/n37/full/onc2010273a.html", null));
    assertEquals("http://www.nature.com/onc/journal/v29/n37/full/onc2010273a.html",
        normalizer.normalizeUrl("http://www.nature.com/onc/journal/v29/n37/full/onc2010273a.html?message=remove", null));
    assertEquals("http://www.nature.com/onc/journal/v29/n37/full/onc2010273a.html",
        normalizer.normalizeUrl("http://www.nature.com/onc/journal/v29/n37/full/onc2010273a.html?message-global=remove", null));
    assertEquals("http://www.nature.com/onc/journal/v29/n37/full/onc2010273a.html?message-foo=remove",
        normalizer.normalizeUrl("http://www.nature.com/onc/journal/v29/n37/full/onc2010273a.html?message-foo=remove", null));
    assertEquals("http://www.nature.com/onc/journal/v29/n37/full/onc2010273a.html?other=blah",
        normalizer.normalizeUrl("http://www.nature.com/onc/journal/v29/n37/full/onc2010273a.html?other=blah", null));
    //in the middle of the argument list
    assertEquals("http://www.nature.com/am/journal/v5/n9/suppinfo/am201343s1.html?url=/am/journal/v5/n9/abs/am201343a.html",
        normalizer.normalizeUrl("http://www.nature.com/am/journal/v5/n9/suppinfo/am201343s1.html?message=remove&url=/am/journal/v5/n9/abs/am201343a.html",null));
    // not seen yet but being proactive
    assertEquals("http://www.nature.com/am/journal/v5/n9/suppinfo/am201343s1.html?url=/am/journal/v5/n9/abs/am201343a.html&arg=foo",
        normalizer.normalizeUrl("http://www.nature.com/am/journal/v5/n9/suppinfo/am201343s1.html?url=/am/journal/v5/n9/abs/am201343a.html&message=remove&arg=foo",null));
    // what if it was at the end, but not the only argument
    assertEquals("http://www.nature.com/am/journal/v5/n9/suppinfo/am201343s1.html?url=/am/journal/v5/n9/abs/am201343a.html&arg=foo",
        normalizer.normalizeUrl("http://www.nature.com/am/journal/v5/n9/suppinfo/am201343s1.html?url=/am/journal/v5/n9/abs/am201343a.html&arg=foo&message=remove",null));

  }
  public void testUrlNormalizerTOCindex() throws Exception { 
    UrlNormalizer normalizer = new NaturePublishingGroupUrlNormalizer();
    // article still works
    assertEquals("http://www.nature.com/onc/journal/v29/n37/full/onc2010273a.html",
        normalizer.normalizeUrl("http://www.nature.com/onc/journal/v29/n37/full/onc2010273a.html", null));
    // a correct toc still works
    assertEquals("http://www.nature.com/onc/journal/v29/n37/index.html",
        normalizer.normalizeUrl("http://www.nature.com/onc/journal/v29/n37/index.html", null));
    // correct the incomplete toc url
    assertEquals("http://www.nature.com/onc/journal/v29/n37/index.html",
        normalizer.normalizeUrl("http://www.nature.com/onc/journal/v29/n37", null));
  }

  
}
