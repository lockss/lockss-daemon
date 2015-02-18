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

package org.lockss.plugin.georgthiemeverlag;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


/**
 * Thin class to extract DOI from filename
 * There are some PDF only articles like 
 * https://www.thieme-connect.de/ejournals/pdf/10.1055/s-0032-1321390.pdf
 * see TOC https://www.thieme-connect.de/ejournals/issue/10.1055/s-002-23551
 * 
 * Note: It appears that a hidden abstract is available for PDF only
 * see https://www.thieme-connect.de/ejournals/abstract/10.1055/s-0032-1321389
 * Therefore another approach would be to create subclass of GoslingHtmlLinkExtractor
 * like GeorgThiemeVerlagHtmlLinkExtractor, whose only purpose would be to add an
 * abstract url for all pdf links, just in case it existed
 */
public class GeorgThiemeVerlagPdfMetadataExtractorFactory implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(GeorgThiemeVerlagPdfMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
      throws PluginException {
    return new GeorgThiemeVerlagPdfMetadataExtractor();
  }
  
  public static class GeorgThiemeVerlagPdfMetadataExtractor
    extends SimpleFileMetadataExtractor {
    
    protected static final Pattern DOI_PAT = 
        Pattern.compile("/pdf/(10[.][^/]*/[^.]*)[.]pdf");
    
    private static MultiMap tagMap = new MultiValueMap();
    static {
      //<meta name="citation_doi" content="10.1055/s-0...
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
    }
    
    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
        throws IOException {
      ArticleMetadata am = new ArticleMetadata();
      Matcher m = DOI_PAT.matcher(cu.getUrl());
      if (m.find()) {
        String doi = m.group(1);
        am.putRaw("citation_doi", doi);
        am.cook(tagMap);
      }
      // PD-440
      am.replace(MetadataField.FIELD_PUBLISHER, "Georg Thieme Verlag KG");
      return am;
    }
  }
}
