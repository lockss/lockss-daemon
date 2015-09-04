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

package org.lockss.plugin.highwire.bmj;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;

import org.htmlparser.Attribute;
import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.filter.html.HtmlTransform;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.highwire.HighWireDrupalHtmlFilterFactory;
import org.lockss.util.Logger;

public class BMJDrupalHtmlHashFilterFactory extends HighWireDrupalHtmlFilterFactory {
  
  private static final Logger log = Logger.getLogger(BMJDrupalHtmlHashFilterFactory.class);
  
  /**
   * body tag.  Registered with PrototypicalNodeFactory to cause body
   * to be a CompositeTag.  See code samples in org.htmlparser.tags.
   * @see HtmlFilterInputStream#makeParser()
   */
  public static class bodyTag extends CompositeTag {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * The set of names handled by this tag.
     */
    private static final String[] mIds = new String[] {"body"};
    
    @Override
    public String[] getIds() {
      return mIds;
    }
    
  }
  
  // Transform to modify select attribute from select tag ( a )
  // href attributes change over time, use UrlNormalizer <a href
  protected static HtmlTransform xformHref = new HtmlTransform() {
    @Override
    public NodeList transform(NodeList nodeList) throws IOException {
      try {
        nodeList.visitAllNodesWith(new NodeVisitor() {
          @Override
          public void visitTag(Tag tag) {
            String tagName = tag.getTagName().toLowerCase();
            try {
              if ("a".equals(tagName)) {
                Attribute ha = tag.getAttributeEx("href");
                if (ha != null) {
                  String url = ha.getValue();
                  if (url.contains(BMJDrupalUrlNormalizer.BMJ_UN_STATIC) && 
                      url.contains(BMJDrupalUrlNormalizer.BMJ_UN_REPLACE)) {
                    Matcher mat = BMJDrupalUrlNormalizer.BMJ_UN_PREFIX_PAT.matcher(url);
                    if (mat.find()) {
                      url = mat.replaceFirst(BMJDrupalUrlNormalizer.BMJ_UN_REPLACE);
                      ha.setValue(url);
                    }
                  }
                }
              }
            }
            catch (Exception exc) {
              log.debug2("Internal error (visitor)", exc); // Ignore this tag and move on
            }
            // Always
            super.visitTag(tag);
          }
        });
      }
      catch (ParserException pe) {
        log.debug2("Internal error (parser)", pe); // Bail
      }
      return nodeList;
    }
  };
  
  protected static NodeFilter[] filters = new NodeFilter[] {
    HtmlNodeFilters.tag("head"),
    // only highwire-markup contents are hashed
    HtmlNodeFilters.allExceptSubtree(HtmlNodeFilters.tag("body"),
        HtmlNodeFilters.tagWithAttribute("div", "class", "highwire-markup")),
    //    HtmlNodeFilters.tag("article")),
    // remove cit-extra, etc. that change based on institution access or over time
    HtmlNodeFilters.tagWithAttribute("div", "class", "cit-extra"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "subscribe"),
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "pager"),
    HtmlNodeFilters.tagWithAttribute("div", "class", "panel-separator"),
  };
  
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    
    InputStream filtered = super.createFilteredInputStream(au, in, encoding, filters);
    ((HtmlFilterInputStream) filtered).registerTag(new bodyTag());
    
    return new HtmlFilterInputStream(filtered, encoding, xformHref);
  }
  
  @Override
  public boolean doWSFiltering() {
    return false;
  }
  @Override
  public boolean doTagAttributeFiltering() {
    return false;
  }
  @Override
  public boolean doXformToText() {
    return false;
  }
}
