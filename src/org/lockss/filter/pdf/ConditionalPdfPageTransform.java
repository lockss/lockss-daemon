/*
 * $Id: ConditionalPdfPageTransform.java,v 1.1 2006-09-01 06:47:00 thib_gc Exp $
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

import org.lockss.util.*;

/**
 * <p>A PDF page transform decorator that applies a given PDF page
 * transform only if the PDF page to be transformed is recognized by
 * the {@link #identify} method.</p>
 * @author Thib Guicherd-Callin
 */
public abstract class ConditionalPdfPageTransform implements PdfPageTransform {

  /**
   * <p>The PDF page transform to be applied conditionally.</p>
   */
  protected PdfPageTransform pdfPageTransform;

  /**
   * <p>Decorates the given PDF page transform.</p>
   * @param pdfPageTransform A PDF page transform to be applied
   *                         conditionally.
   */
  public ConditionalPdfPageTransform(PdfPageTransform pdfPageTransform) {
    this.pdfPageTransform = pdfPageTransform;
  }

  /**
   * <p>Determines if the argument page should be transformed by this
   * page transform.</p>
   * @param pdfDocument A PDF document (from {@link #transform}).
   * @param pdfPage     A PDF page (from {@link #transform}).
   * @return True if the underlying PDF page transform should be
   *         applied, false otherwise.
   * @throws IOException if any processing error occurs.
   */
  public abstract boolean identify(PdfDocument pdfDocument,
                                   PdfPage pdfPage)
      throws IOException;

  /* Inherit documentation */
  public void transform(PdfDocument pdfDocument,
                        PdfPage pdfPage)
      throws IOException {
    if (identify(pdfDocument, pdfPage)) {
      pdfPageTransform.transform(pdfDocument, pdfPage);
    }
  }

}
