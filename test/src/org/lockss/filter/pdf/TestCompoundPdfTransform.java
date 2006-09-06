/*
 * $Id: TestCompoundPdfTransform.java,v 1.1 2006-09-01 07:32:52 thib_gc Exp $
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

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.util.PdfUtil.CountCallsTransform;

public class TestCompoundPdfTransform extends LockssTestCase {

  /**
   * <p>A PDF transform that adds itself to a list (which is then used
   * to remember in which order several transforms were called).</p>
   * @author Thib Guicherd-Callin
   */
  protected static class RememberOrder extends CountCallsTransform {
    protected List remember;
    public RememberOrder(List remember) {
      this.remember = remember;
    }
    public void transform(PdfDocument pdfDocument) {
      super.transform(pdfDocument);
      remember.add(this);
    }
  }

  public void testCallsExactlyOnce() throws Exception {
    // Make a few transforms
    CountCallsTransform[] transforms = new CountCallsTransform[] {
        new CountCallsTransform(),
        new CountCallsTransform(),
        new CountCallsTransform(),
    };

    // Run all tranforms
    CompoundPdfTransform transform = new CompoundPdfTransform(transforms);
    transform.transform(new MockPdfDocument());

    // Check that all the transforms were called exactly once
    for (int tra = 0 ; tra < transforms.length ; ++tra) {
      assertEquals("Failed: " + tra, 1, transforms[tra].getCallCount());
    }
  }

  public void testRightOrder_ArrayConstructor() throws Exception {
    // Make a few transforms
    List remember = new ArrayList();
    RememberOrder[] transforms = new RememberOrder[] {
        new RememberOrder(remember),
        new RememberOrder(remember),
        new RememberOrder(remember),
    };

    // Run all tranforms
    CompoundPdfTransform transform = new CompoundPdfTransform(transforms);
    transform.transform(new MockPdfDocument());

    // Check that the transforms were called in array order
    assertIsomorphic(transforms, remember);
  }

  public void testRightOrder_TwoArgConstructor() throws Exception {
    // Make two transforms
    List remember = new ArrayList();
    RememberOrder[] transforms = new RememberOrder[] {
        new RememberOrder(remember),
        new RememberOrder(remember),
    };

    // Run all tranforms
    CompoundPdfTransform transform = new CompoundPdfTransform(transforms[0],
                                                              transforms[1]);
    transform.transform(new MockPdfDocument());

    // Check that the transforms were called in constructor order
    assertIsomorphic(transforms, remember);
  }

  public void testRightOrder_UsingAddMethod() throws Exception {
    // Make a few transforms
    List remember = new ArrayList();
    RememberOrder[] transforms = new RememberOrder[] {
        new RememberOrder(remember),
        new RememberOrder(remember),
        new RememberOrder(remember),
    };

    // Run all tranforms
    CompoundPdfTransform transform = new CompoundPdfTransform();
    for (int tra = 0 ; tra < transforms.length ; ++tra) {
      transform.add(transforms[tra]);
    }
    transform.transform(new MockPdfDocument());

    // Check that the transforms were called in method call order
    assertIsomorphic(transforms, remember);
  }

}
