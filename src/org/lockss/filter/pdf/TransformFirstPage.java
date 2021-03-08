/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
 * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
 */
@Deprecated
public class TransformFirstPage extends TransformSelectedPages {

  /**
   * <p>Builds a new document transform using the default result
   * policy, based on the given page transform.</p>
   * @param pageTransform A page transform.
   * @see TransformSelectedPages#TransformSelectedPages(PageTransform)
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
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
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
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
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
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
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
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
   * @see TransformSelectedPages#TransformSelectedPages(PdfUtil.ResultPolicy, PdfUtil.ResultPolicy, PageTransform, PageTransform)
   * @see TransformSelectedPages#POLICY_DEFAULT
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
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
   * @see TransformSelectedPages#TransformSelectedPages(PdfUtil.ResultPolicy, PdfUtil.ResultPolicy, PageTransform, PageTransform, PageTransform)
   * @see TransformSelectedPages#POLICY_DEFAULT
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
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
   * @see TransformSelectedPages#TransformSelectedPages(PdfUtil.ResultPolicy, PdfUtil.ResultPolicy, PageTransform[])
   * @see TransformSelectedPages#POLICY_DEFAULT
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public TransformFirstPage(ResultPolicy pageTransformResultPolicy,
                            PageTransform[] pageTransforms) {
    super(POLICY_DEFAULT,
          pageTransformResultPolicy,
          pageTransforms);
  }

  /* Inherit documentation */
  @Deprecated
  protected ListIterator /* of PdfPage */ getSelectedPages(PdfDocument pdfDocument)
      throws IOException {
    return new SingletonListIterator(pdfDocument.getPage(0));
  }

}
