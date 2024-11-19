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

package org.lockss.plugin.highwire;

import java.io.IOException;
import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlCompoundTransform;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.filter.html.HtmlTransform;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.Logger;

public class HighWireJCoreHashFilterFactory implements FilterFactory{

    private static final Logger log = Logger.getLogger(HighWireJCoreHashFilterFactory.class);

    @Override
    public InputStream createFilteredInputStream(ArchivalUnit au, InputStream inputstream, String encoding)
            throws PluginException {
        NodeFilter[] filters = new org.htmlparser.NodeFilter[]{

            HtmlNodeFilters.tag("head"),
            HtmlNodeFilters.tag("script"),
            HtmlNodeFilters.tag("noscript"),
            HtmlNodeFilters.tag("style"),
            HtmlNodeFilters.tag("header"),
            HtmlNodeFilters.tag("footer"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "advertisement"),
            HtmlNodeFilters.tagWithAttributeRegex("div","class","panel-region-sidebar-right"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "social-media"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "pane-highwire-article-citation"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "id", "hw-article-author-popups-top-node"),
            HtmlNodeFilters.tagWithAttributeRegex("li", "class", "view-popup"),
            
            HtmlNodeFilters.comment()
        };

        HtmlTransform xform = new HtmlTransform() {
            @Override
            public NodeList transform(NodeList nodeList) throws IOException {
                try {
                    nodeList.visitAllNodesWith(new NodeVisitor() {
                        @Override
                        public void visitTag(Tag tag) {
                            String name = tag.getTagName().toLowerCase();
                            if ("a".equals(name)) {
                                String val = tag.getAttribute("rel");
                                if (val != null) {
                                    tag.removeAttribute("rel");
                                }
                                return;
                            }
                            if ("div".equals(name)) {
                                String val = tag.getAttribute("id");
                                if (val != null) {
                                    tag.removeAttribute("id");
                                }
                                return;
                            }
                        }
                    });
                    return nodeList;
                }
                catch (ParserException pe) {
                    throw new IOException(pe);
                }
            }
        };
      // First filter with HtmlParser
      InputStream filtered = new HtmlFilterInputStream(inputstream,
      encoding, new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(filters)),
                                xform));
        return filtered;
    }
    
}
