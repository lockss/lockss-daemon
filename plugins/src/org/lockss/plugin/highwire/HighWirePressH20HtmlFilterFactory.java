/*
 * $Id: HighWirePressH20HtmlFilterFactory.java,v 1.37 2012-06-06 02:27:03 kendrayee Exp $
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class HighWirePressH20HtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
    	// Publisher adding meta tags often
        new TagNameFilter("head"),
        new TagNameFilter("script"),
        new TagNameFilter("noscript"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "header-ac-elements"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "banner-ads"),
        HtmlNodeFilters.tagWithAttribute("ul", "class", "banner-ads"),
        HtmlNodeFilters.tagWithAttribute("ul", "class", "tower-ads"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "leaderboard-ads"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "leaderboard-ads-ft"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "sidebar-current-issue"),
        HtmlNodeFilters.tagWithAttribute("p", "class", "copyright"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "copyright"),
        // May contain institution-specific data e.g. OUP
        HtmlNodeFilters.tagWithAttribute("div", "id", "secondary_footer"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "cited-by"),
        // e.g. PNAS
        HtmlNodeFilters.tagWithAttribute("div", "class", "science-jobs"),
        // e.g. PNAS
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "most-links-box"),
        // e.g. PNAS
        HtmlNodeFilters.tagWithAttribute("div", "id", "cb-art-recm"),
        // e.g. PNAS (optional 'sid' query arg in URLs)
        HtmlNodeFilters.tagWithAttribute("div", "id", "cb-art-svcs"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "cb-art-cit"),
        // e.g. BMJ (optional 'sid' query arg in URLs)
        HtmlNodeFilters.tagWithAttribute("div", "id", "cb-art-rel"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "cb-art-soc"),
        // e.g. SWCS TOC pages
        HtmlNodeFilters.tagWithAttribute("div", "class", "cit-form-select"),
        // For JBC pages
        HtmlNodeFilters.tagWithAttribute("div", "id", "ad-top"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "ad-top2"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "ad-footer"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "ad-footer2"),
        // For Chest pages
        HtmlNodeFilters.tagWithAttribute("span", "class", "free"),
        // For American College of Physicians
        HtmlNodeFilters.tagWithAttribute("div", "class", "acp-menu"),
        // For Royal Society pages
        HtmlNodeFilters.tagWithAttribute("div", "id", "ac"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "social_network"),
        // For biologists.org
        HtmlNodeFilters.tagWithAttribute("div", "id", "authstring"),
        //For BMJ
        HtmlNodeFilters.tagWithAttribute("div", "id", "access"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "feeds-widget1"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "careers-widget"),
        // For JCB
        HtmlNodeFilters.tagWithAttribute("div", "id", "leaderboard-ads"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "current-issue"),
        // Optional institution-specific citation resolver (e.g. SAGE Publications)
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/cgi/openurl"),
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/openurl"),
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/external-ref"),   
        //For SAGE (at least).  Name of the institution. E.g. </a> INDIANA UNIV </div>
        HtmlNodeFilters.tagWithAttribute("div", "id", "header-Uni"),
        //Project HOPE (at least).  <div class="in-this-issue">
        HtmlNodeFilters.tagWithAttribute("div", "class", "in-this-issue"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "sidebar-feed"),   

        // Filter for <li class="subscr-ref">....</li>
        HtmlNodeFilters.tagWithAttribute("li", "class", "subscr-ref"),
        // Filter for <div class="col-3adverTower"> (SAGE)
        HtmlNodeFilters.tagWithAttribute("div", "class", "col-3adverTower"),        
        // Filter for <div class="social-bookmarking"> 
        HtmlNodeFilters.tagWithAttribute("div", "class", "social-bookmarking"), 
        // Normalize the probabilistic substitution of .short for .abstract
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "\\.(abstract|short)(\\?|$)"),   
        // Ahead-of-print markers eventually disappear (e.g. JBC)
        HtmlNodeFilters.tagWithAttributeRegex("span", "class", "ahead-of-print"),
        // Sage Filter for session ids in meta tags
        HtmlNodeFilters.tagWithAttributeRegex("meta", "id", "^session-"),
        // For BMJ related links div 
        HtmlNodeFilters.tagWithAttribute("div", "id", "related-external-links"), 
        // For BMJ related articles div 
        HtmlNodeFilters.tagWithAttribute("div", "id", "rel-relevant-article"), 
        // For BMJ variable poll 
        HtmlNodeFilters.tagWithAttribute("div", "id", "polldaddy-bottom"),
        // For adclicks leaderboard-ads leaderboard-ads-two
        HtmlNodeFilters.tagWithAttribute("div", "class", "leaderboard-ads leaderboard-ads-two"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "pane-article-page-promo-column"),
        HtmlNodeFilters.tagWithAttribute("form", "id", "bmj-advanced-search-channel-form"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "status"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "AdSkyscraper"),
    };
    
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }

}
