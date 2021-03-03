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

package org.lockss.plugin.atypon;

import java.io.InputStream;
import java.io.IOException;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.rewriter.*;
import org.lockss.servlet.ServletUtil.LinkTransform;
import org.lockss.util.Logger;

/**
 * This custom link rewriter performs Atypon specific rewriting 
 * to handle any "created" link extraction links and to handle
 * the download citations form by turning it in to the collected link
 * 
 */
public class BaseAtyponHtmlLinkRewriterFactory implements LinkRewriterFactory {
  
  private static final String DEFAULT_CITATION_ARGS = "&format=ris&include=cit";
  private static final String SHOW_CITATION = "showCitFormats";
  private static final String DOWNLOAD_CITATION = "downloadCitation";
  private static final Logger log =
    Logger.getLogger(BaseAtyponHtmlLinkRewriterFactory.class);
  

  
  /*
   * Create an additional creation method so child plugin can add to the pre and post filters
   */
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
      fact.addPreXform(new OrFilter(childPreFilter,new AtyponPreFilter(au,url)));
    } else {
      // no child - just use the BaseAtypon one
      fact.addPreXform(new AtyponPreFilter(au,url));
    }
    if (childPostFilter != null) {
      // currently no BaseAtypon postfilter - just use the childs
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
    
    fact.addPreXform(new AtyponPreFilter(au,url));
    return fact.createLinkRewriter(mimeType, au, in, encoding, url, xfm);
  }

  static class AtyponPreFilter implements NodeFilter {
    
    protected static final String SHOW_CIT_URL_SNIPPET =
        "action/showCitFormats";
    private String html_url = null;
    private ArchivalUnit thisau = null;
    
    public AtyponPreFilter(ArchivalUnit au, String url) {
      super();
      html_url = url;
      thisau = au;
    }
    
    public boolean accept(Node node) {
      if ((node instanceof FormTag) && (html_url.contains(SHOW_CIT_URL_SNIPPET))) {
        // form tag with name="frmCitMgr" (see link extractor)
        String method = ((FormTag) node).getFormMethod();
        String f_name = ((FormTag) node).getFormName();
        // make the onclick use the ris citation information link we know we have
        if ("post".equalsIgnoreCase(method) && "frmCitmgr".equalsIgnoreCase(f_name)) {
          String newUrl = html_url.replaceAll(SHOW_CITATION,DOWNLOAD_CITATION) + DEFAULT_CITATION_ARGS;
          InputTag buttonInput = ((FormTag) node).getInputTag("submit");
          if (buttonInput != null) {
            buttonInput.setAttribute("onclick",newUrl);
          }
          ((FormTag) node).setAttribute("action", newUrl);
        }
      }
      return false;
    }

  }

}
