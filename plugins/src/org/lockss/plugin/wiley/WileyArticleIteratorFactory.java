/*
 * $Id:
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.wiley;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/*
 * Iterates article files.  Archived source content zip files include files
 * with mime-type pdf and xml. Content has inconstitent file name formats.
 * 
 * Full-text:
 * http://clockss-ingest.lockss.org/sourcefiles/wiley-released
 *                      /2012/A/AAB102.1.zip!/j.1744-7348.1983.tb02660.x.pdf
 * http://clockss-ingest.lockss.org/sourcefiles/wiley-released
 *                      /2011/A/ADMA23.16!/1810_ftp.pdf
 * 
 * Cover image/ Abstract:
 * http://clockss-ingest.lockss.org/sourcefiles/wiley-released
 *                      /2011/A/1803_ftp.pdf
 * http://clockss-ingest.lockss.org/sourcefiles/wiley-released
 *                      /2011/A/1803_hdp.wml.xml
 */
public class WileyArticleIteratorFactory 
  implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = 
                          Logger.getLogger("WileyArticleIteratorFactory");
  
  // no need to set ROOT_TEMPLATE since all content is under <base_url>/<year>
  protected static final String PATTERN_TEMPLATE = 
      "\"%s%d/[A-Z0-9]/[^/]+\\.zip!/.*\\.pdf$\",base_url,year";
     
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    
    return new WileyArticleIterator(au, 
                                    new SubTreeArticleIterator.Spec()
                                        .setTarget(target)
                                        .setVisitArchiveMembers(true)
                                        .setPatternTemplate(PATTERN_TEMPLATE, 
                                                      Pattern.CASE_INSENSITIVE)
                                    );
  }
  
  protected static class WileyArticleIterator extends SubTreeArticleIterator {
	 
    protected final static Pattern PDF_PATTERN = 
      Pattern.compile("(/[^/]+\\.zip!/.*)(\\.pdf)$", Pattern.CASE_INSENSITIVE);
    
    protected final static Pattern FTP_PDF_PATTERN = 
      Pattern.compile("(.*)_ftp\\.pdf$", Pattern.CASE_INSENSITIVE);
    
    protected WileyArticleIterator(ArchivalUnit au,
                                   SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      Matcher matPdf = PDF_PATTERN.matcher(url);
      if (matPdf.find()) {
        return processFullText(cu, matPdf);
      }
      log.warning("Url does not match PDF_PATTERN: " + url);
      return null;
    }
    
    // if found pdfs with file name containing "*_ftp.pdf",
    // then make cached url for its corresponding abstract "*_hdp.wml.xml"
    private CachedUrl getHdpXmlCu(Matcher matFtpPdf) {
       String hdpXml = matFtpPdf.replaceFirst("$1_hdp.wml.xml");
       CachedUrl hdpXmlCu = au.makeCachedUrl(hdpXml);
       return (hdpXmlCu);
    }
    
    private void setRoleFullText(ArticleFiles af, CachedUrl cu) {
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, cu);
      af.setFullTextCu(cu);
    }

    // pdfs are a mix of full-text (most) and cover image and/or abstract
    // images. Abstract xml is recognized by its file name containing '_hdp'
    // coupling with its *_ftp.pdf file.
    protected ArticleFiles processFullText(CachedUrl cu, Matcher matPdf) {
      ArticleFiles af = new ArticleFiles();
      Matcher matFtpPdf = FTP_PDF_PATTERN.matcher(cu.getUrl());
      if (!matFtpPdf.find()) {
        setRoleFullText(af, cu);
      } else {
        CachedUrl hdpXmlCu = getHdpXmlCu(matFtpPdf);
        // if hdp xml exists, then the pdf is a cover image
        // and the hdp xml is an abstract and contains not <body> tag
        if (hdpXmlCu.hasContent()) {
          af.setRoleCu("Cover image", cu); // cover image pdf
          af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, hdpXmlCu);
          af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, hdpXmlCu);
        } else {
          setRoleFullText(af, cu);
        }
      } 
      if (spec.getTarget() != MetadataTarget.Article()) {
        guessAdditionalFiles(af, matPdf);
      }
      return af;
    }
    
    // metadata found in all xmls (full-text and abstract)
    protected void guessAdditionalFiles(ArticleFiles af, Matcher matPdf) {
      CachedUrl xmlCu = au.makeCachedUrl(matPdf.replaceFirst("$1.wml.xml"));
      if (xmlCu.hasContent()) {
        af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, xmlCu);
      }
    }
    
  }
  
  public ArticleMetadataExtractor createArticleMetadataExtractor(
                                                         MetadataTarget target)
        throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}
