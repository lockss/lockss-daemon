/*
 * $Id: TestIngentaUrlNoramlizer.java,v 1.2 2009-08-19 22:26:14 thib_gc Exp $
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

package org.lockss.plugin.ingenta;

import org.lockss.plugin.UrlNormalizer;
import org.lockss.test.LockssTestCase;

public class TestIngentaUrlNoramlizer extends LockssTestCase {

  public void testNormalizer() throws Exception {
    UrlNormalizer normalizer = new IngentaUrlNormalizer();
    assertEquals("http://www.example.com/content/bpsoc/bjp/2004/00000095/00000002/art00003",
                 normalizer.normalizeUrl("http://www.example.com/content/bpsoc/bjp/2004/00000095/00000002/art00003", null));
    assertEquals("http://www.example.com/content/bpsoc/bjp/2004/00000095/00000002/art00003",
                 normalizer.normalizeUrl("http://www.example.com/content/bpsoc/bjp/2004/00000095/00000002/art00003;jsessionid=18t24vno4f29p.alice", null));
    assertEquals("http://www.example.com/content/bpsoc/bjp/2004/00000095/00000002/art00003?format=print&view=popup",
                 normalizer.normalizeUrl("http://www.example.com/content/bpsoc/bjp/2004/00000095/00000002/art00003;jsessionid=18t24vno4f29p.alice?format=print&view=popup", null));
  }
  
}
