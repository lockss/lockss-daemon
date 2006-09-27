/*
 * $Id: SimpleOutputDocumentTransform.java,v 1.2 2006-09-27 08:00:32 thib_gc Exp $
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

import java.io.*;

import org.lockss.filter.pdf.DocumentTransformUtil.DocumentTransformDecorator;
import org.lockss.util.*;

/**
 * <p>An output document transform that processes PDF documents with
 * a given document transform, then saves the resulting PDF document
 * into the output stream.</p>
 * @author Thib Guicherd-Callin
 * @see PdfDocument#save
 */
public class SimpleOutputDocumentTransform
    extends DocumentTransformDecorator
    implements OutputDocumentTransform {

  /**
   * <p>Builds a new simple output document transform based on the
   * given document transform.</p>
   * @param documentTransform A document transform.
   * @see DocumentTransformDecorator#DocumentTransformDecorator(DocumentTransform)
   */
  public SimpleOutputDocumentTransform(DocumentTransform documentTransform) {
    super(documentTransform);
  }

  /* Inherit documentation */
  public boolean transform(PdfDocument pdfDocument) throws IOException {
    return documentTransform.transform(pdfDocument);
  }

  /* Inherit documentation */
  public boolean transform(PdfDocument pdfDocument,
                           OutputStream outputStream) {
    return PdfUtil.applyAndSave(this,
                                pdfDocument,
                                outputStream);
  }

}
