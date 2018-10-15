/*
 * $Id$
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ingenta;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.MetadataTarget;
import org.lockss.extractor.SimpleHtmlMetaTagMetadataExtractor;
import org.lockss.plugin.*;
import org.lockss.util.HeaderUtil;
import org.lockss.util.Logger;

public class IngentaContentValidator {
  
  private static final Logger log = Logger.getLogger(IngentaContentValidator.class);
  //NO: https://www.ingentaconnect.com/content/aspt/sb/2018/00000043/00000002
  //YES: https://www.ingentaconnect.com/content/aspt/sb/2018/00000043/00000003/art00001
  private static final Pattern ART_LANDING_PATTERN = 
      Pattern.compile("^https?://www\\.ingentaconnect\\.com/content(one)?/[^/]+/[^/]+/[0-9]{4}/0*[0-9]+/.{8}/art[0-9]{5}$",
          Pattern.CASE_INSENSITIVE);
  // we don't currently set a mime-type for PDF; just crawler=true but it might be coming so make optional
  private static final Pattern CRAWLER_PDF_PATTERN = 
      Pattern.compile("^https?://api\\.ingentaconnect\\.com/content(one)?/[^/]+/[^/]+/[0-9]{4}/0*[0-9]+/.{8}/art[0-9]{5}\\?crawler=true(&mimetype=application/pdf)?$",
          Pattern.CASE_INSENSITIVE);
  
  
    /*
     * Applied to text/html files
     * 1. It looks at patterns that *should* be PDF and complains if these are html
     * 2.  it then looks at only those patterns that would be an article landing page.
     * It looks at the contents of the file to make sure that the article was collected
     * with preservation support in place - indicated by the existence of a 
     * <meta CRAWLER.fullTextLink<meta name="CRAWLER.fullTextLink" content="ht...
     */

  public static class HtmlTypeValidator implements ContentValidator {
    
    public void validate(CachedUrl cu)
        throws ContentValidationException, PluginException, IOException {
      // validate based on extension (ie .pdf or .jpg)
      String url = cu.getUrl();
      
      Matcher pdfMat = CRAWLER_PDF_PATTERN.matcher(url);
      if (pdfMat.find()) {
        log.debug3("Expected PDF is html type mismatch: " + url);
        throw new ContentValidationException("URL MIME type mismatch");
      }
      
      Matcher artMatch = ART_LANDING_PATTERN.matcher(url);

      if (artMatch.matches()) {
        log.debug3("On an article landing page: " + url);
        
        MetadataTarget at = new MetadataTarget(MetadataTarget.PURPOSE_ARTICLE);
        ArticleMetadata am;
        SimpleHtmlMetaTagMetadataExtractor ext = new SimpleHtmlMetaTagMetadataExtractor();
        boolean fails = false;
        if (cu !=null && cu.hasContent()) {
          try {
            at.setFormat("text/html");
            am = ext.extract(at, cu);
            if ( !am.containsRawKey("CRAWLER.fullTextLink")) {
              fails = true;
            } else {
                log.debug3("Found CRAWLER.fullTextLink: " + am.getRaw("CRAWLER.fullTextLink"));
            }
          }catch (IOException e) {
            log.debug("Unable to parse abstract page html for validation", e);
            fails = true;
          }
        }
        // CHECK THE INTERNAL BITS FOR THE META TAG
        if (fails) {
          throw new ContentValidationException("Article landing page no CRAWLER.fullTextLink");
        }
      }
    }
  }
  
  public static class Factory implements ContentValidatorFactory {
    public ContentValidator createContentValidator(ArchivalUnit au, String contentType) {
      switch (HeaderUtil.getMimeTypeFromContentType(contentType)) {
      case "text/html":
      case "text/*":
        return new HtmlTypeValidator();
      default:
        return null;
      }
    }
  }
  
}

