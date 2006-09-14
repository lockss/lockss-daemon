/*
 * $Id: TransformEachPage.java,v 1.4 2006-09-14 23:10:39 thib_gc Exp $
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

import org.lockss.util.*;
import org.lockss.util.PdfUtil.ResultPolicy;

/**
 * <p>A document transform that applies a page transform to each
 * page of the PDF document.</p>
 * @author Thib Guicherd-Callin
 */
public class TransformEachPage extends TransformSelectedPages {

  /**
   * <p>Builds a new document transform using the default result
   * policy, based on the given page transform.</p>
   * @param pageTransform A page transform.
   * @see TransformSelectedPages#TransformSelectedPages(PageTransform)
   */
  public TransformEachPage(PageTransform pageTransform) {
    super(pageTransform);
  }

  /**
   * <p>Builds a new document transform using the default result
   * policy, based on the aggregation of the given page transforms
   * (using the default aggregation result policy).</p>
   * @param pageTransform1 A page transform.
   * @param pageTransform2 A page transform.
   * @see TransformSelectedPages#TransformSelectedPages(PageTransform, PageTransform)
   */
  public TransformEachPage(PageTransform pageTransform1,
                           PageTransform pageTransform2) {
    super(pageTransform1,
          pageTransform2);
  }

  /**
   * <p>Builds a new document transform using the default result
   * policy, based on the aggregation of the given page transforms
   * (using the default aggregation result policy).</p>
   * @param pageTransform1 A page transform.
   * @param pageTransform2 A page transform.
   * @param pageTransform3 A page transform.
   * @see TransformSelectedPages#TransformSelectedPages(PageTransform, PageTransform, PageTransform)
   */
  public TransformEachPage(PageTransform pageTransform1,
                           PageTransform pageTransform2,
                           PageTransform pageTransform3) {
    super(pageTransform1,
          pageTransform2,
          pageTransform3);
  }

  /**
   * <p>Builds a new document transform using the given result
   * policy, based on the given page transform.</p>
   * @param resultPolicy  A result policy.
   * @param pageTransform A page transform.
   * @see TransformSelectedPages#TransformSelectedPages(ResultPolicy, PageTransform)
   */
  public TransformEachPage(ResultPolicy resultPolicy,
                           PageTransform pageTransform) {
    super(resultPolicy,
          pageTransform);
  }

  /**
   * <p>Builds a new document transform using the given result
   * policy, based on the aggregation of the given page transforms
   * (using the default aggregation result policy).</p>
   * @param resultPolicy   A result policy.
   * @param pageTransform1 A page transform.
   * @param pageTransform2 A page transform.
   * @see TransformSelectedPages#TransformSelectedPages(ResultPolicy, PageTransform, PageTransform)
   */
  public TransformEachPage(ResultPolicy resultPolicy,
                           PageTransform pageTransform1,
                           PageTransform pageTransform2) {
    super(resultPolicy,
          pageTransform1,
          pageTransform2);
  }

  /**
   * <p>Builds a new document transform using the given result
   * policy, based on the aggregation of the given page transforms
   * (using the default aggregation result policy).</p>
   * @param resultPolicy   A result policy.
   * @param pageTransform1 A page transform.
   * @param pageTransform2 A page transform.
   * @param pageTransform3 A page transform.
   * @see TransformSelectedPages#TransformSelectedPages(ResultPolicy, PageTransform, PageTransform, PageTransform)
   */
  public TransformEachPage(ResultPolicy resultPolicy,
                           PageTransform pageTransform1,
                           PageTransform pageTransform2,
                           PageTransform pageTransform3) {
    super(resultPolicy,
          pageTransform1,
          pageTransform2,
          pageTransform3);
  }

  /**
   * <p>Builds a new document transform using the given result
   * policy, based on the aggregation of the given page transforms
   * (using the given aggregation result policy).</p>
   * @param pageIterationResultPolicy A result policy (for the result
   *                                  of transforming selected pages).
   * @param pageTransformResultPolicy A result policy (for the result
   *                                  of the aggregate page transform).
   * @param pageTransform1            A page transform.
   * @param pageTransform2            A page transform.
   * @see TransformSelectedPages#TransformSelectedPages(ResultPolicy, ResultPolicy, PageTransform, PageTransform)
   */
  public TransformEachPage(ResultPolicy pageIterationResultPolicy,
                           ResultPolicy pageTransformResultPolicy,
                           PageTransform pageTransform1,
                           PageTransform pageTransform2) {
    super(pageIterationResultPolicy,
          pageTransformResultPolicy,
          pageTransform1,
          pageTransform2);
  }

  /**
   * <p>Builds a new document transform using the given result
   * policy, based on the aggregation of the given page transforms
   * (using the given aggregation result policy).</p>
   * @param pageIterationResultPolicy A result policy (for the result
   *                                  of transforming selected pages).
   * @param pageTransformResultPolicy A result policy (for the result
   *                                  of the aggregate page transform).
   * @param pageTransform1            A page transform.
   * @param pageTransform2            A page transform.
   * @param pageTransform3            A page transform.
   * @see TransformSelectedPages#TransformSelectedPages(ResultPolicy, ResultPolicy, PageTransform, PageTransform, PageTransform)
   */
  public TransformEachPage(ResultPolicy pageIterationResultPolicy,
                           ResultPolicy pageTransformResultPolicy,
                           PageTransform pageTransform1,
                           PageTransform pageTransform2,
                           PageTransform pageTransform3) {
    super(pageIterationResultPolicy,
          pageTransformResultPolicy,
          pageTransform1,
          pageTransform2,
          pageTransform3);
  }

  /* Inherit documentation */
  protected ListIterator /* of PdfPage */ getSelectedPages(PdfDocument pdfDocument)
      throws IOException {
    return pdfDocument.getPageIterator();
  }

}
