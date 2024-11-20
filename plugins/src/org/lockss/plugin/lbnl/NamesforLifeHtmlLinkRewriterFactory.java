/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.lbnl;

import java.io.*;

import org.htmlparser.*;
import org.htmlparser.tags.*;
import org.htmlparser.util.NodeList;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.rewriter.*;
import org.lockss.servlet.ServletUtil.LinkTransform;

public class NamesforLifeHtmlLinkRewriterFactory implements LinkRewriterFactory {
  
  @Override
  public InputStream createLinkRewriter(String mimeType,
                                        ArchivalUnit au,
                                        InputStream in,
                                        String encoding,
                                        String url,
                                        LinkTransform xform)
      throws PluginException, IOException {

    NodeFilterHtmlLinkRewriterFactory lrf = new NodeFilterHtmlLinkRewriterFactory();
    lrf.addPreXform(new NodeFilter() {
      @Override
      public boolean accept(Node node) {
        if (node != null && node instanceof HeadTag) {
          NodeList headChildren = ((HeadTag)node).getChildren();
          for (int i = headChildren.size() - 1 ; i >= 0 ; i--) {
            Node child = headChildren.elementAt(i);
            if (child != null && child instanceof ScriptTag) {
              String src = ((ScriptTag)child).getAttribute("src");
              if (src != null) {
                switch (src) {
                  case "https://api.namesforlife.com/script/abstract.js":
                  case "https://api.namesforlife.com/script/microbial-earth.js":
                  case "https://api.namesforlife.com/script/sac.js":
                    headChildren.remove(i);
                    break;
                }
              }
            }
          }
        }
        return false;
      }
    });
    return lrf.createLinkRewriter(mimeType, au, in, encoding, url, xform);
  }

}
