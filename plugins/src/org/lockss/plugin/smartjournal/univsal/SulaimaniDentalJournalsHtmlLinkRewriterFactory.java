/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.smartjournal.univsal;

import org.htmlparser.Tag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlTransform;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.rewriter.LinkRewriterFactory;
import org.lockss.rewriter.NodeFilterHtmlLinkRewriterFactory;
import org.lockss.servlet.ServletUtil;
import org.lockss.util.Logger;

import java.io.IOException;
import java.io.InputStream;

/**
 * Link rewriter for SmartJournal platform pages served via ServeContent.
 *
 * Wraps the default NodeFilterHtmlLinkRewriterFactory (which performs the
 * standard relative-URL-to-ServeContent rewriting) and additionally REMOVES
 * SRI integrity and crossorigin attributes from <link> and <script> tags
 * in the served HTML.
 *
 * WHY: pages carry integrity="sha384-..." / "sha512-..." attributes on CDN
 * assets (Bootstrap, Font Awesome). When LOCKSS serves those assets from its
 * cache, the browser recomputes the hash, gets a different value than the
 * original CDN hash, and REFUSES to load the file (SRI enforcement).
 * Removing the attributes at serve time lets the browser load LOCKSS-cached
 * assets without SRI checks.
 *
 * IMPLEMENTATION NOTE: attribute removal uses an HtmlTransform + NodeVisitor
 * (tag.removeAttribute), NOT StringFilter. LOCKSS 1.0 StringFilter performs
 * LITERAL string replacement only — regex patterns like [^"]* are never
 * matched. The boolean flag in makeNestedFilter is ignoreCase, not regex mode.
 *
 * NOTE: the crawl filter cannot fix this either — in LOCKSS 1.0 the crawl
 * filter only affects link extraction, NOT stored content. Stored HTML is the
 * raw fetched content; serve-time modification must happen here.
 */
public class SulaimaniDentalJournalsHtmlLinkRewriterFactory
        implements LinkRewriterFactory {

    private static final Logger log =
            Logger.getLogger(SulaimaniDentalJournalsHtmlLinkRewriterFactory.class);

    @Override
    public InputStream createLinkRewriter(String mimeType,
                                          ArchivalUnit au,
                                          InputStream in,
                                          String encoding,
                                          String srcUrl,
                                          ServletUtil.LinkTransform xform)
            throws PluginException, IOException {

        // First run the standard LOCKSS URL rewriting (relative URLs ->
        // ServeContent?url=... form)
        NodeFilterHtmlLinkRewriterFactory fact =
                new NodeFilterHtmlLinkRewriterFactory();
        InputStream rewritten =
                fact.createLinkRewriter(mimeType, au, in, encoding, srcUrl, xform);

        // Then remove SRI integrity/crossorigin attributes via node-level
        // attribute removal. Confirmed present on: bootstrap CSS/JS (sha384),
        // font-awesome CSS (sha512).
        HtmlTransform stripSriAttributes = new HtmlTransform() {
            @Override
            public NodeList transform(NodeList nodeList) throws IOException {
                try {
                    nodeList.visitAllNodesWith(new NodeVisitor() {
                        @Override
                        public void visitTag(Tag tag) {
                            if (tag.getAttribute("integrity") != null) {
                                tag.removeAttribute("integrity");
                            }
                            if (tag.getAttribute("crossorigin") != null) {
                                tag.removeAttribute("crossorigin");
                            }
                        }
                    });
                } catch (ParserException pe) {
                    log.debug2("ParserException while stripping SRI attributes", pe);
                    throw new IOException(pe);
                }
                return nodeList;
            }
        };

        return new HtmlFilterInputStream(rewritten, encoding, encoding,
                stripSriAttributes);
    }
}
