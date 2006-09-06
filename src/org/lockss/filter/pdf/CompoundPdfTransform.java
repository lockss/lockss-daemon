/*
 * $Id: CompoundPdfTransform.java,v 1.2 2006-08-25 23:19:40 thib_gc Exp $
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
 * <p>A PDF transform made of many other PDF transforms, applied
 * sequentially.</p>
 * @author Thib Guicherd-Callin
 */
public class CompoundPdfTransform implements PdfTransform {

  /**
   * <p>A list of registered {@link PdfTransform} instances.</p>
   */
  protected List /* of PdfTransform */ pdfTransforms;

  /**
   * <p>Builds a new compound transform.</p>
   */
  public CompoundPdfTransform() {
    this((PdfTransform[])null);
  }

  /**
   * <p>Builds a new compound transform, starting with the given
   * transform.</p>
   * @param pdfTransform A transform.
   */
  public CompoundPdfTransform(PdfTransform pdfTransform) {
    this(new PdfTransform[] { pdfTransform });
  }

  /**
   * <p>Builds a new compound transform, starting with the given
   * transforms.</p>
   * @param pdfTransform1 A transform.
   * @param pdfTransform2 A transform.
   */
  public CompoundPdfTransform(PdfTransform pdfTransform1,
                              PdfTransform pdfTransform2) {
    this(new PdfTransform[] { pdfTransform1, pdfTransform2 } );
  }

  /**
   * <p>Builds a new compound transform, starting with the given
   * transforms.</p>
   * @param pdfTransforms An array of transforms. Can be null for no
   *                      initial transforms.
   */
  public CompoundPdfTransform(PdfTransform[] pdfTransforms) {
    this.pdfTransforms = new ArrayList();
    if (pdfTransforms != null) {
      for (int transform = 0 ; transform < pdfTransforms.length ; ++transform) {
        add(pdfTransforms[transform]);
      }
    }
  }

  /**
   * <p>Registers a new {@link PdfTransform} instance with
   * this compound transform.</p>
   * <p>When transforming a PDF document, the actions performed by the
   * registered tranforms are applied in the order the transforms
   * were registered with this method.</p>
   * @param pdfTransform A {@link PdfTransform} instance.
   */
  public synchronized void add(PdfTransform pdfTransform) {
    pdfTransforms.add(pdfTransform);
  }

  /* Inherit documentation */
  public synchronized void transform(PdfDocument pdfDocument)
      throws IOException {
    for (Iterator iter = pdfTransforms.iterator() ; iter.hasNext() ; ) {
      PdfTransform pdfTransform = (PdfTransform)iter.next();
      pdfTransform.transform(pdfDocument);
    }
  }

  /**
   * <p>A logger for use by this class.</p>
   */
  protected static Logger logger = Logger.getLogger("CompoundPdfTransform");

}
