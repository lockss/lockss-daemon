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

package org.lockss.plugin.silverchair.oup;

import java.io.*;
import java.util.Arrays;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.htmlparser.Attribute;
import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

public class OupScHtmlHashFilterFactory implements FilterFactory {

  private static final Logger log = Logger.getLogger(OupScHtmlHashFilterFactory.class);
  
  @Override
  public InputStream createFilteredInputStream(final ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    
    // HTML transform to remove generated attributes like <a href="...?Expires">
    HtmlTransform xform = new HtmlTransform() {
      @Override
      public NodeList transform(NodeList nodeList) throws IOException {
        try {
          nodeList.visitAllNodesWith(new NodeVisitor() {
            @Override
            public void visitTag(Tag tag) {
              String tagName = tag.getTagName().toLowerCase();
              try {
                if ("a".equals(tagName) ||
                    "div".equals(tagName) ||
                    "img".equals(tagName)) {
                  Attribute a = tag.getAttributeEx(tagName);
                  Vector<Attribute> v = new Vector<Attribute>();
                  v.add(a);
                  if (tag.isEmptyXmlTag()) {
                    Attribute end = tag.getAttributeEx("/");
                    v.add(end);
                  }
                  tag.setAttributesEx(v);
                }
                super.visitTag(tag);
              }
              catch (Exception exc) {
                log.debug2("Internal error (visitor)", exc); // Ignore this tag and move on
              }
            }
          });
        }
        catch (ParserException pe) {
          log.debug2("Internal error (parser)", pe); // Bail
        }
        return nodeList;
      }
    };
    
    InputStream filtered = new HtmlFilterInputStream(
      in,
      encoding,
      new HtmlCompoundTransform(
    	  HtmlNodeFilterTransform.include(new OrFilter(new NodeFilter[] {
              HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-list-resources"),
              HtmlNodeFilters.tagWithAttributeRegex("div", "id", "resourceTypeList-OUP_Issue"),
              HtmlNodeFilters.tagWithAttributeRegex("div", "id", "ContentColumn"),
              HtmlNodeFilters.tagWithAttributeRegex("span", "class", "content-inner-wrap"),
              HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-body"),
              HtmlNodeFilters.tagWithAttributeRegex("div", "class", "OUP_Issues_List"),
              HtmlNodeFilters.tagWithAttributeRegex("div", "class", "IssuesAndVolumeListManifest"),
              HtmlNodeFilters.tagWithAttributeRegex("img", "class", "content-image"),
              
          })),
      
    	  HtmlNodeFilterTransform.exclude(new OrFilter(new NodeFilter[] {
    		  HtmlNodeFilters.tagWithAttributeRegex("div", "class", "comment"),
    		  HtmlNodeFilters.tagWithAttributeRegex("div", "class", "graphic-wrap"),
    	  })),
    	  xform
      )
    );
    
    Reader reader = FilterUtil.getReader(filtered, encoding);
    
    // Remove white space
    Reader whiteSpaceFilter = new WhiteSpaceFilter(reader);
    InputStream ret =  new ReaderInputStream(whiteSpaceFilter);
    return ret;
    // Instrumentation
  }
}
