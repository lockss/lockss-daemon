/*
 * $Id: PdfLinkExtractor.java,v 1.2 2009-10-08 00:01:21 thib_gc Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

public class PdfLinkExtractor implements LinkExtractor {

  protected static class OutputAllLinks implements PageTransform {
    
    protected LinkExtractor.Callback callback;
    
    protected String baseUrl;
    
    public OutputAllLinks(String baseUrl,
                          LinkExtractor.Callback callback) {
      this.baseUrl = baseUrl;
      this.callback = callback;
    }
    
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

  private static final Logger logger = Logger.getLogger("PdfLinkExtractor");
  
}
