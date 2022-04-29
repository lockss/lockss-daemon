/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.atypon.ammonsscientific;

import java.io.InputStream;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.LinkTag;
import org.lockss.daemon.PluginException;
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
