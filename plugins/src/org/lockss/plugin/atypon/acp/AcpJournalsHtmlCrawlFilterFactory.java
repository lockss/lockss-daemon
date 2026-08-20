/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University
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
package org.lockss.plugin.atypon.acp;

import java.io.InputStream;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.LinkTag;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;
import org.lockss.util.Logger;


public class AcpJournalsHtmlCrawlFilterFactory
        extends BaseAtyponHtmlCrawlFilterFactory {

    private static final Logger log =
            Logger.getLogger(AcpJournalsHtmlCrawlFilterFactory.class);

    private static final Pattern KEEP =
            Pattern.compile("/doi/(suppl|figure)/", Pattern.CASE_INSENSITIVE);

    private static String doiHref(Node node) {
        if (!(node instanceof LinkTag)) { return null; }
        String href = ((LinkTag) node).getAttribute("href");
        if (href == null || !href.contains("doi/")) { return null; }
        return KEEP.matcher(href).find() ? null : href;
    }

    /** any /doi/ anchor inside section#bodymatter */
    private static final NodeFilter BODYMATTER_DOI_LINK = new NodeFilter() {
        @Override public boolean accept(Node node) {
            String href = doiHref(node);
            if (href == null) { return false; }
            for (Node p = node.getParent(); p != null; p = p.getParent()) {
                if (p instanceof CompositeTag
                        && "bodymatter".equalsIgnoreCase(
                        ((CompositeTag) p).getAttribute("id"))) {
                    log.debug3("acp crawl filter (bodymatter): " + href);
                    return true;
                }
            }
            return false;
        }
    };

    private static final NodeFilter ABSOLUTE_DOI_LINK = new NodeFilter() {
        @Override public boolean accept(Node node) {
            String href = doiHref(node);
            if (href == null) { return false; }
            String h = href.toLowerCase();
            if (h.startsWith("http") || h.startsWith("//") || h.startsWith("www.")) {
                log.debug3("acp crawl filter (absolute): " + href);
                return true;
            }
            return false;
        }
    };

    private static final NodeFilter[] acpFilters = new NodeFilter[] {
            BODYMATTER_DOI_LINK,
            ABSOLUTE_DOI_LINK,
    };

    @Override
    public InputStream createFilteredInputStream(ArchivalUnit au, InputStream in,
                                                 String encoding)
            throws PluginException {
        return super.createFilteredInputStream(au, in, encoding, acpFilters);
    }
}