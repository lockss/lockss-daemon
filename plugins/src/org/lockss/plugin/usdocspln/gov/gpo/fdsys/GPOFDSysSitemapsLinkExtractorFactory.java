/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.usdocspln.gov.gpo.fdsys;

import java.io.*;
import java.net.*;

import javax.xml.parsers.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;
import org.xml.sax.*;

public class GPOFDSysSitemapsLinkExtractorFactory implements LinkExtractorFactory {

  protected static Logger log = Logger.getLogger(GPOFDSysSitemapsLinkExtractorFactory.class);
  
  protected static class NoNeedToContinueException extends SAXException {
    
    public NoNeedToContinueException() {
      super();
    }
    
  }
  
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
          if (log.isDebug2()) {
            log.debug2("Found at " + baseUrl.toString() + ": " + url);
          }
          try {
            String resolved = UrlUtil.resolveUri(baseUrl, url);
            callback.foundLink(resolved);
          }
          catch (MalformedURLException mue) {
            log.warning("Malformed URL at " + baseUrl.toString() + ": " + url, mue);
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
    public void extractUrls(final ArchivalUnit au,
                            InputStream in,
                            String encoding,
                            final String srcUrl,
                            final Callback cb)
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
        saxParser.parse(inputSource, new SitemapsHandler(new URL(srcUrl), 
            new Callback() {
          @Override
          public void foundLink(String url) {
            if (au != null) {
              if (HttpToHttpsUtil.UrlUtil.isSameHost(srcUrl, url)) {
                url = HttpToHttpsUtil.AuUtil.normalizeHttpHttpsFromBaseUrl(au, url);
              }
            }
            cb.foundLink(url);
          }
      }));
      }
      catch (NoNeedToContinueException nntce) {
        if (log.isDebug2()) {
          log.debug2("No need to parse " + srcUrl);
        }
      }
      catch (SAXException se) {
        throw new IOException("Error while parsing", se);
      }
    }

  }

}
