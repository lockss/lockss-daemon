/*
 * $Id$
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

package org.lockss.plugin.bmc;

import java.io.IOException;
import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.ListUtil;
import org.lockss.util.Logger;

public class BioMedCentralPluginArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger("BioMedCentralPluginArticleIteratorFactory");
  // example pdf
  //http://www.jcheminf.com/content/pdf/1758-2946-2-7.pdf
  //http://breast-cancer-research.com/content/pdf/bcr3224.pdf
  //http://genomebiology.com/content/pdf/gb-2002-3-7-research0032.pdf
  protected static final String PDF_ROOT_TEMPLATE = "\"%scontent/pdf\", base_url";
  // example full text html
  //http://www.jcheminf.com/content/2/1/7
  //http://breast-cancer-research.com/content/14/4/R104
  //http://genomebiology.com/2002/3/7/research/0032
  protected static final String ABS_PATTERN_TEMPLATE = "\"^%s(\\d{4}/)?(content/)?%s/.+abstract$\", base_url, volume_name";
  // example abstract html
  //http://www.jcheminf.com/content/2/1/7/abstract
  //http://breast-cancer-research.com/content/14/4/R104/abstract
  //http://genomebiology.com/2002/3/7/research/0032/abstract
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new BioMedCentralPluginArticleIterator(au, new SubTreeArticleIterator.Spec()
                                              .setTarget(target)
                                              .setPatternTemplate(ABS_PATTERN_TEMPLATE,Pattern.CASE_INSENSITIVE));
  }
  protected static class BioMedCentralPluginArticleIterator extends SubTreeArticleIterator {


    protected static Pattern HTML_PATTERN = Pattern.compile("/content/([^/]+)/(\\d)/([^/]+)$", Pattern.CASE_INSENSITIVE);
    protected static Pattern ABSTRACT_PATTERN = Pattern.compile("/((content/\\d+/)?(\\d{4}/\\d+/)?.+)/abstract$", Pattern.CASE_INSENSITIVE);   
    protected static Pattern PDF_PATTERN = Pattern.compile("/content/pdf/([^/]+)\\.pdf$", Pattern.CASE_INSENSITIVE);

    public BioMedCentralPluginArticleIterator(ArchivalUnit au,
                                  SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      log.debug3("Entry point: " + url);
      Matcher mat = ABSTRACT_PATTERN.matcher(url);
      if (mat.find()) {
       return processWithMetadata(cu, mat);
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

      if (absCu !=null && absCu.hasContent()){
        // 
        af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, absCu);
        try {
          at.setFormat("text/html");
          am = ext.extract(at, absCu);
          
          // get the pdf url as a list:string from the metadata
          // keep entries in their raw state
          
          if (am.containsRawKey("citation_pdf_url")){
            pdfCu = au.makeCachedUrl(am.getRaw("citation_pdf_url"));
            if (pdfCu != null && pdfCu.hasContent()) {
              af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
              af.setFullTextCu(pdfCu);
              log.debug3("found pdf in metadata: " + pdfCu);
            }
          }
          
          // get the fulltexthtml url as a list:string from the metadata
          if (am.containsRawKey("citation_fulltext_html_url")){
              htmlCu = au.makeCachedUrl(am.getRaw("citation_fulltext_html_url"));
              if (htmlCu != null && htmlCu.hasContent()) {
                af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, htmlCu);
                log.debug3("found html in metadata: " + htmlCu);
                if (af.getFullTextCu() == null)
                  af.setFullTextCu(htmlCu);
              }
          } 
          
          // if no full text cu found, go to plan B...
          if (af.getFullTextCu() == null) {
            log.debug3("no full text CU found yet");
            guessHtml(af, absMat);
          }     
                    
        } catch (IOException e) {
          // 
          e.printStackTrace();
        }
      }
      return af;
    }

    protected ArticleFiles processFullTextHtml(CachedUrl htmlCu, Matcher htmlMat) {
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(htmlCu);
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, htmlCu);
      if (spec.getTarget() != MetadataTarget.Article) {
       guessAbstract(af, htmlMat);
      }
      return af;
    }
    
    /** Guess the full text Html associated with the given abstract
     * @param af  the ArticleFile 
     * @param mat the Matcher from the abstract found
     * 
     * this will be called if we could not find a pdf to match the abstract,
     * so, we'll guess the html and set it to be the FullTextCu
     * Luckily, the html is easy to manufacture if we have the abstract
     */
    protected void guessHtml(ArticleFiles af, Matcher mat) {
      CachedUrl htmlCu = au.makeCachedUrl(mat.replaceFirst("/$1"));
      log.debug3("guessing html: "+htmlCu);
      if (htmlCu != null && htmlCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, htmlCu);
        af.setFullTextCu(htmlCu);
        AuUtil.safeRelease(htmlCu);
      }
    }
    
    protected void guessAbstract(ArticleFiles af, Matcher mat) {
      CachedUrl absCu = au.makeCachedUrl(mat.replaceFirst("/$1/$2/$3/abstract"));
      if (absCu != null && absCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, absCu);
        AuUtil.safeRelease(absCu);
      }
    }
 }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
  
}
