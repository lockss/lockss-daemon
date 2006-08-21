/*
 * $Id: PdfTransformUtil.java,v 1.3 2006-08-21 15:48:55 thib_gc Exp $
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
import java.util.Iterator;

import org.lockss.filter.*;
import org.pdfbox.pdmodel.PDPage;

/**
 * <p>Utility classes and methods to deal with PDF transforms.</p>
 * @author Thib Guicherd-Callin
 * @see PdfTransform
 */
public class PdfTransformUtil {

  /**
   * <p>A PDF transform decorator that applies a given PDF transform
   * only if the PDF document to be transformed is identified by the
   * {@link #identify} method.</p>
   * @author Thib Guicherd-Callin
   */
  public static abstract class PdfConditionalTransform implements PdfTransform {

    /**
     * <p>The PDF transform to be applied conditionally.</p>
     */
    protected PdfTransform pdfTransform;

    /**
     * <p>Decorates the given PDF transform.</p>
     * @param pdfTransform A PDF transform to be applied conditionally.
     */
    public PdfConditionalTransform(PdfTransform pdfTransform) {
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

  /**
   * <p>A PDF transform that applies a PDF page transform to each page
   * of the PDF document except the first page.</p>
   * @author Thib Guicherd-Callin
   */
  public static class PdfEachPageExceptFirstTransform implements PdfTransform {

    /**
     * <p>A PDF page transform.</p>
     */
    protected PdfPageTransform pdfPageTransform;

    /**
     * <p>Builds a new PDF transform with the given PDF page
     * transform.</p>
     * @param pdfPageTransform A PDF page transform.
     */
    public PdfEachPageExceptFirstTransform(PdfPageTransform pdfPageTransform) {
      this.pdfPageTransform = pdfPageTransform;
    }

    /* Inherit documentation */
    public void transform(PdfDocument pdfDocument) throws IOException {
      Iterator iter = pdfDocument.getPageIterator();
      iter.next(); // skip first page
      while (iter.hasNext()) {
        pdfPageTransform.transform(pdfDocument,
                                   (PDPage)iter.next());
      }
    }

  }

  /**
   * <p>A PDF transform that applies a PDF page transform to each page
   * of the PDF document.</p>
   * @author Thib Guicherd-Callin
   */
  public static class PdfEachPageTransform implements PdfTransform {

    /**
     * <p>A PDF page transform.</p>
     */
    protected PdfPageTransform pdfPageTransform;

    /**
     * <p>Builds a new PDF transform with the given PDF page
     * transform.</p>
     * @param pdfPageTransform A PDF page transform.
     */
    public PdfEachPageTransform(PdfPageTransform pdfPageTransform) {
      this.pdfPageTransform = pdfPageTransform;
    }

    /* Inherit documentation */
    public void transform(PdfDocument pdfDocument) throws IOException {
      for (Iterator iter = pdfDocument.getPageIterator() ; iter.hasNext() ; ) {
        pdfPageTransform.transform(pdfDocument,
                                   (PDPage)iter.next());
      }
    }

  }

  /**
   * <p>A PDF transform that applies a PDF page transform to the
   * first page of the PDF document.</p>
   * @author Thib Guicherd-Callin
   */
  public static class PdfFirstPageTransform implements PdfTransform {

    /**
     * <p>A PDF page transform.</p>
     */
    protected PdfPageTransform pdfPageTransform;

    /**
     * <p>Builds a new PDF transform with the given PDF page
     * transform.</p>
     * @param pdfPageTransform A PDF page transform.
     */
    public PdfFirstPageTransform(PdfPageTransform pdfPageTransform) {
      this.pdfPageTransform = pdfPageTransform;
    }

    /* Inherit documentation */
    public void transform(PdfDocument pdfDocument) throws IOException {
      pdfPageTransform.transform(pdfDocument,
                                 (PDPage)pdfDocument.getPageIterator().next());
    }

  }

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
