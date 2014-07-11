/*
 * $Id: OJS2HtmlFilterFactory.java,v 1.13 2014-07-11 19:01:04 etenbrink Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ojs2;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.htmlparser.tags.CompositeTag;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class OJS2HtmlFilterFactory implements FilterFactory {
  
  /**
   * A B(old) tag.  Registered with PrototypicalNodeFactory to cause B
   * to be a CompositeTag.  See code samples in org.htmlparser.tags.
   * @see HtmlFilterInputStream#makeParser()
   */
  public static class bTag extends CompositeTag {
    
    /**
     * The set of names handled by this tag.
     */
    private static final String[] mIds = new String[] {"b"};
    
    /**
     * Return the set of names handled by this tag.
     * @return The names to be matched that create tags of this type.
     */
    public String[] getIds() {
      return mIds;
    }
    
  }
  
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    NodeFilter[] filters = new NodeFilter[] {
            // Some OJS sites have a tag cloud
            HtmlNodeFilters.tagWithAttribute("div", "id", "sidebarKeywordCloud"),
            // Some OJS sites have a subscription status area
            HtmlNodeFilters.tagWithAttribute("div", "id", "sidebarSubscription"),
            // Some OJS sites have a language switcher, which can change over time
            HtmlNodeFilters.tagWithAttribute("div", "id", "sidebarLanguageToggle"),
            // Top-level menu items sometimes change over time
            HtmlNodeFilters.tagWithAttribute("div", "id", "navbar"),
            // Popular location for sidebar customizations
            HtmlNodeFilters.tagWithAttribute("div", "id", "custom"),
            //  the order of <meta> tags changes capriciously, which slows down testing
            new TagNameFilter("head"),
            // Site customizations often involve Javascript (e.g. Google Analytics), which can change over time
            new TagNameFilter("script"),
            // Date accessed is a variable
            HtmlNodeFilters.tagWithTextRegex("div", "Date accessed: "),
            // The version of the OJS software, which can change over time, appears in a tag
            HtmlNodeFilters.tagWithAttribute("meta", "name", "generator"),
            // Header image with variable dimensions
            HtmlNodeFilters.tagWithAttribute("div", "id", "headerTitle"),
            // For Ubiquity Press
            HtmlNodeFilters.tagWithAttribute("div", "id", "rightSidebar"),
            // For JLIS.it: landing pages contain user view count
            HtmlNodeFilters.tagWithAttribute("span", "class", "ArticleViews"),
            // For ibictpln: PHP Query Profiler
            // e.g. http://seer.bce.unb.br/index.php/Musica/article/view/915
            HtmlNodeFilters.tagWithAttribute("div", "id", "pqp-container"),
            // Total de acessos: keeps changing, there is no 'good' tag wrapped around text
            // e.g. https://www.revistas.unijui.edu.br/index.php/desenvolvimentoemquestao/issue/view/18
            HtmlNodeFilters.tagWithTextRegex("b", "^ *total de acesso( dos artigo)?s: +[0-9]+ *$", true),
            // Footer contains changing non-content, debug etc.
            // http://www.portalseer.ufba.br/index.php/cmbio/article/viewArticle/4093
            HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
            // For Librello: download and view counts
            // e.g. http://www.librelloph.com/challengesinsustainability/issue/view/10
            HtmlNodeFilters.tagWithTextRegex("a", "^(HTML|PDF|Views)$", true),
            new AndFilter(
                HtmlNodeFilters.tagWithAttribute("span", "class", "badge"),
                HtmlNodeFilters.tagWithTextRegex("span", "^[0-9]*$")),
    };
    return new HtmlFilterInputStream(in, encoding,
          HtmlNodeFilterTransform.exclude(new OrFilter(filters))).
          registerTag(new bTag());
  }
  
}
