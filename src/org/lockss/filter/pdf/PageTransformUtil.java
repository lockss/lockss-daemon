/*
 * $Id: PageTransformUtil.java,v 1.2 2006-09-15 22:53:51 thib_gc Exp $
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
 * <p>Utility page transforms.</p>
 * @author Thib Guicherd-Callin
 */
public class PageTransformUtil {

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
     * <p>Builds a new identity page transform that always
     * succeeds.</p>
     * @see #IdentityPageTransform(boolean)
     */
    public IdentityPageTransform() {
      this(true);
    }

    /**
     * <p>Builds a new identity page transform whose
     * {@link #transform} method always returns the given value.</p>
     * @param returnValue The return value for {@link #transform}.
     */
    public IdentityPageTransform(boolean returnValue) {
      this.returnValue = returnValue;
    }

    /* Inherit documentation */
    public boolean transform(PdfPage pdfPage) throws IOException {
      logger.debug3("Indentity page transform: " + returnValue);
      return returnValue;
    }

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
   * <p>The {@link PageTransformDecorator#transform} method in
   * this class simply returns the result of calling
   * {@link PageTransform#transform} on the underlying page
   * transform.</p>
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
      if (pageTransform.transform(pdfPage)) {
        return true;
      }
      else {
        throw new PageTransformException("Strict transform did not succeed: "
                                         + pageTransform.getClass().getName());
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
  protected static Logger logger = Logger.getLogger("PageTransformUtil");

}
