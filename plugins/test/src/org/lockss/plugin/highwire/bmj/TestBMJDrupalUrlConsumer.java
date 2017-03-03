/*
 * $Id: $
 */

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire.bmj;

import java.util.regex.Pattern;

import org.lockss.test.LockssTestCase;

/**
  protected static final String ORIG_STRING = "/content(/.+/[^/]+[.]full[.]pdf|/.+[.]toc)$";
  protected static final String DEST_STRING = "/content/[^/]+(/.+/[^/]+[.]full[.]pdf|/[^/.]+)$";
 */
public class TestBMJDrupalUrlConsumer extends LockssTestCase {

  
  public void testOrigPattern() throws Exception {
    Pattern origPat = BMJDrupalUrlConsumerFactory.getOrigPattern();
    assertTrue(isMatchRe("http://www.example.com/content/349/bmj.g7460.full.pdf", origPat));
    assertTrue(isMatchRe("https://www.example.com/content/349/bmj.g7460.full.pdf", origPat));
    assertTrue(isMatchRe("http://www.example.com/content/349/9/123.full.pdf", origPat));
    assertTrue(isMatchRe("http://www.example.com/content/349/9.toc", origPat));
    assertFalse(isMatchRe("http://www.example.com/content/349/bmj.g7460.full.pdf.gif", origPat));
    assertFalse(isMatchRe("http://www.example.com/content/349/bmj.g7460", origPat));
  }
  
  public void testDestPdfPattern() throws Exception {
    Pattern destPat = BMJDrupalUrlConsumerFactory.getDestPattern();
    assertTrue(isMatchRe("http://www.example.com/content/bmj/349/bmj.g7460.full.pdf", destPat));
    assertTrue(isMatchRe("https://www.example.com/content/bmj/349/bmj.g7460.full.pdf", destPat));
    assertTrue(isMatchRe("http://www.example.com/content/bmj/349/9/123.full.pdf", destPat));
    assertTrue(isMatchRe("http://www.example.com/content/349/9", destPat));
    assertFalse(isMatchRe("http://www.example.com/content/349/bmj.g7460.full.pdf", destPat));
    assertFalse(isMatchRe("http://www.example.com/content/bmj/349/9", destPat));
  }
  
}
