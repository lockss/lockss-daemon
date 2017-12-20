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
          buttonInput.setAttribute("onclick",newUrl);
          ((FormTag) node).setAttribute("action", newUrl);
        }
      }
      return false;
    }

  }

}
