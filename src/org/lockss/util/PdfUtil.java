/*
 * $Id: PdfUtil.java,v 1.1 2006-08-23 22:23:45 thib_gc Exp $
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

import org.lockss.filter.pdf.PdfTransform;
import org.pdfbox.cos.*;
import org.pdfbox.util.PDFOperator;

/**
 * <p>Utilities for PDF processing and filtering.</p>
 * @author Thib Guicherd-Callin
 */
public class PdfUtil {

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

  /**
   * <p>The "begin text object" PDF operator (<code>BT</code>).</p>
   */
  public static final String BEGIN_TEXT_OBJECT = "BT";

  /**
   * <p>The "end text object" PDF operator (<code>ET</code>).</p>
   */
  public static final String END_TEXT_OBJECT = "ET";

  /**
   * <p>The "show text" PDF operator (<code>Tj</code>).</p>
   */
  public static final String SHOW_TEXT = "Tj";

  /**
   * <p>A logger for use by this class.</p>
   */
  protected static Logger logger = Logger.getLoggerWithInitialLevel("PdfUtil", Logger.LEVEL_DEBUG3);

  /**
   * <p>Parses a PDF document from an input stream, applies a
   * transform to it, and writes the result to an output stream.</p>
   * <p>This method closes the PDF document at the end of processing</p>
   * @param pdfTransform    A PDF transform.
   * @param pdfInputStream  An input stream containing a PDF document.
   * @param pdfOutputStream An output stream into which to write the
   *                        transformed PDF document.
   * @throws IOException if any processing error occurs.
   */
  public static void applyPdfTransform(PdfTransform pdfTransform,
                                       InputStream pdfInputStream,
                                       OutputStream pdfOutputStream)
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

  /**
   * <p>Extracts the float data associated with a PDF token that is
   * a PDF float.</p>
   * <p>Preconditions:</p>
   * <ul>
   *  <li><code>isPdfFloat(pdfFloat)</code></li>
   * </ul>
   * @param pdfFloat A PDF float.
   * @return The float associated with this PDF float.
   * @see #isPdfFloat
   */
  public static float getPdfFloat(Object pdfFloat) {
    return ((COSFloat)pdfFloat).floatValue();
  }

  /**
   * <p>Extracts the string data associated with a PDF token that is
   * a PDF string.</p>
   * <p>Preconditions:</p>
   * <ul>
   *  <li><code>isPdfString(pdfString)</code></li>
   * </ul>
   * @param pdfString A PDF string.
   * @return The {@link String} associated with this PDF string.
   * @see #isPdfString
   */
  public static String getPdfString(Object pdfString) {
    return ((COSString)pdfString).getString();
  }

  /**
   * <p>Determines if a candidate PDF token is the "begin text object"
   * PDF operator.</p>
   * @param candidateToken A candidate PDF token.
   * @return True is the argument is the expected operator, false
   *         otherwise.
   * @see #BEGIN_TEXT_OBJECT
   * @see #isPdfOperator
   */
  public static boolean isBeginTextObjectOperator(Object candidateToken) {
    return isPdfOperator(candidateToken, BEGIN_TEXT_OBJECT);
  }

  /**
   * <p>Determines if a candidate PDF token is the "end text object"
   * PDF operator.</p>
   * @param candidateToken A candidate PDF token.
   * @return True is the argument is the expected operator, false
   *         otherwise.
   * @see #END_TEXT_OBJECT
   * @see #isPdfOperator
   */
  public static boolean isEndTextObjectOperator(Object candidateToken) {
    return isPdfOperator(candidateToken, END_TEXT_OBJECT);
  }

  /**
   * <p>Determines if a candidate PDF token is a PDF float token.</p>
   * @param candidateToken A candidate PDF toekn.
   * @return True if the argument is a PDF float, false otherwise.
   * @see COSFloat
   */
  public static boolean isPdfFloat(Object candidateToken) {
    return candidateToken instanceof COSFloat;
  }

  /**
   * <p>Determines if a PDF token is a PDF operator, if is so,
   * if it is the expected operator.</p>
   * @param candidateToken   A candidate PDF token.
   * @param expectedOperator A PDF operator name (as a string).
   * @return True if the argument is a PDF operator of the expected
   *         type, false otherwise.
   */
  public static boolean isPdfOperator(Object candidateToken,
                                      String expectedOperator) {
    if (candidateToken != null && candidateToken instanceof PDFOperator) {
      return ((PDFOperator)candidateToken).getOperation().equals(expectedOperator);
    }
    return false;
  }

  /**
   * <p>Determines if a candidate PDF token is a PDF string token.</p>
   * @param candidateToken A candidate PDF toekn.
   * @return True if the argument is a PDF string, false otherwise.
   * @see COSString
   */
  public static boolean isPdfString(Object candidateToken) {
    return candidateToken instanceof COSString;
  }

  /**
   * <p>Determines if a candidate PDF token is the "show text"
   * PDF operator.</p>
   * @param candidateToken A candidate PDF token.
   * @return True is the argument is the expected operator, false
   *         otherwise.
   * @see #SHOW_TEXT
   * @see #isPdfOperator
   */
  public static boolean isShowTextOperator(Object candidateToken) {
    return isPdfOperator(candidateToken, SHOW_TEXT);
  }

}
