/*
 * $Id: ConditionalPdfTransform.java,v 1.1 2006-08-23 19:14:06 thib_gc Exp $
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

import org.lockss.util.PdfDocument;

/**
 * <p>A PDF transform decorator that applies a given PDF transform
 * only if the PDF document to be transformed is recognized by the
 * {@link #identify} method.</p>
 * @author Thib Guicherd-Callin
 */
public abstract class ConditionalPdfTransform implements PdfTransform {

  /**
   * <p>The PDF transform to be applied conditionally.</p>
   */
  protected PdfTransform pdfTransform;

  /**
   * <p>Decorates the given PDF transform.</p>
   * @param pdfTransform A PDF transform to be applied conditionally.
   */
  public ConditionalPdfTransform(PdfTransform pdfTransform) {
    this.pdfTransform = pdfTransform;
  }

  /**
   * <p>Determines if the argument should be transformed by this
   * transform.</p>
   * @param pdfDocument A PDF document (from {@link #transform}).
   * @return True if the underlying PDF transform should be applied,
   *         false otherwise.
   * @throws IOException if any processing error occurs.
   */
  public abstract boolean identify(PdfDocument pdfDocument) throws IOException;

  /* Inherit documentation */
  public void transform(PdfDocument pdfDocument) throws IOException {
    if (identify(pdfDocument)) {
      pdfTransform.transform(pdfDocument);
    }
  }

}