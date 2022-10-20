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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.Text;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.*;
import org.htmlparser.util.NodeList;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
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
  // [citado 21 de mayo de 2022]
  // [citado 17 de octubre de 2022]
  public static final Pattern ESPANOL_VANCOUVER_CIT_PATTERN =
      Pattern.compile("\\[citado \\d\\d?\\s?([a-z]+\\s?)+\\d\\d\\d\\d\\s?]", Pattern.CASE_INSENSITIVE);
  // Accessed May 18, 2022.
  public static final Pattern TURABIAN_CIT_PATTERN =
      Pattern.compile("Accessed [a-z]+\\.? \\d\\d?, \\d\\d\\d\\d", Pattern.CASE_INSENSITIVE);
  //(Accessed: 20May2022)
  public static final Pattern HARVARD_CIT_PATTERN =
      Pattern.compile("\\(Accessed: \\d\\d?\\s?[a-z]+\\.?\\s?\\d\\d\\d\\d\\s?\\)", Pattern.CASE_INSENSITIVE);
  // Acesso em: 19 may. 2022.
  public static final Pattern ASSOCIACAO_BRAZILEIRA_DE_NORMAS_TECNICAS_PATTERN =
      Pattern.compile("Acesso em: \\d\\d?\\s?[a-z]+\\.?\\s?\\d\\d\\d\\d", Pattern.CASE_INSENSITIVE);
  // Accedido mayo 21, 2022.
  public static final Pattern ESPANOL_CIT_PATTERN =
      Pattern.compile("Accedido [a-z]+\\s?\\d\\d?,\\s?\\d\\d\\d\\d\\.", Pattern.CASE_INSENSITIVE);

  private static final NodeFilter[] includeNodes = new NodeFilter[] {
    // some pages are full of scripts with no discernable content pre-browser building.
    // only thing to do is include the title tag. this may need to be removed if other pages title tags are disagreeing.
    HtmlNodeFilters.tag("title"),
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
    // content of article on article html view
    HtmlNodeFilters.tagWithAttribute("div", "class", "content col-md-9"),
    // article page, https://journals.vgtu.lt/index.php/BME/article/view/10292
    HtmlNodeFilters.tagWithAttribute("h2", "class", "headings"),
    HtmlNodeFilters.tagWithAttribute("div", "class", "authors"),
    HtmlNodeFilters.tagWithAttribute("div", "id", "article_tab"),
    // spanish language articles
    HtmlNodeFilters.tagWithAttribute("div", "id", "articulo"),
    // article page: https://www.clei.org/cleiej/index.php/cleiej/article/view/202
    HtmlNodeFilters.tagWithAttribute("div", "class", "main_entry"),
    // https://mapress.com/jib/article/view/2018.07.1.1
    HtmlNodeFilters.tagWithAttribute("div", "id", "jatsParserFullText"),
    // article page: 	https://haematologica.org/article/view/10276
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-container"),
    HtmlNodeFilters.tagWithAttribute("div", "class", "panel content document"),
    // issue page: https://haematologica.org/issue/view/377
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "one-article-intoc"),
    // https://www.mhgcj.org/index.php/MHGCJ/issue/view/14/15
    // https://www.mhgcj.org/index.php/MHGCJ/article/view/140/133
    HtmlNodeFilters.tagWithAttributeRegex("div", "data-page-no", "\\d+"),

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
      /*
      the print html pages are have basic html elements. these patterns should id most of them.
       */
      new NodeFilter() {
        @Override public boolean accept(Node node) {
          if (node instanceof BodyTag) {
            Node firstChild = node.getFirstChild();
            if (firstChild instanceof Text) {
              if (firstChild.toPlainTextString().matches("\\s*\\[?1](\\s|.)*")) {
                return true;
              }
            }
            Node firstTag = getNextTag(firstChild);
            if (firstTag instanceof Tag) {
              if (((Tag) firstTag).getTagName().equals("BLOCKQUOTE")) {
                // https://journals.uic.edu/ojs/index.php/fm/article/download/10812/10591?inline=1
                return true;
              } else if (hasFontTag(firstTag)) {
                // https://revistas.udea.edu.co/index.php/lecturasdeeconomia/article/download/14773/17899?inline=1
                // https://revistas.udea.edu.co/index.php/lecturasdeeconomia/article/download/7874/18139?inline=1
                return true;
              } else if (firstTag instanceof Div) {
                Node nextChild = getNextTag(firstTag.getNextSibling());
                if (nextChild instanceof ParagraphTag) {
                  String pClass = ((ParagraphTag) nextChild).getAttribute("class");
                  if (pClass.matches("c\\d+.*")) {
                    // various body.class="c\d\d" with classless div, followed by many p.class="c\d\d? ?c?\d?\d?"
                    // https://ejournals.library.vanderbilt.edu/index.php/ameriquests/article/download/5200/3054?inline=1
                    // https://ejournals.library.vanderbilt.edu/index.php/ameriquests/article/download/5203/3053?inline=1
                    return true;
                  }
                } else if (nextChild instanceof Div) {
                  log.info("two divs in a row!");
                  String d1id = ((Div) firstTag).getAttribute("id");
                  String d2id = ((Div) nextChild).getAttribute("id");
                  if (d1id != null &&
                      d2id != null &&
                      d1id.matches("article(-level-)?\\d.*") &&
                      d2id.matches("article(-level-)?\\d.*")) {
                    // https://revistas.udea.edu.co/index.php/lecturasdeeconomia/article/download/344224/20808299?inline=1
                    // https://journals.uic.edu/ojs/index.php/jbdc/article/download/2574/2401?inline=1
                    return true;
                  }
                }

              }
            }
          }
          return false;
        }
      },
      /*
      The
       */
      new NodeFilter() {
        @Override
        public boolean accept(Node node) {
          if (node instanceof Span) {
            String titleAttr = ((Tag) node).getAttribute("title");
            if (titleAttr != null && titleAttr.contains("rft_id")) {
              NodeList nl = new NodeList();
              Text titleText = new TextNode(titleAttr);
              nl.add(titleText);
              node.setChildren(nl);
              return true;
            }
          }
          return false;
        }
      },
  };

  public static Node getNextTag(Node child) {
    while (child instanceof Text) {
      child = child.getNextSibling();
      if (child instanceof Tag) {
        return child;
      }
    }
    return child;
  }
  /*
  Looks for <font> tag nodes that may be embedded within <p> tags and preceded by <b> tags.
  e.g. <font></font>, <p><b><font></font></b></p>, <b><p><font></font></p></b> all return true.
   */
  public static boolean hasFontTag(Node child) {
    if (child instanceof HtmlTags.Font) {
      return true;
    } else if (child instanceof ParagraphTag) {
      return hasFontTag(getNextTag(child.getFirstChild()));
    } else if (child instanceof Tag && ((Tag) child).getTagName().equals("B")) {
      return hasFontTag(getNextTag(child.getNextSibling()));
    }
    return false;
  }

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
    citPats.add(ESPANOL_VANCOUVER_CIT_PATTERN);
    citPats.add(ASSOCIACAO_BRAZILEIRA_DE_NORMAS_TECNICAS_PATTERN);
    citPats.add(HARVARD_CIT_PATTERN);
    citPats.add(TURABIAN_CIT_PATTERN);
    citPats.add(ESPANOL_CIT_PATTERN);
    Matcher mat;
    for (Pattern citPat : citPats) {
      mat = citPat.matcher(allText);
      if (mat.find()) {
        // remove the identified Date Accessed bit, and accept the node
        node.setText(mat.replaceAll(""));
      }
    }
  }

  private static final NodeFilter[] excludeNodes = new NodeFilter[] {
      // on the article landing page - remove the bottom stuff
      HtmlNodeFilters.tagWithAttribute("section","class","article-more-details"),
      HtmlNodeFilters.tag("script"),
      HtmlNodeFilters.tag("noscript"),
      // powered by ... bit
      HtmlNodeFilters.tagWithAttribute("div", "id", "creditos"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "downloads_chart"),
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article_statistic"),
      //
      HtmlNodeFilters.tagWithAttributeRegex("a", "href", "email-protection"),
      /*
      want to remove this whole bit from this url https://revistas.udea.edu.co/index.php/lecturasdeeconomia/article/view/342002
      <div style="padding-left: 4%;">
      |Resumen <div class="fa fa-eye"></div> = <b>5729</b> veces
       |
      PDF <div class="fa fa-eye"></div> = <b>2414</b> veces|
       |
      XML <div class="fa fa-eye"></div> = <b>306</b> veces|
       |
      HTML <div class="fa fa-eye"></div> = <b>42</b> veces|
      </div>
       */
      new NodeFilter() {
        @Override public boolean accept(Node node) {

          if (node instanceof Div) {
            String classattr = ((Div) node).getAttribute("class");
            if (classattr == null) {
              Node[] children = ((Div) node).getChildrenAsNodeArray();
              for (Node child: children) {
                if (child instanceof Div) {
                  String childClass = ((Div) child).getAttribute("class");
                  if (childClass != null && childClass.contains("eye")) {
                    return true;
                  }
                }
              }
            }
          }
          return false;
        }
      },
  };

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
