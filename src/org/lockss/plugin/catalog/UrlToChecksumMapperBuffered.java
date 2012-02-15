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

/**
 * Utility class to map the urls of a single {@link ArchivalUnit} to their
 * checksum
 */
public class UrlToChecksumMapperBuffered extends UrlToChecksumMapper {

  protected UrlToChecksumMap generateMap(ArchivalUnit au) throws Exception {
    UrlToChecksumMap map = new UrlToChecksumMap();
    iterateUrls(au, new UrlProcessor(map));
    return map;
  }

  /**
   * Generates and marshals to XML the url-to-checksum mapping for the specified
   * {@link ArchivalUnit} by using an intermediate Map as buffer
   */
  @Override
  public void generateXMLMap(ArchivalUnit au, Writer out) throws Exception {
    UrlToChecksumMap map = generateMap(au);

    XStream xStream = new XStream(new DomDriver());
    xStream.alias("UrlToChecksumMap", UrlToChecksumMap.class);
    xStream.registerConverter(new UrlToChecksumMapConverter());
    xStream.toXML(map, out);
  }

  private class UrlProcessor implements UrlToChecksumMapper.UrlProcessor {
    UrlToChecksumMap map;

    public UrlProcessor(UrlToChecksumMap map) {
      this.map = map;
    }

    @Override
    public void process(String url, String checksum) {
      map.put(url, checksum);
    }
  }
}
