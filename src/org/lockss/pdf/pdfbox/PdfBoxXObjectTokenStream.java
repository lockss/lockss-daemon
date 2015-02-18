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

package org.lockss.pdf.pdfbox;

import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectForm;
import org.lockss.pdf.*;

/**
 * <p>
 * A {@link PdfBoxTokenStream} implementation suitable for a form
 * XObject, based on PDFBox 1.6.0.
 * </p>
 * <p>
 * This class acts as an adapter for the {@link PDXObjectForm} class.
 * </p>
 * @author Thib Guicherd-Callin
 * @since 1.56
 * @see PdfBoxDocumentFactory
 */
public class PdfBoxXObjectTokenStream extends PdfBoxTokenStream {

  /**
   * <p>
   * The {@link PDXObjectForm} instance underpinning this instance.
   * </p>
   * @since 1.56
   */
  protected PDXObjectForm pdXObjectForm;
  
  /**
   * <p>
   * This constructor is accessible to classes in this package and
   * subclasses.
   * </p>
   * @param pdfBoxPage The parent PDF page.
   * @param pdXObjectForm The {@link PDXObjectForm} being wrapped.
   * @since 1.56
   */
  protected PdfBoxXObjectTokenStream(PdfBoxPage pdfBoxPage, PDXObjectForm pdXObjectForm) {
    super(pdfBoxPage);
    this.pdXObjectForm = pdXObjectForm;
  }

  @Override
  public void setTokens(List<PdfToken> newTokens) throws PdfException {
    try {
      PDStream newPdStream = makeNewPdStream();
      newPdStream.getStream().setName("Subtype", PDXObjectForm.SUB_TYPE);
      ContentStreamWriter tokenWriter = new ContentStreamWriter(newPdStream.createOutputStream());
      tokenWriter.writeTokens(PdfBoxTokens.unwrapList(newTokens));
      pdXObjectForm.getCOSStream().replaceWithStream(newPdStream.getStream());
    }
    catch (IOException ioe) {
      throw new PdfException("Error while writing XObject token stream", ioe);
    }
  }

  @Override
  protected PDStream getPdStream() {
    return pdXObjectForm.getPDStream();
  }

  @Override
  protected PDResources getStreamResources() {
    return pdXObjectForm.getResources();
  }
  
}
