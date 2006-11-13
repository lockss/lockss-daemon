/*
 * $Id: ConditionalPageTransform.java,v 1.7 2006-11-13 21:27:12 thib_gc Exp $
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

import org.lockss.filter.pdf.PageTransformUtil.*;
import org.lockss.util.*;
import org.lockss.util.PdfUtil.ResultPolicy;

/**
 * <p>A page transform decorator that applies a "then" page
 * transform only if the PDF page to be transformed is recognized
 * by an "if" page transform.</p>
 * @author Thib Guicherd-Callin
 */
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
   */
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
   */
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
   */
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
   */
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
   */
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
   */
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
   */
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
   */
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
   */
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
   */
  public ConditionalPageTransform(PageTransform ifTransform,
                                  ResultPolicy thenResultPolicy,
                                  PageTransform[] thenTransforms) {
    this(ifTransform,
         STRICTNESS_DEFAULT,
         thenResultPolicy,
         thenTransforms);
  }

  /* Inherit documentation */
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
   */
  public static final boolean STRICTNESS_DEFAULT = true;

  /**
   * <p>A logger for use by this class.</p>
   */
  private static Logger logger = Logger.getLogger("ConditionalPageTransform");

}
