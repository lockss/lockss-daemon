package org.lockss.plugin.catalog;

import java.io.Writer;
import java.util.Iterator;

import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.plugin.CachedUrlSetNode;
import org.lockss.util.CIProperties;
import org.lockss.util.StringUtil;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
/*
 * Utility class to map the urls of a single {@link ArchivalUnit} to their checksum
 */
public class UrlToChecksumMapper {
  
  /*
   * Generates a {@link UrlToChecksumMap} object that contains the url-to-checksum mapping 
   * for the specified {@link ArchivalUnit}
   */
  protected static UrlToChecksumMap generateMap(ArchivalUnit au) {
    UrlToChecksumMap map = new UrlToChecksumMap();
    
    CachedUrlSet cus = au.getAuCachedUrlSet();
    Iterator iter = cus.contentHashIterator();
    while (iter.hasNext()) {
      CachedUrlSetNode node = (CachedUrlSetNode) iter.next();
      switch (node.getType()) {
        case CachedUrlSetNode.TYPE_CACHED_URL_SET:
          //Do nothing: No properties attached to a CachedUrlSet
          break;
        case CachedUrlSetNode.TYPE_CACHED_URL:
          //only process entries with content
          if( node.hasContent() ) {
            String url = node.getUrl();
            CachedUrl cu = (CachedUrl) node;
            CIProperties headers = cu.getProperties();
            //headers.getProperty(CachedUrl.PROPERTY_NODE_URL) is also available. Should we use that one instead ?
            String checksum = headers.getProperty(CachedUrl.PROPERTY_CHECKSUM);
            //only store entries with checksums
            if( ! StringUtil.isNullString(checksum) ) {
              map.put(url, checksum);
            }
          }
          break;
      }
    }
    return map;
  }
  
  /*
   * Generates and marshals to XML the url-to-checksum mapping 
   * for the specified {@link ArchivalUnit}
   */
  public static void generateXMLMap(ArchivalUnit au, Writer out) {
    UrlToChecksumMap map = generateMap(au);
    
    XStream xStream = new XStream(new DomDriver());
    xStream.alias("UrlToChecksumMap",UrlToChecksumMap.class);
    xStream.registerConverter(new UrlToChecksumMapConverter());
    xStream.toXML(map, out);
  }
}
