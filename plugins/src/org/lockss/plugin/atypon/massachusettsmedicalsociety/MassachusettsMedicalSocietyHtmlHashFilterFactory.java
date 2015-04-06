/*
 * $Id$
 */
/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

Except as contained in this notice, tMassachusettsMedicalSocietyHtmlFilterFactoryhe name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

 */

package org.lockss.plugin.atypon.massachusettsmedicalsociety;

import java.io.*;

import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.htmlparser.tags.*;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;

public class MassachusettsMedicalSocietyHtmlHashFilterFactory extends BaseAtyponHtmlHashFilterFactory {

  private static final Logger log = Logger.getLogger(MassachusettsMedicalSocietyHtmlHashFilterFactory.class);

  // include a whitespace filter
  @Override
  public boolean doWSFiltering() {
    return true;
  }
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in,
      String encoding) {

    // First filter with HtmlParser
    NodeFilter[] MmsFilters = new NodeFilter[] {
        /*
         * Hash filter
         */
        HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "authInfo"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "topNav"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "corners dropDown"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "boxContent"),


        // Contain cross-links to other articles in other journals/volumes
        HtmlNodeFilters.tagWithAttribute("div", "id", "related"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "trendsBox"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "trendsMod"),
        // Contains ads
        HtmlNodeFilters.tagWithAttribute("div", "id", "topAdBar"),
        //Remove Advertisement from Topbanner
        HtmlNodeFilters.tagWithAttribute("div", "class", "Topbanner CM8"),
        //Remove this week audio summary
        HtmlNodeFilters.tagWithAttribute("div", "class", "audioTitle"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "topLeftAniv"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "ad"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "rightRail"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "rightRailAd"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "rightAd"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "rightAd"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "toolsAd"),
        //Remove Advertisement on bottomAdBar
        HtmlNodeFilters.tagWithAttribute("div", "id", "bottomAdBar"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "bottomAd"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "bannerAdTower"),
        //Certain ads do not have a specified div and must be removed based on regex
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/action/clickThrough"),
        //Contains comments by users with possible links to articles in other journals/volumes
        HtmlNodeFilters.tagWithAttribute("dd", "id", "comments"),
        //Letter possible from a future au
        HtmlNodeFilters.tagWithAttribute("dd", "id", "letters"),
        //Contains links to articles currently citing in other volumes
        HtmlNodeFilters.tagWithAttribute("dd", "id", "citedby"),
        //Contains a link to the correction or the article which is possibly part of another au
        HtmlNodeFilters.tagWithAttribute("div", "class", "articleCorrection"),
        //Group of images/videos that link to other articles
        HtmlNodeFilters.tagWithAttribute("div", "id", "galleryContent"),
        //constantly changing discussion thread with links to current article +?sort=newest...
        HtmlNodeFilters.tagWithAttribute("div", "class", "discussion"),

        // Contains the number of articles currently citing
        HtmlNodeFilters.tagWithAttribute("dt", "id", "citedbyTab"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "articleActivity"),
        // Contains institution name
        HtmlNodeFilters.tagWithAttribute("div", "id", "institutionBox"),
        // Contains copyright year
        HtmlNodeFilters.tagWithAttribute("div", "id", "copyright"),
        // Contains recent issues
        HtmlNodeFilters.tagWithAttribute("a", "class", "issueArchive-recentIssue"),
        //More in ...
        HtmlNodeFilters.tagWithAttribute("div", "id", "moreIn"),
        //ID of the media player tag changes
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "layerPlayer"),
        //For recent issues you can submit a letter then this feature disappears
        HtmlNodeFilters.tagWithAttribute("li", "class", "submitLetter"),
        //For recent issues you can submit a comment then this feature disappears
        HtmlNodeFilters.tagWithAttribute("p", "class", "openUntilInfo"),
        //Poll if collected while poll is open will change
        HtmlNodeFilters.tagWithAttribute("div", "class", "poll"),
        //the emailAlert is extraneous info and often contains inconsistent whitespace
        HtmlNodeFilters.tagWithAttribute("div", "class", "emailAlert"),
        //Metadata contains either "current-issue" or "Last-6-months" or...
        HtmlNodeFilters.tagWithAttribute("meta", "name", "evt-ageContent"),
        //removes "OpenURL" button local url reference (eg Stanford)
        HtmlNodeFilters.tagWithAttributeRegex("a", "title", "OpenURL "),
        // references current mp3s "More Weekly Audio Summaries" from page
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "jcarousel-skin-audio"),
        // remove toolsbox
        HtmlNodeFilters.tagWithAttribute("div", "id", "toolsBox")

    };
    
    /*
     * This removes a "will be temporarily unavailable" warning from the page,
     * identified as <div> tag with the appropriate warning-type text 
     * within a <div align="center"> tag 
     */
    HtmlTransform xform = new HtmlTransform() {
      @Override
      public NodeList transform(NodeList nodeList) throws IOException {
        try {
          nodeList.visitAllNodesWith(new NodeVisitor() {
            @Override
            public void visitTag(Tag tag) {
              try {
                if (tag instanceof Div && (tag.getAttribute("align").equalsIgnoreCase("center"))) {
                  if ((((CompositeTag) tag).getChildCount() > 1)) {
                    String html;
                    for (int i = 0; i < ((CompositeTag) tag).getChildCount(); i++) {
                      log.debug3("CHILD TAG:"+i);
                      if ((((CompositeTag) tag).getChild(i) instanceof Div)) {
                        html = ((Div) ((CompositeTag) tag).getChild(i)).getStringText();
                        log.debug3(html);
                        if (html.contains("ATTENTION:") && 
                            html.contains("temporarily unavailable")){
                          log.debug3("FOUND IT!");
                          ((CompositeTag) tag).removeChild(i);
                        }
                      }
                    }
                  }
                } else {
                  log.debug3("NOT FOUND");
                  super.visitTag(tag);
                }
              } catch (Exception exc) {
                log.debug2("Internal error (visitor)", exc); // Ignore this tag and move on
              }
            }
          });
        } catch (ParserException pe) {
          log.debug2("Internal error (parser)", pe); // Bail
        }
        return nodeList;
      }
    };
    // initial html filtering
    InputStream filteredStream = new HtmlFilterInputStream(in, encoding,
      new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(MmsFilters)),xform));
    return super.createFilteredInputStream(au, filteredStream, encoding, MmsFilters);
  }

}