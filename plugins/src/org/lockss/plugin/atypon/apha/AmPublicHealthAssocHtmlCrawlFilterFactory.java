/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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
