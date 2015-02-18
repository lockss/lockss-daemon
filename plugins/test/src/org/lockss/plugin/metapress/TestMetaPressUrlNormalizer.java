/*
 * $Id$
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.metapress;

import org.lockss.plugin.UrlNormalizer;
import org.lockss.test.LockssTestCase;

public class TestMetaPressUrlNormalizer extends LockssTestCase {

  public void testNormalizeQuery() {
    // Null and empty string
    assertEquals("",
                 MetaPressUrlNormalizer.normalizeQuery(null));
    assertEquals("",
                 MetaPressUrlNormalizer.normalizeQuery(""));
    // No modification is expected
    assertEquals("ok1=good1",
                 MetaPressUrlNormalizer.normalizeQuery("ok1=good1"));
    assertEquals("ok1=good1&ok2=good2",
                 MetaPressUrlNormalizer.normalizeQuery("ok1=good1&ok2=good2"));
    // Queries beginning with sortorder=... are usually all disqualified and sortorder is removed
    assertEquals("",
                 MetaPressUrlNormalizer.normalizeQuery("sortorder=something"));
    assertEquals("",
                 MetaPressUrlNormalizer.normalizeQuery("sortorder=something&ok1=good1"));
    assertEquals("",
        MetaPressUrlNormalizer.normalizeQuery("sortorder=something&p_o=0"));
    assertEquals("p_o=10",
        MetaPressUrlNormalizer.normalizeQuery("sortorder=something&p_o=10"));
    assertEquals("ok1=good1",
                 MetaPressUrlNormalizer.normalizeQuery("ok1=good1&sortorder=something"));
    // Removal of p=..., pi=..., p_o=0..., mark=..., sw=...
    // is expected, in any order or combination
    assertEquals("",
                 MetaPressUrlNormalizer.normalizeQuery("p=badp"));
    assertEquals("",
                 MetaPressUrlNormalizer.normalizeQuery("pi=badpi"));
    assertEquals("",
                 MetaPressUrlNormalizer.normalizeQuery("p_o=0"));
    assertEquals("p_o=okayp_o",
                 MetaPressUrlNormalizer.normalizeQuery("p_o=okayp_o"));
    assertEquals("",
                 MetaPressUrlNormalizer.normalizeQuery("mark=badmark"));
    assertEquals("",
                 MetaPressUrlNormalizer.normalizeQuery("sw=badsw"));
    assertEquals("",
                 MetaPressUrlNormalizer.normalizeQuery("p=badp&pi=badpi&p_o=0&mark=badmark&sw=badsw"));
    assertEquals("",
                 MetaPressUrlNormalizer.normalizeQuery("sw=badsw&mark=badmark&p_o=0&pi=badpi&p=badp"));
    assertEquals("ok1=good1&ok2=good2&ok3=good3&ok4=good4&ok5=good5&ok6=good6",
                 MetaPressUrlNormalizer.normalizeQuery("ok1=good1&p=badp&ok2=good2&pi=badpi&ok3=good3&p_o=0&ok4=good4&mark=badmark&ok5=good5&sw=badsw&ok6=good6"));
    // Pathological
    assertEquals("",
                 MetaPressUrlNormalizer.normalizeQuery("p="));
    assertEquals("ok1=good1",
                 MetaPressUrlNormalizer.normalizeQuery("p=&ok1=good1"));
    assertEquals("ok1=good1",
                 MetaPressUrlNormalizer.normalizeQuery("p=&&ok1=good1"));
    assertEquals("ok1=good1",
                 MetaPressUrlNormalizer.normalizeQuery("&ok1=good1"));
    assertEquals("ok1=good1",
                 MetaPressUrlNormalizer.normalizeQuery("ok1=good1&"));
  }
  
  public void testNormalizeUrl() throws Exception {
    UrlNormalizer normalizer = new MetaPressUrlNormalizer();
    assertEquals("http://www.example.com/foo",
                 normalizer.normalizeUrl("http://www.example.com/foo", null));
    assertEquals("http://www.example.com/foo",
                 normalizer.normalizeUrl("http://www.example.com/foo?", null));
    assertEquals("http://www.example.com/foo?ok1=good1",
                 normalizer.normalizeUrl("http://www.example.com/foo?ok1=good1", null));
    assertEquals("http://www.example.com/foo?p_o=ok_p_o",
        normalizer.normalizeUrl("http://www.example.com/foo?p=badp&pi=badpi&p_o=ok_p_o&mark=badmark&sw=badsw", null));
    assertEquals("http://www.example.com/foo",
                 normalizer.normalizeUrl("http://www.example.com/foo?p=badp&pi=badpi&mark=badmark&sw=badsw", null));
    assertEquals("http://www.example.com/foo?ok1=good1&ok2=good2&ok3=good3&ok4=good4&ok5=good5&ok6=good6",
                 normalizer.normalizeUrl("http://www.example.com/foo?ok1=good1&p=badp&ok2=good2&pi=badpi&ok3=good3&ok4=good4&mark=badmark&ok5=good5&sw=badsw&ok6=good6", null));
  }
  
}
