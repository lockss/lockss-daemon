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

package org.lockss.plugin.ojs3;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import org.apache.commons.io.IOUtils;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.Text;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.*;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

/*
 * SO far we only have two publishers who have migrated to OJS3 and the html is basic and
 * very similar 
 */

public class Ojs3HtmlHashFilterFactory implements FilterFactory {

  private static final Logger log = Logger.getLogger(Ojs3HtmlHashFilterFactory.class);

  private static final String CIT_ENTRY_CLASS = "csl-entry";
  private static final String CIT_RIGHT_INLINE_CLASS = "csl-right-inline";
  // [cited 2022May18]
  public static final Pattern VANCOUVER_CIT_PATTERN =
      Pattern.compile("\\[cited \\d\\d\\d\\d\\s?[a-z]+\\.?\\s?\\d\\d?\\s?]", Pattern.CASE_INSENSITIVE);
  // Accessed May 18, 2022.
  public static final Pattern TURABIAN_CIT_PATTERN =
      Pattern.compile("Accessed [a-z]+\\.? \\d\\d?, \\d\\d\\d\\d", Pattern.CASE_INSENSITIVE);
  //(Accessed: 20May2022)
  public static final Pattern HARVARD_CIT_PATTERN =
      Pattern.compile("\\(Accessed: \\d\\d?\\s?[a-z]+\\.?\\s?\\d\\d\\d\\d\\s?\\)", Pattern.CASE_INSENSITIVE);
  // Acesso em: 19 may. 2022.
  public static final Pattern ASSOCIACAO_BRAZILEIRA_DE_NORMAS_TECNICAS_PATTERN =
      Pattern.compile("Acesso em: \\d\\d?\\s?[a-z]+\\.?\\s?\\d\\d\\d\\d", Pattern.CASE_INSENSITIVE);

  private static final NodeFilter[] includeNodes = new NodeFilter[] {
    // manifest page
    HtmlNodeFilters.tagWithAttributeRegex("div",  "class", "page clockss"),
    HtmlNodeFilters.tagWithAttributeRegex("div",  "class", "page lockss"),
    // toc - contents only
    HtmlNodeFilters.tagWithAttribute("div", "class", "issue-toc"),
    HtmlNodeFilters.tagWithAttribute("div", "class", "obj_issue_toc"),
    // abstract landing page html
    HtmlNodeFilters.tagWithAttribute("article", "class", "article-details"),
    // pdf landing page - just get the header with title and link information
    HtmlNodeFilters.tagWithAttribute("header", "class", "header_view"),
    // article page, https://journals.vgtu.lt/index.php/BME/article/view/10292
    HtmlNodeFilters.tagWithAttribute("h2", "class", "headings"),
    HtmlNodeFilters.tagWithAttribute("div", "class", "authors"),
    HtmlNodeFilters.tagWithAttribute("div", "id", "article_tab"),
    // article page: https://www.clei.org/cleiej/index.php/cleiej/article/view/202
    HtmlNodeFilters.tagWithAttribute("div", "class", "main_entry"),
    // https://mapress.com/jib/article/view/2018.07.1.1
    HtmlNodeFilters.tagWithAttribute("div", "id", "jatsParserFullText"),
    // article page: 	https://haematologica.org/article/view/10276
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-container"),
    // issue page: https://haematologica.org/issue/view/377
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "one-article-intoc"),

    // the citation files https://ojs.aut.ac.nz/hospitality-insights/citationstylelanguage/get/acm-sig-proceedings?submissionId=58
    //HtmlNodeFilters.tagWithAttribute("div", "class", "csl-entry"), // alternatively div.csl-bib-body
    /*
     * Custom node filter which first accepts and div.csl-entry nodes.
     * additionally, it searches children nodes of this div for common citation date accessed patterns
     * and removes them.
     */
    new NodeFilter() {
      @Override public boolean accept(Node node) {

        if (node instanceof Div) {
          String className = ((Div) node).getAttribute("class");
          if (className != null && className.equals(CIT_ENTRY_CLASS)) {
            recurseCslNodes(node);
            // if no transform was made, still accept the node
            return true;
          }
        }
        return false;
      }
    },
  };

  private static void recurseCslNodes(Node node) {
    for (Node child : ((Div) node).getChildrenAsNodeArray()) {
      checkText(child);
      if (child instanceof Div) {
        recurseCslNodes(child);
      }
    }
  }

  private static void checkText(Node node) {
    String allText = node.getText();
    ArrayList<Pattern> citPats = new ArrayList<>();
    citPats.add(VANCOUVER_CIT_PATTERN);
    citPats.add(ASSOCIACAO_BRAZILEIRA_DE_NORMAS_TECNICAS_PATTERN);
    citPats.add(HARVARD_CIT_PATTERN);
    citPats.add(TURABIAN_CIT_PATTERN);
    Matcher mat;
    for (Pattern citPat : citPats) {
      mat = citPat.matcher(allText);
      if (mat.find()) {
        // remove the identified Date Accessed bit, and accept the node
        node.setText(mat.replaceAll(""));
      }
    }
  }

  private static final NodeFilter[] includeNodes2 = new NodeFilter[] {};

  private static final NodeFilter[] excludeNodes = new NodeFilter[] {
      // on the article landing page - remove the bottom stuff
      HtmlNodeFilters.tagWithAttribute("section","class","article-more-details"),
      HtmlNodeFilters.tag("script"),
      HtmlNodeFilters.tag("noscript"),
  };

  private static final NodeFilter[] excludeNodes2 = new NodeFilter[] {};
 
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    InputStream htmlFiltered = new HtmlFilterInputStream(
      in,
      encoding,
      new HtmlCompoundTransform(
        HtmlNodeFilterTransform.include(new OrFilter(includeNodes)),
        HtmlNodeFilterTransform.exclude(new OrFilter(excludeNodes))
      )
    );
    // replace any multiple whitespaces with a single whitespace.
    return new ReaderInputStream(new WhiteSpaceFilter(new InputStreamReader(htmlFiltered)));
  }

}
