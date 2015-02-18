/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.peerj;

import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.Vector;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.htmlparser.util.NodeList;
import org.htmlparser.visitors.NodeVisitor;
import org.htmlparser.Tag;
import org.htmlparser.util.ParserException;
import org.htmlparser.Attribute;

import org.lockss.filter.FilterUtil;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.ReaderInputStream;

/*
 * Maximal hash filtering
 * Html pages reviewed for filtering:
 * Archives (main):
 * year                 - https://peerj.com/archives/
 * volume/start url     - https://peerj.com/archives/?year=2013
 * issue toc            - https://peerj.com/articles/index.html?month=2013-02
 * article              - https://peerj.com/articles/46/
 * Preprints:
 * year -               - https://peerj.com/archives-preprints/
 * volume/start url     - https://peerj.com/archives-preprints/?year=2013
 * issue toc            - https://peerj.com/preprints/index.html?month=2013-04
 * article              - https://peerj.com/preprints/14/
 */
public class PeerJHtmlHashFilterFactory implements FilterFactory {
  
  public InputStream createFilteredInputStream(ArchivalUnit au, 
                                          InputStream in, String encoding) {

    NodeFilter[] filters = new NodeFilter[] {
        // generally we should not remove the whole <head> tag
        // since it contains metadata and css. However, since ris file is 
        // used to extract metadata and css paths looks varied, it makes
        // sense to remove the whole <head> tag
        new TagNameFilter("head"),
        new TagNameFilter("script"),
        new TagNameFilter("noscript"),
        // stylesheets
        HtmlNodeFilters.tagWithAttribute("link", "rel", "stylesheet"),
        // filter out comments
        HtmlNodeFilters.comment(),
        // from preprints article - ex: https://peerj.com/preprints/13v1/
        // institution alert
        HtmlNodeFilters.tagWithAttribute(
            "div", "id", "instit-alert"),
        // read annoucement alert
        HtmlNodeFilters.tagWithAttribute(
            "div", "id", "read-announce-alert"),
        // qa announcement alert
        HtmlNodeFilters.tagWithAttribute(
            "div", "id", "qa-announce-alert"),
        // submit announcement alert
        HtmlNodeFilters.tagWithAttribute(
            "div", "id", "submit-announce-alert"),    
        // top navbar - brand name, search, article, etc.
        HtmlNodeFilters.tagWithAttributeRegex(
            "div", "class", "navbar-fixed-top"),
        // top navbar - brand name, search, article, etc.
        HtmlNodeFilters.tagWithAttributeRegex(
            "div", "class", "item-top-navbar"),
        // left column - all except 'Download as' dropdown menu
        HtmlNodeFilters.allExceptSubtree(
            HtmlNodeFilters.tagWithAttributeRegex(
                "div", "class", "article-item-leftbar"),
            HtmlNodeFilters.tagWithAttribute("ul", "class", "dropdown-menu")),
        // right column - all
        HtmlNodeFilters.tagWithAttributeRegex(
            "div", "class", "article-item-rightbar-wrap"),
        // these modals are found after "Right sidebar" html code near bottom 
        HtmlNodeFilters.tagWithAttribute("div", "id", "flagModal"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "followModal"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "unfollowModal"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "metricsModal"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "shareModal"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "citing-modal"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "article-links-modal"),
        // found under comment <!-- annotations -->
        HtmlNodeFilters.tagWithAttributeRegex(
           "ul", "class", "annotation-tabs-nav"),
        HtmlNodeFilters.tagWithAttributeRegex(
            "div", "class", "annotation-tab-content"),
        // foot
        HtmlNodeFilters.tagWithAttribute("div", "class", "foot"),
        // under foot
        HtmlNodeFilters.tagWithAttribute(
            "div", "class", "annotations-outer-heatmap"),
     };
    
    // removing attributes "style" and "itemprop", by adding the wanted
    // attributes one by one to the each tag, then inserting a space
    // between the attributes.
    HtmlTransform xform = new HtmlTransform() {      
      @Override
      public NodeList transform(NodeList nodeList) throws IOException {
        try {
          nodeList.visitAllNodesWith(new NodeVisitor() {
            @Override
            public void visitTag(Tag tag) {
              // get the attributes in each tag
              Vector<Attribute> attributes;
              Attribute attribute;
              String attrKey;
              int size;
              Vector<Attribute> vecAttr = new Vector<Attribute>();  
              // gets the attributes in the tag
              attributes = tag.getAttributesEx();
              if (attributes != null) {
                size = attributes.size();
                for (int i = 0; i < size; i++) {
                  attribute = attributes.elementAt(i);
                  attrKey = attribute.getName();
                  if (attrKey != null) {
                    if (!(("style".equalsIgnoreCase(attrKey)) 
                        || ("itemprop".equalsIgnoreCase(attrKey)))) {
                      vecAttr.add(attribute);
                    }
                  }
                }
                // insert space between attributes, starting from
                // the last element of the attributes vector.
                for (int i = vecAttr.size()-1; i >= 1; i--) {
                  vecAttr.insertElementAt(new Attribute (" "), i);
                }
                tag.setAttributesEx(vecAttr);
              } 
            }
          });
        }
        catch (ParserException pe) {
          throw new IOException(pe);
        }
        return nodeList;
      }
    };
  
    InputStream filtered = new HtmlFilterInputStream(
        in, encoding, new HtmlCompoundTransform(
            HtmlNodeFilterTransform.exclude(new OrFilter(filters)), xform));
  
    // add whitespace filter
    Reader filteredReader = FilterUtil.getReader(filtered, encoding);
    return new ReaderInputStream(new WhiteSpaceFilter(filteredReader));
    
  }
    
}
