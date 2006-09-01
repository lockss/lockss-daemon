/*
 * $Id: CompoundPdfPageTransform.java,v 1.2 2006-09-01 06:47:00 thib_gc Exp $
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

public class CompoundPdfPageTransform implements PdfPageTransform {

  /**
   * <p>A list of registered {@link PdfPageTransform} instances.</p>
   */
  protected List /* of PdfPageTransform */ pdfPageTransforms;

  /**
   * <p>Builds a new compound page transform.</p>
   */
  public CompoundPdfPageTransform() {
    this((PdfPageTransform[])null);
  }

  /**
   * <p>Builds a new compound page transform, starting with the given
   * page transform.</p>
   * @param pdfPageTransform A page transform.
   */
  public CompoundPdfPageTransform(PdfPageTransform pdfPageTransform) {
    this(new PdfPageTransform[] { pdfPageTransform });
  }

  /**
   * <p>Builds a new compound page transform, starting with the given
   * page transforms.</p>
   * @param pdfPageTransform1 A page transform.
   * @param pdfPageTransform2 A page transform.
   */
  public CompoundPdfPageTransform(PdfPageTransform pdfPageTransform1,
                                  PdfPageTransform pdfPageTransform2) {
    this(new PdfPageTransform[] { pdfPageTransform1, pdfPageTransform2 } );
  }

  /**
   * <p>Builds a new compound page transform, starting with the given
   * page transforms.</p>
   * @param pdfPageTransforms An array of page transforms. Can be null
   *                          for no initial page transforms.
   */
  public CompoundPdfPageTransform(PdfPageTransform[] pdfPageTransforms) {
    this.pdfPageTransforms = new ArrayList();
    if (pdfPageTransforms != null) {
      for (int transform = 0 ; transform < pdfPageTransforms.length ; ++transform) {
        add(pdfPageTransforms[transform]);
      }
    }
  }

  /**
   * <p>Registers a new {@link PdfPageTransform} instance with
   * this compound page transform.</p>
   * <p>When transforming a PDF page, the actions performed by the
   * registered page tranforms are applied in the order the page
   * transforms were registered with this method.</p>
   * @param pdfPageTransform A {@link PdfPageTransform} instance.
   */
  public synchronized void add(PdfPageTransform pdfPageTransform) {
    pdfPageTransforms.add(pdfPageTransform);
  }

  /* Inherit documentation */
  public synchronized void transform(PdfDocument pdfDocument,
                                     PdfPage pdfPage)
      throws IOException {
    for (Iterator iter = pdfPageTransforms.iterator() ; iter.hasNext() ; ) {
      PdfPageTransform pdfPageTransform = (PdfPageTransform)iter.next();
      pdfPageTransform.transform(pdfDocument, pdfPage);
    }
  }

  /**
   * <p>A logger for use by this class.</p>
   */
  protected static Logger logger = Logger.getLogger("CompoundPdfPageTransform");

}
