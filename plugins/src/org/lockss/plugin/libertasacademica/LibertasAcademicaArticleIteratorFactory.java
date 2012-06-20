/*
 * $Id: LibertasAcademicaArticleIteratorFactory.java,v 1.1.2.2 2012-06-20 00:03:08 nchondros Exp $
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

package org.lockss.plugin.libertasacademica;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.*;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.PrototypicalNodeFactory;
import org.htmlparser.lexer.InputStreamSource;
import org.htmlparser.lexer.Lexer;
import org.htmlparser.lexer.Page;
import org.htmlparser.tags.MetaTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.filter.html.HtmlTags;
import org.lockss.plugin.*;
import org.lockss.util.Constants;
import org.lockss.util.Logger;

/*
 * PDF Full Text: http://www.la-press.com/redirect_file.php?fileType=pdf&fileId=4199&filename=3103-ACI-Holistic-Control-of-Herbal-Teas-and-Tinctures-Based-on-Sage-(Salvia-of.pdf&nocount=1
 * HTML Abstract: http://www.la-press.com/holistic-control-of-herbal-teas-and-tinctures-based-on-sage-salvia-off-article-a3103
 * <meta content="http://la-press.com/redirect_file.php?fileId=961&filename=ACI-1-Zayas(Pr)&fileType=pdf" name="citation_pdf_url">
 */

public class LibertasAcademicaArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger("IgiArticleIteratorFactory");
  
  protected static final String ROOT_TEMPLATE = "\"%s\", base_url"; // params from tdb file corresponding to AU
  
  protected static final String PATTERN_TEMPLATE = "\"^%s[^/]+-article-a[0-9]+$\", base_url";

  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    return new LibertasAcademicaArticleIterator(au,
                                         new SubTreeArticleIterator.Spec()
                                             .setTarget(target)
                                             .setRootTemplate(ROOT_TEMPLATE)
                                             .setPatternTemplate(PATTERN_TEMPLATE));
  }

  protected static class LibertasAcademicaArticleIterator extends SubTreeArticleIterator {

    protected Pattern ABSTRACT_PATTERN = Pattern.compile("([^/]+)-article-a[0-9]+$", Pattern.CASE_INSENSITIVE);
   
    public LibertasAcademicaArticleIterator(ArchivalUnit au,
                                     SubTreeArticleIterator.Spec spec) {
      super(au, spec);
    }
    
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      log.info("article url?: " + url);
      Matcher mat;
      mat = ABSTRACT_PATTERN.matcher(url);
      if (mat.find()) {
        return processAbstract(cu, mat);
      }

      log.warning("Mismatch between article iterator factory and article iterator: " + url);
      return null;
    }
    

    protected ArticleFiles processAbstract(CachedUrl absCu, Matcher absMat) {
    	NodeList nl = null;
    	if (absCu != null && absCu.hasContent()) {
	    	try {
			    InputStreamSource is = new InputStreamSource(absCu.getUnfilteredInputStream());
			    Page pg = new Page(is);
			    Lexer lx = new Lexer(pg);
			    Parser parser = new Parser(lx);
			    Lexer.STRICT_REMARKS = false;
			    nl = parser.parse(null);
	    	} catch(ParserException e) {
	    		e.printStackTrace();
	    	} catch(UnsupportedEncodingException e) {
				e.printStackTrace();
			} finally {
	    		absCu.release();
	    	}
    	}
    	
    	if(nl != null) {
    		ArticleFiles af = new ArticleFiles();
    		NodeFilter nf = new NodeFilter() {
    	        public boolean accept(Node node) {
    	        	if (!(node instanceof MetaTag)) return false;
    	        	MetaTag meta = (MetaTag)node;
    	        	if (!"citation_pdf_url".equalsIgnoreCase(meta.getMetaTagName())) return false;
    	        	return true;
    	        }
    	    };
    	    nl.keepAllNodesThatMatch(nf);
    	    String pdfUrl = ((MetaTag)nl.elementAt(0)).getMetaContent();
    	    List<String> paramList = new ArrayList<String>();
    	    paramList.add("fileType");
    	    paramList.add("fileId");
    	    paramList.add("fileName");
    	    pdfUrl = reArrangeUrlParams(pdfUrl, paramList);
    	    if(!pdfUrl.startsWith("http://www.")) {
    	    	pdfUrl.replace("http://", "http://www.");
    		}
    	    CachedUrl pdfCu = au.makeCachedUrl(pdfUrl);
    	    if (pdfCu != null && pdfCu.hasContent()) {
    	    	af.setFullTextCu(pdfCu);
    	    	af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, absCu);
    	    	return af;
    	    }
    	}

      return null;
    }
    
    private String reArrangeUrlParams(String url, List<String> paramList) {
  	  return reArrangeUrlParams(url, paramList, "&");
    }
    
    private String reArrangeUrlParams(String url, List<String> paramList, String paramDelim) {
  	  String newUrl = "";
  	  String [] urlTokens = url.split("[?" + paramDelim + "]");
  	  if(urlTokens.length > 1) {
  		  newUrl = urlTokens[0];
  		  for(String param : paramList) {
  			  for(int i = 1; i < urlTokens.length; i++){
  				  if(urlTokens[i].startsWith(param)) {
  					  newUrl = newUrl + urlTokens[i];
  					  break;
  				  }
  			  }
  		  }
  	  }
  	  return newUrl;
    }

  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ABSTRACT);
    // Ask Phil how to talk to our real metadata extractor here
  }

}
