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

package org.lockss.plugin.janeway;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.tags.BodyTag;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlTags;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.rewriter.LinkRewriterFactory;
import org.lockss.rewriter.NodeFilterHtmlLinkRewriterFactory;
import org.lockss.servlet.ServletUtil.LinkTransform;
import org.lockss.util.Logger;

import java.io.IOException;
import java.io.InputStream;

public class HtmlLinkRewriterFactory implements LinkRewriterFactory {

  private static final Logger log =
    Logger.getLogger(HtmlLinkRewriterFactory.class);
  
  
  public static InputStream createLinkRewriter(String mimeType,
                                        ArchivalUnit au,
                                        InputStream in,
                                        String encoding,
                                        String url,
                                        LinkTransform xfm,
                                        NodeFilter childPreFilter,
                                        NodeFilter childPostFilter)
    throws PluginException, IOException {
  
  
    NodeFilterHtmlLinkRewriterFactory fact =
        new NodeFilterHtmlLinkRewriterFactory();

    if (childPreFilter != null) {
      fact.addPreXform(new OrFilter(childPreFilter,new UofMichigan(au,url)));
    } else {
      fact.addPreXform(new UofMichigan(au,url));
    }
    if (childPostFilter != null) {
      fact.addPostXform(childPostFilter);
    }
    return fact.createLinkRewriter(mimeType, au, in, encoding, url, xfm);
    
  }
      
  public InputStream createLinkRewriter(String mimeType,
                                        ArchivalUnit au,
                                        InputStream in,
                                        String encoding,
                                        String url,
                                        LinkTransform xfm)
      throws PluginException, IOException {
    
    NodeFilterHtmlLinkRewriterFactory fact =
        new NodeFilterHtmlLinkRewriterFactory();
    
    fact.addPreXform(new UofMichigan(au,url));
    return fact.createLinkRewriter(mimeType, au, in, encoding, url, xfm);
  }

  /*
    This is used to handle UofMichigan video only
   */
  static class UofMichigan implements NodeFilter {

    private String html_url = null;
    private ArchivalUnit thisau = null;

    public UofMichigan(ArchivalUnit au, String url) {
      super();
      html_url = url;
      thisau = au;
    }

    /*
	Article page:
    https://journals.publishing.umich.edu/conversations/article/id/2354/ has the following chain

    https://journals.publishing.umich.edu/conversations/article/id/2354/ => https://www.fulcrum.org/embed?hdl=2027%2Ffulcrum.jm214r214&fs=1 => https://www.fulcrum.org/downloads/jm214r214?file=mp4&locale=en

    <video id="video"
       preload="metadata"
       width="8000px"
       data-able-player
       data-skin="2020"
       data-captions-position="overlay"
       data-include-transcript="false"
       data-heading-level="0"
       data-allow-fullscreen=true
       poster="/downloads/jm214r214?file=jpeg&amp;locale=en"
       data-transcript-div="video-hidden-transcript-container" data-lyrics-mode>
 */

    public boolean accept(Node node) {
      // Only do it for the embeded html
      if (html_url.contains("https://www.fulcrum.org/embed")) {

        log.debug3("Video page html_url = " + html_url);

        if ((node instanceof HtmlTags.Video)) {

          // Step-1: remove with
          String width = ((HtmlTags.Video) node).getAttribute("width");

          log.debug3("Video page video tag found, width = " + width);
          ((HtmlTags.Video) node).removeAttribute("width"); //Remove width

        }

        if ((node instanceof TagNode) && "HTML".equalsIgnoreCase(((TagNode) node).getTagName())) {
          ((TagNode) node).setAttribute("style", "overflow:scroll");
          log.debug3("Video page html tag found, set overflow attribute");
        }

        if ((node instanceof TagNode) && "BODY".equalsIgnoreCase(((TagNode) node).getTagName())) {
          ((TagNode) node).setAttribute("style", "overflow:scroll");
          log.debug3("Video page body tag found, set overflow attribute");
        }
      }
      return false;
    }
  }
}
