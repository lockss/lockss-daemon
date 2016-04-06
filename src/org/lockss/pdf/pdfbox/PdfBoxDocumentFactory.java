/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.List;

import org.apache.pdfbox.exceptions.*;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectForm;
import org.lockss.pdf.*;
import org.lockss.util.IOUtil;

/**
 * <p>
 * A {@link PdfDocumentFactory} implementation based on PDFBox 1.8.11.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.56
 * @see <a href="http://pdfbox.apache.org/">PDFBox site</a>
 */
public class PdfBoxDocumentFactory implements PdfDocumentFactory {

  @Override
  public PdfTokenFactory getTokenFactory() {
    return PdfBoxTokens.getAdapterInstance();
  }
  
  @Override
  public PdfBoxDocument makeDocument(InputStream pdfInputStream)
      throws IOException,
             PdfCryptographyException,
             PdfException {
    try {
      PDDocument pdDocument = makePdDocument(pdfInputStream);
      processAfterParse(pdDocument);
      return makeDocument(this, pdDocument);
    }
    catch (CryptographyException ce) {
      throw new PdfCryptographyException(ce);
    }
    catch (IOException ioe) {
      throw new PdfException(ioe);
    }
    finally {
      // PDFBox normally closes the input stream, but just in case
      IOUtil.safeClose(pdfInputStream);
    }
  }
  
  @Override
  public PdfBoxDocument makeDocument(PdfDocumentFactory pdfDocumentFactory,
                                     Object pdfDocumentObject)
      throws PdfException {
    return new PdfBoxDocument(this, (PDDocument)pdfDocumentObject);
  }

  @Override
  public PdfBoxPage makePage(PdfDocument pdfDocument,
                             Object pdfPageObject)
      throws PdfException {
    return new PdfBoxPage((PdfBoxDocument)pdfDocument, (PDPage)pdfPageObject);
  }
  
  /**
   * <p>
   * Makes a new PDF page token stream instance from the given PDF page instance
   * and from the given PDF token stream object data suitable for this PDF
   * implementation.
   * </p>
   * 
   * @param pdfPage
   *          The PDF page the XObject token stream comes from
   * @param pdfTokenStreamObject
   *          The PDF token stream object data (implementation-dependent)
   * @return A PDF page token stream instance suitable for this PDF
   *         implementation
   * @throws PdfException
   *           If an error occurs
   * @since 1.70
   */
  public PdfBoxPageTokenStream makePageTokenStream(PdfPage pdfPage,
                                                   Object pdfTokenStreamObject)
      throws PdfException {
    return new PdfBoxPageTokenStream((PdfBoxPage)pdfPage, (PDStream)pdfTokenStreamObject);
  }

  @Override
  public PdfTokenStream makeTokenStream(PdfPage pdfPage,
                                        Object pdfToenStreamObject)
      throws PdfException {
    throw new UnsupportedOperationException("Not supported by this class; use makePageTokenStream and makeXObjectTokenStream instead");
  }
  
  /**
   * <p>
   * Makes a new PDF XObject token stream instance from the given PDF page
   * instance and from the given PDF token stream object data suitable for this
   * PDF implementation (a list made of the {@link PDXObjectForm} instance, the
   * parent {@link PDResources} instance and the proper {@link PDResources}
   * instance).
   * </p>
   * 
   * @param pdfPage
   *          The PDF page the XObject token stream comes from
   * @param pdfTokenStreamObject
   *          The PDF token stream object data (implementation-dependent)
   * @return A PDF XObject token stream instance suitable for this PDF
   *         implementation
   * @throws PdfException
   *           If an error occurs
   * @since 1.70
   */
  public PdfBoxXObjectTokenStream makeXObjectTokenStream(PdfPage pdfPage,
                                                         Object pdfTokenStreamObject)
      throws PdfException {
    List<?> varargs = (List<?>)pdfTokenStreamObject;
    return new PdfBoxXObjectTokenStream((PdfBoxPage)pdfPage,
                                        (PDXObjectForm)varargs.get(0),
                                        (PDResources)varargs.get(1),
                                        (PDResources)varargs.get(2));
  }
  
  @Override
  @Deprecated
  public PdfDocument parse(InputStream pdfInputStream)
      throws IOException,
             PdfCryptographyException,
             PdfException {
    return makeDocument(pdfInputStream);
  }

  /**
   * 
   * @param pdfInputStream
   * @return
   * @throws IOException
   * @since 1.70
   */
  protected PDDocument makePdDocument(InputStream pdfInputStream)
      throws IOException {
    PDFParser pdfParser = new PDFParser(pdfInputStream);
    pdfParser.parse(); // Probably closes the input stream
    PDDocument pdDocument = pdfParser.getPDDocument();
    return pdDocument;
  }
  
  /**
   * <p>
   * Override this method to alter the processing of the {@link PDDocument}
   * instance after it has been parsed by {@link PDFParser#parse()}.
   * </p>
   * 
   * @param pdDocument
   *          A freshly parsed {@link PDDocument} instance
   * @throws CryptographyException
   *           if a cryptography exception is thrown
   * @throws IOException
   *           if an I/O exception is thrown
   * @since 1.67
   */
  protected void processAfterParse(PDDocument pdDocument)
      throws CryptographyException, IOException {
    pdDocument.setAllSecurityToBeRemoved(true);
    if (pdDocument.isEncrypted()) {
      pdDocument.decrypt("");
    }
  }
  
}
