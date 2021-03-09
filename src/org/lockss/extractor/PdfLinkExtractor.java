/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.extractor;

import java.io.*;
import java.util.Map;

import org.lockss.daemon.PluginException;
import org.lockss.pdf.*;
import org.lockss.pdf.PdfDocument;
import org.lockss.pdf.PdfPage;
import org.lockss.pdf.PdfUtil;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;

/**
 * <p>
 * A PDF link extractor. 
 * </p>
 * <p>
 * 
 * </p>
 * 
 * @author Thib Guicherd-Callin
 */
public class PdfLinkExtractor implements LinkExtractor {

  protected PdfDocumentFactory pdfDocumentFactory;
  
  /**
   * <p>
   * Makes a new PDF link extractor using the default PDF document factory
   * ({@link DefaultPdfDocumentFactory}).
   * </p>
   * 
   * @since 1.76
   * @see #PdfLinkExtractor(PdfDocumentFactory) 
   * @see DefaultPdfDocumentFactory
   */
  public PdfLinkExtractor() {
    this(DefaultPdfDocumentFactory.getInstance());
  }
  
  /**
   * <p>
   * Makes a new PDF link extractor using the given PDF document factory
   * ({@link DefaultPdfDocumentFactory}).
   * </p>
   * 
   * @since 1.76
   */
  public PdfLinkExtractor(PdfDocumentFactory pdfDocumentFactory) {
    this.pdfDocumentFactory = pdfDocumentFactory;
  }
  
  @Override
  public void extractUrls(ArchivalUnit au,
                          InputStream in,
                          String encoding,
                          String srcUrl,
                          Callback cb)
      throws IOException,
             PluginException {
    // Inspired by PDFBox 1.8.16 PrintURLs (https://github.com/apache/pdfbox/blob/1.8.16/examples/src/main/java/org/apache/pdfbox/examples/pdmodel/PrintURLs.java)
    PdfDocument pdfDocument = null;
    try {
      pdfDocument = pdfDocumentFactory.makeDocument(in);
      for (PdfPage pdfPage : pdfDocument.getPages()) { // FIXME 1.76
        for (PdfToken annotToken : pdfPage.getAnnotations()) {
          if (!annotToken.isDictionary()) {
            continue; // not supposed to happen
          }
          Map<String, PdfToken> annotDictionary = annotToken.getDictionary();
          PdfToken annotSubtype = annotDictionary.get("Subtype");
          if (annotSubtype == null || !annotSubtype.isName() || !annotSubtype.getName().equals("Link")) {
            continue;
          }
          PdfToken actionToken = annotDictionary.get("A");
          if (actionToken == null || !actionToken.isDictionary()) {
            continue;
          }
          Map<String, PdfToken> actionDictionary = actionToken.getDictionary();
          PdfToken actionSubtype = actionDictionary.get("S");
          if (actionSubtype == null || !actionSubtype.isName() || !actionSubtype.getName().equals("URI")) {
            continue;
          }
          PdfToken uriToken = actionDictionary.get("URI");
          if (uriToken == null || !uriToken.isString()) {
            continue;
          }
          String uri = uriToken.getString();
          log.debug2("Found link: " + uri);
          cb.foundLink(uri);
        }
      }
    }
    catch (PdfException pe) {
      throw new IOException(pe);
    }
    finally {
      PdfUtil.safeClose(pdfDocument);
    }
  }
  
  /**
   * <p>
   * A logger for use by this class.
   * </p>
   */
  private static final Logger log = Logger.getLogger(PdfLinkExtractor.class);

}
