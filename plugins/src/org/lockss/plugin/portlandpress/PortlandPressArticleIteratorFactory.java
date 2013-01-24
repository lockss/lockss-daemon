/*
 * $Id: PortlandPressArticleIteratorFactory.java,v 1.1 2013-01-24 22:40:18 alexandraohlson Exp $
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

package org.lockss.plugin.portlandpress;

import java.util.Iterator;
import java.util.regex.*;

import org.apache.commons.lang.StringUtils;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/*
 * Article lives at three locations:  
 * Abstract: <baseurl>/<jid>/<volnum>/
 *           <lettersnums>.htm
 *           <lettersnums>add.htm - supplementary stuff
 *           <lettersnums>add.mov  - ex - quicktime movie
 *           <lettersnums>add.pdf - ex - online data
 * PDF & legacy html: <base-url>/<jid>/<volnum>/startpagenum/ 
 *           <lettersnums>.htm
 *           <lettersnums>.pdf  (note that pdf filename does not start with jid)
 * Enhanced full text version: <base-url>/<jid>/ev/<volnum>/<stpage>
 *           <lettersnums_ev.htm>
 * notes startpgaenum can have letters in it
 * lettersnums seems to be catenated <jid><volnum><startpagenum> except for pdf which is <volnjum><startpagenum>
 */

public class PortlandPressArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger("PortlandPressArticleIteratorFactory");
  
 protected static final String ROOT_TEMPLATE = "\"%s%s/%s/\", base_url, journal_id, volume_name";  
//  protected static final String ROOT_TEMPLATE = "\"%s\", base_url";  
  // pick up the abstract as the logical definition of one article - lives one level higher than pdf & fulltext
  // pick up <lettersnums>.htm, but not <lettersnums>add.htm
  protected static final String PATTERN_TEMPLATE = "\"^%s%s/%s/(?![^/]+add\\.htm)%s[^/]+\\.htm\", base_url,journal_id, volume_name, journal_id";
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
        return new PortlandPressArticleIterator(au,
                                         new SubTreeArticleIterator.Spec()
                                             .setTarget(target)
                                             .setRootTemplate(ROOT_TEMPLATE)
                                             .setPatternTemplate(PATTERN_TEMPLATE));
  }

  protected static class PortlandPressArticleIterator extends SubTreeArticleIterator {
    
    // Identify groups in the pattern "/(<jid>)/(<volnum>)/(<articlenum>).htm
    // articlenum is <jid><volnum><pageno> and we need pageno to find the content files
    protected Pattern ABSTRACT_PATTERN = Pattern.compile("/([^/]+)/([^/]+)/([^/]+)\\.htm$", Pattern.CASE_INSENSITIVE);

    public PortlandPressArticleIterator(ArchivalUnit au,
                                     SubTreeArticleIterator.Spec spec) {
      super(au, spec);
      }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      log.info("article url?: " + url);
      
      Matcher mat = ABSTRACT_PATTERN.matcher(url);
      if (mat.find()) {
        return processAbstract(cu, mat);
      }
      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }
    
    protected ArticleFiles processAbstract(CachedUrl cu, Matcher mat) {
      ArticleFiles af = new ArticleFiles();

      /* the abstract is NOT a full text cu so you're going to have to look for 
       * one even ifyou're only counting articles
       */
      af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, cu);
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE, cu);
      af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, cu);
      guessAdditionalFiles(af, mat);
      return af;
    }
 
    protected void guessAdditionalFiles(ArticleFiles af, Matcher mat) {    
      /*
       * If the standard html file exists, set that as the fullTextCu
       * If not, look for a PDF first and then an extended html
       * Once you have the full text CU, only proceed if you aren't just counting articles
       */
      String jidString = mat.group(1);
      String volString = mat.group(2);
      String fileString = mat.group(3);
      String newURL = null;
      boolean articleTarget = false;
      
      /* minimize the work you do if you are just counting articles */
      if ( (spec.getTarget() != null) && (spec.getTarget().isArticle())) {
        articleTarget = true;
      }
      
      // fileString should be <jid><vol><pageno> and we need to pull out <pageno>
      if ( fileString.contains(volString)) {
        String pageString = StringUtils.substringAfter(fileString,volString);
        String pdfFilename = StringUtils.substringAfter(fileString, jidString);

        /* legacy html lives in a <pageno> subdirectory */
        newURL = mat.replaceFirst("/$1/$2/" + pageString + "/$3.htm");
        CachedUrl htmlCu = au.makeCachedUrl(newURL);
        if (htmlCu != null && htmlCu.hasContent()) {
          af.setFullTextCu(htmlCu);
          af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, htmlCu);
          
        }
        AuUtil.safeRelease(htmlCu);

        if ((af.getFullTextCu() == null) || !articleTarget){
          // still need an article or doing more AF identification for metadata extraction 
          /* pdf file lives in a <pageno> subdirectory and the filename is just <vol><pageno>.pdf with no <jid>*/
          newURL = mat.replaceFirst("/$1/$2/" + pageString + "/" + pdfFilename + ".pdf");
          CachedUrl pdfCu = au.makeCachedUrl(newURL);
          if (pdfCu != null && pdfCu.hasContent()) {
            if (af.getFullTextCu() == null) {
              af.setFullTextCu(pdfCu);
            }
            af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
          }
          AuUtil.safeRelease(pdfCu);
          
        }

        if ((af.getFullTextCu() == null) || !articleTarget){
          /* extended html lives in ../ev/<volnum>/<pageno> directory */
          newURL = mat.replaceFirst("/$1/ev/$2/" + pageString + "/$3_ev.htm");
          htmlCu = au.makeCachedUrl(newURL);
          if (htmlCu != null && htmlCu.hasContent()) {
            if (af.getFullTextCu() == null) {
              af.setFullTextCu(htmlCu);
            }
            if (af.getRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML) == null) {
              af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, htmlCu);
            }
          }
          AuUtil.safeRelease(htmlCu);
        }
    
        if (!articleTarget){
          /* you'll only pick up this stuff if you are doing metadata */
          /* supplementary files landing page, lives in the same place as the abstract */
          newURL = mat.replaceFirst("/$1/$2/$3add.htm");
          CachedUrl supCu = au.makeCachedUrl(newURL);
          if (supCu != null && supCu.hasContent()) {
            af.setRoleCu(ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS, supCu);
          }
          AuUtil.safeRelease(supCu);
        }

      } /* if the filename doesn't contain the volnum then the scheme is different than expected */
    }
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
