/*

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.plugin.catalog;

import java.io.Writer;
import java.util.Iterator;

import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.plugin.CachedUrlSetNode;
import org.lockss.util.CIProperties;
import org.lockss.util.StringUtil;

public abstract class UrlToChecksumMapper {

  /**
   * Generates and marshals to XML the url-to-checksum mapping for the specified
   * {@link ArchivalUnit}
   */
  public abstract void generateXMLMap(ArchivalUnit au, Writer out)
      throws Exception;

  protected interface UrlProcessor {
    void process(String url, String checksum) throws Exception;
  }

  /**
   * Iterates through all Urls of an {@link ArchivalUnit} and calls the supplied
   * visitor
   */
  protected void iterateUrls(ArchivalUnit au, UrlProcessor processor)
      throws Exception {
    CachedUrlSet cus = au.getAuCachedUrlSet();
    Iterator iter = cus.contentHashIterator();
    while (iter.hasNext()) {
      CachedUrlSetNode node = (CachedUrlSetNode) iter.next();
      switch (node.getType()) {
      case CachedUrlSetNode.TYPE_CACHED_URL_SET:
        // Do nothing: No properties attached to a CachedUrlSet
        break;
      case CachedUrlSetNode.TYPE_CACHED_URL:
        // only process entries with content
        if (node.hasContent()) {
          String url = node.getUrl();
          CachedUrl cu = (CachedUrl) node;
          CIProperties headers = cu.getProperties();
          // headers.getProperty(CachedUrl.PROPERTY_NODE_URL) is also available.
          // Should we use that one instead ?
          String checksum = headers.getProperty(CachedUrl.PROPERTY_CHECKSUM);
          // only store entries with checksums
          if (!StringUtil.isNullString(checksum)) {
            processor.process(url, checksum);
          }
        }
        break;
      }
    }
  }

}