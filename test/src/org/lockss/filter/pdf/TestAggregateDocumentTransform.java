/*
 * $Id: TestAggregateDocumentTransform.java,v 1.1 2006-09-10 07:50:49 thib_gc Exp $
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

import org.lockss.filter.pdf.MockTransforms.RememberDocumentTransform;
import org.lockss.test.*;

public class TestAggregateDocumentTransform extends LockssTestCase {

  public void testRightOrder_TwoArgConstructor() throws Exception {
    List remember = new ArrayList();
    RememberDocumentTransform[] transforms = new RememberDocumentTransform[] {
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
    };
    AggregateDocumentTransform transform = new AggregateDocumentTransform(transforms[0],
                                                                          transforms[1]);
    assertTrue(transform.transform(new MockPdfDocument()));
    for (int ix = 0 ; ix < transforms.length ; ++ix) {
      String failed = "Failed index: " + ix;
      assertSame(failed, transforms[ix], remember.get(ix));
      assertEquals(failed, 1, transforms[ix].getCallCount());
    }
  }

  public void testRightOrder_OneArgConstructor() throws Exception {
    // Make two transforms
    List remember = new ArrayList();
    RememberDocumentTransform[] transforms = new RememberDocumentTransform[] {
        new RememberDocumentTransform(remember),
    };
    AggregateDocumentTransform transform = new AggregateDocumentTransform(transforms[0]);
    assertTrue(transform.transform(new MockPdfDocument()));
    for (int ix = 0 ; ix < transforms.length ; ++ix) {
      String failed = "Failed index: " + ix;
      assertSame(failed, transforms[ix], remember.get(ix));
      assertEquals(failed, 1, transforms[ix].getCallCount());
    }
  }

  public void testRightOrder_ThreeArgConstructor() throws Exception {
    // Make two transforms
    List remember = new ArrayList();
    RememberDocumentTransform[] transforms = new RememberDocumentTransform[] {
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
    };
    AggregateDocumentTransform transform = new AggregateDocumentTransform(transforms[0],
                                                                          transforms[1],
                                                                          transforms[2]);
    assertTrue(transform.transform(new MockPdfDocument()));
    for (int ix = 0 ; ix < transforms.length ; ++ix) {
      String failed = "Failed index: " + ix;
      assertSame(failed, transforms[ix], remember.get(ix));
      assertEquals(failed, 1, transforms[ix].getCallCount());
    }
  }

  public void testRightOrder_FourArgConstructor() throws Exception {
    // Make two transforms
    List remember = new ArrayList();
    RememberDocumentTransform[] transforms = new RememberDocumentTransform[] {
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
    };
    AggregateDocumentTransform transform = new AggregateDocumentTransform(transforms[0],
                                                                          transforms[1],
                                                                          transforms[2],
                                                                          transforms[3]);
    assertTrue(transform.transform(new MockPdfDocument()));
    for (int ix = 0 ; ix < transforms.length ; ++ix) {
      String failed = "Failed index: " + ix;
      assertSame(failed, transforms[ix], remember.get(ix));
      assertEquals(failed, 1, transforms[ix].getCallCount());
    }
  }

  public void testRightOrder_UsingAddMethod() throws Exception {
    // Make a few transforms
    List remember = new ArrayList();
    RememberDocumentTransform[] transforms = new RememberDocumentTransform[] {
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
    };
    AggregateDocumentTransform transform = new AggregateDocumentTransform();
    for (int tra = 0 ; tra < transforms.length ; ++tra) {
      transform.add(transforms[tra]);
    }
    assertTrue(transform.transform(new MockPdfDocument()));
    for (int ix = 0 ; ix < transforms.length ; ++ix) {
      String failed = "Failed index: " + ix;
      assertSame(failed, transforms[ix], remember.get(ix));
      assertEquals(failed, 1, transforms[ix].getCallCount());
    }
  }

}
