/*
 * $Id: AggregateDocumentTransform.java,v 1.6 2006-11-01 22:25:45 thib_gc Exp $
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
import java.util.*;

import org.lockss.util.*;
import org.lockss.util.PdfUtil.ResultPolicy;

/**
 * <p>A document transform made of many other document transforms,
 * applied sequentially.</p>
 * @author Thib Guicherd-Callin
 */
public class AggregateDocumentTransform implements DocumentTransform {

  /**
   * <p>A list of registered {@link DocumentTransform} instances.</p>
   */
  protected List /* of DocumentTransform */ documentTransforms;

  /**
   * <p>A result policy determining the boolean result of the
   * transform.</p>
   */
  protected ResultPolicy resultPolicy;

  /**
   * <p>Builds a new aggregate document transform using the default
   * result policy.</p>
   * @see #AggregateDocumentTransform(PdfUtil.ResultPolicy)
   * @see #POLICY_DEFAULT
   */
  public AggregateDocumentTransform() {
    this(POLICY_DEFAULT);
  }

  /**
   * <p>Builds a new aggregate document transform using the default
   * result policy and registers the given document transforms.</p>
   * @param documentTransform A document transform.
   * @see #AggregateDocumentTransform(PdfUtil.ResultPolicy, DocumentTransform)
   * @see #POLICY_DEFAULT
   */
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
   */
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
   */
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
   */
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
   */
  public AggregateDocumentTransform(DocumentTransform[] documentTransforms) {
    this(POLICY_DEFAULT,
         documentTransforms);
  }

  /**
   * <p>Builds a new aggregate document transform using the given
   * result policy.</p>
   * @param resultPolicy A result policy.
   */
  public AggregateDocumentTransform(ResultPolicy resultPolicy) {
    if (resultPolicy == null) {
      String logMessage = "Cannot specify a null result policy";
      logger.error(logMessage);
      throw new NullPointerException(logMessage);
    }
    logger.debug2("Setting up result policy " + resultPolicy.toString());
    this.resultPolicy = resultPolicy;
    this.documentTransforms = new ArrayList();
  }

  /**
   * <p>Builds a new aggregate document transform using the given
   * result policy and registers the given document transforms.</p>
   * @param resultPolicy       A result policy.
   * @param documentTransform A document transform.
   * @see #AggregateDocumentTransform(PdfUtil.ResultPolicy)
   */
  public AggregateDocumentTransform(ResultPolicy resultPolicy,
                                    DocumentTransform documentTransform) {
    this(resultPolicy);
    logger.debug2("Setting up first document transform");
    add(documentTransform);
  }

  /**
   * <p>Builds a new aggregate document transform using the given
   * result policy and registers the given document transforms.</p>
   * @param resultPolicy       A result policy.
   * @param documentTransform1 A document transform.
   * @param documentTransform2 A document transform.
   * @see #AggregateDocumentTransform(PdfUtil.ResultPolicy, DocumentTransform)
   */
  public AggregateDocumentTransform(ResultPolicy resultPolicy,
                                    DocumentTransform documentTransform1,
                                    DocumentTransform documentTransform2) {
    this(resultPolicy,
         documentTransform1);
    logger.debug2("Setting up second document transform");
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
   */
  public AggregateDocumentTransform(ResultPolicy resultPolicy,
                                    DocumentTransform documentTransform1,
                                    DocumentTransform documentTransform2,
                                    DocumentTransform documentTransform3) {
    this(resultPolicy,
         documentTransform1,
         documentTransform2);
    logger.debug2("Setting up third document transform");
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
   */
  public AggregateDocumentTransform(ResultPolicy resultPolicy,
                                    DocumentTransform documentTransform1,
                                    DocumentTransform documentTransform2,
                                    DocumentTransform documentTransform3,
                                    DocumentTransform documentTransform4) {
    this(resultPolicy,

         documentTransform1,
         documentTransform2,
         documentTransform3);
    logger.debug2("Setting up fourth document transform");
    add(documentTransform4);
  }

  /**
   * <p>Builds a new aggregate transform using the given result
   * policy and registers the given document transforms.</p>
   * @param resultPolicy       A result policy.
   * @param documentTransforms An array of document transforms.
   * @see #AggregateDocumentTransform(PdfUtil.ResultPolicy)
   * @see #add(DocumentTransform[])
   */
  public AggregateDocumentTransform(ResultPolicy resultPolicy,
                                    DocumentTransform[] documentTransforms) {
    this(resultPolicy);
    logger.debug2("Setting up " + documentTransforms.length + " document transforms");
    add(documentTransforms);
  }


  /**
   * <p>Registers a new {@link DocumentTransform} instance with
   * this aggregate document transform.</p>
   * <p>When transforming a PDF document, the actions performed by the
   * registered tranforms are applied in the order the transforms
   * were registered with this method.</p>
   * @param documentTransform A {@link DocumentTransform} instance.
   */
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
   */
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
    logger.debug("Aggregate document transform result: " + success);
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
   */
  public static final ResultPolicy POLICY_DEFAULT = PdfUtil.AND;

  /**
   * <p>A logger for use by this class.</p>
   */
  private static Logger logger = Logger.getLogger("AggregateDocumentTransform");

}
