/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

import org.apache.pdfbox.exceptions.*;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.lockss.pdf.*;
import org.lockss.util.IOUtil;

/**
 * <p>
 * A {@link PdfDocumentFactory} implementation based on PDFBox 1.6.0.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.56
 * @see <a href="http://pdfbox.apache.org/">PDFBox site</a>
 */
public class PdfBoxDocumentFactory implements PdfDocumentFactory {

  @Override
  public PdfDocument parse(InputStream pdfInputStream)
      throws IOException,
             PdfCryptographyException,
             PdfException {
    try {
      // Parse the input stream
      PDFParser pdfParser = new PDFParser(pdfInputStream);
      pdfParser.parse(); // Probably closes the input stream
      PDDocument pdDocument = pdfParser.getPDDocument();
      processAfterParse(pdDocument);
      return new PdfBoxDocument(pdDocument);
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
