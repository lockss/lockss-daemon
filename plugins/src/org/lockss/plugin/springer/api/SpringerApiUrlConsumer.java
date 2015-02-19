/*
 * $Id: PermissionUrlConsumer.java,v 1.3 2014/11/25 01:41:43 wkwilson Exp $
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

package org.lockss.plugin.springer.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.lockss.daemon.Crawler;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.util.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class SpringerApiUrlConsumer extends SimpleUrlConsumer implements Callback {
  static Logger logger = Logger.getLogger(SpringerApiUrlConsumer.class);
  protected SpringerApiCrawlSeed seed;
  protected String charset;
  protected List<String> doiList = new ArrayList<String>(100);
  
  public SpringerApiUrlConsumer(Crawler.CrawlerFacade crawlFacade,
                                FetchedUrlData fud, SpringerApiCrawlSeed seed) {
    super(crawlFacade, fud);
    this.seed = seed;
    charset = AuUtil.getCharsetOrDefault(fud.headers);
  }
  
  public void consume() throws IOException {
    SpringerPamLinkExtractor ple = new SpringerPamLinkExtractor();
    try {
      ple.extractUrls(au, fud.input, charset, fud.origUrl, this);
    } catch (PluginException e) {
      throw new IOException("Error while parsing", e);
    }
    seed.updateDoiList(doiList, false);
  }

  public void foundLink(String doi) {
    doiList.add(doi);
  }
}

class SpringerPamLinkExtractor implements LinkExtractor {

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
    
    try {
      saxParser.parse(inputSource, new PamHandler(cb));
    }
    catch (SAXException se) {
      throw new IOException("Error while parsing", se);
    }
  }
  
  protected static class PamHandler extends org.xml.sax.helpers.DefaultHandler {
    
    public InputSource resolveEntity(String publicID, String systemID) 
      throws SAXException {
      return new InputSource(new StringReader(""));
    }
    
    protected Callback callback;
    protected StringBuilder doiBuilder;
    
    public PamHandler(Callback cb) {
      this.callback = cb;
      this.doiBuilder = null;
    }
    
    @Override
    public void characters(char[] ch,
                           int start,
                           int length)
        throws SAXException {
      super.characters(ch, start, length);
      if (doiBuilder != null) {
        doiBuilder.append(ch, start, length);
      }
    }
    
    @Override
    public void startElement(String uri,
                             String localName,
                             String qName,
                             Attributes attributes)
        throws SAXException {
      super.startElement(uri, localName, qName, attributes);
      if (!"prism:doi".equalsIgnoreCase(qName)) {
        doiBuilder = new StringBuilder();
      }
    }
    
    @Override
    public void endElement(String uri,
                           String localName,
                           String qName)
        throws SAXException {
      super.endElement(uri, localName, qName);
      if ("prism:doi".equalsIgnoreCase(qName)) {
        String doi = doiBuilder.toString();
        doiBuilder = null;
        callback.foundLink(doi);
      }
    }
  }
}
