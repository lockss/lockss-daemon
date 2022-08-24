/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.springer;

import java.util.Iterator;
import java.util.regex.*;

import org.apache.commons.lang3.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class SpringerSourceArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(SpringerSourceArticleIteratorFactory.class);
  
  /*
   * Modified on 5/15/2018 to handle THREE variants of deliveries/plugins:
   * 1. ClockssSpringerSourcePlugin (param of base_url & year)
   * 	<base_url>/<year>/ftp_xyz.zip!/XXX=/....
   * 2. ClockssSpringerDirSourcePlugin  (param of base_url & directory)
   * 	<base_url>/<directory>/ftp_xyz.zip!/XXX=/....
   * 3. ClockssSpringereDeliveredSourcePlugin (param of base_url & year & directory)
   *   just one additional level <virtual_dir> after year/dir and before zip
   * 	<base_url>/<year>/<virtual_dir>/JOU=xxxx.zip!/XXX=   We archived in to zip files
   */
  // find anyfiles that end in "BodyRef/PDF/XXX.pdf"
  // We do not know how many levels down because the book/journal might be at various levels
  // book series
  //BSE=0304/BOK=978-3-540-35043-9/CHP=10_10.1007BFb0103161/BodyRef/PDF/978-3-540-35043-9_Chapter_10.pdf
  //BSE=8913/BOK=978-94-6265-114-2/PRT=1/CHP=7_10.1007978-94-6265-114-2_7/BodyRef/PDF/978-94-6265-114-2_Chapter_7.pdf
  // book
  //BOK=978-981-10-0886-3/PRT=4/CHP=12_10.1007978-981-10-0886-3_12/BodyRef/PDF/978-981-10-0886-3_Chapter_12.pdf
  //BOK=978-981-10-0886-3/CHP=1_10.1007978-981-10-0886-3_1/BodyRef/PDF/978-981-10-0886-3_Chapter_1.pdf
  // journal
  //JOU=13678/VOL=2012.1/ISU=1/ART=12/BodyRef/PDF/13678_2012_Article_12.pdf
  
  // base_url + year or dir param (unspecified) + (OPTIONAL) HD dir THEN zip... 
  protected static final String PATTERN_TEMPLATE = "\"%s[^/]+/(HD[^/]+/)?[^/]+\\.zip!/[A-Z]+=.*/BodyRef/PDF/[^/]+\\.pdf$\",base_url";
  //any archive lower than top zip
  protected static final String NESTED_ARCHIVE_PATTERN_TEMPLATE = "\"%s[^/]+/(HD[^/]+/)?[^/]+\\.zip!/.+\\.(zip|tar|gz|tgz|tar\\.gz)$\",base_url";

 
 
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new SpringerArticleIterator(au, new SubTreeArticleIterator.Spec()
                                       .setTarget(target)
                                       .setVisitArchiveMembers(true)
                                       .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE)
                                       .setExcludeSubTreePatternTemplate(NESTED_ARCHIVE_PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));
  }
  
  protected static class SpringerArticleIterator extends SubTreeArticleIterator {
	
    // break it in to three parts in order to find the corresponding xml.Meta file
	//1. is  /<notslash>.zip!  to anchor top of content sequence
    protected static Pattern PATTERN = Pattern.compile("(/[^/]+\\.zip!/[A-Z]+=.*/)(BodyRef/PDF/)([^/]+)(\\.pdf)$", Pattern.CASE_INSENSITIVE);
    //ART=54253/40278_2018_54253_Article.xml
    protected static Pattern ODD_META_PATTERN = Pattern.compile("(/ART=[^/]+/[^_]+_[^_]+)_([^_]+)_Article\\.xml$", Pattern.CASE_INSENSITIVE);
    
    
    protected SpringerArticleIterator(ArchivalUnit au,
                                  SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      
      Matcher mat = PATTERN.matcher(url);
      if (mat.find()) {
        return processFullText(cu, mat);
      }
      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }

    protected ArticleFiles processFullText(CachedUrl cu, Matcher mat) {
      ArticleFiles af = new ArticleFiles();
      af.setFullTextCu(cu);
      log.debug3("setting role FullTextCu: " + cu.getUrl());
      af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, cu);
      log.debug3("setting ROLE_FULL_TEXT_PDF: " + cu.getUrl());
      
      log.debug3("target: " + spec.getTarget().getPurpose());
      if(spec.getTarget() != MetadataTarget.Article)
	guessAdditionalFiles(af, mat);
      
      return af;
    }
    
    
    /*
     * Springer has added in some variants.
     * USUALLY, the path looks like this:
     * PDF      ./JOU=40317/VOL=2018.6/ISU=1/ART=159/BodyRef/PDF/40317_2018_Article_159.pdf
     * XML      ./JOU=40317/VOL=2018.6/ISU=1/ART=159/40317_2018_Article_159.xml
     * XML.META ./JOU=40317/VOL=2018.6/ISU=1/ART=159/40317_2018_Article_159.xml.Meta
     * where the xml/xml.Meta are the same filename as that found under BodyRef/PDF
     * But now we're also seeing:
     * PDF      ./JOU=40278/VOL=2018.1727/ISU=1/ART=54253/BodyRef/PDF/40278_2018_54253_OnlinePDF.pdf
     * XML      ./JOU=40278/VOL=2018.1727/ISU=1/ART=54253/40278_2018_54253_Article.xml
     * XML.META ./JOU=40278/VOL=2018.1727/ISU=1/ART=54253/40278_2018_Article_54253.xml.Meta
     * where the XML replaces OnlinePDF with Article
     * and the XML.META swaps the Article and article number
     * yeesh.
     * 
     * $1 = all the path up to the "BodyRef/PDF/"
     * $3 = the filename after the "BodyRef/PDF/" and before ".pdf"
     */
    protected void guessAdditionalFiles(ArticleFiles af, Matcher mat) {
    	CachedUrl metadataCu;
    	CachedUrl xmlCu;

    	String pdfName = mat.group(3);
    	if(StringUtils.containsIgnoreCase(pdfName,"OnlinePDF")) {
    		//replaceIgnoreCase needs to wait for daemon > 1.74.2 to get necessary commons jar
    		//String newName = StringUtils.replaceIgnoreCase(pdfName, "OnlinePDF", "Article");
    		String newName = StringUtils.replace(pdfName, "OnlinePDF", "Article");
    		String xmlurl = mat.replaceFirst("$1") + newName + ".xml";
    		xmlCu = au.makeCachedUrl(xmlurl);
    		Matcher xmat = ODD_META_PATTERN.matcher(xmlurl);
    		if (xmat.find()) {
    			metadataCu = au.makeCachedUrl(xmat.replaceFirst("$1_Article_$2.xml.Meta"));        	  
    		} else {
    			// try the same as the XML name
    			metadataCu = au.makeCachedUrl(mat.replaceFirst(xmlurl + ".Meta"));
    		}
    	} else {
    		metadataCu = au.makeCachedUrl(mat.replaceFirst("$1$3.xml.Meta"));
    		xmlCu = au.makeCachedUrl(mat.replaceFirst("$1$3.xml"));
    	}
    	log.debug3("guessAdditionalFiles metadataCu: " + metadataCu + " and xmlCu: " + xmlCu);
    	boolean setMD = false;
    	if ((metadataCu != null) && (metadataCu.hasContent())) {
    		setMD = true;
    		af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, metadataCu);
    		log.debug3("setting ROLE_ARTICLE_METADATA: " + metadataCu.getUrl());
    	}
      
      if ((xmlCu != null) && (xmlCu.hasContent())) {
        // this will be our backup if the ".xml.Meta wasn't there
        if (!setMD) {
          af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, xmlCu);
          log.debug3("setting ROLE_ARTICLE_METADATA: " + xmlCu.getUrl());
        }
        // and make it your full-text xml
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, xmlCu);
        log.debug3("setting ROLE_FULL_TEXT_HTML: " + xmlCu.getUrl());
      }
    }
  }
  
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
	      throws PluginException {
    // the custom ArticleMetadataExtractor wasn't adding unique value over Base... 
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}
