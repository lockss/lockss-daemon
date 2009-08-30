/*
 * $Id: NaturePublishingGroupHtmlFilterFactory.java,v 1.5 2009-08-30 00:07:18 thib_gc Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.nature;

import java.io.InputStream;

import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

/**
 * <p>Normalizes HTML pages from Nature Publishing Group journals.</p>
 * @author Thib Guicherd-Callin
 */
public class NaturePublishingGroupHtmlFilterFactory implements FilterFactory {

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    
    HtmlTransform[] transforms = new HtmlTransform[] {
        
        /*
         * At the top of an issue table of contents, a "leaderboard"
         * can contain a banner ad which is clearly marked as such
         * (there is an "ADVERTISEMENT" logo vertically along both
         * sides of the banner).
         * 
         * Remove <div class="leaderboard>
         */
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "class",
                                                                         "leaderboard")),
        /*
         * Along the right side of an issue table of contents, there
         * can be a vertical banner ad also clearly marked as such.
         * 
         * Remove <div class="ad-vert">
         */
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "class",
                                                                         "ad-vert")),

        /*
         * There can be ads in mid-bastract. These ads are enclosed in
         * a div whose class name begins with "ad" but has other words
         * in it.
         * 
         * Remove <div class="ad ...">
         */
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttributeRegex("div",
                                                                              "class",
                                                                              "^ad ")),


        /*
         * At the top of various pages, the institution name is
         * visible; remove it.
         * 
         * Remove <div class="logon ..."
         */
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttributeRegex("div",
                                                                              "class",
                                                                              "^logon ")),
                                                                              
        /*
         * Articles can have user-posted comments. We need to first
         * remove the paragraph that appears when there are no
         * comments.
         * 
         * Remove <p>There are currently no comments.</p>
         */
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithText("p",
                                                                    "There are currently no comments.")),                                                            


        /*
         * When there are posted comments, that paragraph is replaced
         * by a list of comments, which we also need to remove.
         * 
         * Remove <ul class="comments ...">
         */
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttributeRegex("ul",
                                                                              "class",
                                                                              "^comments ")),
                                                                    
        /*
         * The elements in the left-hand column of most pages are not
         * particularly dynamic but they are also liable to change
         * over time: links to other journals and databases,
         * navigational conveniences, etc. To minimize spurious
         * disagreement as this element evolves, removing it entirely
         * seems prudent.
         * 
         * Remove <div id="journalnav">
         */
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "id",
                                                                         "journalnav")),
        /*
         * Most of the elements in the right-hand column of most pages
         * are dynamic: ticker from the Open Innovation Challenge,
         * ticker from Nature Jobs, ads. Rather than characterizing
         * all these (otherwise well-delineated) elements, it seems
         * more practical to remove the entirety of the column, which
         * will likely evolve over time anyway.
         * 
         * Remove <div id="extranav">
         */
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "id",
                                                                         "extranav")),
        /*
         * The footer of all pages contains a copyright statement for
         * the year in progress (not for the year of publication).
         * Removing a large portion of the footer is more than needed,
         * but this is the smallest easily-addressable element that
         * encloses the copyright statement.
         * 
         * Remove <div class="logo">
         */
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "class",
                                                                         "logo")),
        /*
         * Article pages may have a dynamically-generated list of
         * articles similar to the current one.
         * 
         * Remove <div id="more-like-this">
         */
        HtmlNodeFilterTransform.exclude(HtmlNodeFilters.tagWithAttribute("div",
                                                                         "id",
                                                                         "more-like-this")),
                                                                         
    };

    return new HtmlFilterInputStream(in,
                                     encoding,
                                     new HtmlCompoundTransform(transforms));
  }

}
