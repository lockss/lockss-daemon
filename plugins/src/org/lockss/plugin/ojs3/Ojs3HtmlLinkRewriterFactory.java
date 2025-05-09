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

import org.lockss.filter.html.HtmlTags.Iframe;
import org.lockss.rewriter.NodeFilterHtmlLinkRewriterFactory;
import org.lockss.util.Logger;
import org.htmlparser.*;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.ScriptTag;

public class Ojs3HtmlLinkRewriterFactory extends NodeFilterHtmlLinkRewriterFactory{
    static Logger log = Logger.getLogger(Ojs3HtmlLinkRewriterFactory.class);

    public Ojs3HtmlLinkRewriterFactory() {
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
                }/*else if(node instanceof ScriptTag){
                    ScriptTag script = (ScriptTag)node;
                    String scriptContent = script.toPlainTextString();
                    if(scriptContent.contains("/plugins/generic/pdfJsViewer/pdf.js/web/viewer.html")){
                        return true;
                    }*/
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
