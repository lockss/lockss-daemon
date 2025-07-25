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

package org.lockss.plugin.ubiquitypress.upn;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.filters.*;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.daemon.PluginException;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.StringFilter;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

public class UbiquityPartnerNetworkHtmlFilterFactory implements FilterFactory {
  
  private static final Logger log = Logger.getLogger(UbiquityPartnerNetworkHtmlFilterFactory.class);

  protected static Pattern PNG_WITH_TIMESTAMP = UbiquityPartnerNetworkUrlNormalizer.PNG_WITH_TIMESTAMP;
  protected static Pattern JPG_WITH_TIMESTAMP_AND_WIDTH = UbiquityPartnerNetworkUrlNormalizer.JPG_WITH_TIMESTAMP_AND_WIDTH;
  protected static Pattern JPG_WITH_TIMESTAMP = UbiquityPartnerNetworkUrlNormalizer.JPG_WITH_TIMESTAMP; 

  /**
   * A B(old) tag.  Registered with PrototypicalNodeFactory to cause B
   * to be a CompositeTag.  See code samples in org.htmlparser.tags.
   * @see HtmlFilterInputStream#makeParser()
   */
  public static class bTag extends CompositeTag {
    
    /**
     * The set of names handled by this tag.
     */
    private static final String[] mIds = new String[] {"b"};
    
    /**
     * Return the set of names handled by this tag.
     * @return The names to be matched that create tags of this type.
     */
    @Override
    public String[] getIds() {
      return mIds;
    }
    
  }
  
  protected static NodeFilter[] baseFilters = new NodeFilter[] {
    // Boilerplate
    HtmlNodeFilters.comment(),
    // the order of <meta> tags changes capriciously, which slows down testing
    HtmlNodeFilters.tag("head"),
    HtmlNodeFilters.tag("style"),
    // Site customizations often involve Javascript (e.g. Google Analytics), which can change over time
    HtmlNodeFilters.tag("script"),
    HtmlNodeFilters.tag("noscript"),
    HtmlNodeFilters.tag("input"),
    // No need to hash these tags in OJS sites, not content and things change
    HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
    // HtmlNodeFilters.tagWithAttribute("div", "id", "sidebar"), sidebar filter below removes this tag
    HtmlNodeFilters.tagWithAttributeRegex("div", "id", "commentsOnArticle"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "id", "citation-block"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "id", "sidebar", true),
    HtmlNodeFilters.tagWithAttribute("div", "id", "alm"),
    HtmlNodeFilters.tagWithAttribute("div", "class", "separator"),
    HtmlNodeFilters.tagWithTextRegex("span", "Metrics powered by.+PLOS ALM", true),
    HtmlNodeFilters.tag("br"),
    // <div id="articleSubject">
    // Some OJS sites have a tag cloud, sidebar filter above removes this tag
    // HtmlNodeFilters.tagWithAttribute("div", "id", "sidebarKeywordCloud"),
    // Some OJS sites have a subscription status area
    HtmlNodeFilters.tagWithAttributeRegex("div", "id", "Subscription"),
    // Some OJS sites have a language switcher, which can change over time, sidebar filter above removes this tag
    // HtmlNodeFilters.tagWithAttribute("div", "id", "sidebarLanguageToggle"),
    // Top-level menu items sometimes change over time
    HtmlNodeFilters.tagWithAttribute("div", "id", "navbar"),
    // Popular location for sidebar customizations
    HtmlNodeFilters.tagWithAttribute("div", "id", "custom"),
    // Date accessed is a variable
    HtmlNodeFilters.tagWithTextRegex("div", "Date accessed: "),
    // The version of the OJS software, which can change over time, appears in a tag
    HtmlNodeFilters.tagWithAttribute("meta", "name", "generator"),
    // Header image with variable dimensions
    HtmlNodeFilters.tagWithAttribute("div", "id", "headerTitle"),
    // For Ubiquity Press, sidebar filter above removes this tag
    // HtmlNodeFilters.tagWithAttribute("div", "id", "rightSidebar"),
    // For JLIS.it: landing pages contain user view count
    HtmlNodeFilters.tagWithAttribute("span", "class", "ArticleViews"),
    // For ibictpln: PHP Query Profiler
    // e.g. http://seer.bce.unb.br/index.php/Musica/article/view/915
    HtmlNodeFilters.tagWithAttribute("div", "id", "pqp-container"),
    // Total de acessos: keeps changing, there is no 'good' tag wrapped around text
    // e.g. https://www.revistas.unijui.edu.br/index.php/desenvolvimentoemquestao/issue/view/18
    HtmlNodeFilters.tagWithTextRegex("b", "^ *total de acesso( dos artigo)?s: +[0-9]+ *$", true),
    // Footer contains changing non-content, debug etc.
    // http://www.portalseer.ufba.br/index.php/cmbio/article/viewArticle/4093
    HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
    // For Librello: download and view counts
    // e.g. http://www.librelloph.com/challengesinsustainability/issue/view/10
    HtmlNodeFilters.tagWithTextRegex("a", "^(HTML|PDF|Views)$", true),
    new AndFilter(
        HtmlNodeFilters.tagWithAttribute("span", "class", "badge"),
        HtmlNodeFilters.tagWithTextRegex("span", "^[0-9]*$")),
    // EU-style cookies disclosure banner
    HtmlNodeFilters.tagWithAttribute("div", "id", "cookiesAlert"), // http://ojs.statsbiblioteket.dk/
    //Stats
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "stat-"),
    HtmlNodeFilters.tagWithAttribute("div", "class", "article-stats"),
   // HtmlNodeFilters.tagWithAttribute("section", "aria-label","Brand navigation"),
    // Intermittent appearance of empty <div> right after the <body> tag, seen at e.g. https://jachs.org/articles/10.22599/jachs.102
    HtmlNodeFilters.tagWithAttribute("div", "hidden"),
  };

  //some tags reference image tags that have timestamps so let's remove those
    protected final HtmlTransform xForm_removeTimestamp = new HtmlTransform() {
    @Override
    public NodeList transform(NodeList nodeList) throws IOException {
      try {
        nodeList.visitAllNodesWith(new NodeVisitor() {
          @Override
          public void visitTag(Tag tag) {
            if (tag instanceof ImageTag){
              if(tag.getAttribute("src") != null){
                String srcurl = tag.getAttribute("src");
                log.debug3("the source of image tag is " + tag.getAttribute("src"));
                if(PNG_WITH_TIMESTAMP.matcher(srcurl).find()){
                  String newUrl = PNG_WITH_TIMESTAMP.matcher(srcurl).replaceFirst(".png");
                  tag.setAttribute("src", newUrl);
                }else if(JPG_WITH_TIMESTAMP_AND_WIDTH.matcher(srcurl).find()){
                  String newUrl = JPG_WITH_TIMESTAMP_AND_WIDTH.matcher(srcurl).replaceFirst(".jpg?&w=");
                  tag.setAttribute("src", newUrl);
                }else if(JPG_WITH_TIMESTAMP.matcher(srcurl).find()){
                  String newUrl = JPG_WITH_TIMESTAMP.matcher(srcurl).replaceFirst(".jpg");
                  tag.setAttribute("src", newUrl);
                }
                log.debug3("the NEW source of image tag is " + tag.getAttribute("src"));
              }
              if(tag.getAttribute("srcSet") != null){
                log.debug3("the sourceSet of image tag is " + tag.getAttribute("srcSet"));
                String srcSetUrl = tag.getAttribute("srcSet");
                if(PNG_WITH_TIMESTAMP.matcher(srcSetUrl).find()){
                  String newUrl = PNG_WITH_TIMESTAMP.matcher(srcSetUrl).replaceAll(".png");
                  tag.setAttribute("srcSet", newUrl);
                }else if(JPG_WITH_TIMESTAMP_AND_WIDTH.matcher(srcSetUrl).find()){
                  String newUrl = JPG_WITH_TIMESTAMP_AND_WIDTH.matcher(srcSetUrl).replaceAll(".jpg?&w=");
                  tag.setAttribute("srcSet", newUrl);
                }else if(JPG_WITH_TIMESTAMP.matcher(srcSetUrl).find()){
                  String newUrl = JPG_WITH_TIMESTAMP.matcher(srcSetUrl).replaceAll(".jpg");
                  tag.setAttribute("srcSet", newUrl);
                }
                log.debug3("the NEW sourceSet of image tag is " + tag.getAttribute("srcSet"));
              }
            }
            //while we're here, let's remove ids and aria-labelledby since this is causing some hashing issues
            tag.removeAttribute("id");
            tag.removeAttribute("aria-labelledby");
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
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
     
    return doFiltering(in, encoding, null);
  }
  
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding,
                                               NodeFilter[] moreNodes)
      throws PluginException {
    
    return doFiltering(in, encoding, moreNodes);
  }
  
  /* the shared portion of the filtering
   * pick up the extra nodes from the child if there are any
   */
  protected InputStream doFiltering(InputStream in, String encoding, NodeFilter[] moreNodes) {
    NodeFilter[] filters = baseFilters;
    if (moreNodes != null) {
      filters = addTo(moreNodes);
    }
    
    HtmlFilterInputStream filteredStream = new HtmlFilterInputStream(in, encoding,
        new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(filters)), xForm_removeTimestamp));
    filteredStream.registerTag(new bTag());
    Reader filteredReader = FilterUtil.getReader(filteredStream, encoding);
    Reader httpFilter = new StringFilter(filteredReader, "http:", "https:");
    return new ReaderInputStream(new WhiteSpaceFilter(httpFilter));
  }
  
  /** Create an array of NodeFilters that combines the parent with the given array
   *  @param nodes The array of NodeFilters to add
   */
  protected NodeFilter[] addTo(NodeFilter[] nodes) {
    NodeFilter[] result  = Arrays.copyOf(baseFilters, baseFilters.length + nodes.length);
    System.arraycopy(nodes, 0, result, baseFilters.length, nodes.length);
    return result;
  }
  
}
