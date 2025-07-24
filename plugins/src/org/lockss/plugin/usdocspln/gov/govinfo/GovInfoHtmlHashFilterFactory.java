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

package org.lockss.plugin.usdocspln.gov.govinfo;

import java.io.InputStream;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.ReaderInputStream;

public class GovInfoHtmlHashFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        /*
         * High probability of churn: comments, <style> and <script> tags.
         */
        HtmlNodeFilters.comment(),
        HtmlNodeFilters.tag("style"),
        HtmlNodeFilters.tag("script"),
        
        /*
         * ID and timestamp injected by the Prerender service, e.g.:
         * 
         * https://www.govinfo.gov/app/details/BUDGET-2023-APP/BUDGET-2023-APP-1-1

<meta rel="x-prerender-render-id" content="5fe4ae94-ce33-4e2a-a783-d7a32e107390" />
<meta rel="x-prerender-render-at" content="2023-06-06T20:53:01.004Z" />

         * vs.

<meta rel="x-prerender-render-id" content="7ae17b42-264a-4ae9-80a5-413ae0bbc4b5" />
<meta rel="x-prerender-render-at" content="2023-06-05T02:59:25.940Z" />

         */
        HtmlNodeFilters.tagWithAttribute("meta", "rel", "x-prerender-render-id"),
        HtmlNodeFilters.tagWithAttribute("meta", "rel", "x-prerender-render-at"),
    
        /*
         * "Share by e-mail" (and similar) have had a variety of tracking identifiers:
         * 
         * - At one point, a <div> with the 'id' attribute set to email-share-search
         * - https://www.govinfo.gov/content/pkg/LSA-2022-01/html/LSA-2022-01-pg1.htm 02:55:30 06/20/23 has:

<a href="/cdn-cgi/l/email-protection" class="__cf_email__" data-cfemail="452320213720226b2c2b232a052b2437246b222a336b">[email&#160;protected]</a>

         * Keep these cumulatively.
         */
        HtmlNodeFilters.tagWithAttribute("a", "id", "email-share-search"),
        HtmlNodeFilters.tagWithAttribute("a", "class", "__cf_email__"),
        HtmlNodeFilters.tagWithAttribute("a", "data-cfemail"),
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "email-protection"),
        // A few things in this <div> started having random numeric suffixes in 'id' attributes, ostensibly to support toggling and similar dynamic behaviors
        HtmlNodeFilters.tagWithAttribute("div", "id", "contentdetaildocinContextview"),

        new AndFilter(HtmlNodeFilters.tagWithAttribute("div", "class", "row"), new NodeFilter(){
          @Override
          public boolean accept(Node node){
            if(!(node instanceof Tag)){
              return false;
            }
            class MyVisitor extends NodeVisitor {
              private boolean accepted = false;
              private boolean nested = false;

              @Override
              public void visitTag(Tag tag){
                if(HtmlNodeFilters.tagWithAttributeRegex("div", "class", "\\bmessage-current-flag\\b").accept(tag)){
                  accepted = true;
                }
                if(HtmlNodeFilters.tagWithAttribute("div", "class", "row").accept(tag)){
                  nested = true;
                }
                super.visitTag(tag);
              }
            };
            MyVisitor visitor = new MyVisitor();
            try {
              node.getChildren().visitAllNodesWith(visitor);
              return (!visitor.nested && visitor.accepted);
            } catch (ParserException e) {
              // TODO Auto-generated catch block
              return false;
            }
          }
        })
    };



    HtmlFilterInputStream htmlFiltered = new HtmlFilterInputStream(in,
                                             encoding,
                                             HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    return new ReaderInputStream(new WhiteSpaceFilter(FilterUtil.getReader(htmlFiltered, encoding)));
  }
  
}
