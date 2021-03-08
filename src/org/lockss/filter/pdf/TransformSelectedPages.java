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
import java.util.*;

import org.lockss.filter.pdf.DocumentTransformUtil.PageTransformWrapper;
import org.lockss.util.*;
import org.lockss.util.PdfUtil.ResultPolicy;

/**
 * <p>A document transform that applies a page transform to selected
 * pages of the PDF document.</p>
 * @author Thib Guicherd-Callin
 * @see #getSelectedPages
 * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
 */
@Deprecated
public abstract class TransformSelectedPages extends PageTransformWrapper {

  /**
   * <p>A result policy determining the boolean result of the
   * transform.</p>
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  protected ResultPolicy resultPolicy;

  /**
   * <p>Builds a new document transform using the default result
   * policy, based on the given page transform.</p>
   * @param pageTransform A page transform.
   * @see #TransformSelectedPages(PdfUtil.ResultPolicy, PageTransform)
   * @see #POLICY_DEFAULT
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  protected TransformSelectedPages(PageTransform pageTransform) {
    this(POLICY_DEFAULT,
         pageTransform);
  }

  /**
   * <p>Builds a new document transform using the default result
   * policy, based on the aggregation of the given page transforms
   * (using the default aggregation result policy).</p>
   * @param pageTransform1 A page transform.
   * @param pageTransform2 A page transform.
   * @see #TransformSelectedPages(PdfUtil.ResultPolicy, PageTransform, PageTransform)
   * @see #POLICY_DEFAULT
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  protected TransformSelectedPages(PageTransform pageTransform1,
                                   PageTransform pageTransform2) {
    this(POLICY_DEFAULT,
         pageTransform1,
         pageTransform2);
  }

  /**
   * <p>Builds a new document transform using the default result
   * policy, based on the aggregation of the given page transforms
   * (using the default aggregation result policy).</p>
   * @param pageTransform1 A page transform.
   * @param pageTransform2 A page transform.
   * @param pageTransform3 A page transform.
   * @see #TransformSelectedPages(PdfUtil.ResultPolicy, PageTransform, PageTransform, PageTransform)
   * @see #POLICY_DEFAULT
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  protected TransformSelectedPages(PageTransform pageTransform1,
                                   PageTransform pageTransform2,
                                   PageTransform pageTransform3) {
    this(POLICY_DEFAULT,
         pageTransform1,
         pageTransform2,
         pageTransform3);
  }

  /**
   * <p>Builds a new document transform using the default result
   * policy, based on the aggregation of the given page transforms
   * (using the default aggregation result policy).</p>
   * @param pageTransforms An array of page transforms.
   * @see #TransformSelectedPages(PdfUtil.ResultPolicy, PageTransform[])
   * @see #POLICY_DEFAULT
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  protected TransformSelectedPages(PageTransform[] pageTransforms) {
    this(POLICY_DEFAULT,
         pageTransforms);
  }

  /**
   * <p>Builds a new document transform using the given result
   * policy, based on the given page transform.</p>
   * @param resultPolicy  A result policy.
   * @param pageTransform A page transform.
   * @see PageTransformWrapper#PageTransformWrapper(PageTransform)
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  protected TransformSelectedPages(ResultPolicy resultPolicy,
                                   PageTransform pageTransform) {
    super(pageTransform);
    if (resultPolicy == null) {
      String logMessage = "Cannot specify a null result policy";
      logger.error(logMessage);
      throw new NullPointerException(logMessage);
    }
    logger.debug3("Setting up result policy " + resultPolicy.toString());
    this.resultPolicy = resultPolicy;
  }

  /**
   * <p>Builds a new document transform using the given result
   * policy, based on the aggregation of the given page transforms
   * (using the default aggregation result policy).</p>
   * @param resultPolicy   A result policy.
   * @param pageTransform1 A page transform.
   * @param pageTransform2 A page transform.
   * @see #TransformSelectedPages(PdfUtil.ResultPolicy, PageTransform)
   * @see AggregatePageTransform#AggregatePageTransform(PageTransform, PageTransform)
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  protected TransformSelectedPages(ResultPolicy resultPolicy,
                                   PageTransform pageTransform1,
                                   PageTransform pageTransform2) {
    this(resultPolicy,
         new AggregatePageTransform(pageTransform1,
                                    pageTransform2));
    logger.debug3("Implicitly aggregated two page transforms");
  }

  /**
   * <p>Builds a new document transform using the given result
   * policy, based on the aggregation of the given page transforms
   * (using the default aggregation result policy).</p>
   * @param resultPolicy   A result policy.
   * @param pageTransform1 A page transform.
   * @param pageTransform2 A page transform.
   * @param pageTransform3 A page transform.
   * @see #TransformSelectedPages(PdfUtil.ResultPolicy, PageTransform)
   * @see AggregatePageTransform#AggregatePageTransform(PageTransform, PageTransform, PageTransform)
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  protected TransformSelectedPages(ResultPolicy resultPolicy,
                                   PageTransform pageTransform1,
                                   PageTransform pageTransform2,
                                   PageTransform pageTransform3) {
    this(resultPolicy,
         new AggregatePageTransform(pageTransform1,
                                    pageTransform2,
                                    pageTransform3));
    logger.debug3("Implicitly aggregated three page transforms");
  }

  /**
   * <p>Builds a new document transform using the given result
   * policy, based on the aggregation of the given page transforms
   * (using the default aggregation result policy).</p>
   * @param resultPolicy   A result policy.
   * @param pageTransforms An array of page transforms.
   * @see #TransformSelectedPages(PdfUtil.ResultPolicy, PageTransform)
   * @see AggregatePageTransform#AggregatePageTransform(PageTransform[])
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  protected TransformSelectedPages(ResultPolicy resultPolicy,
                                   PageTransform[] pageTransforms) {
    this(resultPolicy,
         new AggregatePageTransform(pageTransforms));
    logger.debug3("Implicitly aggregated " + pageTransforms.length + " page transforms");
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
   * @see #TransformSelectedPages(PdfUtil.ResultPolicy, PageTransform)
   * @see AggregatePageTransform#AggregatePageTransform(PdfUtil.ResultPolicy, PageTransform, PageTransform)
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  protected TransformSelectedPages(ResultPolicy pageIterationResultPolicy,
                                   ResultPolicy pageTransformResultPolicy,
                                   PageTransform pageTransform1,
                                   PageTransform pageTransform2) {
    this(pageIterationResultPolicy,
         new AggregatePageTransform(pageTransformResultPolicy,
                                    pageTransform1,
                                    pageTransform2));
    logger.debug3("Implicitly aggregated two page transforms");
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
   * @see #TransformSelectedPages(PdfUtil.ResultPolicy, PageTransform)
   * @see AggregatePageTransform#AggregatePageTransform(PdfUtil.ResultPolicy, PageTransform, PageTransform, PageTransform)
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  protected TransformSelectedPages(ResultPolicy pageIterationResultPolicy,
                                   ResultPolicy pageTransformResultPolicy,
                                   PageTransform pageTransform1,
                                   PageTransform pageTransform2,
                                   PageTransform pageTransform3) {
    this(pageIterationResultPolicy,
         new AggregatePageTransform(pageTransformResultPolicy,
                                    pageTransform1,
                                    pageTransform2,
                                    pageTransform3));
    logger.debug3("Implicitly aggregated three page transforms");
  }

  /**
   * <p>Builds a new document transform using the given result
   * policy, based on the aggregation of the given page transforms
   * (using the given aggregation result policy).</p>
   * @param pageIterationResultPolicy A result policy (for the result
   *                                  of transforming selected pages).
   * @param pageTransformResultPolicy A result policy (for the result
   *                                  of the aggregate page transform).
   * @param pageTransforms            An array of page transforms.
   * @see #TransformSelectedPages(PdfUtil.ResultPolicy, PageTransform)
   * @see AggregatePageTransform#AggregatePageTransform(PdfUtil.ResultPolicy, PageTransform[])
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  protected TransformSelectedPages(ResultPolicy pageIterationResultPolicy,
                                   ResultPolicy pageTransformResultPolicy,
                                   PageTransform[] pageTransforms) {
    this(pageIterationResultPolicy,
         new AggregatePageTransform(pageTransformResultPolicy,
                                    pageTransforms));
    logger.debug3("Implicitly aggregated " + pageTransforms.length + " page transforms");
  }

  /* Inherit documentation */
  @Deprecated
  public boolean transform(PdfDocument pdfDocument) throws IOException {
    logger.debug3("Begin selected pages transform with result policy " + resultPolicy.toString());
    boolean success = resultPolicy.initialValue();
    logger.debug3("Aggregate success flag initially " + success);
    for (Iterator iter = getSelectedPages(pdfDocument) ; iter.hasNext() ; ) {
      try {
        PdfPage pdfPage = (PdfPage)iter.next();
        success = resultPolicy.updateResult(success, pageTransform.transform(pdfPage));
        logger.debug3("Aggregate success flag now " + success);
      }
      catch (PageTransformException pte) {
        String logMessage = "Underlying page transform failed";
        logger.error(logMessage, pte);
        throw new DocumentTransformException(logMessage, pte);
      }
      if (!resultPolicy.shouldKeepGoing(success)) {
        logger.debug3("Aggregation should not keep going");
        break;
      }
    }
    logger.debug2("Selected pages transform result: " + success);
    return success;
  }

  /**
   * <p>Gets an iterator of the pages selected for transformation by
   * this transform.</p>
   * @param pdfDocument A PDF document.
   * @return An iterator of PDF pages ({@link PdfPage}).
   * @throws IOException if any processing error occurs.
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  protected abstract ListIterator /* of PdfPage */ getSelectedPages(PdfDocument pdfDocument)
      throws IOException;

  /**
   * <p>The default result policy used by this class.</p>
   * @see #TransformSelectedPages(PageTransform)
   * @see #TransformSelectedPages(PageTransform, PageTransform)
   * @see #TransformSelectedPages(PageTransform, PageTransform, PageTransform)
   * @see #TransformSelectedPages(PageTransform[])
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static final ResultPolicy POLICY_DEFAULT = PdfUtil.AND;

  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  private static Logger logger = Logger.getLogger(TransformSelectedPages.class);

}
