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