/**
 * $Id: SitemapParser.java,v 1.1 2013-03-19 18:42:23 ldoan Exp $
 */

/**

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

package org.lockss.extractor;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.lockss.extractor.Sitemap.SitemapType;
import org.lockss.util.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * SitemapParser.java - Parses a Sitemap (http://www.sitemaps.org/)
 * This class is modified from SourceForge open-source by Frank McCown.
 */
public class SitemapParser {
  
  private static Logger log =
      Logger.getLogger(SitemapParser.class);

  /** According to the specs, 50K URLs per Sitemap is the max */
  private int MAX_URLS_ALLOWED = 50000;
  
  /** Sitemap docs must be limited to 10MB (10,485,760 bytes) */
  public static int MAX_BYTES_ALLOWED = 10485760;
  
  
  /**
   * Parse the given XML Sitemap content.
   * @throws SitemapException 
   * @throws IOException 
   * @throws SAXException 
   */
  public Sitemap processXmlSitemap(InputStream inStream, String encoding) 
      throws SitemapException, SAXException, IOException {
		
    Document doc = null;
		
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();			
      //doc = dbf.newDocumentBuilder().parse(inStream);
      InputSource is = new InputSource(inStream);
      is.setEncoding(encoding);
      doc = dbf.newDocumentBuilder().parse(is);
          
    }
    catch (ParserConfigurationException pce) {
      log.error("Error setting up DocumentBuilder", pce);
      throw new SitemapException(pce);
    }	
    catch (IOException e) {
      log.error("IO error", e);
    }
    
    /** If this document contains <sitemapindex> */
    NodeList nodeList = doc.getElementsByTagName("sitemapindex");
    if (nodeList.getLength() > 0) {			
      nodeList = doc.getElementsByTagName("sitemap");
      return (parseXMLSitemapIndex(nodeList));
    }
    
    /** If this document contains <urlset> */
    nodeList = doc.getElementsByTagName("urlset");
    if (nodeList.getLength() > 0) {
      nodeList = doc.getElementsByTagName("url");
      return (parseXmlSitemapUrlSet(nodeList));
    }
    //else if (doc.getElementsByTagName("link").getLength() > 0) {
      // Could be RSS or Atom
      //parseSyndicationFormat(sitemapUrl, doc);
    //}
    else {
      throw new SitemapException("Unknown Sitemap format");
    }	

  } /** end processXmlSitemap */
	
   
  /**
   * Parse XML Sitemap, using <sitemapindex>.
   * @param nodeList
   * @return Sitemap object
   * @throws SitemapException 
   */
  private Sitemap parseXMLSitemapIndex(NodeList nodeList) throws SitemapException {
  			
    Sitemap sitemap = new Sitemap(SitemapType.INDEX);
        		
    /** Loop through the <sitemap>s */
    for (int i = 0; i < nodeList.getLength(); i++) {
      
      if (i >= MAX_URLS_ALLOWED) {
        log.siteWarning("Exceeds max urls allowed 50K" + i);
        break;
      }
      
      Node node = nodeList.item(i);

      SitemapUrl sitemapUrl = processSitemapNode(node);
      sitemap.addSitemapUrl(sitemapUrl);
      log.debug("  " + (i+1) + ". " + sitemapUrl);
        
    } // for
    
    return (sitemap);

  } /** end parseXMLSitemapIndex */

  /**
   * Get sitemap node element values for <loc> and <lastmod>.
   * @param node
   * @return
   */
  private SitemapUrl processSitemapNode(Node node) {
    
    String url = null;
    String lastmod = null;
        
    /** node name is <sitemap> */
    NodeList cNodes = node.getChildNodes();
    
    /** nodename is <loc> or <lastmod>*/
    for (int i = 0; i < cNodes.getLength(); i++) {
      Node cNode = cNodes.item(i);

      if (cNode.getNodeName() == "loc") {
        url = getNodeValueByName(cNode, "loc");
        continue;
      }
      
      if (cNode.getNodeName() == "lastmod") {
        lastmod = getNodeValueByName(cNode, "lastmod");
        continue;
      }
      
    }
    
    return (new SitemapUrl(url, lastmod));
    
  } /** end processSitemapNode */
  
  private String getNodeValueByName(Node node, String name) {
    
    if (node.getNodeName() == name) {
      return (node.getTextContent());
    }
    
    return (null);
  }
  
  /**
   * Parse XML containing Sitemap, using <urlset>.
   * 
   * Example of a Sitemap:
   * <?xml version="1.0" encoding="UTF-8"?>
        <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
	  <url>
 	    <loc>http://www.example.com/</loc>
 	    <lastmod>2005-01-01</lastmod>
 	    <changefreq>monthly</changefreq>
 	    <priority>0.8</priority>
	  </url>
	  <url>
 	    <loc>http://www.example.com/catalog?item=12&amp;desc=vacation_hawaii</loc>
 	    <changefreq>weekly</changefreq>
	  </url>
       </urlset>
   *
   * @param doc
   * @throws SitemapException 
   */
  private Sitemap parseXmlSitemapUrlSet(NodeList nodeList) throws SitemapException {
    
    Sitemap sitemap = new Sitemap(SitemapType.XML);
    
    /**Loop through the <url> nodes */
    for (int i = 0; i < nodeList.getLength(); i++) {
      
      if (i >= MAX_URLS_ALLOWED) {
        log.siteWarning("Exceeds max urls allowed 50K" + i);
        break;
      }
          
      Node node = nodeList.item(i);
      SitemapUrl sitemapUrl = processSitemapUrlSetNode(node);
      sitemap.addSitemapUrl(sitemapUrl);
      log.debug("  " + (i+1) + ". " + sitemapUrl);
             
    }	
    
    return (sitemap);
  
  } /** end parseXmlSitemapUrlSet */
  
  /**
   * Get UrlSet node element values for
   * <loc>, <lastmod>, <changefreq> and <priority>.
   * @param node
   * @return
   */
  private SitemapUrl processSitemapUrlSetNode(Node node) {
    
    String url = null;
    String lastmod = null;
    String changeFreq = null;
    String priority = null;
        
    /** node name is <sitemap> */
    NodeList cNodes = node.getChildNodes();
    
    /** node name is <loc>, <lastmod>, <changefred> or <priority> */
    for (int i = 0; i < cNodes.getLength(); i++) {
      Node cNode = cNodes.item(i);
      
      if (cNode.getNodeName() == "loc") {
        url = getNodeValueByName(cNode, "loc");
        continue;
      }
      
      if (cNode.getNodeName() == "lastmod") {
        lastmod = getNodeValueByName(cNode, "lastmod");
        continue;
      }
      
      if (cNode.getNodeName() == "changefreq") {
        changeFreq = getNodeValueByName(cNode, "changefreq");
        continue;
      }

      if (cNode.getNodeName() == "priority") {
        priority = getNodeValueByName(cNode, "priority");
          continue;
      }
    
    }
    
    return (new SitemapUrl(url, lastmod, changeFreq, priority));
    
  } /** end processSitemapUrlSetNode */
  
} /** end class SitemapParser */

