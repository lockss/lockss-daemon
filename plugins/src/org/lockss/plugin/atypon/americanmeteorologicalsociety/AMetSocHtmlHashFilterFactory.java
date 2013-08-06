/*
 * $Id: AMetSocHtmlHashFilterFactory.java,v 1.2 2013-08-06 22:45:13 alexandraohlson Exp $
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

package org.lockss.plugin.atypon.americanmeteorologicalsociety;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;
import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.filters.*;
import org.htmlparser.util.NodeList;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.daemon.PluginException;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.HtmlTagFilter;
import org.lockss.filter.StringFilter;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.ListUtil;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

/* 
 * Don't extend BaseAtyponHtmlHashFilterFactory because we need to do more 
 * extensive filtering with spaces, etc.
 */
public class AMetSocHtmlHashFilterFactory implements FilterFactory {

  Logger log = Logger.getLogger("AMetSocHtmlHashFilterFactoryy");

  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in,
      String encoding)
          throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        // Variable identifiers - institution doing the crawl
        new TagNameFilter("script"),
        // Contains the library specific "find it" button
        HtmlNodeFilters.tagWithAttribute("a", "class", "sfxLink"),
        // May be empty, may contain "free" glif if appropriate
        HtmlNodeFilters.tagWithAttribute("div",  "class", "accessLegend"),
        // Contains name and logo of institution, access, etc
        HtmlNodeFilters.tagWithAttribute("div", "id", "header"),
        // Contains copyright year
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
        // Contains "current issue" link which will change over time
        HtmlNodeFilters.tagWithAttribute("div", "id", "journalNavPanel"),
        // Contains "current issue" link which will change over time
        HtmlNodeFilters.tagWithAttribute("div", "id", "journalInfoPanel"),
        // Contains "current issue" link which will change over time
        HtmlNodeFilters.tagWithAttribute("div", "id", "sitetoolsPanel"),
        // Remove the image with the free glif since it may be added later
        HtmlNodeFilters.tagWithAttribute("image", "class", "accessIcon"),

        // Contains the changeable list of citations
        HtmlNodeFilters.tagWithAttribute("div", "class", "citedBySection"),

        // Remove <hX> tags that have no content or only spaces as content
        // It would be nicer to do this as "all heading tags" but that doesn't seem available in the api
        HtmlNodeFilters.tagWithTextRegex("h1","^(\\s|(&nbsp;))*$"),
        HtmlNodeFilters.tagWithTextRegex("h2","^(\\s|(&nbsp;))*$"),
        HtmlNodeFilters.tagWithTextRegex("h3","^(\\s|(&nbsp;))*$"),
        HtmlNodeFilters.tagWithTextRegex("h4","^(\\s|(&nbsp;))*$"),
        HtmlNodeFilters.tagWithTextRegex("h5","^(\\s|(&nbsp;))*$"),
        HtmlNodeFilters.tagWithTextRegex("h6","^(\\s|(&nbsp;))*$"),
    };
    HtmlTransform xform = new HtmlTransform() {
      @Override
      public NodeList transform(NodeList nodeList) throws IOException {
        try {
          nodeList.visitAllNodesWith(new NodeVisitor() {
            @Override
            public void visitTag(Tag tag) {
              String tagName = tag.getTagName().toLowerCase();
              // Need to remove <span class="titleX" id="xxxxx"> because id is variable
              // cannot remove span tag pair because contents are content. Just remove the id value
              if ( ("span".equals(tagName))  && (tag.getAttribute("id") != null) ){
                tag.setAttribute("id", "0");
              }
            }
          });
        }
        catch (Exception exc) {
          log.debug2("Internal error (visitor)", exc); // Ignore this tag and move on
        }
        return nodeList;
      }
    };   


    // Also need white space filter to condense multiple white spaces down to 1
    InputStream filtered = new HtmlFilterInputStream(in,
        encoding,
        new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(filters)), xform));

    Reader reader = FilterUtil.getReader(filtered, encoding);

    // first subsitute plain white space for &nbsp;
    String[][] unifySpaces = new String[][] { 
        // inconsistent use of nbsp v empty space - do this replacement first
        {"&nbsp;", " "}, 
    };
    Reader NBSPFilter = StringFilter.makeNestedFilter(reader,
        unifySpaces, false);   

    //now consolidate white space before doing additional tagfilter stuff
    Reader WSReader = new WhiteSpaceFilter(NBSPFilter);

    Reader filtReader = makeFilteredReader(WSReader);
    return new ReaderInputStream(filtReader);
  }


  // Noisy whitespace has already been removed by the time we get to this call
  static Reader makeFilteredReader(Reader reader) {
    /* comments contain dates specific stuff, like
     * <!--totalCount14--><!--modified:1368461028000-->
     * we aren't currently using comments for any other search/replace, so just remove them all
     */
    List tagList = ListUtil.list(
        // Remove DOCTYPE declaration which seems to vary but is not a node in the DOM
        new TagPair("<!--","-->")
        );
    Reader tagFilter = HtmlTagFilter.makeNestedFilter(reader, tagList);

    /*
     *  Now remove tags with a single white space in between them
     */
    String[][] findAndReplace = new String[][] { 
        // remove empty space between tags
        {"> <", "><"},
        // remove leading space after tags (extra spaces will already have been consolidated down to one 
        {"> ", ">"},
    };
    Reader stringFilter = StringFilter.makeNestedFilter(tagFilter,
        findAndReplace, false);
    return stringFilter;
  }
}

