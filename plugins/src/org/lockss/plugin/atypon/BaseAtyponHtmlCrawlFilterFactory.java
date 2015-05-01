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

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

 */

package org.lockss.plugin.atypon;

import java.io.InputStream;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.LinkTag;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

/**
 * BaseAtyponHtmlCrawlFilterFactory
 * The basic AtyponHtmlCrawlFilterFactory
 * Child plugins can extend this class and add publisher specific crawl filters,
 * if necessary.  Common crawl filters can be easily added and be available to 
 * children.  Otherwise, this can be used by child plugins if no other crawl 
 * filters are needed.
 */

public class BaseAtyponHtmlCrawlFilterFactory implements FilterFactory {
  protected static final Pattern corrections = Pattern.compile("Original Article|Corrigendum|Correction|Errata|Erratum", Pattern.CASE_INSENSITIVE);
  protected static NodeFilter[] baseAtyponFilters = new NodeFilter[] {
    
    HtmlNodeFilters.tagWithAttribute("div", "class", "citedBySection"),
    
    // Since overcrawling is a constant problem for Atypon, put common
    // next article-previous article link for safety; 
    // AIAA, AMetSoc, ASCE, Ammons, APHA, SEG,Siam, 
    HtmlNodeFilters.tagWithAttribute("a", "class", "articleToolsNav"),
    // BIR, Maney, Endocrine - also handles next/prev issue - also for issues
    HtmlNodeFilters.tagWithAttributeRegex("td", "class", "journalNavRightTd"),
    HtmlNodeFilters.tagWithAttributeRegex("td", "class", "journalNavLeftTd"),
    // BQ, BioOne, Edinburgh, futurescience, nrc
    //        all handle next/prev article link in plugin
    // T&F doesn't have prev/next article links

    // breadcrumb or other link back to TOC from article page
    // AMetSoc, Ammons, APHA, NRC,  
    HtmlNodeFilters.tagWithAttribute("div", "id", "breadcrumbs"),
    // ASCE, BiR, Maney, SEG, SIAM, Endocrine 
    HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "^(linkList )?breadcrumbs$"),

    // on TOC next-prev issue
    // AIAA, AMetSoc, Ammons, APHA, 
    HtmlNodeFilters.tagWithAttribute("div", "id", "nextprev"),
    // ASCE, SEG, SIAM
    HtmlNodeFilters.tagWithAttribute("div", "id", "prevNextNav"),

    //on TOC left column with listing of all volumes/issues
    HtmlNodeFilters.tagWithAttribute("ul", "class", "volumeIssues"),
    
    // Not all Atypon plugins necessarily need this but MANY do and it is
    // an insidious source of over crawling
    new NodeFilter() {
      @Override public boolean accept(Node node) {
        if (!(node instanceof LinkTag)) return false;
        String allText = ((CompositeTag)node).toPlainTextString();
        return corrections.matcher(allText).find();
      }
    },
  };

  /** Create an array of NodeFilters that combines the atyponBaseFilters with
   *  the given array
   *  @param nodes The array of NodeFilters to add
   */
  private NodeFilter[] addTo(NodeFilter[] nodes) {
    NodeFilter[] result  = Arrays.copyOf(baseAtyponFilters, baseAtyponFilters.length + nodes.length);
    System.arraycopy(nodes, 0, result, baseAtyponFilters.length, nodes.length);
    return result;
  }
  
  /** Create a FilteredInputStream that excludes the the atyponBaseFilters
   * @param au  The archival unit
   * @param in  Incoming input stream
   * @param encoding  The encoding
   */
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in, String encoding) throws PluginException{

    return new HtmlFilterInputStream(in, encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(baseAtyponFilters)));
  }
  
  /** Create a FilteredInputStream that excludes the the atyponBaseFilters and
   * moreNodes
   * @param au  The archival unit
   * @param in  Incoming input stream
   * @param encoding  The encoding
   * @param moreNodes An array of NodeFilters to be excluded with atyponBaseFilters
   */ 
  public InputStream createFilteredInputStream(ArchivalUnit au,
              InputStream in, String encoding, NodeFilter[] moreNodes) 
    throws PluginException {
    NodeFilter[] bothFilters = addTo(moreNodes);
    return new HtmlFilterInputStream(in, encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(bothFilters)));
  }
}
