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

package org.lockss.plugin.atypon.ammonsscientific;

import java.io.InputStream;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

public class AmmonsScientificHtmlCrawlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {
  
  protected static final Pattern corrections = Pattern.compile("Original Article|Original|Corrigendum|Correction|Errata|Erratum", Pattern.CASE_INSENSITIVE);

  NodeFilter[] filters = new NodeFilter[] {
      // do not follow an "original article" link back to a previous volume
      // do not follow a "errata" link ahead to a future volume
      new NodeFilter() {
        @Override public boolean accept(Node node) {
          // on a TOC, class="ref nowrap" on an article page, class="errata"
          if (!(node instanceof LinkTag)) return false;
          String classVal = ((CompositeTag)node).getAttribute("class"); 
          // if there is no "class" set, this could be article page, eg
          //<li><a href="/doi/full/NO"> Original </a></li> or 
          //<li><a href="/doi/full/NO">Errata</a></li> 
          // if there is a class val, it will equal "ref" or "ref nowrap"
          if ( (classVal == null) || classVal.startsWith("ref") ) {
            String allText = (((CompositeTag)node).toPlainTextString()).trim();
            // Because we match against the not-very-unique word "Original", we've trimmed
            // the text and must match exactly (not just find)
            return corrections.matcher(allText).matches(); 
            //return (allText.matches("(?is)Original Article") || allText.matches("(?is)Original") || allText.matches("(?is)Errat.*") );
          }
          return false;
        }
      },
  };
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) throws PluginException{ 
    return super.createFilteredInputStream(au, in, encoding, filters);
  }

}
