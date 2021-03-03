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
