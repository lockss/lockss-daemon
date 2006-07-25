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

import java.io.*;

import org.lockss.util.Logger;
import org.pdfbox.exceptions.COSVisitorException;
import org.pdfbox.pdfparser.PDFParser;

/**
 * <p>Utility classes and methods to deal with PDF transforms.</p>
 * @author Thib Guicherd-Callin
 * @see PdfTransform
 */
public class PdfTransformUtil {

  /**
   * <p>A logger for use by {@link PdfTransform#transform} methods
   * when no logger is passed by the caller.</p>
   */
  protected static Logger defaultLogger = Logger.getLogger("PdfMultiTransform");

  /**
   * <p>A PDF transform that does nothing.</p>
   * @author Thib Guicherd-Callin
   */
  public static class IdentityPdfTransform implements PdfTransform {

    public void transform(PDFParser pdfParser,
                          Logger logger)
        throws IOException {
      (logger == null ? defaultLogger : logger).debug("Identity PDF transform");
    }

  }

  public static void parse(InputStream pdfInputStream,
                           OutputStream pdfOutputStream,
                           PdfTransform pdfTransform,
                           Logger logger)
      throws IOException {
    try {
      // Parse
      PDFParser pdfParser = new PDFParser(pdfInputStream);
      pdfParser.parse();

      // Transform
      pdfTransform.transform(pdfParser, logger);

      // Save
      pdfParser.getPDDocument().save(pdfOutputStream);
      pdfParser.getPDDocument().close();
    }
    catch (COSVisitorException cve) {
      IOException ioe = new IOException();
      ioe.initCause(cve);
      throw ioe;
    }
  }

}
