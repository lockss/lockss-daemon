/*
 * $Id: PdfBoxTokenStream.java,v 1.2 2012-07-11 23:53:38 thib_gc Exp $
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

package org.lockss.pdf.pdfbox;

import java.io.IOException;
import java.util.*;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.lockss.pdf.*;

/**
 * <p>
 * A {@link PdfTokenStream} implementation based on PDFBox 1.6.0.
 * </p>
 * <p>
 * This class acts as an adapter for the {@link PDStream} class, but
 * the origin of the wrapped instance comes from {@link #getPdStream()}
 * </p>
 * @author Thib Guicherd-Callin
 * @since 1.56
 * @see PdfBoxDocumentFactory
 */
public abstract class PdfBoxTokenStream implements PdfTokenStream {

  /**
   * <p>
   * The parent {@link PdfBoxPage} instance.
   * </p>
   * @since 1.56
   */
  protected PdfBoxPage pdfBoxPage;
  
  /**
   * <p>
   * The cached result of conputing {@link #getTokens()}.
   * </p>
   * @since 1.56
   */
  private List<PdfToken> cachedTokens;
  
  /**
   * <p>
   * This constructor is accessible to classes in this package and
   * subclasses.
   * </p>
   * @param pdfBoxPage The parent {@link PdfBoxPage} instance.
   * @since 1.56
   */
  protected PdfBoxTokenStream(PdfBoxPage pdfBoxPage) {
    this.pdfBoxPage = pdfBoxPage;
  }
  
  @Override
  public PdfPage getPage() {
    return pdfBoxPage;
  }
  
  @Override
  public PdfTokenFactory getTokenFactory() throws PdfException {
    return getPage().getDocument().getTokenFactory();
  }

  @Override
  public List<PdfToken> getTokens() throws PdfException {
    try {
      if (cachedTokens == null) {
        cachedTokens = PdfBoxTokens.convertList(getPdStream().getStream().getStreamTokens());
      }
      return cachedTokens;
    }
    catch (IOException ioe) {
      throw new PdfException(ioe);
    }
  }
  
  /**
   * <p>
   * Retrieves the {@link PDStream} instance underpinning this PDF
   * token stream.
   * </p>
   * @return The {@link PDStream} instance this instance represents.
   */
  protected abstract PDStream getPdStream();

  /**
   * <p>
   * Convenience method to create a new {@link PDStream} instance.
   * </p>
   * @return A new {@link PDStream} instance based on this document.
   */
  protected PDStream makeNewPdStream() {
    return new PDStream(pdfBoxPage.pdfBoxDocument.pdDocument);
  }
  
}
