/*
 * $Id: PdfDocument.java,v 1.1 2006-07-26 22:40:15 thib_gc Exp $
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

package org.lockss.util;

import java.io.*;
import java.util.Iterator;

import org.pdfbox.cos.COSDocument;
import org.pdfbox.exceptions.COSVisitorException;
import org.pdfbox.pdfparser.PDFParser;
import org.pdfbox.pdmodel.*;
import org.pdfbox.pdmodel.fdf.FDFDocument;

/**
 * <p>Convenience class to provide easy access to the internals of
 * a PDF document.</p>
 * <p>The PDFBox API provides several levels of access to a PDF
 * document via different class hierarchies ({@link COSDocument},
 * {@link PDDocument}, {@link FDFDocument}). The only unified view of
 * a PDF document is through {@link PDFParser}, but unintuitively
 * it is a parser more than a document object. This class exposes an
 * API more related to the PDF document under the parser.</p>
 * @author Thib Guicherd-Callin
 */
public class PdfDocument extends PDFParser {

  /**
   * <p>Builds a new PDF document (actually a new PDF parser).</p>
   * <p><em>You must call {@link #close} to release the expensive
   * resources associated with this object, when it is no longer
   * needed but before it is finalized by the runtime system.</em></p>
   * @param inputStream The input stream that contains the PDF document.
   * @throws IOException if any processing error occurs.
   * @see PDFParser#PDFParser(InputStream)
   */
  public PdfDocument(InputStream inputStream) throws IOException {
    super(inputStream);
    super.parse(); // do not call this.parse(), call super.parse()
  }

  /**
   * <p>This will close the underlying {@link COSDocument} instance
   * and release many expensive resources held by this object.</p>
   * @throws IOException if any processing error occurs.
   * @see PDDocument#close
   */
  public void close() throws IOException {
    getPDDocument().close();
  }

  public PDPage getPage(int index) throws IOException {
    return (PDPage)getPDDocument().getDocumentCatalog().getAllPages().get(index);
  }

  public Iterator /* of PDPage */ getPageIterator() throws IOException {
    return getPDDocument().getDocumentCatalog().getAllPages().iterator();
  }

  /**
   * <p><em>Do not explicitly call this method as it is already called
   * by the constructor.</em></p>
   * <p>There is no harm in an accidental call; calling this method
   * has no effect.</p>
   * @see #PdfDocument(InputStream)
   */
  public void parse() throws IOException {
    /*
     * Let clients forget that they have to build the object first and
     * then parse it. Give the illusion that building a PdfDocument
     * builds the whole PDF object graph. The constructor calls
     * super.parse().
     */
  }

  /**
   * <p>This will save the underlying {@link PDDocument} instance to
   * an output stream.</p>
   * @param outputStream An output stream into which this document
   *                     will be saved.
   * @throws IOException if any processing error occurs.
   */
  public void save(OutputStream outputStream) throws IOException {
    try {
      getPDDocument().save(outputStream);
    }
    catch (COSVisitorException cve) {
      IOException ioe = new IOException();
      ioe.initCause(cve);
      throw ioe;
    }
  }

}
