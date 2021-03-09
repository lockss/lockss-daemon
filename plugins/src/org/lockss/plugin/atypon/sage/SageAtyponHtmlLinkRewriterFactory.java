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

package org.lockss.plugin.atypon.sage;

import java.io.InputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Attribute;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.tags.LinkTag;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlLinkRewriterFactory;
import org.lockss.rewriter.*;
import org.lockss.servlet.ServletUtil.LinkTransform;
import org.lockss.util.Logger;

/**
 * This custom link rewriter performs Sagepub specific rewriting 
 */
public class SageAtyponHtmlLinkRewriterFactory implements LinkRewriterFactory {
  
  
  private static final Logger log = Logger.getLogger(SageAtyponHtmlLinkRewriterFactory.class);
  
  /**
   * This link rewriter adds special processing to substitute a link to show the RIC citation
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
    
    return BaseAtyponHtmlLinkRewriterFactory.createLinkRewriter(mimeType, au, in, encoding, url, xfm, new SageAtyponPreFilter(au,url), null);

  }
  
  
  static class SageAtyponPreFilter implements NodeFilter {
    //<a class="citationsTool" href="#" ...
    // becomes
    // <a class="citationsTool" href="/action/downloadCitation?doi=10.1177%2F0001345516665507&amp;format=ris&amp;include=cit" target="_blank">
    private static final Pattern DOI_URL_PATTERN = Pattern.compile("^(?:https?://.*/)doi/(?:abs|figure|full|ref(?:erences)?|suppl)/([.0-9]+)/([^/]+)$");
    private static final String CIT_DOWNLOAD_ACTION = "action/downloadCitation";
    private static final String CITATIONS_TOOLS = "citationsTool";
    
    private String html_url = null;
    private ArchivalUnit thisau = null;
    
    public SageAtyponPreFilter(ArchivalUnit au, String url) {
      super();
      html_url = url;
      thisau = au;
    }
    
    public boolean accept(Node node) {
      // store the value of the link arguments for later reassembly
      if (node instanceof LinkTag) {
          Matcher doiMat = DOI_URL_PATTERN.matcher(html_url);
          // Are we on a page for which this would be pertinent?
          if (doiMat.find()) {
            // now do we have a link with class citationsTool?
            Attribute linkval = ((LinkTag) node).getAttributeEx("class");
            if (linkval == null) {
              return false;
            }
            if (linkval.getValue().contains(CITATIONS_TOOLS)) {
              String newUrl =  "/" + CIT_DOWNLOAD_ACTION + "?doi=" + doiMat.group(1) + "/" + doiMat.group(2);
              // XXX if http-2-https is needed
              // newUrl = AuUtil.normalizeHttpHttpsFromBaseUrl(thisau, newUrl);
              ((TagNode) node).setAttribute("target", "_blank");
              ((TagNode) node).removeAttribute("onclick");
              ((LinkTag) node).setLink(newUrl);
            }
          }
        }
      return false;
      }
    }


}
