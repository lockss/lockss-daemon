/*
 * $Id: TestConditionalPdfPageTransform.java,v 1.1 2006-09-01 07:32:52 thib_gc Exp $
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

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.util.PdfUtil.CountCallsPageTransform;

public class TestConditionalPdfPageTransform extends LockssTestCase {

  public void testDoesNotRunWhenIdentifyFalse() throws Exception {
    PdfPageTransform transform = new PdfPageTransform() {
      public void transform(PdfDocument pdfDocument, PdfPage pdfPage) throws IOException {
        fail("Transform was called but identify() had returned false");
      }
    };
    ConditionalPdfPageTransform conditional = new ConditionalPdfPageTransform(transform) {
      public boolean identify(PdfDocument pdfDocument, PdfPage pdfPage) throws IOException {
        return false;
      }
    };
    conditional.transform(new MockPdfDocument(), new MockPdfPage());
    // Did not throw: all is well
  }

  public void testRunsWhenIdentifyTrue() throws Exception {
    CountCallsPageTransform transform = new CountCallsPageTransform();
    ConditionalPdfPageTransform conditional = new ConditionalPdfPageTransform(transform) {
      public boolean identify(PdfDocument pdfDocument, PdfPage pdfPage) throws IOException {
        return true;
      }
    };
    conditional.transform(new MockPdfDocument(), new MockPdfPage());
    assertEquals(1, transform.getCallCount());
  }

}
