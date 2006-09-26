/*
 * $Id: ConditionalDocumentTransform.java,v 1.5 2006-09-26 07:32:24 thib_gc Exp $
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

import org.lockss.filter.pdf.DocumentTransformUtil.*;
import org.lockss.util.*;
import org.lockss.util.PdfUtil.ResultPolicy;

/**
 * <p>A document transform decorator that applies a "then" document
 * transform only if the PDF document to be transformed is recognized
 * by an "if" document transform.</p>
 * @author Thib Guicherd-Callin
 */
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
   */
  public ConditionalDocumentTransform(DocumentTransform ifTransform,
                                      boolean thenStrictness,
                                      DocumentTransform thenTransform) {
    super(new AggregateDocumentTransform(ifTransform,
                                         !thenStrictness || thenTransform instanceof StrictDocumentTransform
                                         ? thenTransform
                                         : new StrictDocumentTransform(thenTransform)));
    if (logger.isDebug2()) {
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
      logger.debug2(buffer.toString());
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
   */
  public ConditionalDocumentTransform(DocumentTransform ifTransform,
                                      boolean thenStrictness,
                                      DocumentTransform thenTransform1,
                                      DocumentTransform thenTransform2) {
    this(ifTransform,
         thenStrictness,
         new AggregateDocumentTransform(thenTransform1,
                                        thenTransform2));
    logger.debug2("Implicitly aggregated two \"then\" transforms");
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
   */
  public ConditionalDocumentTransform(DocumentTransform ifTransform,
                                      boolean thenStrictness,
                                      DocumentTransform[] thenTransforms) {
    this(ifTransform,
         thenStrictness,
         new AggregateDocumentTransform(thenTransforms));
    logger.debug2("Implicitly aggregated " + thenTransforms.length + " \"then\" transforms");
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
   */
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
    logger.debug2("Implicitly aggregated two \"then\" transforms");
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
   * @see AggregateDocumentTransform#AggregateDocumentTransform(ResultPolicy, DocumentTransform[])
   */
  public ConditionalDocumentTransform(DocumentTransform ifTransform,
                                      boolean thenStrictness,
                                      ResultPolicy thenResultPolicy,
                                      DocumentTransform[] thenTransforms) {
    this(ifTransform,
         thenStrictness,
         new AggregateDocumentTransform(thenResultPolicy,
                                        thenTransforms));
    logger.debug2("Implicitly aggregated " + thenTransforms.length + " \"then\" transforms");
  }

  /**
   * <p>Builds a new conditional document transform using the default
   * strictness, out of the given "if" document transform and
   * "then" document transform.</p>
   * @param ifTransform    An "if" document transform.
   * @param thenTransform  A "then" document transform.
   * @see #ConditionalDocumentTransform(DocumentTransform, boolean, DocumentTransform)
   * @see #STRICTNESS_DEFAULT
   */
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
   */
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
   * @param ifTransform    An "if" document transform.
   * @param thenTransforms  An array of "then" document transform.
   * @see ConditionalDocumentTransform#ConditionalDocumentTransform(DocumentTransform, boolean, DocumentTransform[])
   * @see #STRICTNESS_DEFAULT
   */
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
   * @see #ConditionalDocumentTransform(DocumentTransform, boolean, ResultPolicy, DocumentTransform, DocumentTransform)
   * @see #STRICTNESS_DEFAULT
   */
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
   * @param ifTransform      An "if" document transform.
   * @param thenResultPolicy A result policy for the aggregate "then"
   *                         document transform.
   * @param thenTransforms   An array of "then" document transform.
   * @see #ConditionalDocumentTransform(DocumentTransform, boolean, ResultPolicy, DocumentTransform[])
   * @see #STRICTNESS_DEFAULT
   */
  public ConditionalDocumentTransform(DocumentTransform ifTransform,
                                      ResultPolicy thenResultPolicy,
                                      DocumentTransform[] thenTransforms) {
    this(ifTransform,
         STRICTNESS_DEFAULT,
         thenResultPolicy,
         thenTransforms);
  }

  /* Inherit documentation */
  public boolean transform(PdfDocument pdfDocument) throws IOException {
    logger.debug2("Begin conditional document transform");
    boolean ret = documentTransform.transform(pdfDocument);
    logger.debug("Conditional document transform result: " + ret);
    return ret;
  }

  /**
   * <p>Te default strict policy for "then" transforms used by this
   * class.</p>
   * @see #ConditionalDocumentTransform(DocumentTransform, DocumentTransform)
   * @see #ConditionalDocumentTransform(DocumentTransform, DocumentTransform, DocumentTransform)
   * @see #ConditionalDocumentTransform(DocumentTransform, DocumentTransform[])
   * @see #ConditionalDocumentTransform(DocumentTransform, ResultPolicy, DocumentTransform, DocumentTransform)
   * @see #ConditionalDocumentTransform(DocumentTransform, ResultPolicy, DocumentTransform[])
   */
  public static final boolean STRICTNESS_DEFAULT = true;

  /**
   * <p>A logger for use by this class.</p>
   */
  private static Logger logger = Logger.getLogger("ConditionalDocumentTransform");

}
