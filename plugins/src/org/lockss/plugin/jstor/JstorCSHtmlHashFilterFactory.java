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

package org.lockss.plugin.jstor;

import java.io.InputStream;
import java.io.Reader;
import java.util.regex.Pattern;

//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import org.apache.commons.io.IOUtils;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.BulletList;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.HeadingTag;
import org.htmlparser.tags.ScriptTag;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.HtmlTagFilter;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.StringFilter;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.HtmlCompoundTransform;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

public class JstorCSHtmlHashFilterFactory implements FilterFactory {

  private static final Logger log = Logger.getLogger(JstorCSHtmlHashFilterFactory.class);

  private static final Pattern MANIFEST_TITLE_PATTERN =
      Pattern.compile("(Cl|L)ockss App Manifest", Pattern.CASE_INSENSITIVE);


  /*
   * Some AUs only get TOC and pdf
   * But some provide full text (see chaucer review)
   * 
   */	  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, 
      String encoding) {
    NodeFilter[] includeNodes = new NodeFilter[] {
        //manifest page - very basic page
        //<h1> with manifest page title followed by a 
        //<ul> of links
        //Find the UL that has a parent that is an H1 with the correct pattern
        new NodeFilter() {
          @Override
          public boolean accept(Node node) {
            if (node instanceof BulletList) {
              Node prev_sib = node.getPreviousSibling();
              while (prev_sib != null && (prev_sib instanceof TextNode)) {
                prev_sib = prev_sib.getPreviousSibling();
              }
              if (prev_sib != null && 
                  prev_sib instanceof HeadingTag) {
                String allText = ((CompositeTag)prev_sib).toPlainTextString();
                return MANIFEST_TITLE_PATTERN.matcher(allText).find();
              }
            } else if (node instanceof ScriptTag) {
              ScriptTag st = (ScriptTag) node;
              String stt = st.getStringText();
              if (stt.contains("fullTextRendition")) {
                int x = stt.indexOf("fullTextRendition");
                int i = stt.indexOf("<article", x);
                int j = stt.indexOf("</article>", x);
                int k = stt.indexOf("<div class=\\\"back\\\">", x);
                j = (k != -1) ? k : j+10;
                stt = stt.substring(i, j);
                stt = stt.replace("\\n", " ");
                //stt = stt.replace("\\\"", "\"");
                st.setScriptCode(stt);
                return true;
              }
            }
            return false;
          }
        },

        // toc - contents only
        HtmlNodeFilters.tagWithAttribute("div", "class", "toc-view"),
        // citation/info
        HtmlNodeFilters.tagWithAttribute("div", "id", "citationBody"),
        // for those papers that have full text html
        HtmlNodeFilters.tagWithAttribute("div", "id", "full_text_tab_contents"),
        // must pick small portion of journal_info or it will hash0 - parts of it are ever changing
        // http://www.jstor.org/stable/10.5325/pennhistory.82.1.0001?item_view=journal_info
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "journal_description"),
    };

    NodeFilter[] excludeNodes = new NodeFilter[] {
        HtmlNodeFilters.tagWithAttribute("div","id","citation-tools"),
        // the span will change over time
        HtmlNodeFilters.tagWithAttribute("div","id","journal_info_drop"),
        //might change offerings and not pertinent content
        HtmlNodeFilters.tagWithAttribute("ul","id","export-bulk-drop"),

        // the value of data-issue-key is variable - just remove the associated tag
        HtmlNodeFilters.tagWithAttribute("div", "data-issue-key"),
        // additional filtering
        HtmlNodeFilters.tagWithAttribute("div", "class", "keywords"),
        HtmlNodeFilters.tagWithAttribute("section", "class", "references"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "back"),
    };
    return getFilteredInputStream(au, in, encoding,
        includeNodes, excludeNodes);
  }


  public InputStream getFilteredInputStream(ArchivalUnit au, InputStream in,
      String encoding, NodeFilter[] includeNodes, NodeFilter[] excludeNodes) {
    if (excludeNodes == null) {
      throw new NullPointerException("excludeNodes array is null");
    }  
    if (includeNodes == null) {
      throw new NullPointerException("includeNodes array is null!");
    }   

    HtmlCompoundTransform combinedFiltered = new HtmlCompoundTransform(
        HtmlNodeFilterTransform.include(new OrFilter(includeNodes)),
        HtmlNodeFilterTransform.exclude(new OrFilter(excludeNodes)));

    InputStream is = new HtmlFilterInputStream(in, encoding, combinedFiltered);
    String[][] strArray = new String[][] {
      // inconsistent use of nbsp v empty space - do this replacement first
      {"&nbsp;", " "},
      {"http:", "https:"},
      {"<", " <"},
    };
    Reader tagFilter = StringFilter.makeNestedFilter(FilterUtil.getReader(is, encoding), strArray, false);
    /*
     * additional processing -
     *    removal of all tags and removal of WS & https conversion
     */
    tagFilter = new HtmlTagFilter(tagFilter, new TagPair("<", ">"));

    return new ReaderInputStream(new WhiteSpaceFilter(tagFilter));
  }

  /*public static void main(String[] args) throws Exception {
    String[] files = new String[] {
        "/tmp/data/jstor0.html",
        //"/tmp/data/jstor2.html",
    };
    for (String file : files) {
      IOUtils.copy(new JstorCSHtmlHashFilterFactory().createFilteredInputStream(null, new FileInputStream(file), null),
          new FileOutputStream(file + ".out"));
    }
  }*/

}
