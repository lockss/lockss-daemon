/*
 * $Id: PageTransformUtil.java,v 1.3 2006-09-19 16:54:53 thib_gc Exp $
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
import java.util.List;

import org.lockss.util.*;
import org.pdfbox.util.PDFOperator;

/**
 * <p>Utility page transforms.</p>
 * @author Thib Guicherd-Callin
 */
public class PageTransformUtil {

  public static class ExtractText extends PageStreamTransform {

    protected static class AppendToStringBuffer extends ProcessString {
      public void processString(PageStreamTransform pageStreamTransform,
                                PDFOperator operator,
                                List operands,
                                String str) {
        pageStreamTransform.signalChange(); // At least one string processed
        append(pageStreamTransform, str);
      }
    }

    protected StringBuffer buffer;

    public ExtractText(StringBuffer buffer) throws IOException {
      super(PdfUtil.SHOW_TEXT, AppendToStringBuffer.class,
            PdfUtil.SHOW_TEXT_GLYPH_POSITIONING, AppendToStringBuffer.class,
            PdfUtil.MOVE_TO_NEXT_LINE_SHOW_TEXT, AppendToStringBuffer.class,
            PdfUtil.SET_SPACING_MOVE_TO_NEXT_LINE_SHOW_TEXT, AppendToStringBuffer.class);
      this.buffer = buffer;
    }

    protected synchronized void writeResult(PdfPage pdfPage) {
      // Do nothing
    }

    protected static void append(PageStreamTransform pageStreamTransform, String str) {
      ExtractText extractText = (ExtractText)pageStreamTransform;
      extractText.buffer.append(str);
    }

  }

  /**
   * <p>A page transform that does nothing.</p>
   * @author Thib Guicherd-Callin
   */
  public static class IdentityPageTransform implements PageTransform {

    /**
     * <p>The return value for {@link #transform}.</p>
     */
    protected boolean returnValue;

    /**
     * <p>Builds a new identity page transform whose
     * {@link #transform} method always returns the default
     * result value.</p>
     * @see #IdentityPageTransform(boolean)
     * @see #RESULT_DEFAULT
     */
    public IdentityPageTransform() {
      this(RESULT_DEFAULT);
    }

    /**
     * <p>Builds a new identity page transform whose
     * {@link #transform} method always returns the given
     * result value.</p>
     * @param returnValue The return value for {@link #transform}.
     */
    public IdentityPageTransform(boolean returnValue) {
      this.returnValue = returnValue;
    }

    /* Inherit documentation */
    public boolean transform(PdfPage pdfPage) throws IOException {
      logger.debug2("Identity page transform result: " + returnValue);
      return returnValue;
    }

    /**
     * <p>The constant return value used by default by this class.</p>
     * @see #IdentityPageTransform()
     */
    public static final boolean RESULT_DEFAULT = true;

  }

  /**
   * <p>A page transform decorator that returns the opposite
   * boolean value of its underlying page transform's
   * {@link PageTransform#transform} method.</p>
   * @author Thib Guicherd-Callin
   */
  public static class OppositePageTransform extends PageTransformDecorator {

    /**
     * <p>Builds a new page transform decorating the given
     * page transform.</p>
     * @param pageTransform A page transform to be wrapped.
     */
    public OppositePageTransform(PageTransform pageTransform) {
      super(pageTransform);
    }

    /* Inherit documentation */
    public boolean transform(PdfPage pdfPage) throws IOException {
      return !pageTransform.transform(pdfPage);
    }

  }

  /**
   * <p>A base wrapper for another page transform.</p>
   * @author Thib Guicherd-Callin
   */
  public static abstract class PageTransformDecorator implements PageTransform {

    /**
     * <p>The underlying page transform.</p>
     */
    protected PageTransform pageTransform;

    /**
     * <p>Builds a new page transform with the given underlying
     * page transform.</p>
     * @param pageTransform A page transform to be wrapped.
     */
    protected PageTransformDecorator(PageTransform pageTransform) {
      this.pageTransform = pageTransform;
    }

  }

  /**
   * <p>A page transform decorator that throws a
   * {@link PageTransformException} when its underlying page
   * transform fails.</p>
   * @author Thib Guicherd-Callin
   */
  public static class StrictPageTransform extends PageTransformDecorator {

    /**
     * <p>Builds a new strict page transform decorating the given
     * page transform.</p>
     * @param pageTransform A page transform.
     */
    public StrictPageTransform(PageTransform pageTransform) {
      super(pageTransform);
    }

    /* Inherit documentation */
    public boolean transform(PdfPage pdfPage) throws IOException {
      logger.debug2("Begin strict page transform based on "
                    + pageTransform.getClass().getName());
      if (pageTransform.transform(pdfPage)) {
        logger.debug2("Strict page transform result: true");
        return true;
      }
      else {
        logger.debug2("Strict page transform result: throw");
        throw new PageTransformException("Strict page transform did not succeed");
      }
    }

  }

  /**
   * <p>Not publicly instantiable.</p>
   */
  private PageTransformUtil() { }

  /**
   * <p>A logger for use by this class.</p>
   */
  private static Logger logger = Logger.getLogger("PageTransformUtil");

}
