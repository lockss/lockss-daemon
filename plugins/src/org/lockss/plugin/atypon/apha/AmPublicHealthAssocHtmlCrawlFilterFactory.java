/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.atypon.apha;

import java.io.InputStream;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.tags.BulletList;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.LinkTag;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

public class AmPublicHealthAssocHtmlCrawlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {
  protected static final Pattern corrections = Pattern.compile("Original Article|Original|Corrigendum|Correction|Errata|Erratum", Pattern.CASE_INSENSITIVE);

  NodeFilter[] filters = new NodeFilter[] {
    //Leave in this specific filter even though something similar is in BaseAtypon
    // This one checks the very ambigous text "Original" and we like the additional
    // context checks to be more careful  
      // Avoid following links between Errata <--> Original articles on either a TOC or article page
      // The links are only differentiated by the title they are given which varies 
      // depending on location
      // On table of contents, the links have a 'class="ref ...."'
      // On article page, no class, but grandparent is <ul id="articleToolsFormat">
      new NodeFilter() {
        @Override public boolean accept(Node node) {
 
          if (!(node instanceof LinkTag)) return false;
          String classAttr = ((CompositeTag)node).getAttribute("class");
          String allText = ((CompositeTag)node).toPlainTextString();
          Boolean testText = false;
          
          // figure out if we meet either situation to warrant checking the text value
          if ( classAttr != null) {
            if ( !(classAttr.contains("ref")) ) return false;
            testText = true; //we've meet one set of conditions
          } else {
            // could be the article page case, no class set
            Node parentNode = node.getParent();
            Node grandparentNode = (parentNode != null) ? parentNode.getParent() : null;
            if ( (grandparentNode != null) && ( grandparentNode instanceof BulletList) ) {
              String idAttr = ((CompositeTag)grandparentNode).getAttribute("id");
              if ( (idAttr == null) || (!(idAttr.equals("articleToolsFormats"))) ) return false;
              testText = true;
            }
          }
          //using regex - the "i" is for case insensitivity; the "s" is for accepting newlines
          //On a TOC page it is "Erratum" or "Original Article" on an article page it can be "Original"
          if (testText) {
            return corrections.matcher(allText).find();
          }
          return false; // neither case
        }
      },
  };
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    return super.createFilteredInputStream(au, in, encoding, filters);
  }

}
