/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.ubiquitypress.upn;

import java.io.*;
import java.net.MalformedURLException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.lockss.util.UrlUtil;
import org.htmlparser.*;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.ScriptTag;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.rewriter.*;
import org.lockss.servlet.ServletUtil.LinkTransform;
import org.lockss.util.Logger;
import org.lockss.rewriter.NodeFilterHtmlLinkRewriterFactory.HtmlBaseProcessor;
import org.lockss.util.*;

public class UbiquityPartnerNetworkHtmlLinkRewriterFactory implements LinkRewriterFactory {

  static Logger log = Logger.getLogger(UbiquityPartnerNetworkHtmlLinkRewriterFactory.class);
  //{\"href\":\"/api/1.8.4/spritesheet#user\"}
  static Pattern jsonHref = Pattern.compile("(\\\\\"href\\\\\":\\s*\\\\\")(.*?)(\\\\\")");

  @Override
  public InputStream createLinkRewriter(String mimeType, 
                                        ArchivalUnit au,
                                        InputStream in,
                                        String encoding,
                                        String url,
                                        LinkTransform xform)
      throws PluginException, IOException {
    NodeFilterHtmlLinkRewriterFactory fact = new NodeFilterHtmlLinkRewriterFactory();
    
    /*
     * Some "Download" PDF links in Utrecht University Library's Studium
     * (https://www.gewina-studium.nl/) have an href value with extraneous
     * whitespace before and after, even spanning multiple lines. The regular
     * expressions of NodeFilterHtmlLinkRewriterFactory are anchored and don't
     * rewrite such links. Pre-process all <a> tags by trimming the href value.
     * 
     * Example from
     * https://www.gewina-studium.nl/articles/abstract/10.18352/studium.1451/:
     * 
<a
    
        class="piwik_download"
        data-trackThis='downloads'
        data-category="PDF"
        data-label=
                "
            
                10.18352/studium.1451#1480
            "

    
        href="
    
        /articles/10.18352/studium.1451/galley/1480/download/
    ">
    PDF
    (EN)
</a>
     */

    class JSONRewritingFilter implements NodeFilter{
      String baseUrl;
      public void setBaseUrl(String newBase) {
        baseUrl = newBase;
      }
      @Override
      public boolean accept(Node node) {
        if (node instanceof LinkTag) {
          LinkTag link = (LinkTag)node;
          String href = link.getLink();
          if (href != null) {
            link.setLink(href.trim());
          }
        }
        if(node instanceof ScriptTag){
          ScriptTag script = (ScriptTag)node;
          String s = script.toPlainTextString();
          //script.removeAttribute("src");
          log.debug3("the text BEFORE replacement is " + s);
          if(s.startsWith("self.__next") || s.startsWith("(self.__next")){
            Matcher mat = jsonHref.matcher(s);
            StringBuffer sb = new StringBuffer();
            while(mat.find()){
              String hrefVal = mat.group(2);
              log.debug3("the second matching group is " + hrefVal);
              String newUrl;
              try{
                newUrl = xform.rewrite(UrlUtil.encodeUrl(UrlUtil.resolveUri(baseUrl, hrefVal)));
              }catch (MalformedURLException e){
                  log.warning("Couldn't resolve: the base url is " + baseUrl + " and the second group is " +  hrefVal);
                  newUrl = hrefVal;
              };
              log.debug3("new url is " + newUrl);
              String rep = "$1" + Matcher.quoteReplacement(newUrl) + "$3";
              mat.appendReplacement(sb, rep);
              log.debug3(rep);
            }
            mat.appendTail(sb);
            script.setScriptCode(sb.toString());
            log.debug3("the text AFTER replacement is " + sb.toString());
          }
        }
        return false;
      }
    }

    JSONRewritingFilter jsonFilt = new JSONRewritingFilter();
    jsonFilt.setBaseUrl(url);
    HtmlBaseProcessor baseProc = new HtmlBaseProcessor(url);
    baseProc.setXforms(ListUtil.list(jsonFilt));
    fact.addPreXform(baseProc);
    fact.addPreXform(jsonFilt);
    
    /*
     * Images in Utrecht University Library's Studium
     * (https://www.gewina-studium.nl/) are displayed in a Featherlight
     * (https://noelboss.github.io/featherlight/) widget. In this context, an
     * <a> tag has a 'data-featherlight' attribute with a relative URL, and
     * contains a regular <img> tag inside it. When clicked, the image is
     * rendered in a popup, but using the 'data-featherlight' link from the <a>
     * tag.
     * 
     * Example from
     * https://www.gewina-studium.nl/articles/10.18352/studium.10120/:
     * 
<a href="#" data-featherlight="figures/Cocquyt_fig1.jpg">
  <img src="figures/Cocquyt_fig1.jpg">
</a>
     */
    fact.addAttrToRewrite("data-featherlight");
    
    return fact.createLinkRewriter(mimeType, au, in, encoding, url, xform);
  }
  
}
