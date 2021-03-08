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

import org.lockss.filter.pdf.DocumentTransformUtil.*;
import org.lockss.util.*;
import org.lockss.util.PdfUtil.ResultPolicy;

/**
 * <p>A document transform decorator that applies a "then" document
 * transform only if the PDF document to be transformed is recognized
 * by an "if" document transform.</p>
 * @author Thib Guicherd-Callin
 * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
 */
@Deprecated
public class ConditionalDocumentTransform extends DocumentTransformDecorator {

  /**
   * <p>Builds a new conditional document transform using the given
   * strictness, out of the given "if" document transform and
   * "then" document transform.</p>
   * @param ifTransform    An "if" document transform.
   * @param thenStrictness True to wrap the "then" document transform
   *                       as a {@link StrictDocumentTransform} if it
   *                       is not one already, false otherwise.
   * @param thenTransform  A "then" document transform.
   * @see DocumentTransformDecorator#DocumentTransformDecorator(DocumentTransform)
   * @see AggregateDocumentTransform#AggregateDocumentTransform(DocumentTransform, DocumentTransform)
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ConditionalDocumentTransform(DocumentTransform ifTransform,
                                      boolean thenStrictness,
                                      DocumentTransform thenTransform) {
    super(new AggregateDocumentTransform(ifTransform,
                                         !thenStrictness || thenTransform instanceof StrictDocumentTransform
                                         ? thenTransform
                                         : new StrictDocumentTransform(thenTransform)));
    if (logger.isDebug3()) {
      StringBuffer buffer = new StringBuffer();
      buffer.append("Done setting up conditional document transform ");
      if (thenTransform instanceof StrictDocumentTransform) {
        buffer.append("with existing");
      }
      else if (thenStrictness) {
        buffer.append("with added");
      }
      else {
        buffer.append("without");
      }
      buffer.append(" \"then\" strictness");
      logger.debug3(buffer.toString());
    }
  }

  /**
   * <p>Builds a new conditional document transform using the given
   * strictness, out of the given "if" document transform and the
   * aggregation of the given "then" document transforms (using the
   * default aggregation result policy).</p>
   * @param ifTransform    An "if" document transform.
   * @param thenStrictness True to wrap the aggregated "then" document
   *                       transform as a
   *                       {@link StrictDocumentTransform}, false
   *                       otherwise.
   * @param thenTransform1  A "then" document transform.
   * @param thenTransform2  A "then" document transform.
   * @see #ConditionalDocumentTransform(DocumentTransform, boolean, DocumentTransform)
   * @see AggregateDocumentTransform#AggregateDocumentTransform(DocumentTransform, DocumentTransform)
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ConditionalDocumentTransform(DocumentTransform ifTransform,
                                      boolean thenStrictness,
                                      DocumentTransform thenTransform1,
                                      DocumentTransform thenTransform2) {
    this(ifTransform,
         thenStrictness,
         new AggregateDocumentTransform(thenTransform1,
                                        thenTransform2));
    logger.debug3("Implicitly aggregated two \"then\" transforms");
  }

  /**
   * <p>Builds a new conditional document transform using the given
   * strictness, out of the given "if" document transform and the
   * aggregation of the given "then" document transforms (using the
   * default aggregation result policy).</p>
   * @param ifTransform    An "if" document transform.
   * @param thenStrictness True to wrap the aggregated "then" document
   *                       transform as a
   *                       {@link StrictDocumentTransform}, false
   *                       otherwise.
   * @param thenTransform1  A "then" document transform.
   * @param thenTransform2  A "then" document transform.
   * @param thenTransform3  A "then" document transform.
   * @see #ConditionalDocumentTransform(DocumentTransform, boolean, DocumentTransform)
   * @see AggregateDocumentTransform#AggregateDocumentTransform(DocumentTransform, DocumentTransform)
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ConditionalDocumentTransform(DocumentTransform ifTransform,
                                      boolean thenStrictness,
                                      DocumentTransform thenTransform1,
                                      DocumentTransform thenTransform2,
                                      DocumentTransform thenTransform3) {
    this(ifTransform,
         thenStrictness,
         new AggregateDocumentTransform(thenTransform1,
                                        thenTransform2,
                                        thenTransform3));
    logger.debug3("Implicitly aggregated three \"then\" transforms");
  }

  /**
   * <p>Builds a new conditional document transform using the given
   * strictness, out of the given "if" document transform and the
   * aggregation of the given "then" document transforms (using the
   * default aggregation result policy).</p>
   * @param ifTransform    An "if" document transform.
   * @param thenStrictness True to wrap the aggregated "then" document
   *                       transform as a
   *                       {@link StrictDocumentTransform}, false
   *                       otherwise.
   * @param thenTransforms  An array of "then" document transform.
   * @see #ConditionalDocumentTransform(DocumentTransform, boolean, DocumentTransform)
   * @see AggregateDocumentTransform#AggregateDocumentTransform(DocumentTransform[])
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ConditionalDocumentTransform(DocumentTransform ifTransform,
                                      boolean thenStrictness,
                                      DocumentTransform[] thenTransforms) {
    this(ifTransform,
         thenStrictness,
         new AggregateDocumentTransform(thenTransforms));
    logger.debug3("Implicitly aggregated " + thenTransforms.length + " \"then\" transforms");
  }

  /**
   * <p>Builds a new conditional document transform using the given
   * strictness, out of the given "if" document transform and the
   * aggregation of the given "then" document transforms (using the
   * given aggregation result policy).</p>
   * @param ifTransform      An "if" document transform.
   * @param thenStrictness   True to wrap the aggregated "then" document
   *                         transform as a
   *                         {@link StrictDocumentTransform}, false
   *                         otherwise.
   * @param thenResultPolicy A result policy for the aggregate "then"
   *                         document transform.
   * @param thenTransform1   A "then" document transform.
   * @param thenTransform2   A "then" document transform.
   * @see #ConditionalDocumentTransform(DocumentTransform, boolean, DocumentTransform)
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ConditionalDocumentTransform(DocumentTransform ifTransform,
                                      boolean thenStrictness,
                                      ResultPolicy thenResultPolicy,
                                      DocumentTransform thenTransform1,
                                      DocumentTransform thenTransform2) {
    this(ifTransform,
         thenStrictness,
         new AggregateDocumentTransform(thenResultPolicy,
                                        thenTransform1,
                                        thenTransform2));
    logger.debug3("Implicitly aggregated two \"then\" transforms");
  }

  /**
   * <p>Builds a new conditional document transform using the given
   * strictness, out of the given "if" document transform and the
   * aggregation of the given "then" document transforms (using the
   * given aggregation result policy).</p>
   * @param ifTransform      An "if" document transform.
   * @param thenStrictness   True to wrap the aggregated "then" document
   *                         transform as a
   *                         {@link StrictDocumentTransform}, false
   *                         otherwise.
   * @param thenResultPolicy A result policy for the aggregate "then"
   *                         document transform.
   * @param thenTransform1   A "then" document transform.
   * @param thenTransform2   A "then" document transform.
   * @param thenTransform3   A "then" document transform.
   * @see #ConditionalDocumentTransform(DocumentTransform, boolean, DocumentTransform)
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ConditionalDocumentTransform(DocumentTransform ifTransform,
                                      boolean thenStrictness,
                                      ResultPolicy thenResultPolicy,
                                      DocumentTransform thenTransform1,
                                      DocumentTransform thenTransform2,
                                      DocumentTransform thenTransform3) {
    this(ifTransform,
         thenStrictness,
         new AggregateDocumentTransform(thenResultPolicy,
                                        thenTransform1,
                                        thenTransform2,
                                        thenTransform3));
    logger.debug3("Implicitly aggregated three \"then\" transforms");
  }

  /**
   * <p>Builds a new conditional document transform using the given
   * strictness, out of the given "if" document transform and the
   * aggregation of the given "then" document transforms (using the
   * given aggregation result policy).</p>
   * @param ifTransform      An "if" document transform.
   * @param thenStrictness   True to wrap the aggregated "then" document
   *                         transform as a
   *                         {@link StrictDocumentTransform}, false
   *                         otherwise.
   * @param thenResultPolicy A result policy for the aggregate "then"
   *                         document transform.
   * @param thenTransforms   An array of "then" document transform.
   * @see #ConditionalDocumentTransform(DocumentTransform, boolean, DocumentTransform)
   * @see AggregateDocumentTransform#AggregateDocumentTransform(PdfUtil.ResultPolicy, DocumentTransform[])
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ConditionalDocumentTransform(DocumentTransform ifTransform,
                                      boolean thenStrictness,
                                      ResultPolicy thenResultPolicy,
                                      DocumentTransform[] thenTransforms) {
    this(ifTransform,
         thenStrictness,
         new AggregateDocumentTransform(thenResultPolicy,
                                        thenTransforms));
    logger.debug3("Implicitly aggregated " + thenTransforms.length + " \"then\" transforms");
  }

  /**
   * <p>Builds a new conditional document transform using the default
   * strictness, out of the given "if" document transform and
   * "then" document transform.</p>
   * @param ifTransform    An "if" document transform.
   * @param thenTransform  A "then" document transform.
   * @see #ConditionalDocumentTransform(DocumentTransform, boolean, DocumentTransform)
   * @see #STRICTNESS_DEFAULT
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ConditionalDocumentTransform(DocumentTransform ifTransform,
                                      DocumentTransform thenTransform) {
    this(ifTransform,
         STRICTNESS_DEFAULT,
         thenTransform);
  }

  /**
   * <p>Builds a new conditional document transform using the default
   * strictness, out of the given "if" document transform and the
   * aggregation of the given "then" document transforms (using the
   * default aggregation result policy).</p>
   * @param ifTransform      An "if" document transform.
   * @param thenTransform1   A "then" document transform.
   * @param thenTransform2   A "then" document transform.
   * @see #ConditionalDocumentTransform(DocumentTransform, boolean, DocumentTransform, DocumentTransform)
   * @see #STRICTNESS_DEFAULT
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ConditionalDocumentTransform(DocumentTransform ifTransform,
                                      DocumentTransform thenTransform1,
                                      DocumentTransform thenTransform2) {
    this(ifTransform,
         STRICTNESS_DEFAULT,
         thenTransform1,
         thenTransform2);
  }

  /**
   * <p>Builds a new conditional document transform using the default
   * strictness, out of the given "if" document transform and the
   * aggregation of the given "then" document transforms (using the
   * default aggregation result policy).</p>
   * @param ifTransform      An "if" document transform.
   * @param thenTransform1   A "then" document transform.
   * @param thenTransform2   A "then" document transform.
   * @param thenTransform3   A "then" document transform.
   * @see #ConditionalDocumentTransform(DocumentTransform, boolean, DocumentTransform, DocumentTransform)
   * @see #STRICTNESS_DEFAULT
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ConditionalDocumentTransform(DocumentTransform ifTransform,
                                      DocumentTransform thenTransform1,
                                      DocumentTransform thenTransform2,
                                      DocumentTransform thenTransform3) {
    this(ifTransform,
         STRICTNESS_DEFAULT,
         thenTransform1,
         thenTransform2,
         thenTransform3);
  }

  /**
   * <p>Builds a new conditional document transform using the default
   * strictness, out of the given "if" document transform and the
   * aggregation of the given "then" document transforms (using the
   * default aggregation result policy).</p>
   * @param ifTransform    An "if" document transform.
   * @param thenTransforms  An array of "then" document transform.
   * @see ConditionalDocumentTransform#ConditionalDocumentTransform(DocumentTransform, boolean, DocumentTransform[])
   * @see #STRICTNESS_DEFAULT
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ConditionalDocumentTransform(DocumentTransform ifTransform,
                                      DocumentTransform[] thenTransforms) {
    this(ifTransform,
         STRICTNESS_DEFAULT,
         thenTransforms);
  }

  /**
   * <p>Builds a new conditional document transform using the default
   * strictness, out of the given "if" document transform and the
   * aggregation of the given "then" document transforms (using the
   * given aggregation result policy).</p>
   * @param ifTransform    An "if" document transform.
   * @param thenResultPolicy A result policy for the aggregate "then"
   *                         document transform.
   * @param thenTransform1  A "then" document transform.
   * @param thenTransform2  A "then" document transform.
   * @see #ConditionalDocumentTransform(DocumentTransform, boolean, PdfUtil.ResultPolicy, DocumentTransform, DocumentTransform)
   * @see #STRICTNESS_DEFAULT
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ConditionalDocumentTransform(DocumentTransform ifTransform,
                                      ResultPolicy thenResultPolicy,
                                      DocumentTransform thenTransform1,
                                      DocumentTransform thenTransform2) {
    this(ifTransform,
         STRICTNESS_DEFAULT,
         thenResultPolicy,
         thenTransform1,
         thenTransform2);
  }

  /**
   * <p>Builds a new conditional document transform using the default
   * strictness, out of the given "if" document transform and the
   * aggregation of the given "then" document transforms (using the
   * given aggregation result policy).</p>
   * @param ifTransform    An "if" document transform.
   * @param thenResultPolicy A result policy for the aggregate "then"
   *                         document transform.
   * @param thenTransform1  A "then" document transform.
   * @param thenTransform2  A "then" document transform.
   * @param thenTransform3  A "then" document transform.
   * @see #ConditionalDocumentTransform(DocumentTransform, boolean, PdfUtil.ResultPolicy, DocumentTransform, DocumentTransform)
   * @see #STRICTNESS_DEFAULT
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ConditionalDocumentTransform(DocumentTransform ifTransform,
                                      ResultPolicy thenResultPolicy,
                                      DocumentTransform thenTransform1,
                                      DocumentTransform thenTransform2,
                                      DocumentTransform thenTransform3) {
    this(ifTransform,
         STRICTNESS_DEFAULT,
         thenResultPolicy,
         thenTransform1,
         thenTransform2,
         thenTransform3);
  }

  /**
   * <p>Builds a new conditional document transform using the default
   * strictness, out of the given "if" document transform and the
   * aggregation of the given "then" document transforms (using the
   * given aggregation result policy).</p>
   * @param ifTransform      An "if" document transform.
   * @param thenResultPolicy A result policy for the aggregate "then"
   *                         document transform.
   * @param thenTransforms   An array of "then" document transform.
   * @see #ConditionalDocumentTransform(DocumentTransform, boolean, PdfUtil.ResultPolicy, DocumentTransform[])
   * @see #STRICTNESS_DEFAULT
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ConditionalDocumentTransform(DocumentTransform ifTransform,
                                      ResultPolicy thenResultPolicy,
                                      DocumentTransform[] thenTransforms) {
    this(ifTransform,
         STRICTNESS_DEFAULT,
         thenResultPolicy,
         thenTransforms);
  }

  /* Inherit documentation */
  @Deprecated
  public boolean transform(PdfDocument pdfDocument) throws IOException {
    logger.debug3("Begin conditional document transform");
    boolean ret = documentTransform.transform(pdfDocument);
    logger.debug2("Conditional document transform result: " + ret);
    return ret;
  }

  /**
   * <p>Te default strict policy for "then" transforms used by this
   * class.</p>
   * @see #ConditionalDocumentTransform(DocumentTransform, DocumentTransform)
   * @see #ConditionalDocumentTransform(DocumentTransform, DocumentTransform, DocumentTransform)
   * @see #ConditionalDocumentTransform(DocumentTransform, DocumentTransform, DocumentTransform, DocumentTransform)
   * @see #ConditionalDocumentTransform(DocumentTransform, DocumentTransform[])
   * @see #ConditionalDocumentTransform(DocumentTransform, PdfUtil.ResultPolicy, DocumentTransform, DocumentTransform)
   * @see #ConditionalDocumentTransform(DocumentTransform, PdfUtil.ResultPolicy, DocumentTransform, DocumentTransform, DocumentTransform)
   * @see #ConditionalDocumentTransform(DocumentTransform, PdfUtil.ResultPolicy, DocumentTransform[])
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static final boolean STRICTNESS_DEFAULT = true;

  /**
   * <p>A logger for use by this class.</p>
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  private static Logger logger = Logger.getLogger(ConditionalDocumentTransform.class);

}
