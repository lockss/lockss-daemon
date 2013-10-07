/*
 * $Id: PeerJHtmlHashFilterFactory.java,v 1.1 2013-10-07 05:53:44 ldoan Exp $
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

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/*
 * Maximal hash filtering
 * Html pages reviewed for filtering:
 * Archives:
 * year                 - https://peerj.com/archives/
 * volume/start url     - https://peerj.com/archives/?year=2013
 * issue toc            - https://peerj.com/articles/index.html?month=2013-02
 * article              - https://peerj.com/articles/46/
 * Archives-Preprints:
 * year -               - https://peerj.com/archives-preprints/
 * volume/start url     - https://peerj.com/archives-preprints/?year=2013
 * issue toc            - https://peerj.com/preprints/index.html?month=2013-04
 * article              - https://peerj.com/preprints/14/
 */
public class PeerJHtmlHashFilterFactory implements FilterFactory {
  
  private static Logger log = 
      Logger.getLogger(PeerJHtmlHashFilterFactory.class);

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
        //filter out comments
        HtmlNodeFilters.commentWithRegex(".*"),
        // topnavbar <div class="navbar navbar-fixed-top navbar-inverse">
        HtmlNodeFilters.tagWithAttribute(
            "div", "class", "navbar navbar-fixed-top navbar-inverse"),
        // topnavbar <div class="item-top-navbar-inner">
        HtmlNodeFilters.tagWithAttribute(
            "div", "class", "item-top-navbar-inner"),
        // <div class="alert alert-warning"    
        HtmlNodeFilters.tagWithAttribute(
            "div", "class", "alert alert-warning"),
        // follow button and flag
        // <div class="btn-group notification-actions-btn" 
        HtmlNodeFilters.tagWithAttributeRegex(
            "div", "class", ".*notification-actions-btn"),
        // leftnav <div class="article-navigation">
        HtmlNodeFilters.tagWithAttribute(
            "div", "class", "article-navigation"),
        // leftbar <div class="subjects-navigation">
        HtmlNodeFilters.tagWithAttribute(
            "div", "class", "subjects-navigation"),
        // leftbar counter <div id="article-item-metrics-container">
        HtmlNodeFilters.tagWithAttribute(
            "div", "id", "article-item-metrics-container"),
        // <div class="pj-socialism-container">
        HtmlNodeFilters.tagWithAttribute(
            "div", "class", "pj-socialism-container"),
        // remove "Save to Mendeley" and "Read in ReadCube" from "Download as"
        // leftbar section, since it can be varied from preprints side such as
        // http://www.mendeley.com/import/?doi=10.7287/peerj.preprints.14v1
        // http://www.readcube.com/articles/10.7717/peerj.46
        HtmlNodeFilters.tagWithText("a", "Save to Mendeley"),
        HtmlNodeFilters.tagWithText("a", "Read in ReadCube"),
        // rightbar <div class="span2 article-item-rightbar-wrap article-sidebar"
        HtmlNodeFilters.tagWithAttributeRegex(
            "div", "class", ".*article-item-rightbar-wrap.*"),
        // <div id="flagModal"
        HtmlNodeFilters.tagWithAttribute("div", "id", "flagModal"),
        // <div id="followModal"
        HtmlNodeFilters.tagWithAttribute("div", "id", "followModal"),
        // <div id="unfollowModal"
        HtmlNodeFilters.tagWithAttribute("div", "id", "unfollowModal"),
        // <div id="metricsModal"
        HtmlNodeFilters.tagWithAttribute("div", "id", "metricsModal"),
        // <div id="shareModal"
        HtmlNodeFilters.tagWithAttribute("div", "id", "shareModal"),
        // foot <div class="foot">
        HtmlNodeFilters.tagWithAttribute("div", "class", "foot"),
        // <div class="tab-content annotation-tab-content">
        HtmlNodeFilters.tagWithAttribute(
            "div", "class", "tab-content annotation-tab-content"),
        // <ul class="nav nav-tabs annotation-tabs-nav">
        HtmlNodeFilters.tagWithAttribute(
            "ul", "class", "nav nav-tabs annotation-tabs-nav"),
     };

    return new HtmlFilterInputStream(in, encoding, 
        HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }
    
}
