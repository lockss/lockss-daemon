/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.test.LockssTestCase;

public class TestMetapressUrlNormalizer extends LockssTestCase {

  protected MetapressUrlNormalizer norm;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.norm = new MetapressUrlNormalizer();
  }
  
  public void testNormalizeQuery() {
    // Null and empty string
    assertEquals("",
                 norm.normalizeQuery(null));
    assertEquals("",
                 norm.normalizeQuery(""));
    // No modification is expected
    assertEquals("ok1=good1",
                 norm.normalizeQuery("ok1=good1"));
    assertEquals("ok1=good1&ok2=good2",
                 norm.normalizeQuery("ok1=good1&ok2=good2"));
    // Queries beginning with sortorder=... are usually all disqualified and sortorder is removed
    assertEquals("",
                 norm.normalizeQuery("sortorder=something"));
    assertEquals("ok1=good1",
                 norm.normalizeQuery("sortorder=something&ok1=good1"));
    assertEquals("",
                 norm.normalizeQuery("sortorder=something&p_o=0"));
    assertEquals("p_o=10",
                 norm.normalizeQuery("sortorder=something&p_o=10"));
    assertEquals("ok1=good1",
                 norm.normalizeQuery("ok1=good1&sortorder=something"));
    // Removal of p=..., pi=..., p_o=0..., mark=..., sw=...
    // is expected, in any order or combination
    assertEquals("",
                 norm.normalizeQuery("p=badp"));
    assertEquals("",
                 norm.normalizeQuery("pi=badpi"));
    assertEquals("",
                 norm.normalizeQuery("p_o=0"));
    assertEquals("p_o=okayp_o",
                 norm.normalizeQuery("p_o=okayp_o"));
    assertEquals("",
                 norm.normalizeQuery("mark=badmark"));
    assertEquals("",
                 norm.normalizeQuery("sw=badsw"));
    assertEquals("",
                 norm.normalizeQuery("p=badp&pi=badpi&p_o=0&mark=badmark&sw=badsw"));
    assertEquals("",
                 norm.normalizeQuery("sw=badsw&mark=badmark&p_o=0&pi=badpi&p=badp"));
    assertEquals("ok1=good1&ok2=good2&ok3=good3&ok4=good4&ok5=good5&ok6=good6",
                 norm.normalizeQuery("ok1=good1&p=badp&ok2=good2&pi=badpi&ok3=good3&p_o=0&ok4=good4&mark=badmark&ok5=good5&sw=badsw&ok6=good6"));
    // Ordering
    assertEquals("a=1&b=2",
                 norm.normalizeQuery("a=1&b=2"));
    assertEquals("a=1&b=2",
                 norm.normalizeQuery("b=2&a=1"));
    // Pathological
    assertEquals("",
                 norm.normalizeQuery("p="));
    assertEquals("ok1=good1",
                 norm.normalizeQuery("p=&ok1=good1"));
    assertEquals("ok1=good1",
                 norm.normalizeQuery("p=&&ok1=good1"));
    assertEquals("ok1=good1",
                 norm.normalizeQuery("&ok1=good1"));
    assertEquals("ok1=good1",
                 norm.normalizeQuery("ok1=good1&"));
  }
  
  public void testNormalizeUrl() throws Exception {
    assertEquals("http://www.example.com/foo",
                 norm.normalizeUrl("http://www.example.com/foo", null));
    assertEquals("http://www.example.com/foo",
                 norm.normalizeUrl("http://www.example.com/foo?", null));
    assertEquals("http://www.example.com/foo?ok1=good1",
                 norm.normalizeUrl("http://www.example.com/foo?ok1=good1", null));
    assertEquals("http://www.example.com/foo?p_o=ok_p_o",
                 norm.normalizeUrl("http://www.example.com/foo?p=badp&pi=badpi&p_o=ok_p_o&mark=badmark&sw=badsw", null));
    assertEquals("http://www.example.com/foo",
                 norm.normalizeUrl("http://www.example.com/foo?p=badp&pi=badpi&mark=badmark&sw=badsw", null));
    assertEquals("http://www.example.com/foo?ok1=good1&ok2=good2&ok3=good3&ok4=good4&ok5=good5&ok6=good6",
                 norm.normalizeUrl("http://www.example.com/foo?ok1=good1&p=badp&ok2=good2&pi=badpi&ok3=good3&ok4=good4&mark=badmark&ok5=good5&sw=badsw&ok6=good6", null));
  }
  
}
