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

package org.lockss.plugin.usdocspln.gov.gpo.fdsys;

import java.util.*;
import java.util.regex.*;

import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.extractor.*;
import org.lockss.daemon.PluginException;

public class GPOFDSysBulkDataArticleIteratorFactory
  implements ArticleIteratorFactory,
       ArticleMetadataExtractorFactory {
  
  protected static Logger log = Logger.getLogger(GPOFDSysBulkDataArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE =
    "\"%sfdsys/bulkdata/%s/%s/\", base_url, collection_id, volume_name";

  protected static final String PATTERN_TEMPLATE =
    "\"^%sfdsys/bulkdata/%s/%s/%s-%s\", base_url, collection_id, volume_name, collection_id, volume_name, collection_id";

  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new GPOFDSysBulkDataArticleIterator(au, new SubTreeArticleIterator.Spec()
              .setTarget(target)
              .setRootTemplate(ROOT_TEMPLATE)
              .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));
  }
  
  protected static class GPOFDSysBulkDataArticleIterator
    extends SubTreeArticleIterator {
    
    protected static Pattern HTML_PATTERN = 
      Pattern.compile("fdsys/bulkdata/([^/]+)/([^/]+)/([^/]+)-([^/]+)/(?!.*About).*\\.html$",
        Pattern.CASE_INSENSITIVE);
    
    protected static Pattern TEXT_PATTERN = 
      Pattern.compile("fdsys/bulkdata/([^/]+)/([^/]+)/([^/]+)-([^/]+)/(?!.*About).*\\.txt$",
        Pattern.CASE_INSENSITIVE);
    
    protected static Pattern XML_PATTERN = 
      Pattern.compile("fdsys/bulkdata/([^/]+)/([^/]+)/([^/]+)-([^/]+)/(?!.*About).*\\.xml$",
        Pattern.CASE_INSENSITIVE);    
    
    protected GPOFDSysBulkDataArticleIterator(ArchivalUnit au,
                                  SubTreeArticleIterator.Spec spec) {
      super(au, spec);
      spec.setVisitArchiveMembers(true);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      
      // There's no appreciable difference between .html, .txt, and .xml
      // articles. No metadata can be extracted from any of the formats.
      if (HTML_PATTERN.matcher(url).find() ||
          TEXT_PATTERN.matcher(url).find() ||
          XML_PATTERN.matcher(url).find()) {
        return processFullText(cu);
      }
      
      System.out.println("Matched no patterns for URL: " + url);
      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }
    
    protected ArticleFiles processFullText(CachedUrl cu) {
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(cu);
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, cu);
      return af;
    }
  }
  
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(null);
  }
}