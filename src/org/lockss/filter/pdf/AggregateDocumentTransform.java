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

import org.lockss.util.*;
import org.lockss.util.PdfUtil.ResultPolicy;

/**
 * <p>A document transform made of many other document transforms,
 * applied sequentially.</p>
 * @author Thib Guicherd-Callin
 * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
 */
@Deprecated
public class AggregateDocumentTransform implements DocumentTransform {

  /**
   * <p>A list of registered {@link DocumentTransform} instances.</p>
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  protected List /* of DocumentTransform */ documentTransforms;

  /**
   * <p>A result policy determining the boolean result of the
   * transform.</p>
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  protected ResultPolicy resultPolicy;

  /**
   * <p>Builds a new aggregate document transform using the default
   * result policy.</p>
   * @see #AggregateDocumentTransform(PdfUtil.ResultPolicy)
   * @see #POLICY_DEFAULT
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public AggregateDocumentTransform() {
    this(POLICY_DEFAULT);
  }

  /**
   * <p>Builds a new aggregate document transform using the default
   * result policy and registers the given document transforms.</p>
   * @param documentTransform A document transform.
   * @see #AggregateDocumentTransform(PdfUtil.ResultPolicy, DocumentTransform)
   * @see #POLICY_DEFAULT
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public AggregateDocumentTransform(DocumentTransform documentTransform) {
    this(POLICY_DEFAULT,
         documentTransform);
  }

  /**
   * <p>Builds a new aggregate document transform using the default
   * result policy and registers the given document transforms.</p>
   * @param documentTransform1 A document transform.
   * @param documentTransform2 A document transform.
   * @see #AggregateDocumentTransform(PdfUtil.ResultPolicy, DocumentTransform, DocumentTransform)
   * @see #POLICY_DEFAULT
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public AggregateDocumentTransform(DocumentTransform documentTransform1,
                                    DocumentTransform documentTransform2) {
    this(POLICY_DEFAULT,
         documentTransform1,
         documentTransform2);
  }

  /**
   * <p>Builds a new aggregate document transform using the default
   * result policy and registers the given document transforms.</p>
   * @param documentTransform1 A document transform.
   * @param documentTransform2 A document transform.
   * @param documentTransform3 A document transform.
   * @see #AggregateDocumentTransform(PdfUtil.ResultPolicy, DocumentTransform, DocumentTransform, DocumentTransform)
   * @see #POLICY_DEFAULT
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public AggregateDocumentTransform(DocumentTransform documentTransform1,
                                    DocumentTransform documentTransform2,
                                    DocumentTransform documentTransform3) {
    this(POLICY_DEFAULT,
         documentTransform1,
         documentTransform2,
         documentTransform3);
  }

  /**
   * <p>Builds a new aggregate document transform using the default
   * result policy and registers the given document transforms.</p>
   * @param documentTransform1 A document transform.
   * @param documentTransform2 A document transform.
   * @param documentTransform3 A document transform.
   * @param documentTransform4 A document transform.
   * @see #AggregateDocumentTransform(PdfUtil.ResultPolicy, DocumentTransform, DocumentTransform, DocumentTransform, DocumentTransform)
   * @see #POLICY_DEFAULT
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public AggregateDocumentTransform(DocumentTransform documentTransform1,
                                    DocumentTransform documentTransform2,
                                    DocumentTransform documentTransform3,
                                    DocumentTransform documentTransform4) {
    this(POLICY_DEFAULT,
         documentTransform1,
         documentTransform2,
         documentTransform3,
         documentTransform4);
  }

  /**
   * <p>Builds a new aggregate transform using the default result
   * policy and registers the given document transforms.</p>
   * @param documentTransforms An array of document transforms.
   * @see #AggregateDocumentTransform(DocumentTransform[])
   * @see #POLICY_DEFAULT
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public AggregateDocumentTransform(DocumentTransform[] documentTransforms) {
    this(POLICY_DEFAULT,
         documentTransforms);
  }

  /**
   * <p>Builds a new aggregate document transform using the given
   * result policy.</p>
   * @param resultPolicy A result policy.
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public AggregateDocumentTransform(ResultPolicy resultPolicy) {
    if (resultPolicy == null) {
      String logMessage = "Cannot specify a null result policy";
      logger.error(logMessage);
      throw new NullPointerException(logMessage);
    }
    logger.debug3("Setting up result policy " + resultPolicy.toString());
    this.resultPolicy = resultPolicy;
    this.documentTransforms = new ArrayList();
  }

  /**
   * <p>Builds a new aggregate document transform using the given
   * result policy and registers the given document transforms.</p>
   * @param resultPolicy       A result policy.
   * @param documentTransform A document transform.
   * @see #AggregateDocumentTransform(PdfUtil.ResultPolicy)
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public AggregateDocumentTransform(ResultPolicy resultPolicy,
                                    DocumentTransform documentTransform) {
    this(resultPolicy);
    logger.debug3("Setting up first document transform");
    add(documentTransform);
  }

  /**
   * <p>Builds a new aggregate document transform using the given
   * result policy and registers the given document transforms.</p>
   * @param resultPolicy       A result policy.
   * @param documentTransform1 A document transform.
   * @param documentTransform2 A document transform.
   * @see #AggregateDocumentTransform(PdfUtil.ResultPolicy, DocumentTransform)
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public AggregateDocumentTransform(ResultPolicy resultPolicy,
                                    DocumentTransform documentTransform1,
                                    DocumentTransform documentTransform2) {
    this(resultPolicy,
         documentTransform1);
    logger.debug3("Setting up second document transform");
    add(documentTransform2);
  }

  /**
   * <p>Builds a new aggregate document transform using the given
   * result policy and registers the given document transforms.</p>
   * @param resultPolicy       A result policy.
   * @param documentTransform1 A document transform.
   * @param documentTransform2 A document transform.
   * @param documentTransform3 A document transform.
   * @see #AggregateDocumentTransform(PdfUtil.ResultPolicy, DocumentTransform, DocumentTransform)
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public AggregateDocumentTransform(ResultPolicy resultPolicy,
                                    DocumentTransform documentTransform1,
                                    DocumentTransform documentTransform2,
                                    DocumentTransform documentTransform3) {
    this(resultPolicy,
         documentTransform1,
         documentTransform2);
    logger.debug3("Setting up third document transform");
    add(documentTransform3);
  }

  /**
   * <p>Builds a new aggregate transform using the given result
   * policy and registers the given document transforms.</p>
   * @param resultPolicy       A result policy.
   * @param documentTransform1 A document transform.
   * @param documentTransform2 A document transform.
   * @param documentTransform3 A document transform.
   * @param documentTransform4 A document transform.
   * @see #AggregateDocumentTransform(PdfUtil.ResultPolicy, DocumentTransform, DocumentTransform, DocumentTransform)
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public AggregateDocumentTransform(ResultPolicy resultPolicy,
                                    DocumentTransform documentTransform1,
                                    DocumentTransform documentTransform2,
                                    DocumentTransform documentTransform3,
                                    DocumentTransform documentTransform4) {
    this(resultPolicy,

         documentTransform1,
         documentTransform2,
         documentTransform3);
    logger.debug3("Setting up fourth document transform");
    add(documentTransform4);
  }

  /**
   * <p>Builds a new aggregate transform using the given result
   * policy and registers the given document transforms.</p>
   * @param resultPolicy       A result policy.
   * @param documentTransforms An array of document transforms.
   * @see #AggregateDocumentTransform(PdfUtil.ResultPolicy)
   * @see #add(DocumentTransform[])
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public AggregateDocumentTransform(ResultPolicy resultPolicy,
                                    DocumentTransform[] documentTransforms) {
    this(resultPolicy);
    logger.debug3("Setting up " + documentTransforms.length + " document transforms");
    add(documentTransforms);
  }


  /**
   * <p>Registers a new {@link DocumentTransform} instance with
   * this aggregate document transform.</p>
   * <p>When transforming a PDF document, the actions performed by the
   * registered tranforms are applied in the order the transforms
   * were registered with this method.</p>
   * @param documentTransform A {@link DocumentTransform} instance.
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public synchronized void add(DocumentTransform documentTransform) {
    if (documentTransform == null) {
      String logMessage = "Cannot add a null document transform";
      logger.error(logMessage);
      throw new NullPointerException(logMessage);
    }
    logger.debug3("Adding a document transform");
    documentTransforms.add(documentTransform);
  }

  /**
   * <p>Registers new {@link DocumentTransform} instances with
   * this aggregate document transform.</p>
   * <p>When transforming a PDF document, the actions performed by the
   * registered tranforms are applied in the order the transforms
   * were registered with this method. This method registers the
   * document transforms in the array in array order.</p>
   * @param documentTransforms An array of {@link DocumentTransform}
   *                           instances.
   * @see #add(DocumentTransform)
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void add(DocumentTransform[] documentTransforms) {
    if (documentTransforms == null) {
      String logMessage = "Cannot add a null array of document transforms";
      logger.error(logMessage);
      throw new NullPointerException(logMessage);
    }
    for (int tra = 0 ; tra < documentTransforms.length ; ++tra) {
      logger.debug3("Adding document transform at index " + tra);
      add(documentTransforms[tra]);
    }
  }

  /* Inherit documentation */
  @Deprecated
  public synchronized boolean transform(PdfDocument pdfDocument) throws IOException {
    logger.debug2("Begin aggregate document transform with result policy " + resultPolicy.toString());
    boolean success = resultPolicy.initialValue();
    logger.debug3("Aggregate success flag initially " + success);
    for (Iterator iter = documentTransforms.iterator() ; iter.hasNext() ; ) {
      DocumentTransform documentTransform = (DocumentTransform)iter.next();
      success = resultPolicy.updateResult(success, documentTransform.transform(pdfDocument));
      logger.debug3("Aggregate success flag now " + success);
      if (!resultPolicy.shouldKeepGoing(success)) {
        logger.debug3("Aggregation should not keep going");
        break;
      }
    }
    logger.debug2("Aggregate document transform result: " + success);
    return success;
  }

  /**
   * <p>The default result policy used by this class.</p>
   * @see #AggregateDocumentTransform()
   * @see #AggregateDocumentTransform(DocumentTransform)
   * @see #AggregateDocumentTransform(DocumentTransform, DocumentTransform)
   * @see #AggregateDocumentTransform(DocumentTransform, DocumentTransform, DocumentTransform)
   * @see #AggregateDocumentTransform(DocumentTransform, DocumentTransform, DocumentTransform, DocumentTransform)
   * @see #AggregateDocumentTransform(DocumentTransform[])
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static final ResultPolicy POLICY_DEFAULT = PdfUtil.AND;

  /**
   * <p>A logger for use by this class.</p>
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  private static Logger logger = Logger.getLogger(AggregateDocumentTransform.class);

}
