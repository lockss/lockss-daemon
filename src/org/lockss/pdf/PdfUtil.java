/*
 * $Id: PdfUtil.java,v 1.1 2012-07-10 23:59:49 thib_gc Exp $
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

package org.lockss.pdf;

import java.io.*;
import java.util.*;

import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.util.*;
import org.lockss.util.CloseCallbackInputStream.DeleteFileOnCloseInputStream;

public class PdfUtil {

  public static final String CONFIG_PREFIX = Configuration.PREFIX + "pdf.";
  
  public static final int DEFAULT_PDF_MEMORY_LIMIT = 5 * 1024 * 1024; // %MB

  public static final String NAME_ID = "ID";

  public static final String NAME_LINK = "Link";

  public static final String NAME_SUBTYPE = "Subtype";

  public static final String PARAM_PDF_MEMORY_LIMIT = CONFIG_PREFIX + "pdfMemoryLimit";

  /**
   * <p>A suggested prefix for non-definitional parameters conveying
   * hints about PDF filter factories.</p>
   * @see DefinableArchivalUnit#SUFFIX_FILTER_FACTORY
   */
  public static final String PREFIX_PDF_FILTER_FACTORY_HINT = "hint_";

  private static final Logger logger = Logger.getLogger(PdfUtil.class);

  /**
   * <p>
   * Convenience call to {@link #asInputStream(PdfDocument, int)} using
   * a default memory limit defined by the parameter
   * {@link #PARAM_PDF_MEMORY_LIMIT}.
   * </p>
   * @param pdfDocument A PDF document.
   * @return The saved PDF document, as an input stream.
   * @throws PdfException
   *           If processing fails at the PDF level.
   * @throws IOException
   *           If processing fails at the I/O level.
   * @see #asInputStream(PdfDocument, int)
   */
  public static InputStream asInputStream(PdfDocument pdfDocument)
      throws PdfException, IOException {
    return asInputStream(pdfDocument,
                         getPdfMemoryLimit());
  }

  /**
   * <p>
   * Saves the given PDF document and returns the result as an input stream,
   * staying if possible in memory up to the given number of megabytes.
   * </p>
   * 
   * @param pdfDocument
   *          A PDF document.
   * @param memoryLimit
   *          The number of megabytes of memory up to which processing can be
   *          done entirely in memory.
   * @return The saved PDF document, as an input stream.
   * @throws PdfException
   *           If processing fails at the PDF level.
   * @throws IOException
   *           If processing fails at the I/O level.
   * @see DeferredTempFileOutputStream
   */
  public static InputStream asInputStream(PdfDocument pdfDocument,
                                          int memoryLimit) 
      throws PdfException, IOException {
    DeferredTempFileOutputStream os = new DeferredTempFileOutputStream(memoryLimit);
    pdfDocument.save(os);
    os.close();
    if (os.isInMemory()) {
      return new ByteArrayInputStream(os.getData());
    }
    else {
      return new BufferedInputStream(new DeleteFileOnCloseInputStream(os.getFile()));
    }
  }

  public static String getPdfHint(ArchivalUnit au) {
    String key = PREFIX_PDF_FILTER_FACTORY_HINT + Constants.MIME_TYPE_PDF + DefinableArchivalUnit.SUFFIX_HASH_FILTER_FACTORY;
    return AuUtil.getTitleAttribute(au, key);
  }

  public static int getPdfMemoryLimit() {
    return CurrentConfig.getCurrentConfig().getInt(PARAM_PDF_MEMORY_LIMIT,
                                             DEFAULT_PDF_MEMORY_LIMIT);
  }

  /**
   * <p>
   * Sets the ID array of the given PDF document to one consisting of the
   * arbitrary ID string "12345678901234567890123456789012" twice.
   * </p>
   * 
   * @param pdfDocument
   *          A PDF document.
   * @throws PdfException
   *           If processing fails.
   */
  public static void normalizeTrailerId(PdfDocument pdfDocument)
      throws PdfException {
    setTrailerId(pdfDocument,
                 "12345678901234567890123456789012",
                 "12345678901234567890123456789012");
  }
  
  /**
   * <p>
   * If the given PDF document is not <code>null</code>, closes it ignoring any
   * exception thrown by {@link PdfDocument#close()}.
   * </p>
   * 
   * @param pdfDocument
   *          A PDF document.
   * @see PdfDocument#close()
   */
  public static void safeClose(PdfDocument pdfDocument) {
    if (pdfDocument == null) {
      return;
    }
    try {
      pdfDocument.close();
    }
    catch (PdfException pdfe) {
      logger.debug2("Error closing a PDF document", pdfe);
    }
  }
  
  /**
   * <p>
   * Sets the trailer ID array of the given PDF document to one consisting of
   * the two given strings.
   * </p>
   * 
   * @param pdfDocument
   *          A PDF document.
   * @param id0
   *          The first string in the trailer ID array.
   * @param id1
   *          The second string in the trailer ID array.
   * @throws PdfException
   *           If processing fails.
   */
  public static void setTrailerId(PdfDocument pdfDocument,
                                  String id0,
                                  String id1)
      throws PdfException {
    PdfAdapter pdfAdapter = pdfDocument.getAdapter();
    Map<String, PdfToken> trailerMapping = pdfDocument.getTrailer();
    trailerMapping.remove(NAME_ID);
    List<PdfToken> idArray = new ArrayList<PdfToken>(2);
    idArray.add(pdfAdapter.makeString(id0));
    idArray.add(pdfAdapter.makeString(id1));
    trailerMapping.put(NAME_ID, pdfAdapter.makeArray(idArray));
    pdfDocument.setTrailer(trailerMapping);
  }
  
  /**
   * <p>
   * Unravels all the token streams of each page of the given PDF document.
   * </p>
   * 
   * @param pdfDocument
   *          A PDF document.
   * @throws PdfException
   *           If processing fails.
   * @see #unravelTokenStream(PdfTokenStream)
   * @see #unravelAllTokenStreams(PdfPage)
   */
  public static void unravelAllTokenStreams(PdfDocument pdfDocument)
      throws PdfException {
    for (PdfPage pdfPage : pdfDocument.getPages()) {
      unravelAllTokenStreams(pdfPage);
    }
  }
  
  /**
   * <p>
   * Unravels all the token streams of the given PDF page.
   * </p>
   * 
   * @param pdfPage
   *          A PDF page.
   * @throws PdfException
   *           If processing fails.
   * @see #unravelTokenStream(PdfTokenStream)
   */
  public static void unravelAllTokenStreams(PdfPage pdfPage)
      throws PdfException {
    for (PdfTokenStream pdfTokenStream : pdfPage.getAllTokenStreams()) {
      unravelTokenStream(pdfTokenStream);
    }
  }

  /**
   * <p>
   * Unravels the page token stream of the given PDF page.
   * </p>
   * 
   * @param pdfPage
   *          A PDF page.
   * @throws PdfException
   *           If processing fails.
   * @see #unravelTokenStream(PdfTokenStream)
   */
  public static void unravelPageTokenStream(PdfPage pdfPage)
      throws PdfException {
    unravelTokenStream(pdfPage.getPageTokenStream());
  }

  /**
   * <p>
   * Unravels the page stream of each page of the given PDF document.
   * </p>
   * 
   * @param pdfDocument
   *          A PDF document.
   * @throws PdfException
   *           If processing fails.
   * @see #unravelTokenStream(PdfTokenStream)
   */
  public static void unravelPageTokenStreams(PdfDocument pdfDocument)
      throws PdfException {
    for (PdfPage pdfPage : pdfDocument.getPages()) {
      unravelTokenStream(pdfPage.getPageTokenStream());
    }
  }

  /**
   * <p>
   * Reads all the tokens from the given stream, then writes the result back to
   * it.
   * </p>
   * <p>
   * The purpose of this seemingly idempotent operation is to force the
   * underlying stream implementation to unravel any parts of the stream it may
   * have been able to delay interpreting until the stream is accessed, such as
   * decoding a filtered stream.
   * </p>
   * 
   * @param pdfTokenStream A token stream.
   * @throws PdfException If processing fails.
   */
  public static void unravelTokenStream(PdfTokenStream pdfTokenStream)
      throws PdfException {
    pdfTokenStream.setTokens(pdfTokenStream.getTokens());
  }
  
  private PdfUtil() {
  }

}
