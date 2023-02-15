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

package org.lockss.plugin.projmuse;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.tags.LinkTag;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.rewriter.LinkRewriterFactory;
import org.lockss.rewriter.NodeFilterHtmlLinkRewriterFactory;
import org.lockss.servlet.ServletUtil.LinkTransform;
import org.lockss.util.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProjectMuse2017HtmlLinkRewriterFactory implements LinkRewriterFactory {
  
  private static final Logger logger = Logger.getLogger(ProjectMuse2017HtmlLinkRewriterFactory.class);

  /**
   * This link rewriter adds special processing to substitute a link to show the RIS citation
   *
   */
  public InputStream createLinkRewriter(String mimeType,
                                        ArchivalUnit au,
                                        InputStream in,
                                        String encoding,
                                        String url,
                                        LinkTransform xfm)
      throws PluginException, IOException {

    NodeFilterHtmlLinkRewriterFactory fact =
        new NodeFilterHtmlLinkRewriterFactory();

    fact.addPreXform(new ProjectMuse2017PreFilter(au,url));
    return fact.createLinkRewriter(mimeType, au, in, encoding, url, xfm);
  }

  static class ProjectMuse2017PreFilter implements NodeFilter {
    /*
      https://muse.jhu.edu/pub/424/article/813469
      becomes
      https://muse.jhu.edu/article/813469
     */

    protected static final Pattern PUB_ID_PAT = Pattern.compile("/pub/\\d+/");
    private String html_url = null;
    private ArchivalUnit thisau = null;

    public ProjectMuse2017PreFilter(ArchivalUnit au, String url) {
      super();
      html_url = url;
      thisau = au;
    }

    public boolean accept(Node node) {
      // store the value of tgit statuhe link arguments for later reassembly
      if (node instanceof LinkTag) {
        String linkval = ((LinkTag) node).getLink();
        if (linkval == null) {
          return false;
        }
        Matcher mat = PUB_ID_PAT.matcher(linkval);
        if (mat.find()) {
          // https://muse.jhu.edu/pub/424/article/813469
          // https://muse.jhu.edu/article/813469
          ((LinkTag) node).setLink(mat.replaceFirst("/"));
        }
      }
      return false;
    }
  }
}
