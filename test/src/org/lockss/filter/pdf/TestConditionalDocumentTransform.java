/*
 * $Id: TestConditionalDocumentTransform.java,v 1.3 2006-11-01 22:25:16 thib_gc Exp $
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

import org.lockss.filter.pdf.DocumentTransformUtil.IdentityDocumentTransform;
import org.lockss.filter.pdf.MockTransforms.RememberDocumentTransform;
import org.lockss.test.*;
import org.lockss.util.PdfDocument;

public class TestConditionalDocumentTransform extends LockssTestCase {

  public void testIfFalse() throws Exception {
    DocumentTransform transform = new DocumentTransform() {
      public boolean transform(PdfDocument pdfDocument) throws IOException {
        fail("Should not have been called");
        return false;
      }
    };
    ConditionalDocumentTransform conditional = new ConditionalDocumentTransform(new IdentityDocumentTransform(false),
                                                                                true,
                                                                                transform);
    assertFalse(conditional.transform(new MockPdfDocument()));
  }

  public void testIfTrueStrictThenFalse() throws Exception {
    ConditionalDocumentTransform conditional = new ConditionalDocumentTransform(new IdentityDocumentTransform(true),
                                                                                true,
                                                                                new IdentityDocumentTransform(false));
    try {
      conditional.transform(new MockPdfDocument());
      fail("Should have thrown");
    }
    catch (DocumentTransformException dte) {
      // Threw; all is well
    }
  }

  public void testIfTrueStrictThenTrue() throws Exception {
    RememberDocumentTransform transform = new RememberDocumentTransform(true, new ArrayList());
    ConditionalDocumentTransform conditional = new ConditionalDocumentTransform(new IdentityDocumentTransform(true),
                                                                                true,
                                                                                transform);
    assertTrue(conditional.transform(new MockPdfDocument()));
    assertEquals(1, transform.getCallCount());
  }

  public void testIfTrueThenFalse() throws Exception {
    RememberDocumentTransform transform = new RememberDocumentTransform(false, new ArrayList());
    ConditionalDocumentTransform conditional = new ConditionalDocumentTransform(new IdentityDocumentTransform(true),
                                                                                false,
                                                                                transform);
    assertFalse(conditional.transform(new MockPdfDocument()));
    assertEquals(1, transform.getCallCount());
  }

  public void testIfTrueThenTrue() throws Exception {
    RememberDocumentTransform transform = new RememberDocumentTransform(true, new ArrayList());
    ConditionalDocumentTransform conditional = new ConditionalDocumentTransform(new IdentityDocumentTransform(true),
                                                                                false,
                                                                                transform);
    assertTrue(conditional.transform(new MockPdfDocument()));
    assertEquals(1, transform.getCallCount());
  }

}
