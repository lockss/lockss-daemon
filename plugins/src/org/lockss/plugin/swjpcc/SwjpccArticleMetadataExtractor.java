/*
 * $Id$
 */

/*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.swjpcc;

import java.io.IOException;

// Make a custom AF object so that we can pass the already scraped DOI to the extractor
// then just fill in the metadata from what is known by the landing url
// there is nothing more to get

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.swjpcc.SwjpccArticleIteratorFactory.SwjpccArticleFiles;
import org.lockss.util.Logger;


/*
 *  We don't actually parse any file. We actually just emit the information 
 *  that we can glean from the URLs in the AF and from any info that we found 
 *  while parsing the article page in the iterator while finding the PDF link
 *  and that we stored in this custom version of the AF
 */
public class SwjpccArticleMetadataExtractor extends BaseArticleMetadataExtractor{

  private static Logger log = 
      Logger.getLogger(SwjpccArticleMetadataExtractor.class);
  private static final String SWJPCC_PUB = "Southwest Journal of Pulmonary and Critical Care";
  private static final String SWJPCC_PUBTITLE = "Southwest Journal of Pulmonary and Critical Care";
  private static final String SWJPCC_EISSN = "2160-6773";
  //http://www.swjpcc.com/sleep/2018/2/5/sleep-board-review-question-restless-legs.html

  final static Pattern MD_URL_PATTERN = 
      Pattern.compile("https?://www\\.swjpcc\\.com/([^/]+)/([^/]+)/.*\\.html?$", Pattern.CASE_INSENSITIVE);  
  // http://www.swjpcc.com/storage/manuscripts/volume-16/issue-2-feb/028-18/028-18.pdf
  final static Pattern FT_PDF_VOL_PATTERN =
      Pattern.compile("storage/(manuscripts|pdf-version-of-articles)/volume-([^/]+)/issue-([^/]+)/.*\\.pdf$", Pattern.CASE_INSENSITIVE);  
  
  @Override
  public void extract(MetadataTarget target, ArticleFiles af, Emitter emitter)
      throws IOException, PluginException {

    SwjpccEmitter emit = new SwjpccEmitter(af, emitter);

    CachedUrl metadataCu = af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA);
    emit.emitMetadata(metadataCu, new ArticleMetadata());
  }
  
  
  static class SwjpccEmitter implements FileMetadataExtractor.Emitter {
    private Emitter parent;
    private ArticleFiles af;

    
    SwjpccEmitter(ArticleFiles af, Emitter parent) {
      this.af = af;
      this.parent = parent;
    }

    public void emitMetadata(CachedUrl cu, ArticleMetadata am) {
      Matcher mat = MD_URL_PATTERN.matcher(cu.getUrl());
      if (!(mat.matches())) {
        log.debug3("Unexpected metadata URL" + cu.getUrl());
        return;
      }
      // Get hardcoded values, what we can from the urls and what we parsed out of the page
      am.put(MetadataField.FIELD_PUBLISHER, SWJPCC_PUB);
      am.put(MetadataField.FIELD_PUBLICATION_TITLE,  SWJPCC_PUBTITLE);
      am.put(MetadataField.FIELD_EISSN, SWJPCC_EISSN);
      
      // and parsed from the page and stored on the AF
      String found = ((SwjpccArticleFiles)(af)).getFoundDoi();
      if (found != null) {
        am.put(MetadataField.FIELD_DOI,found);
      }
      found = ((SwjpccArticleFiles)(af)).getFoundTitle();
      if (found != null) {
        am.put(MetadataField.FIELD_ARTICLE_TITLE,found);
      }
      found = ((SwjpccArticleFiles)(af)).getFoundYear();
      if (found != null) {
        am.put(MetadataField.FIELD_DATE,found);
      }
      found = ((SwjpccArticleFiles)(af)).getFoundVol();
      if (found != null) {
        am.put(MetadataField.FIELD_VOLUME,found);
      }
      found = ((SwjpccArticleFiles)(af)).getFoundIss();
      if (found != null) {
        am.put(MetadataField.FIELD_ISSUE,found);
      }
      found = ((SwjpccArticleFiles)(af)).getFoundStart();
      if (found != null) {
        am.put(MetadataField.FIELD_START_PAGE,found);
      }
      
      // From the article url
      String year = mat.group(2);
      String topic = mat.group(1);
      am.put(MetadataField.FIELD_KEYWORDS,topic);
      String foundYear = am.get(MetadataField.FIELD_DATE);
      if (foundYear == null) {
        am.put(MetadataField.FIELD_DATE,year);
      } else if (!foundYear.equals(year)) {
        log.debug3("URL year and CITATION year don't agree: " + cu.getUrl() + ", " + foundYear);
      }
         

      // from the pdf url if we have one and if it's of this form
      String ftCu_url = (af.getFullTextCu()).getUrl();
      if (ftCu_url.endsWith(".pdf") && ftCu_url.contains("volume")) {
        // http://www.swjpcc.com/storage/manuscripts/volume-16/issue-2-feb/028-18/028-18.pdf
        Matcher pdfMat = FT_PDF_VOL_PATTERN.matcher(ftCu_url);
          if (pdfMat.matches()) {
            String foundVol = ((SwjpccArticleFiles)(af)).getFoundVol();
            String urlVol = pdfMat.group(2);
            if (foundVol == null) {
              am.put(MetadataField.FIELD_DATE,urlVol);
            } else if (!foundVol.equals(urlVol)) {
              log.debug3("URL volume and CITATION volume don't agree: " + ftCu_url + ", " + foundVol);
            }
          }
      }


      parent.emitMetadata(af, am);
    }

    void setParentEmitter(Emitter parent) {
      this.parent = parent;
    }
  }
}