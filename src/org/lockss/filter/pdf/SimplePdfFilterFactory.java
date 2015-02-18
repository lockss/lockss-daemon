/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.daemon.PluginException;
import org.lockss.pdf.*;
import org.lockss.plugin.*;

/**
 * <p>
 * A filter factory that interprets its input as a PDF document,
 * applies a transform to it, and outputs the resulting PDF document
 * as a byte stream which is a valid PDF file.
 * </p>
 * @author Thib Guicherd-Callin
 * @since 1.56
 */
public abstract class SimplePdfFilterFactory
    implements FilterFactory, PdfTransform<PdfDocument> {

  /**
   * <p>
   * This instance's PDF document factory.
   * </p>
   * @since 1.56
   */
  protected PdfDocumentFactory pdfDocumentFactory;

  /**
   * <p>
   * Makes an instance using {@link DefaultPdfDocumentFactory}.
   * </p>
   * @since 1.56
   * @see DefaultPdfDocumentFactory
   */
  public SimplePdfFilterFactory() {
    this(DefaultPdfDocumentFactory.getInstance());
  }

  /**
   * <p>
   * Makes an instance using the given PDF document factory.
   * </p>
   * @param pdfDocumentFactory A PDF document factory.
   * @since 1.56
   */
  public SimplePdfFilterFactory(PdfDocumentFactory pdfDocumentFactory) {
    this.pdfDocumentFactory = pdfDocumentFactory;
  }

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    PdfDocument pdfDocument = null;
    try {
      pdfDocument = pdfDocumentFactory.parse(in);
      transform(au, pdfDocument);
      return PdfUtil.asInputStream(pdfDocument);
    }
    catch (IOException ioe) {
      throw new PluginException(ioe);
    }
    catch (PdfException pdfe) {
      throw new PluginException(pdfe);
    }
    finally {
      PdfUtil.safeClose(pdfDocument);
    }
  }

}
