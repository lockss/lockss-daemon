/*
 * $Id$
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

/*
 * Some portion of this code is Copyright.
 * 
 * SitemapUrl.java - Represents a URL found in a Sitemap 
 *  
 * Copyright 2009 Frank McCown
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.lockss.extractor;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.lockss.extractor.Sitemap.SitemapType;
import org.lockss.util.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Parses a Sitemap created according to the sitemap protocol 
 * http://www.sitemaps.org. Modified from SourceForge open-source 
 * by Frank McCown.
 */
public class SitemapParser {
  
  private static Logger log =
      Logger.getLogger(SitemapParser.class);

  /** According to the specs, 50K URLs per Sitemap is the max */
  private int MAX_URLS_ALLOWED = 50000;
  /** Sitemap docs must be limited to 10MB (10,485,760 bytes) */
  public static int MAX_BYTES_ALLOWED = 10485760;
  
  /**
   * Parses the given XML Sitemap content.
   * 
   * @param inStream the input XML file
   * @param encoding the encoding value passed in
   * @return Sitemap the Sitemap object
   * @throws SitemapException ParserConfigurationException or SAXException
   * @throws IOException if error in processing inStream
   */
  public Sitemap processXmlSitemap(InputStream inStream, String encoding) 
      throws SitemapException, IOException {
    Document doc = null;
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();			
      InputSource is = new InputSource(inStream);
      is.setEncoding(encoding);
      doc = dbf.newDocumentBuilder().parse(is);
    }
    catch (ParserConfigurationException pce) {
      throw new SitemapException(pce);
    } catch (SAXException saxe) {
      throw new SitemapException(saxe);
    }	
    
    // If this document contains <sitemapindex>
    // doc has only one node sitemapindex
    Element rootElem = doc.getDocumentElement();
    String rootName = rootElem.getNodeName(); 
    NodeList nodeList = rootElem.getChildNodes(); // list or sitemap nodes
    if (rootName.equals("sitemapindex")) {
      if (nodeList.getLength() > 0) { // nodeList of <sitemap> nodes
        return (parseXMLSitemapIndex(nodeList));
      }
      log.siteError("Sitemapindex has no <sitemap> nodes");
      return (null);
    } else if (rootName.equals("urlset")) {
      if (nodeList.getLength() > 0) { // nodeList of <url> nodes
        return (parseXmlSitemapUrlSet(nodeList));
      }
      log.siteError("Sitemap urlset has no <url> nodes");
      return (null);
    }    
    // Can handle RSS or Atom
    else {
      throw new SitemapException("Unknown Sitemap format");
    }	
  }
	
  /**
   * Parses XML Sitemap, using &lt;sitemapindex&gt;.
   * 
   * @param nodeList
   * @return
   * @throws SitemapException
   */
  private Sitemap parseXMLSitemapIndex(NodeList nodeList)
      throws SitemapException {
    Sitemap sitemap = new Sitemap(SitemapType.INDEX);

    // Loop through the <sitemap> nodes
    for (int i = 0; i < nodeList.getLength(); i++) {
      if (i >= MAX_URLS_ALLOWED) {
        log.siteWarning("Exceeds max urls allowed 50K" + i);
        break;
      }
      Node node = nodeList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        SitemapUrl sitemapUrl = processSitemapIndexNode(node);
        try {
          sitemap.addSitemapUrl(sitemapUrl);
        } catch (NullPointerException npe) {
          log.siteWarning("sitemapUrl is null", npe);
        }
      }
    }
    return (sitemap);
  }

  /**
   * Gets sitemap node element values for &lt;loc&gt; and &lt;lastmod&gt;.
   * 
   * @param node the node element containing &lt;sitemap&gt;
   * @return a SitemapUrl or null
   */
  private SitemapUrl processSitemapIndexNode(Node node) {
    String url = null;
    String lastmod = null;
    NodeList cNodes = node.getChildNodes(); // node name is <sitemap>
    for (int i = 0; i < cNodes.getLength(); i++) { // nodename is <loc> or <lastmod>
      Node cNode = cNodes.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        if (cNode.getNodeName() == "loc") {
          url = cNode.getTextContent();
          continue;
        }
        if (cNode.getNodeName() == "lastmod") {
          lastmod = cNode.getTextContent();
          continue;
        }
      }
    }
    if (url != null) {
      return (new SitemapUrl(url, lastmod));
    }
    return (null);
  }
  
  /**
   * Parses XML containing Sitemap, using &lt;urlset&gt;.
   * 
   * @param nodeList contains &lt;url&gt; nodes
   * @return a Sitemap
   * @throws SitemapException
   */
  private Sitemap parseXmlSitemapUrlSet(NodeList nodeList)
      throws SitemapException {
    Sitemap sitemap = new Sitemap(SitemapType.XML);
    // Loop through the <url> nodes
    for (int i = 0; i < nodeList.getLength(); i++) {
      if (i >= MAX_URLS_ALLOWED) {
        log.siteWarning("Exceeds max urls allowed 50K" + i);
        break;
      }
      Node node = nodeList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        SitemapUrl sitemapUrl = processSitemapUrlSetNode(node);
        try {
          sitemap.addSitemapUrl(sitemapUrl);
        } catch (NullPointerException npe) {
          log.siteWarning("sitemapUrl is null", npe);
        }
      }
    }
    return (sitemap);
  }
  
  /**
   * Gets UrlSet node element values for
   * &lt;loc&gt;, &lt;lastmod&gt;, &lt;changefreq&gt; and &lt;priority&gt;.

   * @param node the &lt;url&gt; node element
   * @return a SitemapUrl or null
   */
  private SitemapUrl processSitemapUrlSetNode(Node node) {
    String url = null;
    String lastmod = null;
    String changeFreqStr = null;
    String priority = null;
    NodeList cNodes = node.getChildNodes(); // node name is <sitemap>
    // node name is <loc>, <lastmod>, <changefred> or <priority>
    for (int i = 0; i < cNodes.getLength(); i++) {
      Node cNode = cNodes.item(i);
      if (cNode.getNodeType() == Node.ELEMENT_NODE) {
        if (cNode.getNodeName() == "loc") {
          url = cNode.getTextContent();
          continue;
        }
        if (cNode.getNodeName() == "lastmod") {
          lastmod = cNode.getTextContent();
          continue;
        }
        if (cNode.getNodeName() == "changefreq") {
          changeFreqStr = cNode.getTextContent();
          continue;
        }
        if (cNode.getNodeName() == "priority") {
          priority = cNode.getTextContent();
            continue;
        }
      }
    }
    if (url != null) {
      return (new SitemapUrl(url, lastmod, changeFreqStr, priority));
    }
    return (null);
  }
  
}