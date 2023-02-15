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

package org.lockss.plugin.usdocspln.gov.gpo.fdsys;

import java.io.*;
import java.util.regex.*;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.htmlparser.util.*;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.StringFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class GPOFDSysHtmlFilterFactory implements FilterFactory {

  private static final Logger logger = Logger.getLogger(GPOFDSysHtmlFilterFactory.class);

  private static final Pattern titlePat =
      Pattern.compile("<title>U.S.C. Title 42 - THE PUBLIC HEALTH AND WELFARE",
                      Pattern.CASE_INSENSITIVE); 
  
  private static final Pattern chapPat =
      Pattern.compile("<span( [^>]*)?>CHAPTER",
                      Pattern.CASE_INSENSITIVE); 
  
  public boolean shouldFilter(InputStream is, String encoding) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(new BoundedInputStream(is, 2000), encoding));
    boolean title42 = false;
    for (String line = br.readLine() ; line != null ; line = br.readLine()) {
      if (logger.isDebug3()) {
        logger.debug3("line: " + line);
      }
      if (title42) {
        Matcher chapMat = chapPat.matcher(line);
        if (chapMat.find()) {
          logger.debug2("Chapter of Title 42");
          return true;
        }
      }
      else {
        if (line.contains("<title>")) {
          logger.debug2("<title> line");
          Matcher titleMat = titlePat.matcher(line);
          if (titleMat.find()) {
            logger.debug2("Title 42");
            title42 = true;
          }
          else {
            logger.debug2("Not Title 42");
            return true;
          }
        }
      }
    }
    // Filter iff inside Title 42 but not a chapter thereof
    logger.debug2(title42 ? "Title 42 but not a chapter thereof" : "Not Title 42 (final)");
    return !title42;
  }
  
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    BufferedInputStream bis = new BufferedInputStream(in, 2048);
    bis.mark(2048);
    try {
      boolean filter = shouldFilter(bis, encoding);
      bis.reset();
      if (!filter) {
        return bis;
      }
    }
    catch (IOException ioe) {
      logger.debug("IOException while inspecting document to skip filtering", ioe);
    }
    
    NodeFilter[] filters = new NodeFilter[] {
        /*
         * Broad area filtering 
         */
        /* Document header */
        // Differences in the presence and order of <meta> tags and spacing of the <title> tag
        HtmlNodeFilters.tag("head"),
        /* Scripts, inline style */
        HtmlNodeFilters.tag("script"),
        HtmlNodeFilters.tag("noscript"),
        HtmlNodeFilters.tag("style"),
        /* Header */
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "top-menu-one"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "top-banner-inside"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "top-menu-two"),
        /* Left column (various cells of a two-column table layout) */
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "left-menu"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "page-details-left-mask"),
        /* Footer */
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "footer"),
        /*
         * Main area
         */
        // Seen in the field: "null" in <a href="/fdsys/search/pagedetails.action?null&amp;bread=true">More Information</a>
        HtmlNodeFilters.tagWithAttributeRegex("span", "id", "breadcrumbs"),
        // Whitespace differences inside <div id="page-details-form-mask">
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "page-details-form-mask"),
        /*
         * Other
         */
        // Variable Struts identifier, sometime in a comment, sometimes not
        HtmlNodeFilters.tagWithAttributeRegex("input", "type", "hidden"),
        HtmlNodeFilters.comment(),
        // "Email a link to this page" link [probably contained in larger block now]
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^search/notificationPage\\.action\\?emailBody="),
        // Session ID from search results [probably contained in larger block now]
        HtmlNodeFilters.tagWithAttributeRegex("form", "action", "jsessionid="),
        // Differs over time in the presence and placement of rel="nofollow"
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^delivery/getpackage\\.action\\?packageId="),
        // Links here can differ with each fetch (https://www.gpo.gov/fdsys/pkg/CFR-2013-title10-vol2/content-detail.html)
        /* The following should work ... but it doesn't; the parser gets
         * confused, closes an inner <table> soon into this <div> and then
         * closes the <div>, leaving basically everything from it in the stream
         * thereafter. */
        HtmlNodeFilters.tagWithAttribute("div", "id", "browse-drilldown-mask"),
        /* So try to eradicate each row instead, with the following two: */
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "browse-level"),
        HtmlNodeFilters.tag("input"),
    };
  
    HtmlTransform xform = new HtmlTransform() {
      @Override
      public NodeList transform(NodeList nodeList) throws IOException {
        try {
          nodeList.visitAllNodesWith(new NodeVisitor() {
            @Override
            public void visitTag(Tag tag) {
              String tagName = tag.getTagName().toLowerCase();
              if ("a".equals(tagName)) {
                tag.removeAttribute("onclick"); // Javascript calls changed over time
              }
            }
          });
          return nodeList;
        }
        catch (ParserException pe) {
          throw new IOException("ParserException inside HtmlTransform", pe);
        }
      }
    };
    
    InputStream prefilteredStream =
        new HtmlFilterInputStream(bis, // NOTE: this is 'bis', not 'in'
                                  encoding,
                                  new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(filters)),
                                                            xform));
    try {
      Reader filteredReader = new InputStreamReader(prefilteredStream, encoding);
      Reader httpFilter = new StringFilter(filteredReader, "http:", "https:");
      Reader whitespaceReader = new WhiteSpaceFilter(httpFilter);
      return new ReaderInputStream(whitespaceReader, encoding);
    }
    catch (UnsupportedEncodingException uee) {
      throw new PluginException(uee);
    }
  }
}
