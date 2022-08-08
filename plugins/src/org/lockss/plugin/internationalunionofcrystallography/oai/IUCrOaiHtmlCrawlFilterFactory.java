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

package org.lockss.plugin.internationalunionofcrystallography.oai;

import java.io.InputStream;

import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.htmlparser.tags.*;
import org.htmlparser.util.SimpleNodeIterator;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class IUCrOaiHtmlCrawlFilterFactory implements FilterFactory {

  private static final Logger log = Logger.getLogger(IUCrOaiHtmlCrawlFilterFactory.class);

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    NodeFilter[] filters = new NodeFilter[] {
     //filter out script
     HtmlNodeFilters.tag("p"),
     
     /*
      * Links to the previous and next articles (which can be in the previous
      * or next AU).
      */
     HtmlNodeFilters.tagWithAttribute("a", "title", "Previous article"),
     HtmlNodeFilters.tagWithAttribute("a", "title", "Next article"),
     
     /*
      * At one point, there were <a> tags with the 'title' attribute
      * "Previoous article" or "Next article". But as of 12/2020, on article
      * pages (e.g.
      * https://journals.iucr.org/e/issues/2015/02/00/wm5120/index.html),
      * there is the arrow icon has this title but not its encompassing <a>
      * tag:
      * 
      * <a href="/e/issues/2015/02/00/wm5117/">
      *   <img alt="" class="art_icon" style="top: 328px; left: 0px;" id="art_leftbox_wm5120_vertical_previousarticle" src="/logos/buttonlogos/previous_art.png" title="Previous article">
      * </a>
      * 
      * Select <a> tags with an <img> child that has the correct 'title'
      * attribute.
      */
     new NodeFilter() {
       @Override
       public boolean accept(Node node) {
         if (node instanceof LinkTag) {
           LinkTag linkTag = (LinkTag)node;
           SimpleNodeIterator iter = linkTag.children();
           while (iter.hasMoreNodes()) {
             Node child = iter.nextNode();
             if (child instanceof ImageTag) {
               ImageTag img = (ImageTag)child;
               String title = img.getAttribute("title");
               if ("Previous article".equals(title) || "Next article".equals(title)) {
                 return true;
               }
             }
           }
         }
         return false;
      }
     },
    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }

}
