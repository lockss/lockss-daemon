/*
 * $Id: TestAggregateDocumentTransform.java,v 1.2 2006-11-01 22:25:16 thib_gc Exp $
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
import org.lockss.util.PdfUtil;

public class TestAggregateDocumentTransform extends LockssTestCase {

  public void testRightOrder_1ArgConstructor() throws Exception {
    List remember = new ArrayList();
    RememberDocumentTransform[] transforms = new RememberDocumentTransform[] {
        new RememberDocumentTransform(remember),
    };
    AggregateDocumentTransform transform = new AggregateDocumentTransform(PdfUtil.AND_ALL,
                                                                          transforms[0]);
    assertTrue(transform.transform(new MockPdfDocument()));
    assertEquals(transforms.length, remember.size());
    for (int ix = 0 ; ix < transforms.length ; ++ix) {
      String failed = "Failing index: " + ix;
      assertSame(failed, transforms[ix], remember.get(ix));
      assertEquals(failed, 1, transforms[ix].getCallCount());
    }
  }

  public void testRightOrder_2ArgConstructor() throws Exception {
    List remember = new ArrayList();
    RememberDocumentTransform[] transforms = new RememberDocumentTransform[] {
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
    };
    AggregateDocumentTransform transform = new AggregateDocumentTransform(PdfUtil.AND_ALL,
                                                                          transforms[0],
                                                                          transforms[1]);
    assertTrue(transform.transform(new MockPdfDocument()));
    assertEquals(transforms.length, remember.size());
    for (int ix = 0 ; ix < transforms.length ; ++ix) {
      String failed = "Failing index: " + ix;
      assertSame(failed, transforms[ix], remember.get(ix));
      assertEquals(failed, 1, transforms[ix].getCallCount());
    }
  }

  public void testRightOrder_3ArgConstructor() throws Exception {
    List remember = new ArrayList();
    RememberDocumentTransform[] transforms = new RememberDocumentTransform[] {
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
    };
    AggregateDocumentTransform transform = new AggregateDocumentTransform(PdfUtil.AND_ALL,
                                                                          transforms[0],
                                                                          transforms[1],
                                                                          transforms[2]);
    assertTrue(transform.transform(new MockPdfDocument()));
    assertEquals(transforms.length, remember.size());
    for (int ix = 0 ; ix < transforms.length ; ++ix) {
      String failed = "Failing index: " + ix;
      assertSame(failed, transforms[ix], remember.get(ix));
      assertEquals(failed, 1, transforms[ix].getCallCount());
    }
  }

  public void testRightOrder_4ArgConstructor() throws Exception {
    List remember = new ArrayList();
    RememberDocumentTransform[] transforms = new RememberDocumentTransform[] {
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
    };
    AggregateDocumentTransform transform = new AggregateDocumentTransform(PdfUtil.AND_ALL,
                                                                          transforms[0],
                                                                          transforms[1],
                                                                          transforms[2],
                                                                          transforms[3]);
    assertTrue(transform.transform(new MockPdfDocument()));
    assertEquals(transforms.length, remember.size());
    for (int ix = 0 ; ix < transforms.length ; ++ix) {
      String failed = "Failing index: " + ix;
      assertSame(failed, transforms[ix], remember.get(ix));
      assertEquals(failed, 1, transforms[ix].getCallCount());
    }
  }

  public void testRightOrder_AddMethod_Array() throws Exception {
    List remember = new ArrayList();
    RememberDocumentTransform[] transforms = new RememberDocumentTransform[] {
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
    };
    AggregateDocumentTransform transform = new AggregateDocumentTransform(PdfUtil.AND_ALL);
    transform.add(transforms);
    assertTrue(transform.transform(new MockPdfDocument()));
    assertEquals(transforms.length, remember.size());
    for (int ix = 0 ; ix < transforms.length ; ++ix) {
      String failed = "Failing index: " + ix;
      assertSame(failed, transforms[ix], remember.get(ix));
      assertEquals(failed, 1, transforms[ix].getCallCount());
    }
  }

  public void testRightOrder_AddMethod_Single() throws Exception {
    List remember = new ArrayList();
    RememberDocumentTransform[] transforms = new RememberDocumentTransform[] {
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
    };
    AggregateDocumentTransform transform = new AggregateDocumentTransform(PdfUtil.AND_ALL);
    for (int tra = 0 ; tra < transforms.length ; ++tra) {
      transform.add(transforms[tra]);
    }
    assertTrue(transform.transform(new MockPdfDocument()));
    assertEquals(transforms.length, remember.size());
    for (int ix = 0 ; ix < transforms.length ; ++ix) {
      String failed = "Failing index: " + ix;
      assertSame(failed, transforms[ix], remember.get(ix));
      assertEquals(failed, 1, transforms[ix].getCallCount());
    }
  }

  public void testRightOrder_ArrayConstructor() throws Exception {
    List remember = new ArrayList();
    RememberDocumentTransform[] transforms = new RememberDocumentTransform[] {
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
        new RememberDocumentTransform(remember),
    };
    AggregateDocumentTransform transform = new AggregateDocumentTransform(PdfUtil.AND_ALL,
                                                                          transforms);
    assertTrue(transform.transform(new MockPdfDocument()));
    assertEquals(transforms.length, remember.size());
    for (int ix = 0 ; ix < transforms.length ; ++ix) {
      String failed = "Failing index: " + ix;
      assertSame(failed, transforms[ix], remember.get(ix));
      assertEquals(failed, 1, transforms[ix].getCallCount());
    }
  }

}
