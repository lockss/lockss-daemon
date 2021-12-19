/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.pensoft;

import java.io.IOException;
import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class PensoftArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log =
    Logger.getLogger(PensoftArticleIteratorFactory.class);

  //http://www.pensoft.net/
  protected static final String ROOT_TEMPLATE =
    "\"%s\", base_url";
  //[http://www.pensoft.net/journals/jhr/]article/1142/abstract/tiphiidae-wasps-of-madagascar-hymenoptera-tiphiidae-
  protected static final String PATTERN_TEMPLATE =
    "\"journals/%s/article/[\\d]+/abstract(/[^/]*)?$\", journal_name"; 
  protected static final String abstractRegExp = "journals/[\\w]+/article/[\\d]+/abstract(/[^/]*)?$";
  // from the metadata: http://www.pensoft.net/inc/journals/download.php?fileTable=J_GALLEYS&fileId=2758
  protected static final String metadataRegExp ="(http://www.pensoft.net/inc/journals/download.php\\?)(fileTable=J_GALLEYS)(\\&)(fileId=[\\d]+)$";

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
    
    protected static Pattern abstractPattern = Pattern.compile(abstractRegExp, Pattern.CASE_INSENSITIVE);
    protected static Pattern metadataPattern = Pattern.compile(metadataRegExp, Pattern.CASE_INSENSITIVE);
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

      mat = abstractPattern.matcher(url);
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
      CachedUrl htmlCu = null, pdfCu = null, xmlCu = null;
      Matcher mat;
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
            String metaPdf = am.getRaw("citation_pdf_url");
            log.debug3("   contains citation_pdf_url: "+metaPdf);
            CachedUrl rawCu = au.makeCachedUrl(metaPdf);
            
            // if the pdf_url from the metadata is in the au and has content, use it
            // otherwise, convert it to the "crawl" form
            if (rawCu != null && rawCu.hasContent()) {
              pdfCu = rawCu;
            } else {
               mat = metadataPattern.matcher(metaPdf);
              if (mat.matches()) {
                String crawlPdf = mat.replaceFirst("$1$4$3$2");
                log.debug3("crawlPDF = "+crawlPdf);
                pdfCu = au.makeCachedUrl(crawlPdf);
                AuUtil.safeRelease(rawCu);
              } else {
                log.warning("PDF content not found for: " + metaPdf);
              }
            }
     
            if (pdfCu != null && pdfCu.hasContent()) {
              log.debug3("  setROLE_PDF_URL: " + pdfCu);
              af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
              if((af.getFullTextCu()) == null) {
                af.setFullTextCu(pdfCu);
              }
              AuUtil.safeRelease(pdfCu);
            }
          } else {
            log.warning("NO citation_pdf_url for this cu: "+absCu);
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
          // get the xml url as a list:string from the metadata
          // as with the pdf url, keep entries in their raw state
          // the xml entry from the metadata not quite the same as on the page
          // will rearrange to match what we crawled...
          // from the metadata: http://www.pensoft.net/inc/journals/download.php?fileTable=J_GALLEYS&fileId=2758
          // from the    crawl: http://www.pensoft.net/inc/journals/download.php?fileId=2758&fileTable=J_GALLEYS
          // Accept either?

          if (am.containsRawKey("citation_xml_url")){
            String metaXml = am.getRaw("citation_xml_url");
            log.debug3("   contains citation_xml_url: "+metaXml);
            CachedUrl rawCu = au.makeCachedUrl(metaXml);

            // if the xml_url from the metadata is in the au and has content, use it
            // otherwise, convert it to the "crawl" form 
            if (rawCu != null && rawCu.hasContent()) {
              xmlCu = rawCu;
            } else {
              // can use the metadata pattern; xml is nearly identical
              mat = metadataPattern.matcher(metaXml);
              if (mat.matches()) {
                String crawlXml = mat.replaceFirst("$1$4$3$2");
                log.debug3("crawlXML = "+crawlXml);
                xmlCu = au.makeCachedUrl(crawlXml);
                AuUtil.safeRelease(rawCu);
              } else {
                log.warning("XML content not found for: " + metaXml);
              }
            }
            
            log.debug3("  setROLE_XML_URL: " + xmlCu);
            if (xmlCu != null) {
              af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_XML, xmlCu);
              if((af.getFullTextCu()) == null) {
                af.setFullTextCu(xmlCu);
              }
              AuUtil.safeRelease(xmlCu);
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
