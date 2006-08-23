/*
 * $Id: PdfTransformUtil.java,v 1.4 2006-08-23 19:14:07 thib_gc Exp $
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

package org.lockss.util;

import java.io.*;

import org.lockss.filter.*;
import org.lockss.filter.pdf.*;

/**
 * <p>Utility classes and methods to deal with PDF transforms.</p>
 * @author Thib Guicherd-Callin
 * @see PdfTransform
 */
public class PdfTransformUtil {

  /**
   * <p>A PDF transform that does nothing, for testing.</p>
   * @author Thib Guicherd-Callin
   */
  public static class PdfIdentityTransform implements PdfTransform {

    /* Inherit documentation */
    public void transform(PdfDocument pdfDocument)
        throws IOException {
      logger.debug("Identity PDF transform");
    }

  }

  protected static Logger logger = Logger.getLogger("PdfTransformUtil");

  /**
   * <p>Parses a PDF document from an input stream, applies a
   * transform to it, and writes the result to an output stream.</p>
   * <p>This method closes the PDF document at the end of processing</p>
   * @param pdfInputStream  An input stream containing a PDF document.
   * @param pdfOutputStream An output stream into which to write the
   *                        transformed PDF document.
   * @param pdfTransform    A PDF transform.
   * @param logger          A logger into which to write messages.
   * @throws IOException if any processing error occurs.
   */
  public static void parse(InputStream pdfInputStream,
                           OutputStream pdfOutputStream,
                           PdfTransform pdfTransform,
                           Logger logger)
      throws IOException {
    boolean mustReleaseResources = false;
    PdfDocument pdfDocument = null;
    try {
      // Parse
      pdfDocument = new PdfDocument(pdfInputStream);
      mustReleaseResources = true;

      // Transform
      pdfTransform.transform(pdfDocument);

      // Save
      pdfDocument.save(pdfOutputStream);
    }
    finally {
      if (mustReleaseResources) {
        pdfDocument.close();
      }
    }
  }

}
