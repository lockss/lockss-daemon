package org.lockss.plugin.catalog;

import java.io.Writer;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import org.xml.sax.ContentHandler;
import org.lockss.plugin.ArchivalUnit;
/*
 * Utility class to map the urls of a single {@link ArchivalUnit} to their checksum
 */
public class UrlToChecksumMapperDirect extends UrlToChecksumMapper {
  
  /*
   * Generates and marshals to XML the url-to-checksum mapping 
   * for the specified {@link ArchivalUnit} by using an intermediate Map as buffer
   */
  @Override
  public void generateXMLMap(ArchivalUnit au, Writer out) throws Exception {
    OutputFormat of = new OutputFormat("XML", "UTF-8", true);
    XMLSerializer serializer = new XMLSerializer(out, of);
    ContentHandler hd = serializer.asContentHandler();
    hd.startDocument();
    hd.startElement("", "", "UrlToChecksumMap", null);
    
    iterateUrls(au, new UrlProcessor(hd));
    
    hd.endElement("", "", "UrlToChecksumMap");
    hd.endDocument();

  }
  
  private class UrlProcessor implements UrlToChecksumMapper.UrlProcessor {
    private ContentHandler hd;
    public UrlProcessor(ContentHandler hd) {
      this.hd = hd;
    }
    @Override
    public void process(String url, String checksum) throws Exception {
      hd.startElement("", "", "entry", null);
      hd.startElement("", "", "url", null);
      hd.characters(url.toCharArray(), 0, url.length());
      hd.endElement("", "", "url");
      hd.startElement("", "", "checksum", null);
      hd.characters(checksum.toCharArray(), 0, checksum.length());
      hd.endElement("", "", "checksum");
      hd.endElement("","","entry");
    }
  }
}
