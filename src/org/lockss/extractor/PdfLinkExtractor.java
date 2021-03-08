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
import java.net.MalformedURLException;
import java.util.Iterator;

import org.lockss.daemon.PluginException;
import org.lockss.filter.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;
import org.pdfbox.pdmodel.interactive.action.type.*;
import org.pdfbox.pdmodel.interactive.annotation.*;

/**
 * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
 */
@Deprecated
public class PdfLinkExtractor implements LinkExtractor {

  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  protected static class OutputAllLinks implements PageTransform {
    
    /**
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    protected LinkExtractor.Callback callback;
    
    /**
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    protected String baseUrl;
    
    /**
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    public OutputAllLinks(String baseUrl,
                          LinkExtractor.Callback callback) {
      this.baseUrl = baseUrl;
      this.callback = callback;
    }
    
    /**
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    public boolean transform(PdfPage pdfPage) throws IOException {
      for (Iterator iter = pdfPage.getAnnotationIterator() ; iter.hasNext() ; ) {
        PDAnnotation pdAnnotation = (PDAnnotation)iter.next();
        if (pdAnnotation instanceof PDAnnotationLink) {
          PDAnnotationLink pdAnnotationLink = (PDAnnotationLink)pdAnnotation;
          PDAction pdAction = pdAnnotationLink.getAction();
          if (pdAction instanceof PDActionURI) {
            PDActionURI pdActionUri = (PDActionURI)pdAction;
            emit(pdActionUri.getURI());
          }
        }
      }
      return true; // always succeed
    }
    
    /**
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    protected void emit(String url) throws MalformedURLException {
      if ("".equals(url)) {
        throw new MalformedURLException("Empty URL");
      }
      String resolved = UrlUtil.resolveUri(baseUrl, url);
      if (logger.isDebug2()) {
        logger.debug2("Found " + url + " which resolves to " + resolved);
      }
      callback.foundLink(resolved);
    }
    
  }
  
  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public void extractUrls(ArchivalUnit au,
                          InputStream in,
                          String encoding,
                          String srcUrl,
                          LinkExtractor.Callback cb)
      throws IOException, PluginException {
    PdfDocument pdfDocument = new PdfDocument(in);
    PageTransform pageTransform = new OutputAllLinks(srcUrl, cb);
    DocumentTransform documentTransform = new TransformEachPage(pageTransform);
    documentTransform.transform(pdfDocument);
    pdfDocument.close();
  }

  /**
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  private static final Logger logger = Logger.getLogger(PdfLinkExtractor.class);
  
}
