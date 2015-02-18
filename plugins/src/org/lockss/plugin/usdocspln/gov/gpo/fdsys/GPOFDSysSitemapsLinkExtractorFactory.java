/*
 * $Id$
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

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.usdocspln.gov.gpo.fdsys.GPOFDSysSitemapsLinkExtractorFactory.GPOFDSysSitemapsLinkExtractor.SitemapsHandler.NoNeedToContinueException;
import org.lockss.util.*;
import org.xml.sax.*;

public class GPOFDSysSitemapsLinkExtractorFactory implements LinkExtractorFactory {

  protected static Logger logger = Logger.getLogger("GPOFDSysSitemapsLinkExtractorFactory");
  
  @Override
  public LinkExtractor createLinkExtractor(String mimeType) throws PluginException {
    return new GPOFDSysSitemapsLinkExtractor();
  }
  
  protected static class GPOFDSysSitemapsLinkExtractor implements LinkExtractor {
    
    /**
     * <p>Not thread-safe.</p>
     */
    protected static class SitemapsHandler extends org.xml.sax.helpers.DefaultHandler {
      
      public InputSource resolveEntity(String publicID, String systemID) 
        throws SAXException {
        return new InputSource(new StringReader(""));
      }
      
      protected static class NoNeedToContinueException extends SAXException {
        
        public NoNeedToContinueException() {
          super();
        }
        
      }
      
      protected Callback callback;
      
      protected URL baseUrl;
      
      protected StringBuilder locBuilder;
      
      protected boolean hasStarted;
      
      public SitemapsHandler(URL baseUrl,
                             Callback cb) {
        this.baseUrl = baseUrl;
        this.callback = cb;
        this.locBuilder = null;
        this.hasStarted = false;
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
        if (hasStarted) {
          // Not the top-level element and is a sitemap: look for <loc>
          if (!"loc".equalsIgnoreCase(qName)) {
            locBuilder = new StringBuilder();
          }
        }
        else {
          // Top-level element: bail if not a sitemap
          if ("urlset".equalsIgnoreCase(qName)) {
            hasStarted = true;
          }
          else {
            hasStarted = false;
            throw new NoNeedToContinueException();
          }
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
      
      @Override
      public void endDocument() throws SAXException {
        super.endDocument();
        hasStarted = false;
      }
      
    }
    
    @Override
    public void extractUrls(ArchivalUnit au,
                            InputStream in,
                            String encoding,
                            String srcUrl,
                            Callback cb)
        throws IOException, PluginException {
      // Create SAX parser
      SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
      saxParserFactory.setValidating(false);
      InputSource inputSource = new InputSource(in);
      inputSource.setEncoding(encoding);
      inputSource.setPublicId(srcUrl);
      SAXParser saxParser = null;
      try {
        saxParser = saxParserFactory.newSAXParser();
      }
      catch (ParserConfigurationException pce) {
        throw new IOException("Error setting up SAX parser", pce);
      }
      catch (SAXException se) {
        throw new IOException("Error while parsing", se);
      }
      
      // Parse document; may bail early with custom NoNeedToContinueException
      try {
        saxParser.parse(inputSource, new SitemapsHandler(new URL(srcUrl), cb));
      }
      catch (NoNeedToContinueException nntce) {
        if (logger.isDebug2()) {
          logger.debug2("No need to parse " + srcUrl);
        }
      }
      catch (SAXException se) {
        throw new IOException("Error while parsing", se);
      }
    }

  }

}
