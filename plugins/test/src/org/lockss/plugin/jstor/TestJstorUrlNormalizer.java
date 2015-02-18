/*
 * $Id$
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

package org.lockss.plugin.jstor;

import org.lockss.plugin.UrlNormalizer;
import org.lockss.plugin.atypon.BaseAtyponUrlNormalizer;
import org.lockss.test.LockssTestCase;

public class TestJstorUrlNormalizer extends LockssTestCase {

  public void testUrlNormalizer() throws Exception {
    UrlNormalizer norm = new JstorUrlNormalizer();

    // don't do anything to a normal url
    assertEquals("http://www.jstor.org/doi/pdf/11.1111/12345",
        norm.normalizeUrl("http://www.jstor.org/doi/pdf/11.1111/12345", null));

   // test the basics of what the normalizer is set up to do
    assertEquals("http://www.jstor.org/xyz",
        norm.normalizeUrl("http://www.jstor.org/xyz?seq=1&foo=bar", null));
    assertEquals("http://www.jstor.org/xyz",
        norm.normalizeUrl("http://www.jstor.org/xyz?acceptTC=true&foo=bar", null));
    assertEquals("http://www.jstor.org/xyz",
        norm.normalizeUrl("http://www.jstor.org/xyz?&amp;acceptTC&foo=bar", null));
    
  // test some real-world examples (with details changed to protect the innocent)  
  }



}
