/*
 * $Id: DocumentTransformUtil.java,v 1.10 2007-02-23 19:41:34 thib_gc Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;

import org.lockss.util.*;

/**
 * <p>Utility document transforms.</p>
 * @author Thib Guicherd-Callin
 */
public class DocumentTransformUtil {

  /**
   * <p>A base wrapper for another document transform.</p>
   * @author Thib Guicherd-Callin
   */
  public static abstract class DocumentTransformDecorator implements DocumentTransform {

    /**
     * <p>The underlying document transform.</p>
     */
    protected DocumentTransform documentTransform;

    /**
     * <p>Builds a new document transform with the given underlying
     * document transform.</p>
     * @param documentTransform A document transform to be wrapped.
     */
    protected DocumentTransformDecorator(DocumentTransform documentTransform) {
      this.documentTransform = documentTransform;
    }

  }

  /**
   * <p>A document transform that does nothing.</p>
   * @author Thib Guicherd-Callin
   */
  public static class IdentityDocumentTransform implements DocumentTransform {

    /**
     * <p>The return value for {@link #transform}.</p>
     */
    protected boolean returnValue;

    /**
     * <p>Builds a new identity document transform whose
     * {@link #transform} method always returns the default
     * return value.</p>
     * @see #IdentityDocumentTransform(boolean)
     * @see #RESULT_DEFAULT
     */
    public IdentityDocumentTransform() {
      this(RESULT_DEFAULT);
    }

    /**
     * <p>Builds a new identity document transform whose
     * {@link #transform} method always returns the given
     * return value.</p>
     * @param returnValue The return value for {@link #transform}.
     */
    public IdentityDocumentTransform(boolean returnValue) {
      this.returnValue = returnValue;
    }

    /* Inherit documentation */
    public boolean transform(PdfDocument pdfDocument) throws IOException {
      logger.debug2("Identity document transform result: " + returnValue);
      return returnValue;
    }

    /**
     * <p>The constant return value used by default by this class.</p>
     * @see #IdentityDocumentTransform()
     */
    public static final boolean RESULT_DEFAULT = true;

  }

  /**
   * <p>A document transform decorator whose {@link #transform} method
   * returns the opposite of its underlying document transform's
   * {@link DocumentTransform#transform} method.</p>
   * @author Thib Guicherd-Callin
   */
  public static class OppositeDocumentTransform extends DocumentTransformDecorator {

    /**
     * <p>Builds a new document transform decorating the given
     * document transform.</p>
     * @param documentTransform A document transform to be wrapped.
     */
    public OppositeDocumentTransform(DocumentTransform documentTransform) {
      super(documentTransform);
    }

    /* Inherit documentation */
    public boolean transform(PdfDocument pdfDocument) throws IOException {
      logger.debug3("Begin opposite document transform based on " + documentTransform.getClass().getName());
      boolean ret = !documentTransform.transform(pdfDocument);
      logger.debug2("Opposite document transform result: " + ret);
      return ret;
    }

  }

  /**
   * <p>A base document transform that serves as a wrapper around a
   * page transform.</p>
   * @author Thib Guicherd-Callin
   */
  public static abstract class PageTransformWrapper implements DocumentTransform {

    /**
     * <p>The underlying page transform.</p>
     */
    protected PageTransform pageTransform;

    /**
     * <p>Builds a new document transform wrapping the given page
     * transform.</p>
     * @param pageTransform
     */
    protected PageTransformWrapper(PageTransform pageTransform) {
      this.pageTransform = pageTransform;
    }

  }

  /**
   * <p>A document transform decorator that throws a
   * {@link DocumentTransformException} when its underlying document
   * transform fails.</p>
   * @author Thib Guicherd-Callin
   */
  public static class StrictDocumentTransform extends DocumentTransformDecorator {

    /**
     * <p>Builds a new strict document transform decorating the given
     * document transform.</p>
     * @param documentTransform A document transform.
     */
    public StrictDocumentTransform(DocumentTransform documentTransform) {
      super(documentTransform);
    }

    /* Inherit documentation */
    public boolean transform(PdfDocument pdfDocument) throws IOException {
      logger.debug3("Begin strict document transform based on " + documentTransform.getClass().getName());
      if (documentTransform.transform(pdfDocument)) {
        logger.debug2("Strict document transform result: true");
        return true;
      }
      else {
        logger.debug2("Strict document transform result: throw");
        throw new DocumentTransformException("Strict document transform did not succeed");
      }
    }

  }

  /**
   * <p>Not publicly instantiable.</p>
   */
  private DocumentTransformUtil() { }

  /**
   * <p>A logger for use by this class.</p>
   */
  private static Logger logger = Logger.getLogger("DocumentTransformUtil");

}
