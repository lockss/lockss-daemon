/*
 * $Id: PeerJHtmlCrawlFilterFactory.java,v 1.1 2014-10-06 04:40:55 ldoan Exp $
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

package org.lockss.plugin.peerj;

import java.io.InputStream;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/*
 * Filters out preprints articles appeared in PeerJ Archives site.
 *   ex: near 'Note that a peer-reviewed article of this PrePrint also exists'
 *       in https://peerj.com/articles/175/ linking to
 *       https://peerj.com/preprints/1/
 * or also articles appeared on PeerJ PrePrint Archives site.
 *   ex: near 'Note that a PrePrint of this article also exists' in
 *       https://peerj.com/preprints/59/ linking to 
 *       https://peerj.com/articles/199/
 */
public class PeerJHtmlCrawlFilterFactory implements FilterFactory {
  
  public InputStream createFilteredInputStream(ArchivalUnit au, 
      InputStream in, String encoding) throws PluginException {
    
    Logger log = Logger.getLogger(PeerJHtmlCrawlFilterFactory.class);
    String param = au.getConfiguration().get("peerj_site");
    log.info("param: " + param);
    
    if ("archives".equals(param)) {
      NodeFilter[] filters = new NodeFilter[] {
          HtmlNodeFilters.tagWithAttributeRegex(
              "a", "href", "/preprints/[0-9]+/")
      };
      return new HtmlFilterInputStream(in, encoding, 
          HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
      
    } else if ("archives-preprints".equals(param)) {
      NodeFilter[] filters = new NodeFilter[] {
          HtmlNodeFilters.tagWithAttributeRegex(
              "a", "href", "/articles/[0-9]+/")
      };
      return new HtmlFilterInputStream(in, encoding, 
          HtmlNodeFilterTransform.exclude(new OrFilter(filters)));      
    } else {
      // this should not happen since PeerJ currently has only we sites
      // 'archives' or 'archives-preprints'
      throw new PluginException(
          "Can only be 'archives' or 'archives-preprints'");
    }
  }   
}
