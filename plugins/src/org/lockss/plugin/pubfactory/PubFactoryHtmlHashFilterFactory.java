/*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pubfactory;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.Bullet;
import org.htmlparser.tags.Div;
import org.htmlparser.util.NodeList;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Vector;

// Keeps contents only (includeNodes), then hashes out unwanted nodes 
// within the content (excludeNodes).
public class PubFactoryHtmlHashFilterFactory implements FilterFactory {

  private static final Logger log =
      Logger.getLogger(PubFactoryHtmlHashFilterFactory.class);
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in, 
                                               String encoding) {
    NodeFilter[] excludeNodes = new NodeFilter[] {
        HtmlNodeFilters.tag("head"),
        new TagNameFilter("script"),
        new TagNameFilter("noscript"),
        // filter out comments
        HtmlNodeFilters.comment(),
        // citation overlay for download of ris - this has download date
        // and the ris citation has a one-time key
        // so just keep the referring article as a way of hashing
        HtmlNodeFilters.allExceptSubtree(
            HtmlNodeFilters.tagWithAttribute("div", "id", "previewWrapper"),
            HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/view/journals/")),
        //html structure change in Oct/2020
        //https://www.berghahnjournals.com/view/journals/boyhood-studies/12/1/bhs120101.xml
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "headerWrap"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "footerWrap"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "fixed-controls"),
        /*
        // Metrics on AMetSoc https://journals.ametsoc.org/view/journals/wcas/12/2/wcas-d-19-0115.1.xml
        // class name is big e.g. "component component-content-item component-container container-metrics container-wrapper-43132"
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "component-content-metrics"),
        // same with related content
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "component-related-content"),
        */
        // Get rid of entire sidebar, as it has lots of dynamic ids etc
        // "component component-content-item component-container container-sideBar container-wrapper-43148 container-accordion"
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "container-sideBar"),
        // get rid of volume dropdown, it similarly has generated ids
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "component-volume-issue-selector"),
        /* sometimes there is a little div near the bottom of the page that contains the IP address.
         * <div id="debug" style="display: none"> <ul>
         *     <li id="xForwarded">[171.66.236.212]</li>
         *     <li id="modifiedRemoteAddr">171.66.236.212</li>
         * </ul> </div>
         */
        HtmlNodeFilters.tagWithAttribute("div", "id", "debug"),
        // there are a number of input forms, comment boxes, etc to filter out
        HtmlNodeFilters.tagWithAttributeRegex("form", "class", "annotationsForm"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "searchModule"),
        // the access seems to change? maybe we caught them in a migration, but just to be safe, exclude the access icon
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "accessIcon"),
        // cover image alt text changes? weird
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "component-cover-image"),
        // if an article is added to some 'collection' after being published, it gets ammended with this tag
        HtmlNodeFilters.tagWithAttributeRegex("dl", "class", "tax-collections "),
        // offsite-access-message (for bioscientifica only?)
        HtmlNodeFilters.tagWithAttributeRegex("span", "class", "off-site-access-message"),
        // there is a p tag that contains some copyright text.
        // this tag occurs sometimes below the abstract.
        // only way to filter it out is a regex on the content, as there are no attributes associated with the p tag.
        // trying to be as conservative as possible to not remove possibly the whole abstract if the
        // nodes get embedded or moved around from some reason.
        HtmlNodeFilters.tagWithTextRegex("p", "^.{0,20}American Meteorological Society.{0,250}AMS Copyright Policy.{0,250}$"),
        new NodeFilter() {
          @Override
          public boolean accept(Node node) {
            // ifp:body is not a tag class, meaning it will never have children
            // we can use this to our advantage and delete the whole node (as well as the End version) by matching a
            // regex on the node and removing the whole thing without fear of deleting "child" nodes.
            if (node instanceof Tag &&  node.getText().matches("^/?ifp:body.{0,150}$")) {
              return true;
            }
            return false;
          }
        }
    };
    
    return getFilteredInputStream(au, in, encoding,
        excludeNodes);
  }

  HtmlTransform xform = new HtmlTransform() {
    @Override // this tranform removes dynamically generated attribute values from a number of tags and attributes.
    public NodeList transform(NodeList nodeList) throws IOException {
      try {
        nodeList.visitAllNodesWith(new NodeVisitor() {
          @Override
          public void visitTag(Tag tag) {
            String tagName = tag.getTagName().toLowerCase();
            /* Many of the ul, and li tags contain dynamic attributes, aggressivley remove these
            * <ul class="ajax-zone m-0 t-zone" id="zone115228561_1">
            * <ul data-menu-list="list-id-567363a7-9393-49e7-..." ...>
            * <li ... data-menu-item="list-id-fe284..." ...>
            */
            if (tagName.equals("ul")) {
              if (tag.getAttribute("id") != null){
                tag.removeAttribute("id");
              }
              if (tag.getAttribute("data-menu-list") != null) {
                tag.removeAttribute("data-menu-list");
              }
            } else if (tagName.equals("li"))  {
              if (tag.getAttribute("id") != null) {
                tag.removeAttribute("id");
              }
              if (tag.getAttribute("data-menu-item") != null) {
                tag.removeAttribute("data-menu-item");
              }
            }
            /* Remove the generated id's from all the h# tags
             * <h2 class="abstractTitle text-title my-1" id="d3038e2">Abstract</h2>
             * <h3 id="d4951423e445">a. Satellite data</h3>
             * <h4 id="d4951423e1002">On what scale does lightning enhancement occur?</h4>
            */
            else if (tagName.matches("h\\d") && (tag.getAttribute("id") != null)) {
              tag.removeAttribute("id");
            }
            /* remove these data-popover[-anchor] attributes that are dynamically generated from div and button tags
             * <div data-popover-fullscreen="false" data-popover-placement="" data-popover-breakpoints="" data-popover="607a919f-a0fd-41c2-9100-deaaff9a0862" class="position-absolute display-none">
             * <button data-popover-anchor="0979a884-7df8-4d05-a54...
             */
            else if ("div".equals(tagName) || "button".equals(tagName)) {
              if (tag.getAttribute("data-popover-anchor") != null) {
                tag.removeAttribute("data-popover-anchor");
              }
              if (tag.getAttribute("data-popover") != null) {
                tag.removeAttribute("data-popover");
              }
              // the container-wrapper-NUMBERS is dynamic
              // <div class="component component-content-item component-container container-body container-tabbed container-wrapper-43131">
              if (tag.getAttribute("class") != null && tag.getAttribute("class").matches(".*container-wrapper-.*")) {
                tag.removeAttribute("class");
              }
              // <div id="container-43131-item-43166" class="container-item">
              if (tag.getAttribute("id") != null && tag.getAttribute("id").matches(".*container-.*")) {
                tag.removeAttribute("id");
              }
            } else if ("nav".equals(tagName) && (tag.getAttribute("id") != null) && tag.getAttribute("id").matches(".*container-.*") ) {
              //<nav data-container-tab-address="tab_body" id="container-nav-43131" class="container-tabs">
              tag.removeAttribute("id");
            } else if ("a".equals(tagName)) {
              // <a data-tab-id="abstract-display" title="" href="#container-43131-item-43130" tabIndex="0" role="button" type="button" class=" c-Button c-Button--medium ">
              // for hashing, lets not worry about all the possible patterns of the internal dynamic links, just ignore all the internal hrefs
              if ((tag.getAttribute("href") != null) && (tag.getAttribute("href").startsWith("#"))) {
                tag.removeAttribute("href");
              }
              // <a data-tab-id="previewPdf-43621" title=""  tabIndex="0" role="button" type="button" class=" c-Button c-Button--medium ">
              if (tag.getAttribute("data-tab-id") != null) {
                tag.removeAttribute("data-tab-id");
              }
            /*
            path to some svgs has a hashed folder name
            <figure>
              <figcaption>
                <span style="font-variant: small-caps;">
                 Fig</span>
                 . 2.
              </figcaption>
              -<img data-image-src="/view/journals/phoc/50/9/full-jpoD190077-f2.jpg" height="625" width="1023" src="/skin/8f11d65e036447dfc696ad934586604015e8c19b/img/Blank.svg" class="lazy-load" alt="Fig. 2."/>
              +<img data-image-src="/view/journals/phoc/50/9/full-jpoD190077-f2.jpg" height="625" width="1023" src="/skin/cb453204ff7ce459e12f81e25b6f22a656d02164/img/Blank.svg" class="lazy-load" alt="Fig. 2."/>
              ...
            </figure>
            also
            <div ...class="tableWrap..." >
              <img data-image-src="/view/journals/phoc/50/8/full-jpoD200034-t1.jpg" src="/skin/8f11d65e036447dfc696ad934586604015e8c19b/img/Blank.svg" class="lazy-load" alt="Table 1." width="" height="">
            */
            } else if ("img".equals(tagName)) {
              if (tag.getAttribute("src") != null && tag.getAttribute("src").contains("/skin/")) {
                log.debug3("removing: " + tag);
                tag.removeAttribute("src");
              }
            }
          }
        });
      }
      catch (Exception exc) {
        log.debug2("Internal error (visitor)", exc); // Ignore this tag and move on
      }
      return nodeList;
    }
  };

  
  // Takes include and exclude nodes as input. Removes white spaces.
  public InputStream getFilteredInputStream(ArchivalUnit au, InputStream in,
    //  String encoding, NodeFilter[] includeNodes, NodeFilter[] excludeNodes) {
    String encoding, NodeFilter[] excludeNodes) {
    if (excludeNodes == null) {
      throw new NullPointerException("excludeNodes array is null");
    }  
    //if (includeNodes == null) {
    //  throw new NullPointerException("includeNodes array is null!");
    //}
    InputStream filtered;
    filtered = new HtmlFilterInputStream(in, encoding,
                 new HtmlCompoundTransform(
                   //  HtmlNodeFilterTransform.include(new OrFilter(includeNodes)),
                    HtmlNodeFilterTransform.exclude(new OrFilter(excludeNodes)), xform)
               );

    Reader reader = FilterUtil.getReader(filtered, encoding);
    return new ReaderInputStream( new WhiteSpaceFilter(reader));
    //return new ReaderInputStream(reader);
  }

}
