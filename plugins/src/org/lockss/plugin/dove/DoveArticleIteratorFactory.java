/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.dove;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.lexer.InputStreamSource;
import org.htmlparser.lexer.Lexer;
import org.htmlparser.lexer.Page;
import org.htmlparser.lexer.Stream;
import org.htmlparser.tags.MetaTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.SubTreeArticleIterator;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

/*
 * PDF Full Text: https://www.dovepress.com/getfile.php?fileID=4199
 * HTML Abstract: https://www.dovepress.com/title-words-go-here-article-RTC
 * HTML f-t: https://www.dovepress.com/title-words-go-here-fulltext-article-RTC
 * <meta name="citation_pdf_url" content="https://www.dovepress.com/getfile.php?fileID=26034">
 *
 * Confirmed on 10/2021, the publisher will no longer publisher abstract, only full text article,
 * see Jira for more detail
 */

public class DoveArticleIteratorFactory implements ArticleIteratorFactory,
               					     ArticleMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(DoveArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE = "\"%s\", base_url";
  
  //Filter out all non abstract urls inside the iterator
  protected static final String PATTERN_TEMPLATE = "\"^%s[^/]+(-fulltext)?-article-[A-Z]+$\", base_url";
  // fulltext would also match the abstract pattern so manually check inside the method 
  // https://www.dovepress.com/the-articletitle-peer-reviewed-article-GICTT
  // https://www.dovepress.com/the-articletitle-peer-reviewed-fulltext-article-GICTT
  //protected static Pattern ABSTRACT_PATTERN = Pattern.compile("([^/]+)-article-([A-Z]+)$", Pattern.CASE_INSENSITIVE);
  protected static Pattern FULLTEXT_PATTERN = Pattern.compile("([^/]+)-fulltext-article-([A-Z]+)$", Pattern.CASE_INSENSITIVE);

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new DoveArticleIterator(au,
                                     new SubTreeArticleIterator.Spec()
                                                     	       .setTarget(target)
                                                     	       .setRootTemplate(ROOT_TEMPLATE)
                                                     	       .setPatternTemplate(PATTERN_TEMPLATE));
  }

  protected static class DoveArticleIterator extends SubTreeArticleIterator {
   
    public DoveArticleIterator(ArchivalUnit au,
                                 SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      Matcher mat = FULLTEXT_PATTERN.matcher(url);
      if (mat.find()) {
          return processArticle(cu, mat);
      } else {
        // we want to filter out fulltext version (we iterate on the abstract)
        // but if it failed for any reason, log a warning
        log.warning("Mismatch between article iterator factory and article iterator: " + url);
      }
      return null;
    }
    
    /* 
     * In order to find full text PDF you need to find the citation_pdf_url meta tag in the
     * abstract html pull out the pdf url normalize it and find the matching
     * cached URL
     */
    protected ArticleFiles processArticle(CachedUrl absCu, Matcher absMat) {
      NodeList nl = null;
      ArticleFiles af = new ArticleFiles();
      if (absCu != null && absCu.hasContent()) {
        // set absCU as default full text CU in case there is
        af.setFullTextCu(absCu);
	af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, absCu);
        // now guess at the fulltext version and if it's there add it to the ArticleFiles
        String fulltext = absMat.group(1) + "-fulltext-article-" + absMat.group(2);
        log.debug3("guessing at full-text html url: " + fulltext);
        CachedUrl ftCu = au.makeCachedUrl(fulltext);
        if (ftCu != null && ftCu.hasContent()) {
              // replace absCU with ftCU if exists and has content, will become pdf if available
              af.setFullTextCu(ftCu);
              af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, ftCu);
        }	
        // now find the full-text html from the meta tags on the abstract page
        try {
          InputStreamSource is = new InputStreamSource(new Stream(absCu.getUnfilteredInputStream()));
          Page pg = new Page(is);
          Lexer lx = new Lexer(pg);
          Parser parser = new Parser(lx);
          Lexer.STRICT_REMARKS = false;
      	  NodeFilter nf = new NodeFilter() {
	    public boolean accept(Node node) {
    	      if (!(node instanceof MetaTag)) return false;
    	      MetaTag meta = (MetaTag)node;
    	      if (!"citation_pdf_url".equalsIgnoreCase(meta.getMetaTagName())) return false;
    	      return true;
            }
          };
          nl = parser.extractAllNodesThatMatch(nf);
        } catch(ParserException e) {
          log.debug("Unable to parse abstract page html", e);
        } catch(UnsupportedEncodingException e) {
          log.debug("Bad encoding in abstact page html", e);
        } finally {
          absCu.release();
        }
      }
      try {
        if(nl != null) {
          if (nl.size() > 0) {
            // minimally encode URL to prevent URL constructor
            // from stripping trailing spaces
            String pdfUrlStr = ((MetaTag)nl.elementAt(0)).getMetaContent();
            URL pdfUrl = new URL(UrlUtil.minimallyEncodeUrl(pdfUrlStr));

            if(!pdfUrl.getHost().startsWith("www.")) {
              pdfUrl = new URL(pdfUrl.getProtocol(), 
          	               "www." + pdfUrl.getHost(),
          	               pdfUrl.getFile());
            }
            log.debug3("FOUND PDF URL: " + pdfUrl);
            // note: must leave URL encoded because that's how we store URLs
            CachedUrl pdfCu = au.makeCachedUrl(pdfUrl.toString());
            if (pdfCu != null && pdfCu.hasContent()) {
                  // replace absCU with pdfCU if exists and has content
        	  af.setFullTextCu(pdfCu);
        	  af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
            }
          }
        }
      } catch (MalformedURLException e) {
    	  log.debug("Badly formatted pdf url link", e);  
      } catch (IllegalArgumentException e) {
	    log.debug("Badly formatted pdf url link", e);
      }
      
      return af;
    }
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ABSTRACT);
  }
}
