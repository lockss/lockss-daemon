/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.springer.link;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.filters.*;
import org.htmlparser.tags.BodyTag;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.StringFilter;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;
import org.lockss.filter.html.HtmlTags.Section;
import org.lockss.plugin.*;
import org.lockss.util.ReaderInputStream;

public class SpringerLinkBooksHtmlHashFilterFactory implements FilterFactory {
    /**
     * TODO - remove after 1.70 when the daemon recognizes this as an html composite tag
     */

    private static final Pattern IMPACT_PATTERN = Pattern.compile("impact factor", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    public static class MyButtonTag extends CompositeTag {
        private static final String[] mIds = new String[] {"button"};
        public String[] getIds() { return mIds; }
    }

    private static final NodeFilter[] filters = new NodeFilter[] {
            HtmlNodeFilters.tag("script"),
            HtmlNodeFilters.tag("noscript"),
            HtmlNodeFilters.tag("input"),
            HtmlNodeFilters.tag("head"),
            HtmlNodeFilters.tag("aside"),
            HtmlNodeFilters.tag("footer"),
            // filter out comments
            HtmlNodeFilters.comment(),
            // all meta and link tags - some have links with names that change
            HtmlNodeFilters.tag("meta"),
            HtmlNodeFilters.tag("link"),

            //google iframes with weird ids
            HtmlNodeFilters.tag("iframe"),

            //footer
            HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "footer"),

            //more links to pdf and article
            HtmlNodeFilters.tagWithAttribute("div", "class", "bar-dock"),

            //adds on the side
            HtmlNodeFilters.tagWithAttribute("div", "id", "leaderboard"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "skyscraper-ad"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "banner-advert"),
            HtmlNodeFilters.tagWithAttribute("div", "id", "doubleclick-ad"),

            //header and search box
            HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
            HtmlNodeFilters.tagWithAttribute("div", "role", "banner"),

            //non essentials like metrics and related links
            HtmlNodeFilters.tagWithAttribute("ul", "id", "book-metrics"),
            HtmlNodeFilters.tagWithAttributeRegex("span", "class", "article-metrics"),
            HtmlNodeFilters.tagWithAttribute("div", "role", "complementary"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "col-aside"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "document-aside"),

            //random divs floating around
            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "uptodate-recommendations"),
            HtmlNodeFilters.tagWithAttributeRegex("h2", "class", "uptodate-recommendations"),
            HtmlNodeFilters.tagWithAttribute("div", "id", "MathJax_Message"),
            HtmlNodeFilters.tagWithAttribute("div", "id", "web-trekk-abstract"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "look-inside-interrupt"),
            HtmlNodeFilters.tagWithAttribute("div", "id", "colorbox"),
            HtmlNodeFilters.tagWithAttribute("div", "id", "cboxOverlay"),
            HtmlNodeFilters.tagWithAttribute("div", "id", "gimme-satisfaction"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "crossmark-tooltip"),
            HtmlNodeFilters.tagWithAttribute("div", "id", "crossMark"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "banner"),

            HtmlNodeFilters.tagWithAttribute("p", "class", "skip-to-links"),

            // button - let's get rid of all of them...
            HtmlNodeFilters.tag("button"),
     /*class="StickySideButton_left StickySideButton_left--feedback"*/

            HtmlNodeFilters.allExceptSubtree(HtmlNodeFilters.tag("div"),
                    new OrFilter(HtmlNodeFilters.tag("section"),
                            HtmlNodeFilters.tag("p"))),

            new NodeFilter() {
                @Override public boolean accept(Node node) {
                    if (!(node instanceof Section)) return false;
                    if (!("features".equals(((CompositeTag)node).getAttribute("class")))) return false;
                    String allText = ((CompositeTag)node).toPlainTextString();
                    //using regex for case insensitive match on "Impact factor"
                    // the "i" is for case insensitivity; the "s" is for accepting newlines
                    return IMPACT_PATTERN.matcher(allText).matches();
                }
            }
    };

    HtmlTransform xform = new HtmlTransform() {
        @Override
        public NodeList transform(NodeList nodeList) throws IOException {
            try {
                nodeList.visitAllNodesWith(new NodeVisitor() {
                    @Override
                    // the <body '<body class="company XYZ" data-name="XYZ"'>
                    // tag has changed to XYZ from abc- pruning those attributes
                    //the "rel" attribute on link tags are using variable named values
                    public void visitTag(Tag tag) {
                        if (tag instanceof BodyTag && tag.getAttribute("class") != null) {
                            tag.removeAttribute("class");
                        }
                        if (tag instanceof BodyTag && tag.getAttribute("data-name") != null) {
                            tag.removeAttribute("data-name");
                        }
                    }
                });
            } catch (ParserException pe) {
                IOException ioe = new IOException();
                ioe.initCause(pe);
                throw ioe;
            }
            return nodeList;
        }
    };


    public InputStream createFilteredInputStream(ArchivalUnit au,
                                                 InputStream in, String encoding) {

        //HtmlFilterInputStream filteredStream = new HtmlFilterInputStream(in, encoding,
        //  HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
        HtmlFilterInputStream filteredStream = new HtmlFilterInputStream(in, encoding,
                new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(filters)), xform));
        filteredStream.registerTag(new MyButtonTag());
        Reader filteredReader = FilterUtil.getReader(filteredStream, encoding);
        Reader httpFilter = new StringFilter(filteredReader, "http:", "https:");

        // Remove white space
        return new ReaderInputStream(new WhiteSpaceFilter(httpFilter));
    }

}
