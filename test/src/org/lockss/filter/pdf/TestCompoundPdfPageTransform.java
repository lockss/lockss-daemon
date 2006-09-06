/*
 * $Id: TestCompoundPdfPageTransform.java,v 1.1 2006-09-01 07:32:52 thib_gc Exp $
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
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.util.PdfUtil.CountCallsPageTransform;

public class TestCompoundPdfPageTransform extends LockssTestCase {

  /**
   * <p>A PDF page transform that adds itself to a list (which is then
   * used to remember in which order several page transforms were
   * called).</p>
   * @author Thib Guicherd-Callin
   */
  protected static class RememberOrder extends CountCallsPageTransform {
    protected List remember;
    public RememberOrder(List remember) {
      this.remember = remember;
    }
    public void transform(PdfDocument pdfDocument, PdfPage pdfPage) throws IOException {
      super.transform(pdfDocument, pdfPage);
      remember.add(this);
    }
  }

  public void testCallsExactlyOnce() throws Exception {
    // Make a few page transforms
    CountCallsPageTransform[] transforms = new CountCallsPageTransform[] {
        new CountCallsPageTransform(),
        new CountCallsPageTransform(),
        new CountCallsPageTransform(),
    };

    // Run all page tranforms
    CompoundPdfPageTransform transform = new CompoundPdfPageTransform(transforms);
    transform.transform(new MockPdfDocument(), new MockPdfPage());

    // Check that all the page transforms were called exactly once
    for (int tra = 0 ; tra < transforms.length ; ++tra) {
      assertEquals("Failed: " + tra, 1, transforms[tra].getCallCount());
    }
  }

  public void testRightOrder_ArrayConstructor() throws Exception {
    // Make a few page transforms
    List remember = new ArrayList();
    RememberOrder[] transforms = new RememberOrder[] {
        new RememberOrder(remember),
        new RememberOrder(remember),
        new RememberOrder(remember),
    };

    // Run all page tranforms
    CompoundPdfPageTransform transform = new CompoundPdfPageTransform(transforms);
    transform.transform(new MockPdfDocument(), new MockPdfPage());

    // Check that the page transforms were called in array order
    assertIsomorphic(transforms, remember);
  }

  public void testRightOrder_TwoArgConstructor() throws Exception {
    // Make two page transforms
    List remember = new ArrayList();
    RememberOrder[] transforms = new RememberOrder[] {
        new RememberOrder(remember),
        new RememberOrder(remember),
    };

    // Run all page tranforms
    CompoundPdfPageTransform transform = new CompoundPdfPageTransform(transforms[0],
                                                                      transforms[1]);
    transform.transform(new MockPdfDocument(), new MockPdfPage());

    // Check that the page transforms were called in constructor order
    assertIsomorphic(transforms, remember);
  }

  public void testRightOrder_UsingAddMethod() throws Exception {
    // Make a few page transforms
    List remember = new ArrayList();
    RememberOrder[] transforms = new RememberOrder[] {
        new RememberOrder(remember),
        new RememberOrder(remember),
        new RememberOrder(remember),
    };

    // Run all page tranforms
    CompoundPdfPageTransform transform = new CompoundPdfPageTransform();
    for (int tra = 0 ; tra < transforms.length ; ++tra) {
      transform.add(transforms[tra]);
    }
    transform.transform(new MockPdfDocument(), new MockPdfPage());

    // Check that the page transforms were called in method call order
    assertIsomorphic(transforms, remember);
  }

}
