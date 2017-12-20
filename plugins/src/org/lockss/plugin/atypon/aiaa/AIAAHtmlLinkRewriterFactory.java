/*
  * $Id$
 */

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.plugin.atypon.aiaa;

import java.io.InputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.tags.LinkTag;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.atypon.BaseAtyponHtmlLinkRewriterFactory;
import org.lockss.rewriter.*;
import org.lockss.servlet.ServletUtil.LinkTransform;
import org.lockss.util.Logger;

/**
 * This custom link rewriter performs AIAA specific rewriting 
 * for the special link extractor that generates the showCitFormats
 * url.  The javascript is incorrectly rewritten for servecontent
 * so create the desired link before applying the ServeContent
 * transformation. 
 * 
 * 
 */
public class AIAAHtmlLinkRewriterFactory implements LinkRewriterFactory {
  
  
  private static final Logger log =
    Logger.getLogger(AIAAHtmlLinkRewriterFactory.class);
  
  /**
   * This link rewriter adds special processing to replace the showCit
   * javascript method with the link we would normally extract from it
   * 
   */
  @Override
  public InputStream createLinkRewriter(String mimeType,
                                        ArchivalUnit au,
                                        InputStream in,
                                        String encoding,
                                        String url,
                                        LinkTransform xfm)
      throws PluginException, IOException {
    
//    NodeFilterHtmlLinkRewriterFactory fact =
//      new NodeFilterHtmlLinkRewriterFactory();
    
//    fact.addPreXform(new AIAAPreFilter(au,url));
//    return fact.createLinkRewriter(mimeType, au, in, encoding, url, xfm);
    return BaseAtyponHtmlLinkRewriterFactory.createLinkRewriter(mimeType, au, in, encoding, url, xfm, new AIAAPreFilter(au,url), null);

  }
  
  
  static class AIAAPreFilter implements NodeFilter {
    //<a 
    // href="javascript:submitArticles(document.frmAbs, '/action/showCitFormats'
    // becomes
    // href = 
    protected Pattern SUBMIT_ARTICLES_PATTERN = Pattern.compile("javascript:submitArticles\\(([^,]+),([^,]+),*", Pattern.CASE_INSENSITIVE);
    protected Pattern DOI_URL_PATTERN = Pattern.compile("^(https?://.*/)doi/(abs|full|book)/([.0-9]+)/([^/]+)$");
    private static final String CIT_FORMATS_ACTION = "action/showCitFormats";

    private String html_url = null;
    private ArchivalUnit thisau = null;
    
    public AIAAPreFilter(ArchivalUnit au, String url) {
      super();
      html_url = url;
      thisau = au;
    }
    
    public boolean accept(Node node) {
      // store the value of the PDF link arguments for later reassembly
      if (node instanceof LinkTag) {
          Matcher doiMat = DOI_URL_PATTERN.matcher(html_url);
          // Are we on a page for which this would be pertinent?
          if (doiMat.find()) {
            // now do we have a citation download href?
            String linkval = ((LinkTag) node).extractLink();
            if (linkval == null) {
              return false;
            }
            Matcher hrefMat = SUBMIT_ARTICLES_PATTERN.matcher(linkval);
            if ( (hrefMat.find() && hrefMat.group(2).contains(CIT_FORMATS_ACTION))) {
              String newUrl =  doiMat.group(1) + CIT_FORMATS_ACTION + "?doi=" + doiMat.group(3) + "/" + doiMat.group(4);
              newUrl = AuUtil.normalizeHttpHttpsFromBaseUrl(thisau, newUrl);  
              ((LinkTag) node).setLink(newUrl);
            }
          }
        }
      return false;
      } 
    }


}
