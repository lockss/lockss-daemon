/*

Copyright (c) 2000-2025, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.ojs3;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlTags.Iframe;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.rewriter.LinkRewriterFactory;
import org.lockss.rewriter.NodeFilterHtmlLinkRewriterFactory;
import org.lockss.servlet.ServletUtil;
import org.lockss.servlet.ServletUtil.LinkTransform;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.util.UrlUtil;
import org.lockss.util.urlconn.CacheException.MalformedURLException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.*;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.ScriptTag;

public class Ojs3HtmlLinkRewriterFactory implements LinkRewriterFactory{
    private final static Logger log = Logger.getLogger(Ojs3HtmlLinkRewriterFactory.class);

    public Ojs3HtmlLinkRewriterFactory() {
        super();
    }

    @Override
    public InputStream createLinkRewriter(String mimeType, ArchivalUnit au, InputStream in, String encoding, String url,
            LinkTransform xform) throws PluginException, IOException {
        LinkRewriterFactory fact = new Ojs3NodeFilterHtmlLinkRewriterFactory(au, url, xform);
        return fact.createLinkRewriter(mimeType, au, in, encoding, url, xform);
    }

    public static class Ojs3NodeFilterHtmlLinkRewriterFactory extends NodeFilterHtmlLinkRewriterFactory {
        public Ojs3NodeFilterHtmlLinkRewriterFactory(final ArchivalUnit au, final String url, final LinkTransform xform){
            super();
            addPostXform(new NodeFilter(){    
                String pdfhref;             
                @Override
                public boolean accept(Node node) {
                    if (node instanceof LinkTag) {
                        LinkTag link = (LinkTag)node;
                        if("download".equals(link.getAttribute("class"))){
                            pdfhref = link.getAttribute("href");
                            //add request_disposition=attachment to download button
                            link.setAttribute("href",pdfhref+"&requested_disposition=attachment");
                        }
                    }
                    else if(node instanceof ScriptTag){
                        ScriptTag script = (ScriptTag)node;
                        String scriptContent = script.toPlainTextString();
                        if (!StringUtil.isNullString(scriptContent)) {
                            String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
                            String journalID = au.getConfiguration().get(ConfigParamDescr.JOURNAL_ID.getKey());
                            Pattern lensPat = Pattern.compile(String.format("(linkElement.href = \")(%s[^\"]+lens\\.css)(\"; //Replace here)", baseUrl),
                                                                Pattern.CASE_INSENSITIVE);
                            Matcher lensMat = lensPat.matcher(scriptContent);
                            if (lensMat.find()) {
                                /*This script tag lives at https://euchembioj.com/index.php/pub/article/view/13/26. 
                                Example script tag that contains css and xml download links: 
                                <script type="text/javascript">

                                    var linkElement = document.createElement("link");
                                    linkElement.rel = "stylesheet";
                                    linkElement.href = "https://euchembioj.com/plugins/generic/lensGalley/lib/lens/lens.css"; //Replace here

                                    document.head.appendChild(linkElement);

                                    $(document).ready(function(){
                                        var app = new Lens({
                                            document_url: "https://euchembioj.com/index.php/pub/article/download/13/26/491"
                                        });
                                        app.start();
                                        window.app = app;
                                    });
                                </script>
                                */
                                log.debug3("found correct script tag");
                                log.debug3("the script tag is " + scriptContent);
                                scriptContent = lensMat.replaceFirst(String.format("$1%s$3", xform.rewrite(lensMat.group(2))));
                                Pattern lensXmlPat = Pattern.compile(String.format("(document_url: \")(%sindex.php/%s/article/download/[^\"]+)(\")", baseUrl, journalID));
                                Matcher lensXmlMat = lensXmlPat.matcher(scriptContent);
                                if(lensXmlMat.find()){
                                    scriptContent = lensXmlMat.replaceFirst(String.format("$1%s$3",xform.rewrite(lensXmlMat.group(2))));
                                    log.debug3("The scriptContent has become " + scriptContent);
                                    script.setScriptCode(scriptContent);
                                }else{
                                    log.debug("Unable to find xml download link in script tag that contains lens.css link: " + url);
                                }
                            }
                        }
                    }
                    else if(node instanceof Iframe){
                        Iframe iframe = (Iframe)node;
                        Node parent = iframe.getParent();
                        if(parent instanceof Div){
                            Div parentDiv = (Div)parent;
                                if("pdfCanvasContainer".equals(parentDiv.getAttribute("id")) && pdfhref != null){
                                    //add request_disposition=inline to display pdf in browser's default browser
                                    iframe.setAttribute("src",pdfhref+"&requested_disposition=inline");
                                }
                        }
                    }
                    return false;
                }
            });
        }
    }
}