/*
 * $Id: GPOFDSysSitemapsLinkExtractorFactory.java,v 1.2 2011-03-28 23:19:23 thib_gc Exp $
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.usdocspln.gov.gpo.fdsys;

import java.io.*;
import java.net.*;

import javax.xml.parsers.*;

import org.htmlparser.tags.BaseHrefTag;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

public class GPOFDSysSitemapsLinkExtractorFactory implements LinkExtractorFactory {

  protected static Logger logger = Logger.getLogger("GPOFDSysSitemapsLinkExtractorFactory");
  
  @Override
  public LinkExtractor createLinkExtractor(String mimeType) throws PluginException {
    return new GPOFDSysSitemapsLinkExtractor();
  }
  
  protected static class GPOFDSysSitemapsLinkExtractor implements LinkExtractor {
    
    protected class SitemapsHandler extends DefaultHandler {
      
      protected Callback callback;
      
      protected URL baseUrl;
      
      protected StringBuilder locBuilder;
      
      public SitemapsHandler(URL baseUrl,
                             Callback cb) {
        this.baseUrl = baseUrl;
        this.callback = cb;
        this.locBuilder = null;
      }
      
      @Override
      public void characters(char[] ch,
                             int start,
                             int length)
          throws SAXException {
        super.characters(ch, start, length);
        if (locBuilder != null) {
          locBuilder.append(ch, start, length);
        }
      }
      
      @Override
      public void startElement(String uri,
                               String localName,
                               String qName,
                               Attributes attributes)
          throws SAXException {
        super.startElement(uri, localName, qName, attributes);
        if ("loc".equalsIgnoreCase(qName)) {
          locBuilder = new StringBuilder();
        }
      }
      
      @Override
      public void endElement(String uri,
                             String localName,
                             String qName)
          throws SAXException {
        super.endElement(uri, localName, qName);
        if ("loc".equalsIgnoreCase(qName)) {
          String url = locBuilder.toString();
          locBuilder = null;
          if (logger.isDebug2()) {
            logger.debug2("Found at " + baseUrl.toString() + ": " + url);
          }
          try {
            String resolved = UrlUtil.resolveUri(baseUrl, url);
            callback.foundLink(resolved);
          }
          catch (MalformedURLException mue) {
            logger.warning("Malformed URL at " + baseUrl.toString() + ": " + url, mue);
          }
        }
      }
      
    }
    
    @Override
    public void extractUrls(ArchivalUnit au,
                            InputStream in,
                            String encoding,
                            String srcUrl,
                            Callback cb)
        throws IOException, PluginException {
      SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
      saxParserFactory.setValidating(false);
      InputSource inputSource = new InputSource(in);
      inputSource.setEncoding(encoding);
      inputSource.setPublicId(srcUrl);
      try {
        SAXParser saxParser = saxParserFactory.newSAXParser();
        saxParser.parse(inputSource, new SitemapsHandler(new URL(srcUrl), cb));
      }
      catch (ParserConfigurationException pce) {
        throw new IOException("Error setting up SAX parser", pce);
      }
      catch (SAXException se) {
        throw new IOException("Error while parsing", se);
      }
    }

  }

  public static void main(String[] args) throws Exception {
    GPOFDSysSitemapsLinkExtractor ext = new GPOFDSysSitemapsLinkExtractor();
    ext.extractUrls(null,
                    new FileInputStream("/tmp/foo.xml"),
                    "UTF-8",
                    "http://www.gpo.gov/smap/fdsys/sitemap_2010/2010_PLAW_sitemap.xml",
                    new Callback() {
      @Override
      public void foundLink(String url) {
        System.out.println(url);
      }
    });
  }
  
}
