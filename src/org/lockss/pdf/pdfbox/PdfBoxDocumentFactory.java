/*

Copyright (c) 2000-2023 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.exceptions.*;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectForm;
import org.lockss.config.*;
import org.lockss.util.Logger;
import org.lockss.pdf.*;
import org.lockss.util.IOUtil;
import org.lockss.util.TimeBase;
import org.lockss.util.Constants;

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
  static Logger log = Logger.getLogger(PdfBoxDocumentFactory.class);

  public static final PdfDocumentFactory SINGLETON =
    new PdfBoxDocumentFactory();

  static final String PREFIX = Configuration.PREFIX + "pdf.";
  /** The minimum interval between clearing the PDFont and COSName
   * static caches */
  public static final String PARAM_CACHE_FLUSH_INTERVAL =
    PREFIX + "cacheFlushInterval";
  public static final long DEFAULT_CACHE_FLUSH_INTERVAL = 6 * Constants.HOUR;

  // The fields below support a mechanism to periodically flush the
  // static caches in PDFont and COSName.  It tries to not clear those
  // caches while a parse is in progress, but as it doesn't prevent a
  // parse from starting once it has made a decision to flush, or
  // during the flush, this is not guaranteed.  We don't know if
  // flushing during a pdf operation can cause problems.

  // Records all active PdfBoxDocument s, to determine when it might
  // not be safe to call the static clearResources() methods
  private Set<PdfBoxDocument> activePdfBoxDocs = ConcurrentHashMap.newKeySet();

  // Count of parsers running in makeDocument() before a
  // PdfBoxDocument has been created to add to the Set above.
  private volatile int kludgeyInterimParserCounter = 0;

  private static long cacheFlushInterval = DEFAULT_CACHE_FLUSH_INTERVAL;
  private long nextClearResourcesTime = 0;

  // org.lockss.pdf.pdfbox.PdfBoxTokens.Nam points to things in
  // COSName - is that a problem?


  @Override
  public PdfTokenFactory getTokenFactory() {
    return PdfBoxTokens.getFactory();
  }
  
  @Override
  public PdfBoxDocument makeDocument(InputStream pdfInputStream)
      throws IOException,
             PdfCryptographyException,
             PdfException {
    try {
      ++kludgeyInterimParserCounter;
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
      --kludgeyInterimParserCounter;
    }
  }
  
  @Override
  public PdfBoxDocument makeDocument(PdfDocumentFactory pdfDocumentFactory,
                                     Object pdfDocumentObject)
      throws PdfException {
    PdfBoxDocument res =
      new PdfBoxDocument(this, (PDDocument)pdfDocumentObject);
    activePdfBoxDocs.add(res);
    return res;
  }

  void documentClosed(PdfBoxDocument pbdoc) {
    activePdfBoxDocs.remove(pbdoc);
    log.debug2("documentClosed: " + pbdoc +
               ", interim: " + kludgeyInterimParserCounter +
               ", remaining: " + activePdfBoxDocs);
    if (kludgeyInterimParserCounter < 0) {
      log.warning("Oh dear, kludgeyInterimParserCounter is negative: " +
                  kludgeyInterimParserCounter);
    }
    if (kludgeyInterimParserCounter == 0 && activePdfBoxDocs.isEmpty() &&
        TimeBase.nowMs() >= nextClearResourcesTime) {
      clearResources();
    }
  }

  private void clearResources() {
    log.info("Clearing PDF caches");
    PDFont.clearResources();
    COSName.clearResources();
    nextClearResourcesTime = TimeBase.nowMs() + cacheFlushInterval;
  }

  /** Called by org.lockss.config.MiscConfig
   */
  public static void setConfig(Configuration config,
                               Configuration oldConfig,
                               Configuration.Differences diffs) {
    if (diffs.contains(PREFIX)) {
      cacheFlushInterval = config.getTimeInterval(PARAM_CACHE_FLUSH_INTERVAL,
                                                  DEFAULT_CACHE_FLUSH_INTERVAL);
    }
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
