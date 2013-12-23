/*
 * $Id: TaylorAndFrancisArticleIteratorFactory.java,v 1.7 2013-12-23 18:30:44 alexandraohlson Exp $
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.taylorandfrancis;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.SubTreeArticleIterator;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

/*
 * Taylor & Francis has a custom ArticleIterator (instead of using BaseAtypon's)
 * because it has to handle DOI suffixes that have "/"s in them (see jid = wsr20)
 * and the simple pattern replacement used by the builder cannot encode the 
 * "/"s. Once the Builder can handle programmatic replacement, we should go 
 * back to using the BaseAtypon implmementation
 */
public class TaylorAndFrancisArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {
        
  
  protected static Logger log = Logger.getLogger(TaylorAndFrancisArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE = "\"%sdoi/\", base_url";
  protected static final String PATTERN_TEMPLATE = 
      "\"^%sdoi/(abs|full|pdf|pdfplus)/[.0-9]+/\", base_url";
  
  //
  // On an Atypon publisher, article content may look like this but you do not know
  // how many of the aspects will exist for a particular journal
  //
  //  <atyponbase>.org/doi/abs/10.3366/drs.2011.0010 (abstract or summary)
  //  <atyponbase>.org/doi/full/10.3366/drs.2011.0010 (full text html)
  //  <atyponbase>.org/doi/pdf/10.3366/drs.2011.0010 (full text pdf)
  //  <atyponbase>.org/doi/pdfplus/10.3366/drs.2011.0010  (fancy pdf - could be in frameset or could have active links)
  //  <atyponbase>.org/doi/suppl/10.3366/drs.2011.0010 (page from which you can access supplementary info)
  //  <atyponbase>.org/doi/ref/10.3366/drs.2011.0010  (page with references on it)
  //
  // note: at least one publisher has a doi suffix that includes a "/", eg:
  // <base>/doi/pdfplus/10.1093/wsr/wsr0023
  //
  //  There is the possibility of downloaded citation information which will get normalized to look something like this:
  //  <atyponbase>.org/action/downloadCitation?doi=<partone>%2F<parttwo>&format=ris&include=cit
  //
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new TaylorAndFrancisArticleIterator(au,
                                         new SubTreeArticleIterator.Spec()
                                             .setTarget(target)
                                             .setRootTemplate(ROOT_TEMPLATE)
                                             .setPatternTemplate(PATTERN_TEMPLATE));
  }

  protected static class TaylorAndFrancisArticleIterator extends SubTreeArticleIterator {
    
    // various aspects of an article
    // DOI's can have "/"s in the suffix
    final Pattern DOI_PATTERN = Pattern.compile("/doi/(abs|pdf|pdfplus|full)/([.0-9]+)/([^?^&]+)$", Pattern.CASE_INSENSITIVE);
    final Pattern FULLTEXT_PATTERN = Pattern.compile("/doi/(pdf|pdfplus|full)/([.0-9]+)/([^?^&]+)$", Pattern.CASE_INSENSITIVE);

    // how to change from one form (aspect) of article to another
    // when replacing off DOI_PATTERN 9which has additional catch group)
    final String HTML_REPLACEMENT = "/doi/full/$2/$3";
    final String ABSTRACT_REPLACEMENT = "/doi/abs/$2/$3";
    final String PDF_REPLACEMENT = "/doi/pdf/$2/$3";
    final String PDFPLUS_REPLACEMENT = "/doi/pdfplus/$2/$3";
    
    
    // Things not an "article" but in support of an article
    final String REFERENCES_REPLACEMENT = "/doi/ref/$1/$2";
    final String SUPPL_REPLACEMENT = "/doi/suppl/$1/$2";
    // link extractor used forms to pick up this URL
   
    // After normalization, the citation information will live at this URL if it exists
    final String RIS_REPLACEMENT_THRU_PREFIX = "/action/downloadCitation?doi=$2%2F";
    final String RIS_REPLACEMENT_AFTER_SUFFIX = "&format=ris&include=cit";


    public TaylorAndFrancisArticleIterator(ArchivalUnit au,
                                     SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      
      
      String url = cu.getUrl();
      log.debug3("URL: " + url);
      
      // Taylor&Francis has some specific URLs we want to ignore
      //"doi/full/10.1137/null?sequence=...
      if (url.contains("/null?")) { 
        log.debug3("ignoring this URL");
        return null; // ignore these URLs
      }
      
      /* If we're here we know we've matched <base_url>doi/(abs|full|pdf|pdfplus)/[.0-9]+/ */
      Matcher doiMat = DOI_PATTERN.matcher(url);
      if (!doiMat.find()) {
        log.debug3("mismatch between ArticleIterator factory and ArticleIterator");
        return null;
      }

      /* only create the AF and proceed if we are the highest order "aspect" */
      if (!higherAspectExists(url, doiMat)) {
        log.debug3("We are the highest aspect - makeing AF");
        ArticleFiles af = new ArticleFiles();
        Matcher mat = FULLTEXT_PATTERN.matcher(url); // just not if abstract
        if (mat.find()) {
          af.setFullTextCu(cu);
        }
        
        /* figure out metadata options - go for ris citation data first*/
        CachedUrl absCu = au.makeCachedUrl(doiMat.replaceFirst(ABSTRACT_REPLACEMENT));
        CachedUrl htmlCu = au.makeCachedUrl(doiMat.replaceFirst(HTML_REPLACEMENT));
        String doi_suffix = StringUtil.replaceString(doiMat.group(3), "/", "%2F");; 
        CachedUrl risCu = au.makeCachedUrl(doiMat.replaceFirst(RIS_REPLACEMENT_THRU_PREFIX + doi_suffix + RIS_REPLACEMENT_AFTER_SUFFIX));
        log.debug3("risCU is: " + (doiMat.replaceFirst(RIS_REPLACEMENT_THRU_PREFIX + doi_suffix + RIS_REPLACEMENT_AFTER_SUFFIX)));
        if (risCu != null && risCu.hasContent()) {
          af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, risCu);
        } else if (absCu != null && absCu.hasContent()) { 
          af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, absCu);
        } else if (htmlCu != null && htmlCu.hasContent()) { 
          af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, htmlCu);
        } 
        AuUtil.safeRelease(absCu);
        AuUtil.safeRelease(htmlCu);
        AuUtil.safeRelease(risCu);
        return af;
      }
      log.debug3("We are not the highest aspect - deferring");
      return null; //There is a higher aspect that will trigger ArticleFiles
    }
    
    // Used to figure out what type of URL we actually are
    final Pattern HTML_PATTERN = Pattern.compile("/doi/full/([.0-9]+)/([^?^&]+)$", Pattern.CASE_INSENSITIVE);
    final Pattern PDFPLUS_PATTERN = Pattern.compile("/doi/pdfplus/([.0-9]+)/([^?^&]+)$", Pattern.CASE_INSENSITIVE);
    final Pattern PDF_PATTERN = Pattern.compile("/doi/pdf/([.0-9]+)/([^?^&]+)$", Pattern.CASE_INSENSITIVE);
    /*
     *  The pattern trapped a possible trigger for an ArticleFiles but if
     *  a better match for exists, defer to that one. In this case the order is:
     *  PDF, PDFPLUS, FULL HTML, ABSTRACT (the last for abstract only case)
     */
    protected boolean higherAspectExists(String url, Matcher doiMat) {

     
      // Are we the PDF
      Matcher mat = PDF_PATTERN.matcher(url);
      if (mat.find()) {
        return false; //we are the highest aspect
      }
      // Does the PDF exist?
      CachedUrl pdfCu = au.makeCachedUrl(doiMat.replaceFirst(PDF_REPLACEMENT));
      if (pdfCu != null && pdfCu.hasContent()) {
        AuUtil.safeRelease(pdfCu);
        return true; //pdf exists, so defer to that
      }
      // Are we the PDFPLUS
      mat = PDFPLUS_PATTERN.matcher(url);
      if (mat.find()) {
        return false; // we are the highest option left
      }
      // Does the PDFPLUS exist?
      CachedUrl pdfplusCu = au.makeCachedUrl(doiMat.replaceFirst(PDFPLUS_REPLACEMENT));
      if (pdfplusCu != null && pdfplusCu.hasContent()) {
        AuUtil.safeRelease(pdfplusCu);
        return true; //pdfplus exists, so defer to that
      }
      // Are we the full HTML
      mat = HTML_PATTERN.matcher(url);
      if (mat.find()) {
        return false; // we are the highest option left
      }
      // Does the full HTML exist? 
      CachedUrl htmlCu = au.makeCachedUrl(doiMat.replaceFirst(HTML_REPLACEMENT));
      if (htmlCu != null && htmlCu.hasContent()) {
        AuUtil.safeRelease(htmlCu);
        return true; //html exists, so defer to that
      }
      // We must be the ABSTRACT
      return false; 
    }
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
          return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}


/*
 *  The following class definition should be reinstated after the 
 *  SubTreeArticleIteratorBuilder can support smarter pattern replacement and
 *  T&F can go back to using the BaseAtypon implementation.
 *  Until then, T&F must write a custom article iterator in order to 
 *  encode any "/"s in the DOI suffix
 */
/*
public class TaylorAndFrancisArticleIteratorFactory
    extends BaseAtyponArticleIteratorFactory {

  protected static Logger log = Logger.getLogger("TaylorAndFrancisArticleIteratorFactory");

  // Override creation of builder to allow override of underlying createArticleFiles
  // to solve a T&F bug that was generating bogus URLS that looked like article files...
  @Override
  protected SubTreeArticleIteratorBuilder localBuilderCreator(ArchivalUnit au) { 
    return new SubTreeArticleIteratorBuilder(au) {
      @Override
      protected void maybeMakeSubTreeArticleIterator() {
        if (au != null && spec != null && iterator == null) { /// FIXME 1.63
          this.iterator = new BuildableSubTreeArticleIterator(au, spec) {
            @Override
            protected ArticleFiles createArticleFiles(CachedUrl cu) {
              // modify the returned Builder's createArticleFiles to ignore specific URLs
              if (cu.getUrl().contains("/null?")) { // 
                return null; // ignore these URLs
              }
              return super.createArticleFiles(cu); // normal processing
            }
          };
        }
      }
    };
  }

}
*/
