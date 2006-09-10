/*
 * $Id: AggregateDocumentTransform.java,v 1.1 2006-09-10 07:50:50 thib_gc Exp $
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
 * <p>A PDF transform made of many other PDF transforms, applied
 * sequentially.</p>
 * @author Thib Guicherd-Callin
 */
public class AggregateDocumentTransform implements DocumentTransform {

  protected static final ResultPolicy POLICY_BY_DEFAULT = PdfUtil.AND;

  /**
   * <p>A list of registered {@link DocumentTransform} instances.</p>
   */
  protected List /* of DocumentTransform */ documentTransforms;

  protected ResultPolicy resultPolicy;

  public AggregateDocumentTransform(ResultPolicy resultPolicy) {
    this.resultPolicy = resultPolicy;
    this.documentTransforms = new ArrayList();
  }

  public AggregateDocumentTransform(ResultPolicy resultPolicy,
                                    DocumentTransform documentTransform1) {
    this(resultPolicy);
    add(documentTransform1);
  }

  public AggregateDocumentTransform(ResultPolicy resultPolicy,
                                    DocumentTransform documentTransform1,
                                    DocumentTransform documentTransform2) {
    this(resultPolicy,
         documentTransform1);
    add(documentTransform2);
  }

  public AggregateDocumentTransform(ResultPolicy resultPolicy,
                                    DocumentTransform documentTransform1,
                                    DocumentTransform documentTransform2,
                                    DocumentTransform documentTransform3) {
    this(resultPolicy,
         documentTransform1,
         documentTransform2);
    add(documentTransform3);
  }

  public AggregateDocumentTransform(ResultPolicy resultPolicy,
                                    DocumentTransform documentTransform1,
                                    DocumentTransform documentTransform2,
                                    DocumentTransform documentTransform3,
                                    DocumentTransform documentTransform4) {
    this(resultPolicy,
         documentTransform1,
         documentTransform2,
         documentTransform3);
    add(documentTransform4);
  }

  public AggregateDocumentTransform() {
    this(POLICY_BY_DEFAULT);
  }

  public AggregateDocumentTransform(DocumentTransform documentTransform1) {
    this(POLICY_BY_DEFAULT);
    add(documentTransform1);
  }

  public AggregateDocumentTransform(DocumentTransform documentTransform1,
                                    DocumentTransform documentTransform2) {
    this(POLICY_BY_DEFAULT,
         documentTransform1);
    add(documentTransform2);
  }

  public AggregateDocumentTransform(DocumentTransform documentTransform1,
                                    DocumentTransform documentTransform2,
                                    DocumentTransform documentTransform3) {
    this(POLICY_BY_DEFAULT,
         documentTransform1,
         documentTransform2);
    add(documentTransform3);
  }

  public AggregateDocumentTransform(DocumentTransform documentTransform1,
                                    DocumentTransform documentTransform2,
                                    DocumentTransform documentTransform3,
                                    DocumentTransform documentTransform4) {
    this(POLICY_BY_DEFAULT,
         documentTransform1,
         documentTransform2,
         documentTransform3);
    add(documentTransform4);
  }

  /**
   * <p>Registers a new {@link DocumentTransform} instance with
   * this compound transform.</p>
   * <p>When transforming a PDF document, the actions performed by the
   * registered tranforms are applied in the order the transforms
   * were registered with this method.</p>
   * @param documentTransform A {@link DocumentTransform} instance.
   */
  public synchronized void add(DocumentTransform documentTransform) {
    documentTransforms.add(documentTransform);
  }

  public synchronized boolean transform(PdfDocument pdfDocument) throws IOException {
    boolean success = resultPolicy.resetResult();
    for (Iterator iter = documentTransforms.iterator() ; iter.hasNext() ; ) {
      DocumentTransform documentTransform = (DocumentTransform)iter.next();
      success = resultPolicy.updateResult(success, documentTransform.transform(pdfDocument));
      if (!resultPolicy.shouldKeepGoing(success)) {
        break;
      }
    }
    return success;
  }

  /**
   * <p>A logger for use by this class.</p>
   */
  protected static Logger logger = Logger.getLogger("AggregateDocumentTransform");

}
