/*
 * $Id: TransformFirstPage.java,v 1.5 2006-09-19 16:54:53 thib_gc Exp $
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
import java.util.ListIterator;

import org.apache.commons.collections.iterators.SingletonListIterator;
import org.lockss.util.*;
import org.lockss.util.PdfUtil.ResultPolicy;

/**
 * <p>A document transform that applies a page transform to the first
 * page of the PDF document only.</p>
 * @author Thib Guicherd-Callin
 */
public class TransformFirstPage extends TransformSelectedPages {

  /**
   * <p>Builds a new document transform using the default result
   * policy, based on the given page transform.</p>
   * @param pageTransform A page transform.
   * @see TransformSelectedPages#TransformSelectedPages(PageTransform)
   */
  public TransformFirstPage(PageTransform pageTransform) {
    super(pageTransform);
  }

  /**
   * <p>Builds a new document transform based on the aggregation of
   * the given page transforms (using the default aggregation result
   * policy).</p>
   * @param pageTransform1 A page transform.
   * @param pageTransform2 A page transform.
   * @see TransformSelectedPages#TransformSelectedPages(PageTransform, PageTransform)
   */
  public TransformFirstPage(PageTransform pageTransform1,
                            PageTransform pageTransform2) {
    super(pageTransform1,
          pageTransform2);
  }

  /**
   * <p>Builds a new document transform based on the aggregation of
   * the given page transforms (using the default aggregation result
   * policy).</p>
   * @param pageTransform1 A page transform.
   * @param pageTransform2 A page transform.
   * @param pageTransform3 A page transform.
   * @see TransformSelectedPages#TransformSelectedPages(PageTransform, PageTransform, PageTransform)
   */
  public TransformFirstPage(PageTransform pageTransform1,
                            PageTransform pageTransform2,
                            PageTransform pageTransform3) {
    super(pageTransform1,
          pageTransform2,
          pageTransform3);
  }

  /**
   * <p>Builds a new document transform based on the aggregation of
   * the given page transforms (using the default aggregation result
   * policy).</p>
   * @param pageTransforms An array of page transforms.
   * @see TransformSelectedPages#TransformSelectedPages(PageTransform[])
   */
  public TransformFirstPage(PageTransform[] pageTransforms) {
    super(pageTransforms);
  }

  /**
   * <p>Builds a new document transform based on the aggregation of
   * the given page transforms (using the given aggregation result
   * policy).</p>
   * @param pageTransformResultPolicy A result policy (for the result
   *                                  of the aggregate page transform).
   * @param pageTransform1            A page transform.
   * @param pageTransform2            A page transform.
   * @see TransformSelectedPages#TransformSelectedPages(ResultPolicy, ResultPolicy, PageTransform, PageTransform)
   * @see TransformSelectedPages#POLICY_DEFAULT
   */
  public TransformFirstPage(ResultPolicy pageTransformResultPolicy,
                            PageTransform pageTransform1,
                            PageTransform pageTransform2) {
    super(POLICY_DEFAULT,
          pageTransformResultPolicy,
          pageTransform1,
          pageTransform2);
  }

  /**
   * <p>Builds a new document transform based on the aggregation of
   * the given page transforms (using the given aggregation result
   * policy).</p>
   * @param pageTransformResultPolicy A result policy (for the result
   *                                  of the aggregate page transform).
   * @param pageTransform1            A page transform.
   * @param pageTransform2            A page transform.
   * @param pageTransform3            A page transform.
   * @see TransformSelectedPages#TransformSelectedPages(ResultPolicy, ResultPolicy, PageTransform, PageTransform, PageTransform)
   * @see TransformSelectedPages#POLICY_DEFAULT
   */
  public TransformFirstPage(ResultPolicy pageTransformResultPolicy,
                            PageTransform pageTransform1,
                            PageTransform pageTransform2,
                            PageTransform pageTransform3) {
    super(POLICY_DEFAULT,
          pageTransformResultPolicy,
          pageTransform1,
          pageTransform2,
          pageTransform3);
  }

  /**
   * <p>Builds a new document transform based on the aggregation of
   * the given page transforms (using the given aggregation result
   * policy).</p>
   * @param pageTransformResultPolicy A result policy (for the result
   *                                  of the aggregate page transform).
   * @param pageTransforms            An array of page transforms.
   * @see TransformSelectedPages#TransformSelectedPages(ResultPolicy, ResultPolicy, PageTransform[])
   * @see TransformSelectedPages#POLICY_DEFAULT
   */
  public TransformFirstPage(ResultPolicy pageTransformResultPolicy,
                            PageTransform[] pageTransforms) {
    super(POLICY_DEFAULT,
          pageTransformResultPolicy,
          pageTransforms);
  }

  /* Inherit documentation */
  protected ListIterator /* of PdfPage */ getSelectedPages(PdfDocument pdfDocument)
      throws IOException {
    return new SingletonListIterator(pdfDocument.getPage(0));
  }

}
