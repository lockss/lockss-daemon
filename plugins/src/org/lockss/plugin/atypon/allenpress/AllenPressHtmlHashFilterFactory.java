/*
 * $Id:  $
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

package org.lockss.plugin.atypon.allenpress;

import java.io.*;
import org.htmlparser.*;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;

public class AllenPressHtmlHashFilterFactory extends BaseAtyponHtmlHashFilterFactory {

  private static final Logger log = Logger.getLogger(AllenPressHtmlHashFilterFactory.class);

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in,
      String encoding) {

   /* handled by baseAtypon
    *    div id="header", "footer" 
    *    img class="accessIcon"
    *    <head> <script>, etc
    *    div class=citedbySection
    */
            
    NodeFilter[] allenpressFilters = new NodeFilter[] {
        // removing left column with all manner of stuff we dont want to hash
        HtmlNodeFilters.tagWithAttribute("div", "id", "leftColumn"),
        // stuff above the article that we can ignore in the hash - HASH ONLY
        HtmlNodeFilters.tagWithAttribute("div", "class", "article_tools"),

    };

    // additional html filtering to BaseAtyponHashFilters
    return super.createFilteredInputStream(au, in, encoding, allenpressFilters);
  }
  /*
  // include a whitespace filter
  @Override
  public boolean doWSFiltering() {
    return true;
  }
  */
}