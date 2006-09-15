/*
 * $Id: DocumentTransformUtil.java,v 1.2 2006-09-15 22:53:51 thib_gc Exp $
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
 * <p>Utility document transforms.</p>
 * @author Thib Guicherd-Callin
 */
public class DocumentTransformUtil {

  /**
   * <p>A base wrapper for another document transform.</p>
   * <p>The {@link DocumentTransformDecorator#transform} method in
   * this class simply returns the result of calling
   * {@link DocumentTransform#transform} on the underlying document
   * transform.</p>
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
     * <p>Builds a new identity document transform that always
     * succeeds.</p>
     * @see #IdentityDocumentTransform(boolean)
     */
    public IdentityDocumentTransform() {
      this(true);
    }

    /**
     * <p>Builds a new identity document transform whose
     * {@link #transform} method always returns the given value.</p>
     * @param returnValue The return value for {@link #transform}.
     */
    public IdentityDocumentTransform(boolean returnValue) {
      this.returnValue = returnValue;
    }

    /* Inherit documentation */
    public boolean transform(PdfDocument pdfDocument) throws IOException {
      logger.debug3("Indentity document transform: " + returnValue);
      return returnValue;
    }

  }

  /**
   * <p>A document transform decorator that returns the opposite
   * boolean value of its underlying document transform's
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
      return !documentTransform.transform(pdfDocument);
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
      if (documentTransform.transform(pdfDocument)) {
        return true;
      }
      else {
        throw new DocumentTransformException("Strict transform did not succeed: "
                                             + documentTransform.getClass().getName());
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
  protected static Logger logger = Logger.getLogger("DocumentTransformUtil");

}
