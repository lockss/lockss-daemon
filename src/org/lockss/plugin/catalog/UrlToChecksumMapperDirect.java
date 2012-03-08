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
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import org.xml.sax.ContentHandler;
import org.lockss.plugin.ArchivalUnit;

/**
 * Utility class to map the urls of a single {@link ArchivalUnit} to their
 * checksum
 */
public class UrlToChecksumMapperDirect extends UrlToChecksumMapper {

  /**
   * Generates and marshals to XML the url-to-checksum mapping for the specified
   * {@link ArchivalUnit} by using an intermediate Map as buffer
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
      hd.endElement("", "", "entry");
    }
  }
}
