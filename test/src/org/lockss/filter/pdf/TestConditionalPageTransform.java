/*
 * $Id$
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.filter.pdf;

import java.io.IOException;
import java.util.ArrayList;

import org.lockss.filter.pdf.MockTransforms.RememberTransformPageTransform;
import org.lockss.filter.pdf.PageTransformUtil.IdentityPageTransform;
import org.lockss.test.*;
import org.lockss.util.PdfPage;

@Deprecated
public class TestConditionalPageTransform extends LockssTestCase {

  public void testIfFalse() throws Exception {
    PageTransform transform = new PageTransform() {
      public boolean transform(PdfPage pdfPage) throws IOException {
        fail("Should not have been called");
        return false;
      }
    };
    ConditionalPageTransform conditional = new ConditionalPageTransform(new IdentityPageTransform(false),
                                                                        true,
                                                                        transform);
    assertFalse(conditional.transform(new MockPdfPage()));
  }

  public void testIfTrueStrictThenFalse() throws Exception {
    ConditionalPageTransform conditional = new ConditionalPageTransform(new IdentityPageTransform(true),
                                                                        true,
                                                                        new IdentityPageTransform(false));
    try {
      conditional.transform(new MockPdfPage());
      fail("Should have thrown");
    }
    catch (PageTransformException pte) {
      // Threw; all is well
    }
  }

  public void testIfTrueStrictThenTrue() throws Exception {
    RememberTransformPageTransform transform = new RememberTransformPageTransform(true, new ArrayList());
    ConditionalPageTransform conditional = new ConditionalPageTransform(new IdentityPageTransform(true),
                                                                        true,
                                                                        transform);
    assertTrue(conditional.transform(new MockPdfPage()));
    assertEquals(1, transform.getCallCount());
  }

  public void testIfTrueThenFalse() throws Exception {
    RememberTransformPageTransform transform = new RememberTransformPageTransform(false, new ArrayList());
    ConditionalPageTransform conditional = new ConditionalPageTransform(new IdentityPageTransform(true),
                                                                        false,
                                                                        transform);
    assertFalse(conditional.transform(new MockPdfPage()));
    assertEquals(1, transform.getCallCount());
  }

  public void testIfTrueThenTrue() throws Exception {
    RememberTransformPageTransform transform = new RememberTransformPageTransform(true, new ArrayList());
    ConditionalPageTransform conditional = new ConditionalPageTransform(new IdentityPageTransform(true),
                                                                        false,
                                                                        transform);
    assertTrue(conditional.transform(new MockPdfPage()));
    assertEquals(1, transform.getCallCount());
  }

}
