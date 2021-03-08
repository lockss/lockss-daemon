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

import org.lockss.filter.pdf.PageTransformUtil.*;
import org.lockss.util.*;
import org.lockss.util.PdfUtil.ResultPolicy;

/**
 * <p>A page transform decorator that applies a "then" page
 * transform only if the PDF page to be transformed is recognized
 * by an "if" page transform.</p>
 * @author Thib Guicherd-Callin
 * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
 */
@Deprecated
public class ConditionalPageTransform extends PageTransformDecorator {

  /**
   * <p>Builds a new conditional page transform using the given
   * strictness, out of the given "if" page transform and
   * "then" page transform.</p>
   * @param ifTransform    An "if" page transform.
   * @param thenStrictness True to wrap the "then" page transform
   *                       as a {@link StrictPageTransform} if it
   *                       is not one already, false otherwise.
   * @param thenTransform  A "then" page transform.
   * @see PageTransformDecorator#PageTransformDecorator(PageTransform)
   * @see AggregatePageTransform#AggregatePageTransform(PageTransform, PageTransform)
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ConditionalPageTransform(PageTransform ifTransform,
                                  boolean thenStrictness,
                                  PageTransform thenTransform) {
    super(new AggregatePageTransform(ifTransform,
                                     !thenStrictness || thenTransform instanceof StrictPageTransform
                                     ? thenTransform
                                     : new StrictPageTransform(thenTransform)));
    if (logger.isDebug3()) {
      StringBuffer buffer = new StringBuffer();
      buffer.append("Done setting up conditional page transform ");
      if (thenTransform instanceof StrictPageTransform) {
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
   * <p>Builds a new conditional page transform using the given
   * strictness, out of the given "if" page transform and the
   * aggregation of the given "then" page transforms (using the
   * default aggregation result policy).</p>
   * @param ifTransform    An "if" page transform.
   * @param thenStrictness True to wrap the aggregated "then" page
   *                       transform as a
   *                       {@link StrictPageTransform}, false
   *                       otherwise.
   * @param thenTransform1  A "then" page transform.
   * @param thenTransform2  A "then" page transform.
   * @see #ConditionalPageTransform(PageTransform, boolean, PageTransform)
   * @see AggregatePageTransform#AggregatePageTransform(PageTransform, PageTransform)
   * @see AggregatePageTransform#POLICY_DEFAULT
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ConditionalPageTransform(PageTransform ifTransform,
                                  boolean thenStrictness,
                                  PageTransform thenTransform1,
                                  PageTransform thenTransform2) {
    this(ifTransform,
         thenStrictness,
         new AggregatePageTransform(thenTransform1,
                                    thenTransform2));
    logger.debug3("Implicitly aggregated two \"then\" transforms");
  }

  /**
   * <p>Builds a new conditional page transform using the given
   * strictness, out of the given "if" page transform and the
   * aggregation of the given "then" page transforms (using the
   * default aggregation result policy).</p>
   * @param ifTransform      An "if" page transform.
   * @param thenStrictness   True to wrap the aggregated "then"
   *                        page transform as a
   *                         {@link StrictPageTransform}, false
   *                         otherwise.
   * @param thenTransforms   An array of "then" page transforms.
   * @see #ConditionalPageTransform(PageTransform, boolean, PageTransform)
   * @see AggregatePageTransform#AggregatePageTransform(PageTransform[])
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ConditionalPageTransform(PageTransform ifTransform,
                                  boolean thenStrictness,
                                  PageTransform[] thenTransforms) {
    this(ifTransform,
         thenStrictness,
         new AggregatePageTransform(thenTransforms));
    logger.debug3("Implicitly aggregated " + thenTransforms.length + " \"then\" transforms");
  }

  /**
   * <p>Builds a new conditional page transform using the given
   * strictness, out of the given "if" page transform and the
   * aggregation of the given "then" page transforms (using the
   * given aggregation result policy).</p>
   * @param ifTransform      An "if" page transform.
   * @param thenStrictness   True to wrap the aggregated "then"
   *                         page transform as a
   *                         {@link StrictPageTransform}, false
   *                         otherwise.
   * @param thenResultPolicy A result policy for the aggregate "then"
   *                         page transform.
   * @param thenTransform1   A "then" page transform.
   * @param thenTransform2   A "then" page transform.
   * @see #ConditionalPageTransform(PageTransform, boolean, PageTransform)
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ConditionalPageTransform(PageTransform ifTransform,
                                  boolean thenStrictness,
                                  ResultPolicy thenResultPolicy,
                                  PageTransform thenTransform1,
                                  PageTransform thenTransform2) {
    this(ifTransform,
         thenStrictness,
         new AggregatePageTransform(thenResultPolicy,
                                    thenTransform1,
                                    thenTransform2));
    logger.debug3("Implicitly aggregated two \"then\" transforms");
  }

  /**
   * <p>Builds a new conditional page transform using the given
   * strictness, out of the given "if" page transform and the
   * aggregation of the given "then" page transforms (using the
   * given aggregation result policy).</p>
   * @param ifTransform      An "if" page transform.
   * @param thenStrictness   True to wrap the aggregated "then"
   *                         page transform as a
   *                         {@link StrictPageTransform}, false
   *                         otherwise.
   * @param thenResultPolicy A result policy for the aggregate "then"
   *                         page transform.
   * @param thenTransforms   An array of "then" page transforms.
   * @see #ConditionalPageTransform(PageTransform, boolean, PageTransform)
   * @see AggregatePageTransform#AggregatePageTransform(PdfUtil.ResultPolicy, PageTransform[])
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ConditionalPageTransform(PageTransform ifTransform,
                                  boolean thenStrictness,
                                  ResultPolicy thenResultPolicy,
                                  PageTransform[] thenTransforms) {
    this(ifTransform,
         thenStrictness,
         new AggregatePageTransform(thenResultPolicy,
                                    thenTransforms));
    logger.debug3("Implicitly aggregated " + thenTransforms.length + " \"then\" transforms");
  }

  /**
   * <p>Builds a new conditional page transform using the default
   * strictness, out of the given "if" page transform and
   * "then" page transform.</p>
   * @param ifTransform    An "if" page transform.
   * @param thenTransform  A "then" page transform.
   * @see #ConditionalPageTransform(PageTransform, boolean, PageTransform)
   * @see #STRICTNESS_DEFAULT
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ConditionalPageTransform(PageTransform ifTransform,
                                  PageTransform thenTransform) {
    this(ifTransform,
         STRICTNESS_DEFAULT,
         thenTransform);
  }

  /**
   * <p>Builds a new conditional page transform using the default
   * strictness, out of the given "if" page transform and the
   * aggregation of the given "then" page transforms (using the
   * default aggregation result policy).</p>
   * @param ifTransform      An "if" page transform.
   * @param thenTransform1   A "then" page transform.
   * @param thenTransform2   A "then" page transform.
   * @see #ConditionalPageTransform(PageTransform, boolean, PageTransform, PageTransform)
   * @see #STRICTNESS_DEFAULT
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ConditionalPageTransform(PageTransform ifTransform,
                                  PageTransform thenTransform1,
                                  PageTransform thenTransform2) {
    this(ifTransform,
         STRICTNESS_DEFAULT,
         thenTransform1,
         thenTransform2);
  }

  /**
   * <p>Builds a new conditional page transform using the default
   * strictness, out of the given "if" page transform and the
   * aggregation of the given "then" page transforms (using the
   * default aggregation result policy).</p>
   * @param ifTransform      An "if" page transform.
   * @param thenTransforms   An array of "then" page transforms.
   * @see #ConditionalPageTransform(PageTransform, boolean, PdfUtil.ResultPolicy, PageTransform, PageTransform)
   * @see #STRICTNESS_DEFAULT
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ConditionalPageTransform(PageTransform ifTransform,
                                  PageTransform[] thenTransforms) {
    this(ifTransform,
         STRICTNESS_DEFAULT,
         thenTransforms);
  }

  /**
   * <p>Builds a new conditional page transform using the default
   * strictness, out of the given "if" page transform and the
   * aggregation of the given "then" page transforms (using the
   * given aggregation result policy).</p>
   * @param ifTransform    An "if" page transform.
   * @param thenResultPolicy A result policy for the aggregate "then"
   *                         page transform.
   * @param thenTransform1  A "then" page transform.
   * @param thenTransform2  A "then" page transform.
   * @see #ConditionalPageTransform(PageTransform, boolean, PdfUtil.ResultPolicy, PageTransform, PageTransform)
   * @see #STRICTNESS_DEFAULT
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ConditionalPageTransform(PageTransform ifTransform,
                                  ResultPolicy thenResultPolicy,
                                  PageTransform thenTransform1,
                                  PageTransform thenTransform2) {
    this(ifTransform,
         STRICTNESS_DEFAULT,
         thenResultPolicy,
         thenTransform1,
         thenTransform2);
  }

  /**
   * <p>Builds a new conditional page transform using the default
   * strictness, out of the given "if" page transform and the
   * aggregation of the given "then" page transforms (using the
   * given aggregation result policy).</p>
   * @param ifTransform      An "if" page transform.
   * @param thenResultPolicy A result policy for the aggregate "then"
   *                         page transform.
   * @param thenTransforms   An array of "then" page transforms.
   * @see #ConditionalPageTransform(PageTransform, boolean, PdfUtil.ResultPolicy, PageTransform, PageTransform)
   * @see #STRICTNESS_DEFAULT
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public ConditionalPageTransform(PageTransform ifTransform,
                                  ResultPolicy thenResultPolicy,
                                  PageTransform[] thenTransforms) {
    this(ifTransform,
         STRICTNESS_DEFAULT,
         thenResultPolicy,
         thenTransforms);
  }

  /* Inherit documentation */
  @Deprecated
  public boolean transform(PdfPage pdfPage) throws IOException {
    logger.debug3("Begin conditional page transform");
    boolean ret = pageTransform.transform(pdfPage);
    logger.debug2("Conditional page transform result: " + ret);
    return ret;
  }

  /**
   * <p>Te default strict policy for "then" transforms used by this
   * class.</p>
   * @see #ConditionalPageTransform(PageTransform, PageTransform)
   * @see #ConditionalPageTransform(PageTransform, PageTransform, PageTransform)
   * @see #ConditionalPageTransform(PageTransform, PageTransform[])
   * @see #ConditionalPageTransform(PageTransform, PdfUtil.ResultPolicy, PageTransform, PageTransform)
   * @see #ConditionalPageTransform(PageTransform, PdfUtil.ResultPolicy, PageTransform[])
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static final boolean STRICTNESS_DEFAULT = true;

  /**
   * <p>A logger for use by this class.</p>
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  private static Logger logger = Logger.getLogger(ConditionalPageTransform.class);

}
