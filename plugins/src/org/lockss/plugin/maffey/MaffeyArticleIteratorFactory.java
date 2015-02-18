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

package org.lockss.plugin.maffey;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.*;

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
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

/*
 * PDF Full Text: http://www.la-press.com/redirect_file.php?fileType=pdf&fileId=4199&filename=3103-ACI-Holistic-Control-of-Herbal-Teas-and-Tinctures-Based-on-Sage-(Salvia-of.pdf&nocount=1
 * HTML Abstract: http://www.la-press.com/holistic-control-of-herbal-teas-and-tinctures-based-on-sage-salvia-off-article-a3103
 * Meta tag that is in the abstact html: <meta content="http://la-press.com/redirect_file.php?fileId=961&filename=ACI-1-Zayas(Pr)&fileType=pdf" name="citation_pdf_url">
 */

public class MaffeyArticleIteratorFactory implements ArticleIteratorFactory,
               					     ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger("MaffeyArticleIteratorFactory");
  
  protected static final String ROOT_TEMPLATE = "\"%s\", base_url";
  
  //Filter out all non abstract urls
  protected static final String PATTERN_TEMPLATE = "\"^%s[^/]+-article-a[0-9]+$\", base_url";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new MaffeyArticleIterator(au,
                                     new SubTreeArticleIterator.Spec()
                                                     	       .setTarget(target)
                                                     	       .setRootTemplate(ROOT_TEMPLATE)
                                                     	       .setPatternTemplate(PATTERN_TEMPLATE));
  }

  protected static class MaffeyArticleIterator extends SubTreeArticleIterator {

    protected Pattern ABSTRACT_PATTERN = Pattern.compile("([^/]+)-article-a[0-9]+$", Pattern.CASE_INSENSITIVE);
   
    public MaffeyArticleIterator(ArchivalUnit au,
                                 SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      Matcher mat = ABSTRACT_PATTERN.matcher(url);
      if (mat.find()) {
	return processAbstract(cu, mat);
      }

      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }
    
    /* 
     * In order to find full text PDF you need to find the citation_pdf_url meta tag in the
     * abstract html pull out the pdf url normalize it (reorder params...) and find the matching
     * cached URL
     */
    protected ArticleFiles processAbstract(CachedUrl absCu, Matcher absMat) {
      NodeList nl = null;
      ArticleFiles af = new ArticleFiles();
      if (absCu != null && absCu.hasContent()) {
        // TEMPORARY: set absCU as default full text CU in case there is
        // no PDF CU with content; the current metadata manager currently 
        // uses only the full text CU, but this will change with the new
        // metadata schema that can have multiple CUs for an article.
        af.setFullTextCu(absCu);
	af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, absCu);
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
            List<String> paramList = new ArrayList<String>();
            paramList.add("fileType");
            paramList.add("fileId");
            paramList.add("fileName");
            pdfUrl = reArrangeUrlParams(pdfUrl, paramList);
            
            if(!pdfUrl.getHost().startsWith("www.")) {
              pdfUrl = new URL(pdfUrl.getProtocol(), 
          	               "www." + pdfUrl.getHost(),
          	               pdfUrl.getFile());
            }
            
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
    
    private URL reArrangeUrlParams(URL url, List<String> paramList) throws MalformedURLException {
      return reArrangeUrlParams(url, paramList, "&");
    }
    // Pulls parameters in paramList and arranges them in the same order as they
    // are in paramList.
    private URL reArrangeUrlParams(URL url, List<String> paramList, String paramDelim) throws MalformedURLException {
      String [] urlTokens = url.getQuery().split(paramDelim);
      String newQuery = "";
      if(urlTokens.length > 1) {
  	for(String param : paramList) {
  	  for(int i = 0; i < urlTokens.length; i++){
  	    if(urlTokens[i].toLowerCase().startsWith(param.toLowerCase())) {
  	      if(newQuery!="") newQuery = newQuery + "&";
  	      newQuery = newQuery + urlTokens[i] ;
  	      break;
  	    }
  	  }
  	}
      }
      return new URL(url.getProtocol(), url.getHost(), url.getPath() + "?" + newQuery);
    }
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ABSTRACT);
  }
}
