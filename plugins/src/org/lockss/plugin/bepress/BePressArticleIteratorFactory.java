/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.bepress;

import java.io.IOException;
import java.util.*;
import java.util.regex.*;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.extractor.*;
import org.lockss.daemon.PluginException;

public class BePressArticleIteratorFactory
  implements ArticleIteratorFactory,
	     ArticleMetadataExtractorFactory {
  
  /**
   * <p>An article iterator factory, for the Section plugin variant</p>
   * @author Thib Guicherd-Callin
   */
  public static class Section implements ArticleIteratorFactory {
    
    protected static final String ROOT_TEMPLATE = "\"%s%s/%s\", base_url, journal_abbr, journal_section";
    
    protected static final String PATTERN_TEMPLATE = "\"^%s%s/%s/((([^0-9]+/)?(vol)?%d/(iss)?[0-9]+/(art|editorial)?[0-9]+)|(vol%d/(?-i:[A-Z])[0-9]+))$\", base_url, journal_abbr, journal_section, volume, volume";

    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                        MetadataTarget target)
        throws PluginException {
      return new BePressArticleIterator(au,
                                        new SubTreeArticleIterator.Spec()
                                            .setTarget(target)
                                            .setRootTemplate(ROOT_TEMPLATE)
                                            .setPatternTemplate(PATTERN_TEMPLATE),
                                        true);
    }
    
  }
  
  protected static Logger log = Logger.getLogger("BePressArticleIteratorFactory");
  

  protected static final String ROOT_TEMPLATE = "\"%s%s\", base_url, journal_abbr";
  
  // Make the final "art or editorial + number" chunk in the first half optional
  // because a few AUs have issues with single articles which sit at the issue level
  // We'll do a content check in the createArticleFiles()
  // Finally figured out the 2nd half matching group using ?-i: to mandate case sensitivity  
  protected static final String PATTERN_TEMPLATE = "\"^%s%s/((([^0-9]+/)?(vol)?%d/(iss)?[0-9]+(/(art|editorial)?[0-9]+)?)|(vol%d/(?-i:[A-Z])[0-9]+))$\", base_url, journal_abbr, volume, volume";

  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
						      MetadataTarget target)
      throws PluginException {
    return new BePressArticleIterator(au,
                                      new SubTreeArticleIterator.Spec()
				          .setTarget(target)
				          .setRootTemplate(ROOT_TEMPLATE)
				          .setPatternTemplate(PATTERN_TEMPLATE),
				      false);
  }
  
  protected static class BePressArticleIterator extends SubTreeArticleIterator {
    
    protected Pattern pattern;
    protected Pattern TOC_pattern;
    
    public BePressArticleIterator(ArchivalUnit au,
                                  SubTreeArticleIterator.Spec spec,
                                  boolean isSection) {
      super(au, spec);
      String volumeAsString = au.getConfiguration().get(ConfigParamDescr.VOLUME_NUMBER.getKey());
      String journalAbbr = au.getConfiguration().get(ConfigParamDescr.JOURNAL_ABBR.getKey());
      if (isSection) {
        journalAbbr = journalAbbr + "/" + au.getConfiguration().get("journal_section");
      }
      // pick up issue level and lower (make (art)?[0-9]+ optional because a few au's have article at issue level
      this.pattern = Pattern.compile(String.format("/%s/((([^0-9]+/)?(vol)?%s/(iss)?[0-9]+(/(art)?[0-9]+)?)|(vol%s/(?-i:[A-Z])[0-9]+))$", journalAbbr, volumeAsString, volumeAsString), Pattern.CASE_INSENSITIVE);
      this.TOC_pattern = Pattern.compile(String.format("/%s/([^0-9]+/)?(vol)?%s/(iss)?[0-9]+$", journalAbbr, volumeAsString), Pattern.CASE_INSENSITIVE);
    }
    
    
    /*
     * This is comlicated. MOST AUs have articles that live below and issue level TOC
     * that is, 
     * <blah>/<journal_id>/vol#/iss#/ is a toc with no relevant metadata
     * <blah>/<journal_id>/vol#/iss#/xxx is an article with metadata
     * (eg Economist Voice V1)
     * BUT
     * in some AUs there are issues with only 1 article, in which case
     * <blah>/<journal_id>/vol#/iss#/ is an abstract with metadata
     * (eg Rhodes Cook V4)
     * and a few AUs with a mixture
     * (eg Forum for Health Economics V5)
     * So to identify ALL articles, we'll also have to capture issue level items and then look 
     * at the html and if it has article metadata in it, count it as an article. 
     * 
     */
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      Matcher mat = pattern.matcher(url);
      if (mat.find()) {
        // we matched, but could this pattern potentially be a toc?
        Matcher tocmat = TOC_pattern.matcher(url);
        // if we could be a TOC then we must have metadata to be considered an article
        if  (tocmat.find()) {
          if (hasArticleMetadata(cu)) {
            return processUrl(cu, mat);
          }
        } else { 
          // we're not a potential TOC, so treat this as an article without checking
          return processUrl(cu, mat);
        }
        return null; // this was a TOC, not an article
      }
      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }
    
    protected ArticleFiles processUrl(CachedUrl cu, Matcher mat) {
      ArticleFiles af = new ArticleFiles(); 
      af.setFullTextCu(cu);
      af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, cu);
      // XXX Full text PDF link embedded in page, cannot guess URL
      return af;
    }
    
    /*
     * hasArticleMetadata(CachedUrl cu)
     *   Given the CachedUrl for the potential abstract file, using the existing
     *   SimpleHtmlMetaTagMetadataExtractor to parse the file and 
     *   retrieve any contained metadata. If a doi or author exists, it's an article
     *   NOT defining the Metadata Extractor here!
     */
    private boolean hasArticleMetadata(CachedUrl cu) 
    {
      MetadataTarget at = new MetadataTarget(MetadataTarget.PURPOSE_ARTICLE);
      ArticleMetadata am;
      SimpleHtmlMetaTagMetadataExtractor ext = new SimpleHtmlMetaTagMetadataExtractor();
      if (cu !=null && cu.hasContent()) {
        try {
          at.setFormat("text/html");
          am = ext.extract(at, cu);
          if ( (am.containsRawKey("bepress_citation_journal_title")) || (am.containsRawKey("bepress_citation_abstract_html_url"))
              || (am.containsRawKey("bepress_citation_doi")) || (am.containsRawKey("bepress_citation_author")) ) {
            return true;
          }
        }catch (IOException e) {
          e.printStackTrace();
        }
      }
      return false; // no reasonable metadata, probably a toc
    }

  }
  
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(null);
  }
}
