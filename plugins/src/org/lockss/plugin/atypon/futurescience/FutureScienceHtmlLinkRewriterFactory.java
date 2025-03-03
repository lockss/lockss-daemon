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

package org.lockss.plugin.atypon.futurescience;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.tags.LinkTag;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlLinkRewriterFactory;
import org.lockss.rewriter.LinkRewriterFactory;
import org.lockss.servlet.ServletUtil.LinkTransform;
import org.lockss.util.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FutureScienceHtmlLinkRewriterFactory implements LinkRewriterFactory {
  
  private static final Logger logger = Logger.getLogger(FutureScienceHtmlLinkRewriterFactory.class);

  /**
   * This link rewriter adds special processing to substitute a link to show the RIS citation
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

    return BaseAtyponHtmlLinkRewriterFactory.createLinkRewriter(mimeType,
                                                                au,
                                                                in,
                                                                encoding,
                                                                url,
                                                                xfm,
                                                                new FutureSciencPreFilter(au,url),
                                                                null);
  }

  static class FutureSciencPreFilter implements NodeFilter {
    /*
      <li role="presentation" class="article-tool"><a href="/action/showCitFormats?doi=10.2144%2F000113514" role="menuitem"><i aria-hidden="true" class="icon-Icon_Download"></i><span>Download Citations</span></a></li>
      <li role="presentation" class="article-tool"><a href="/action/showCitFormats?doi=10.2217%2Fepi.10.68" role="menuitem"><i aria-hidden="true" class="icon-Icon_Download"></i><span>Download Citations</span></a></li>
    */
    private static final Pattern DOI_URL_PATTERN = Pattern.compile("^(https?://.*/)doi/full/(.*)$");
    //private static final Pattern DOI_URL_PATTERN = Pattern.compile("^(https?://.*/)doi/full/(10\\.2144/000113771)$");
    private static final String CIT_DOWNLOAD_ACTION = "action/downloadCitation";
    private static final String CITATIONS_ANCHOR = "/action/showCitFormats";
    private static final String DOWNLOAD_RIS_TAIL = "&format=ris&include=cit";

    private String html_url = null;
    private ArchivalUnit thisau = null;

    public FutureSciencPreFilter(ArchivalUnit au, String url) {
      super();
      html_url = url;
      thisau = au;
    }

    public boolean accept(Node node) {
      // store the value of tgit statuhe link arguments for later reassembly
      if (node instanceof LinkTag) {
        Matcher doiMat = DOI_URL_PATTERN.matcher(html_url);
        // Are we on a page for which this would be pertinent?
        if (doiMat.find()) {
          // now do we have a link to #pill-citations
          String linkval = ((LinkTag) node).getLink();
          if (linkval == null) {
            return false;
          }
          if (linkval.contains(CITATIONS_ANCHOR)) {
            String newUrl =  "/" + CIT_DOWNLOAD_ACTION + "?doi=" + doiMat.group(1) + "/" + doiMat.group(2) + DOWNLOAD_RIS_TAIL;
            logger.debug3("found citations anchor, rewriting: <" + linkval + "> to: <" + newUrl + ">");
            ((TagNode) node).setAttribute("target", "_blank");
            ((LinkTag) node).setLink(newUrl);
          }
        }
      }
      return false;
    }
  }
}
