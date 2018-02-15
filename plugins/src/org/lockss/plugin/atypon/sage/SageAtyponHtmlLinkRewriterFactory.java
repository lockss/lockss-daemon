/*
  * $Id$
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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
