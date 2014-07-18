/*
 * $Id: IngentaJournalHtmlFilterFactory.java,v 1.29.2.1 2014-07-18 15:54:40 wkwilson Exp $
 */ 

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.plugin.ingenta;

import java.io.*;

import org.htmlparser.*;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.*;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class IngentaJournalHtmlFilterFactory implements FilterFactory {
  Logger log = Logger.getLogger(IngentaJournalHtmlFilterFactory.class);
  
  // XXX remove after 1.66 release
  private static class NavTag extends CompositeTag {
    
    /**
     * The set of names handled by this tag.
     */
    private static final String[] mIds = new String[] {"nav"};
    
    /**
     * Return the set of names handled by this tag.
     * @return The names to be matched that create tags of this type.
     */
    public String[] getIds() {
      return mIds;
    }
    
  }
  
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        // the meta tags (name="DC.subject") in head are not always in same order
        new TagNameFilter("head"),
        // Filter out noscript for Nomadic People
        // script tag filter did not work on some Nomadic People scripts, tag end found early
        new TagNameFilter("noscript"),
        // Filter out <div id="header">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
        // Filter out <div id="rightnavbar">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "rightnavbar"), 
        // Filter out <div id="footerarea">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "footerarea"),
        // Filter out pagers, that get new issues or articles appended
        HtmlNodeFilters.tagWithAttribute("p", "id", "pager"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "article-pager"),
        // Filter out <div id="purchaseexpand"...>...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "purchaseexpand"),
        // Filter out <div id="moredetails">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "moredetails"),
        // Filter out <div id="moreLikeThis">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "moreLikeThis"),
        // PD-1113 Hash Filter needed for shopping cart (filter entire nav tag)
        //  http://www.ingentaconnect.com/content/intellect/ac/2001/00000012/00000001
        new TagNameFilter("footer"),
        new TagNameFilter("nav"),
        // filter out <link rel="stylesheet" href="..."> because Ingenta has
        // bad habit of adding a version number to the CSS file name
        HtmlNodeFilters.tagWithAttribute("link", "rel", "stylesheet"),
        // filter out <div class="heading"> that encloses a statement with
        // the number of references and the number that can be referenced: 
        // number of reference links won't be the same because not all 
        // the referenced articles are available at a given institution.
        HtmlNodeFilters.tagWithAttribute("div", "class", "heading"),
        // filter out <div class="advertisingbanner[ clear]"> that encloses 
        // GA_googleFillSlot("TopLeaderboard") & GA_googleFillSlot("Horizontal_banner")
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "advertisingbanner"),
        // filter out <li class="data"> that encloses a reference for the
        // article: reference links won't be the same because not all 
        // the referenced articles are available at a given institution.
        HtmlNodeFilters.tagWithAttribute("li", "class", "data"),
        // Filter out <div id="subscribe-links" ...> 
        // institution-specific subscription link section
        HtmlNodeFilters.tagWithAttribute("div", "id", "subscribe-links"),
        // Filter out <div id="links">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "links"),
        // Filter out <div id="footer">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
        // Filter out <div id="top-ad-alignment">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "top-ad-alignment"),
        // Filter out <div id="top-ad">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "top-ad"),
        // Filter out <div id="ident">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "ident"),
        // Filter out <div id="ad">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "ad"),
        // Filter out <div id="vertical-ad">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "vertical-ad"),
        // Filter out <div class="right-col-download">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "class", "right-col-download"),
        // Filter out <div id="cart-navbar">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "cart-navbar"),
        // Filter out <div class="heading-macfix article-access-options">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "class", "heading-macfix"), 
        // Filter out <div id="baynote-recommendations">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "baynote-recommendations"),
        // Filter out <div id="bookmarks-container">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "bookmarks-container"),
        // Filter out <div id="llb">...</div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "llb"),
        // Filter out <a href="...">...</a> where the href value includes "exitTargetId" as a parameter
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "[\\?&]exitTargetId="),
        // Filter out <a onclick="...">...</a> where the onclick javascript argument includes "exitTargetId" as a parameter
        HtmlNodeFilters.tagWithAttributeRegex("a", "onclick", "[\\?&]exitTargetId="),
        // Filter out <input name="exitTargetId">
        HtmlNodeFilters.tagWithAttribute("input", "name", "exitTargetId"),
        // Access icon that appears over time -- and its mis-spelling
        HtmlNodeFilters.tagWithAttribute("span", "class", "access-icon"),
        HtmlNodeFilters.tagWithAttribute("span", "class", "acess-icon"),
        // javascript for embedded figure has checksum & expires that changes
        //NOTE - at the moment this does not go beyond nested <p></p> pairs to the closing </a>
        //when possible in the daemon, must subclass and do this for <a> tag
        HtmlNodeFilters.tagWithAttribute("a", "class", "table-popup"),
        // journal-info & breadcrumbs at top of page, added for Nomadic People
        HtmlNodeFilters.tagWithAttribute("div", "id", "journal-info"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "breadcrumb"),
        // added for Nomadic People
        HtmlNodeFilters.tagWithAttribute("span", "class", "pubGAAccount"),
        // Filter out <p class="heading-macfix"> ... free trial
        new AndFilter(new TagNameFilter("p"),
            new HasAttributeFilter("class", "heading-macfix")) {
          @Override
          public boolean accept(Node node) {
            if (super.accept(node)) {
              int i = ((CompositeTag)node).getChildCount();
              if (i <= 2) {
                boolean ret = true;
                for (Node child : ((CompositeTag)node).getChildrenAsNodeArray()) {
                  if (!(child instanceof LinkTag)) {
                    return false;
                  }
                  String text = ((LinkTag)child).getLinkText();
                  String name = ((LinkTag)child).getAttribute("name");
                  if (!(name == null || "trial".equals(name.toLowerCase())) && 
                      !text.toLowerCase().contains("free trial")) {
                    return false;
                  }
                }
                return ret;
              }
            }
            return false;
          }
        },
        // It's not ideal, but ... now removing all br tags, as one is part of the free trial
        HtmlNodeFilters.tag("br"),
        // <a href="#trial" title="trial available">
        // Free trial available!</a> 
        // <br/>
        new AndFilter(new TagNameFilter("a"),
            new HasAttributeFilter("title", "trial available")) {
          @Override
          public boolean accept(Node node) {
            if (super.accept(node)) {
              String allText = ((CompositeTag)node).toPlainTextString();
              return allText.toLowerCase().contains("free trial");
            }
            return false;
          }
        },
    };
    
    HtmlTransform xform = new HtmlTransform() {
      @Override
      public NodeList transform(NodeList nodeList) throws IOException {
        try {
          nodeList.visitAllNodesWith(new NodeVisitor() {
            @Override
            public void visitTag(Tag tag) {
              try {
                if ("li".equalsIgnoreCase(tag.getTagName()) &&
                    tag.getAttribute("class") != null &&
                    tag.getAttribute("class").trim().startsWith("rowShade")) {
                  tag.setAttribute("class", "");
                }
                else {
                  super.visitTag(tag);
                }
              }
              catch (Exception exc) {
                log.debug2("Internal error (visitor)", exc); // Ignore this tag and move on
              }
            }
          });
        }
        catch (ParserException pe) {
          log.debug2("Internal error (parser)", pe); // Bail
        }
        return nodeList;
      }
    };
    
    InputStream filteredStream =  new HtmlFilterInputStream(in, encoding,
        new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(filters)),xform))
    .registerTag(new NavTag()); // XXX remove after 1.66 release
    
    Reader filteredReader = FilterUtil.getReader(filteredStream, encoding);
    Reader tagFilter = HtmlTagFilter.makeNestedFilter(filteredReader,
        ListUtil.list(
            new TagPair("<!--", "-->"),
            new TagPair("<script", "</script>") // Added for Nomadic People
            ));
    
    return new ReaderInputStream(new WhiteSpaceFilter(tagFilter));
  }
  
}

