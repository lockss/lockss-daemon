/*
 * $Id: PdfBoxXObjectTokenStream.java,v 1.3 2013-11-21 00:30:10 thib_gc Exp $
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.xobject.*;
import org.lockss.pdf.*;

/**
 * <p>
 * A {@link PdfBoxTokenStream} implementation suitable for a form XObject, based
 * on PDFBox 1.6.0.
 * </p>
 * <p>
 * This class acts as an adapter for the {@link PDXObjectForm} class.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.56
 * @see PdfBoxDocumentFactory
 */
public class PdfBoxXObjectTokenStream extends PdfBoxTokenStream {

  /**
   * <p>
   * The {@link PDXObjectForm} instance underpinning this instance.
   * </p>
   * 
   * @since 1.56
   */
  protected PDXObjectForm pdXObjectForm;
  
  /**
   * <p>
   * The form's resources, either its own ({@link PDXObjectForm#getResources()})
   * or those of an enclosing context.
   * </p>
   * 
   * @since 1.67.4
   */
  protected PDResources pdResources;
  
  /**
   * <p>
   * Builds a new instance using the underlying form's own resources
   * with {@link PDXObjectForm#getResources()}.
   * </p>
   * <p>
   * This constructor is accessible to classes in this package and subclasses.
   * </p>
   * 
   * @param pdfBoxPage
   *          The parent PDF page.
   * @param pdXObjectForm
   *          The {@link PDXObjectForm} being wrapped.
   * @since 1.56
   * @see #PdfBoxXObjectTokenStream(PdfBoxPage, PDXObjectForm, PDResources)
   */
  protected PdfBoxXObjectTokenStream(PdfBoxPage pdfBoxPage,
                                     PDXObjectForm pdXObjectForm,
                                     PDResources pdResources) {
    super(pdfBoxPage);
    this.pdXObjectForm = pdXObjectForm;
    this.pdResources = pdResources;
  }

  /**
   * <p>
   * This constructor is accessible to classes in this package and subclasses.
   * </p>
   * 
   * @param pdfBoxPage
   *          The parent PDF page.
   * @param pdXObjectForm
   *          The {@link PDXObjectForm} being wrapped.
   * @param pdResources
   *          The form's resources ({@link PDXObjectForm#getResources()} or from
   *          an enclosing context).
   * @since 1.67.4
   */
  protected PdfBoxXObjectTokenStream(PdfBoxPage pdfBoxPage, PDXObjectForm pdXObjectForm) {
    this(pdfBoxPage, pdXObjectForm, pdXObjectForm.getResources());
  }

  @Override
  public void setTokens(List<PdfToken> newTokens) throws PdfException {
    try {
      PDXObjectForm oldForm = pdXObjectForm;
      PDStream newPdStream = makeNewPdStream();
      newPdStream.getStream().setName(COSName.SUBTYPE, PDXObjectForm.SUB_TYPE);
      ContentStreamWriter tokenWriter = new ContentStreamWriter(newPdStream.createOutputStream());
      tokenWriter.writeTokens(PdfBoxTokens.unwrapList(newTokens));
      pdXObjectForm = new PDXObjectForm(newPdStream);
      pdXObjectForm.setResources(pdResources);
      Map<String, PDXObject> xobjects = pdResources.getXObjects();
      for (Map.Entry<String, PDXObject> ent : xobjects.entrySet()) {
        String key = ent.getKey();
        PDXObject val = ent.getValue();
        if (val == oldForm) {
          xobjects.put(key, pdXObjectForm);
        }
      }
//      pdXObjectForm.getCOSStream().replaceWithStream(newPdStream.getStream());
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
    return pdResources;
  }
  
}
