/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.filter.pdf;

import java.io.*;

import org.lockss.util.*;

/**
 * <p>Utility document transforms.</p>
 * @author Thib Guicherd-Callin
 * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
 */
@Deprecated
public class DocumentTransformUtil {

  /**
   * <p>A base wrapper for another document transform.</p>
   * @author Thib Guicherd-Callin
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static abstract class DocumentTransformDecorator implements DocumentTransform {

    /**
     * <p>The underlying document transform.</p>
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    protected DocumentTransform documentTransform;

    /**
     * <p>Builds a new document transform with the given underlying
     * document transform.</p>
     * @param documentTransform A document transform to be wrapped.
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    protected DocumentTransformDecorator(DocumentTransform documentTransform) {
      this.documentTransform = documentTransform;
    }

  }

  /**
   * <p>A document transform that does nothing.</p>
   * @author Thib Guicherd-Callin
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static class IdentityDocumentTransform implements DocumentTransform {

    /**
     * <p>The return value for {@link #transform}.</p>
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    protected boolean returnValue;

    /**
     * <p>Builds a new identity document transform whose
     * {@link #transform} method always returns the default
     * return value.</p>
     * @see #IdentityDocumentTransform(boolean)
     * @see #RESULT_DEFAULT
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    public IdentityDocumentTransform() {
      this(RESULT_DEFAULT);
    }

    /**
     * <p>Builds a new identity document transform whose
     * {@link #transform} method always returns the given
     * return value.</p>
     * @param returnValue The return value for {@link #transform}.
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    public IdentityDocumentTransform(boolean returnValue) {
      this.returnValue = returnValue;
    }

    /* Inherit documentation */
    @Deprecated
    public boolean transform(PdfDocument pdfDocument) throws IOException {
      logger.debug2("Identity document transform result: " + returnValue);
      return returnValue;
    }

    /**
     * <p>The constant return value used by default by this class.</p>
     * @see #IdentityDocumentTransform()
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    public static final boolean RESULT_DEFAULT = true;

  }

  /**
   * <p>A document transform decorator whose {@link #transform} method
   * returns the opposite of its underlying document transform's
   * {@link DocumentTransform#transform} method.</p>
   * @author Thib Guicherd-Callin
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static class OppositeDocumentTransform extends DocumentTransformDecorator {

    /**
     * <p>Builds a new document transform decorating the given
     * document transform.</p>
     * @param documentTransform A document transform to be wrapped.
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    public OppositeDocumentTransform(DocumentTransform documentTransform) {
      super(documentTransform);
    }

    /* Inherit documentation */
    @Deprecated
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
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static abstract class PageTransformWrapper implements DocumentTransform {

    /**
     * <p>The underlying page transform.</p>
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    protected PageTransform pageTransform;

    /**
     * <p>Builds a new document transform wrapping the given page
     * transform.</p>
     * @param pageTransform
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    protected PageTransformWrapper(PageTransform pageTransform) {
      this.pageTransform = pageTransform;
    }

  }

  /**
   * <p>A document transform decorator that throws a
   * {@link DocumentTransformException} when its underlying document
   * transform fails.</p>
   * @author Thib Guicherd-Callin
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static class StrictDocumentTransform extends DocumentTransformDecorator {

    /**
     * <p>Builds a new strict document transform decorating the given
     * document transform.</p>
     * @param documentTransform A document transform.
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    public StrictDocumentTransform(DocumentTransform documentTransform) {
      super(documentTransform);
    }

    /* Inherit documentation */
    @Deprecated
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
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  private DocumentTransformUtil() { }

  /**
   * <p>A logger for use by this class.</p>
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  private static Logger logger = Logger.getLogger(DocumentTransformUtil.class);

}
