/*
 * $Id: TestIngentaUrlNormalizer.java,v 1.1 2009-10-07 23:37:22 thib_gc Exp $
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

import org.lockss.plugin.*;
import org.lockss.test.*;

public class TestIngentaUrlNormalizer extends LockssTestCase {

  protected UrlNormalizer normalizer;
  
  protected MockArchivalUnit au;
  
  public void setUp() throws Exception {
    au = new MockArchivalUnit();
    au.setConfiguration(ConfigurationUtil.fromArgs("base_url", "http://www.example.com/",
                                                   "api_url", "http://api.example.com/"));
    normalizer = new IngentaUrlNormalizer();
  }
  
  public void testJsessionid() throws Exception {
    assertEquals("http://www.example.com/content/bpsoc/bjp/2004/00000095/00000002/art00003",
                 normalizer.normalizeUrl("http://www.example.com/content/bpsoc/bjp/2004/00000095/00000002/art00003", au));
    assertEquals("http://www.example.com/content/bpsoc/bjp/2004/00000095/00000002/art00003",
                 normalizer.normalizeUrl("http://www.example.com/content/bpsoc/bjp/2004/00000095/00000002/art00003;jsessionid=18t24vno4f29p.alice", au));
    assertEquals("http://www.example.com/content/bpsoc/bjp/2004/00000095/00000002/art00003?format=print&view=popup",
                 normalizer.normalizeUrl("http://www.example.com/content/bpsoc/bjp/2004/00000095/00000002/art00003;jsessionid=18t24vno4f29p.alice?format=print&view=popup", au));
  }
  
  public void testOneTimeUrls() throws Exception {
    assertEquals("http://api.example.com/content/publi/jour/2005/00000044/00000003/art00001?crawler=true&mimetype=text/html",
                 normalizer.normalizeUrl("http://www.example.com/search/download?pub=infobike%3a%2f%2fpubli%2fjour%2f2005%2f00000044%2f00000003%2fart00001&mimetype=text%2fhtml&exitTargetId=1234567890123", au));
    assertEquals("http://api.example.com/content/publi/jour/2002/00000009/00000001/art00003?crawler=true&mimetype=application/pdf",
                 normalizer.normalizeUrl("http://www.example.com/search/download?pub=infobike%3a%2f%2fpubli%2fjour%2f2002%2f00000009%2f00000001%2fart00003&mimetype=application%2fpdf", au));
  }
  
  public void testCaseInsensitive() throws Exception {
    assertEquals("http://api.example.com/content/publi/jour/2005/00000044/00000003/art00001?crawler=true&mimetype=text/html",
        normalizer.normalizeUrl("http://www.example.com/search/download?pub=infobike%3A%2F%2Fpubli%2Fjour%2F2005%2F00000044%2F00000003%2Fart00001&mimetype=text%2Fhtml&exitTargetId=1234567890123", au));
  }
  
}
