/*
 * $Id: 
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pensoft;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.pensoft.PensoftHtmlMetadataExtractorFactory.PensoftHtmlMetadataExtractor;
import org.lockss.util.Logger;

public class PensoftArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log =
    Logger.getLogger("PensoftArticleIteratorFactory");

  //http://www.pensoft.net/
  protected static final String ROOT_TEMPLATE =
    "\"%s\", base_url";
  //[http://www.pensoft.net/journals/jhr/]article/1142/abstract/tiphiidae-wasps-of-madagascar-hymenoptera-tiphiidae-
  protected static final String PATTERN_TEMPLATE =
    "\"journals/%s/article/[\\d]+/abstract(/[^/]*)?$\", journal_name";
  //http://www.pensoft.net/inc/journals/download.php?fileTable=J_GALLEYS&fileId=2777
  protected static final String PDF_TEMPLATE =
    "(http://www.pensoft.net/inc/journals/download.php?)(fileTable=J_GALLEYS)(&)(fileId=[\\d]+)$";

  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
                                                      throws PluginException {
    return new
      PensoftArticleIterator(au,
              new SubTreeArticleIterator.Spec()
              .setTarget(target)
              .setRootTemplate(ROOT_TEMPLATE)
              .setPatternTemplate(PATTERN_TEMPLATE));
  }

  protected static class PensoftArticleIterator
    extends SubTreeArticleIterator {
    // pdf examples:
    //"^%sinc/journals/download.php\?fileId=[\d]+&amp;fileTable=J_GALLEYS", base_url</string>
    //"http://www.pensoft.net/J_FILES/10/articles/1100/1100-G-3-layout.pdf"
    //abstract: 
    //http://www.pensoft.net/journals/jhr/article/1142/abstract/tiphiidae-wasps-of-madagascar-hymenoptera-tiphiidae-
    protected static Pattern ABSTRACT_PATTERN =
      Pattern.compile("journals/[\\w]+/article/[\\d]+/abstract(/[^/]*)?$", Pattern.CASE_INSENSITIVE);
    protected static Pattern pdfPattern = Pattern.compile(PDF_TEMPLATE, Pattern.CASE_INSENSITIVE);

    private ArchivalUnit au;
    
    public PensoftArticleIterator(ArchivalUnit au,
                                  SubTreeArticleIterator.Spec spec) {
      super(au, spec);
      this.au = au;
    }
    
    /*
     * createArticleFiles(CachedUrl cu)
     *   Rather than matching a PDF file (as is common), matching the abstract
     *   to use its metadata to identify the associated PDF and HTML --
     *   Pensoft does not have a manifest page to identify these files
     *   (and the article files do not have an easy-to-guess form)
     * (non-Javadoc)
     * @see org.lockss.plugin.SubTreeArticleIterator#createArticleFiles(org.lockss.plugin.CachedUrl)
     */
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      Matcher mat;
      log.debug3("createArticleFiles: " + url);

      mat = ABSTRACT_PATTERN.matcher(url);
      if (mat.find()) {     
        return processWithMetadata(cu,mat);
      }
      
      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }
    
    /*
     * processWithMetadata(CachedUrl absCu, Matcher absMat)
     *   Given the CachedUrl for the abstract file, using the existing
     *   SimpleHtmlMetaTagMetadataExtractor to parse the abstract and 
     *   retrieve the associated fulltext PDF file and fulltext html file.  
     *   NOT defining the Metadata Extractor here!
     */
    protected ArticleFiles processWithMetadata(CachedUrl absCu, Matcher absMat) {
      ArticleFiles af = new ArticleFiles();
      MetadataTarget at = new MetadataTarget(MetadataTarget.PURPOSE_ARTICLE);
      ArticleMetadata am;
      SimpleHtmlMetaTagMetadataExtractor ext = new SimpleHtmlMetaTagMetadataExtractor();
      CachedUrl htmlCu = null, pdfCu = null;
      log.debug3("processWithMetadata: " + absCu);

      if (absCu !=null && absCu.hasContent()){
        // 
        log.debug3("  setROLE_ART_METADATA: " + absCu);
        af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, absCu);
        try {
          at.setFormat("text/html");
          am = ext.extract(at, absCu);
          
          // get the pdf url as a list:string from the metadata
          // keep entries in their raw state
          // the pdf entry from the metadata not quite the same as on the page
          // will rearrange to match what we crawled...
          // from the metadata: http://www.pensoft.net/inc/journals/download.php?fileTable=J_GALLEYS&fileId=2758
          // from the    crawl: http://www.pensoft.net/inc/journals/download.php?fileId=2758&fileTable=J_GALLEYS
          // Accept either?
          
          if (am.containsRawKey("citation_pdf_url")){
            //Pattern p = Pattern.compile("(http://www.pensoft.net/inc/journals/download.php\\?)(fileTable=J_GALLEYS)(\\&)(fileId=[\\d]+)$", Pattern.CASE_INSENSITIVE);
            String rawPdf = am.getRaw("citation_pdf_url");
            log.debug3("   contains citation_pdf_url: "+rawPdf);
            Matcher mat; // = pdfPattern.matcher(rawPdf);
            String newPdf; // = mat.replaceFirst("$1$4$3$2");
            CachedUrl rawCu = au.makeCachedUrl(rawPdf);
            
            if (rawCu != null && rawCu.hasContent()) {
              pdfCu = rawCu;
            } else {
              mat = pdfPattern.matcher(rawPdf);
              newPdf = mat.replaceFirst("$1$4$3$2");
              pdfCu = au.makeCachedUrl(newPdf);
              AuUtil.safeRelease(rawCu);
            }
     
            if (pdfCu != null && pdfCu.hasContent()) {
              log.debug3("  setROLE_PDF_URL: " + pdfCu);
              af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
              if((af.getFullTextCu()) == null) {
                af.setFullTextCu(pdfCu);
              }
              AuUtil.safeRelease(pdfCu);
            }
          }
          
          // get the fulltexthtml url as a list:string from the metadata
          if (am.containsRawKey("citation_fulltext_html_url")){
            htmlCu = au.makeCachedUrl(am.getRaw("citation_fulltext_html_url"));
            if (htmlCu != null && htmlCu.hasContent()) {
              log.debug3("  setROLE_HTML_URL: " + htmlCu);
              af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, htmlCu);
              if((af.getFullTextCu()) == null) {
                af.setFullTextCu(htmlCu);
              }
              AuUtil.safeRelease(htmlCu);
            }
          } 
        } catch (IOException e) {
          // 
          e.printStackTrace();
        }
      }
      return af;
    }
   
  }
  
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
