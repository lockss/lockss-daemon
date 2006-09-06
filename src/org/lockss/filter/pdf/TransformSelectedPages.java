/*
 * $Id: TransformSelectedPages.java,v 1.2 2006-09-01 06:47:00 thib_gc Exp $
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

/**
 * <p>A PDF transform that applies a PDF page transform to selected
 * pages of the PDF document.</p>
 * @author Thib Guicherd-Callin
 * @see #getSelectedPages
 */
public abstract class TransformSelectedPages implements PdfTransform {

  /**
   * <p>A PDF page transform.</p>
   */
  protected PdfPageTransform pdfPageTransform;

  /**
   * <p>Builds a new PDF transform with the given PDF page
   * transform.</p>
   * @param pdfPageTransform A PDF page transform.
   */
  protected TransformSelectedPages(PdfPageTransform pdfPageTransform) {
    this.pdfPageTransform = pdfPageTransform;
  }

  /* Inherit documentation */
  public void transform(PdfDocument pdfDocument) throws IOException {
    for (Iterator iter = getSelectedPages(pdfDocument) ; iter.hasNext() ; ) {
      pdfPageTransform.transform(pdfDocument, (PdfPage)iter.next());
    }
  }

  /**
   * <p>Gets an iterator of the pages selected for transformation by
   * this transform.</p>
   * @param pdfDocument A PDF document.
   * @return An iterator of PDF pages ({@link PdfPage}).
   * @throws IOException if any processing error occurs.
   */
  protected abstract ListIterator /* of PdfPage */ getSelectedPages(PdfDocument pdfDocument)
      throws IOException;

}
