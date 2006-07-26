/*
 * $Id$
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

package org.lockss.filter;

import java.io.IOException;
import java.util.*;

import org.lockss.util.*;

public class PdfMultiTransform implements PdfTransform {

  public PdfMultiTransform() {
    this.pdfTransforms = new ArrayList();
  }

  /**
   * <p>A list of registered {@link PdfTransform} instances.</p>
   */
  protected ArrayList /* of PdfTransform */ pdfTransforms;

  /**
   * <p>Registers a new {@link PdfTransform} instance with
   * this multi-transform.</p>
   * <p>When transforming a PDF document, the actions performed by the
   * registered tranforms are applied in the order the transforms
   * were registered with this method.</p>
   * @param pdfTransform A {@link PdfTransform} instance.
   */
  public synchronized void addPdfTransform(PdfTransform pdfTransform) {
    pdfTransforms.add(pdfTransform);
  }

  /* Inherit documentation */
  public synchronized void transform(PdfDocument pdfDocument,
                                     Logger logger)
      throws IOException {
    if (logger == null) {
      logger = defaultLogger;
      logger.debug2("Starting a multi-transform with no logger from the caller");
    }
    for (Iterator iter = pdfTransforms.iterator() ; iter.hasNext() ; ) {
      PdfTransform pdfTransform = (PdfTransform)iter.next();
      pdfTransform.transform(pdfDocument, logger);
    }
  }

  /**
   * <p>A logger for use by the {@link #transform} method when no
   * logger is passed by the caller.</p>
   */
  protected static Logger defaultLogger = Logger.getLogger("PdfMultiTransform");

}
