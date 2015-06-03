/*
 * $Id$
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
import org.lockss.util.Logger;

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
   * A logger for use by this class.
   * </p>
   * 
   * @since 1.67.6
   */
  private static final Logger log = Logger.getLogger(PdfBoxXObjectTokenStream.class);
  
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
   * @since 1.67.6
   */
  protected PDResources parentResources;
  
  /**
   * <p>
   * The form's own resources.
   * </p>
   * 
   * @since 1.67.6
   */
  protected PDResources ownResources;
  
  /**
   * <p>
   * Builds a new instance using the given parent and own resources.
   * </p>
   * <p>
   * This constructor is accessible to classes in this package and subclasses.
   * </p>
   * 
   * @param pdfBoxPage
   *          The parent PDF page.
   * @param pdXObjectForm
   *          The {@link PDXObjectForm} being wrapped.
   * @param parentResources
   *          The parent resources.
   * @param ownResources
   *          The form's own resources.
   * @since 1.67.6
   */
  protected PdfBoxXObjectTokenStream(PdfBoxPage pdfBoxPage,
                                     PDXObjectForm pdXObjectForm,
                                     PDResources parentResources,
                                     PDResources ownResources) {
    super(pdfBoxPage);
    this.pdXObjectForm = pdXObjectForm;
    this.parentResources = parentResources;
    this.ownResources = ownResources;
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
      pdXObjectForm.setResources(getStreamResources());
      Map<String, PDXObject> xobjects = parentResources.getXObjects();
      boolean found = true;
      for (Map.Entry<String, PDXObject> ent : xobjects.entrySet()) {
        String key = ent.getKey();
        PDXObject val = ent.getValue();
        if (val == oldForm) {
          xobjects.put(key, pdXObjectForm);
          /*
           * IMPLEMENTATION NOTE
           * 
           * The map returned by getXObjects() (PDFBox 1.8.9: PDResources.java
           * lines 326-339) is a HashMap that caches the stored COSDictionary of
           * XObjects. When setXObjects is called, the supplied map is used to
           * supplant the cached map and to regenerate the stored COSDictionary.
           * We observed identical original bytes being filtered to results that
           * differ only in the ordering of resource dictionaries in the
           * ASMscience Journals Plugin on the CLOCKSS production machines after
           * this code was introduced. It turns out that the machines running
           * Java 6 agreed together and those running Java 7 agreed together.
           * Use a sorted map instead (added in 1.68.3).
           */ 
          parentResources.setXObjects(new TreeMap<String, PDXObject>(xobjects));
          break;
        }
      }
      if (!found) {
        log.debug2("No mapping found while replacing form token stream");
      }
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
    return ownResources != null ? ownResources : parentResources;
  }
  
}
