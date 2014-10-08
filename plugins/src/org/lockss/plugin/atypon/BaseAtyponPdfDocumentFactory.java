/*
 * $Id: BaseAtyponPdfDocumentFactory.java,v 1.1 2014-10-08 16:11:26 alexandraohlson Exp $
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

package org.lockss.plugin.atypon;

import java.io.*;

import org.apache.pdfbox.exceptions.*;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.lockss.filter.pdf.SimplePdfFilterFactory;
import org.lockss.pdf.*;
import org.lockss.pdf.pdfbox.PdfBoxDocument;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.IOUtil;

/*
 * FIXME 1.67
 */
public class BaseAtyponPdfDocumentFactory implements PdfDocumentFactory {
  @Override
  public PdfDocument parse(InputStream pdfInputStream)
      throws IOException, PdfCryptographyException, PdfException {
    try {
      PDFParser pdfParser = new PDFParser(pdfInputStream);
      pdfParser.parse();
      PDDocument pdDocument = pdfParser.getPDDocument();
      if (pdDocument.isEncrypted()) {
        pdDocument.decrypt("");
      }
      pdDocument.setAllSecurityToBeRemoved(true);
      return new PdfBoxDocument(pdDocument) {
        // Empty body (constructor is not visible but is visible from subclass)
      };
    }
    catch (CryptographyException ce) {
      throw new PdfCryptographyException(ce);
    }
    catch (IOException ioe) {
      throw new PdfException(ioe);
    }
    catch (Exception exc) {
      // FIXME 1.67
      // InvalidPasswordException thrown by PDFBox <= 1.8.5 but not >= 1.8.6
      // This hack will avoid 1.67 plugins being incompatible with 1.67 
      if (exc instanceof InvalidPasswordException) {
        throw new PdfCryptographyException(exc);
      }
      else {
        throw new PdfException(exc);
      }
    }
    finally {
      IOUtil.safeClose(pdfInputStream);
    }
  }
}
