/*
 * $Id: OaiCrawler.java,v 1.1 2004-10-20 18:51:10 dcfok Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.crawler;

import java.util.*;
import java.io.*;
import org.lockss.util.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.state.*;
import org.lockss.oai.*;

import ORG.oclc.oai.harvester2.verb.ListRecords;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.transform.TransformerException;
import javax.xml.parsers.ParserConfigurationException;
import java.lang.NoSuchFieldException;
import org.xml.sax.SAXException;
import org.apache.xpath.XPathAPI;
import org.apache.xpath.objects.XObject;
import java.text.SimpleDateFormat;

public class OaiCrawler extends FollowLinkCrawler {

//   public static final String PARAM_FOLLOW_LINK =
//     Configuration.PREFIX + "CrawlerImpl.oai_crawl_follow_link";
//   public static final boolean DEFAULT_FOLLOW_LINK = true;  //XXXOAI

//  protected static boolean followLink;

  private OaiCrawlSpec spec;

  private static Logger logger = Logger.getLogger("OaiCrawler");

  //dateformat following ISO 8601
  private static SimpleDateFormat iso8601DateFormatter = new SimpleDateFormat ("yyyy-MM-dd");

  public OaiCrawler(ArchivalUnit au, CrawlSpec crawlSpec, AuState aus){
    super(au, crawlSpec, aus);
    spec = (OaiCrawlSpec) crawlSpec;
    String oaiHandlerUrl = spec.getOaiRequestData().getOaiRequestHandlerUrl();
    crawlStatus = new Crawler.Status(au, ListUtil.list(oaiHandlerUrl), getType());
  }

  public int getType() {
    return Crawler.OAI;
  }

  /**
   * Oai crawler can crawl in 2 mode, the follow link mode and not follow link mode.
   * The "follow link" mode will crawl everything from the URLs we got from OAI response 
   * following links on each page within crawl spec.
   * The "not follow link" mode will crawl just the URLs we got from OAI repsonse, it will 
   * follow the link from the content of the URLs.
   */
  protected boolean doCrawl0(){
    Set extractedUrls = new HashSet();
    //    followLink = Configuration.getBooleanParam(PARAM_FOLLOW_LINK, DEFAULT_FOLLOW_LINK);
    if (shouldFollowLink() ) {
      logger.info("crawling in follow link mode");
      return super.doCrawl0();
    } else {
      logger.info("crawling in not follow link mode");
      setMaxDepth(1);
      return super.doCrawl0();
    }
  }

  /***
   * gather information to issue a OAI request and return a set of
   * url with updated content
   *
   * information needed:
   * 1. OAI handler URL (startingUrls from crawlSpec)
   * 2. harvest verb (ListRecords)
   * 3. metadata format (oai_dc)
   * 4. set name (if we use setSpec to specific a AU)
   * 5. from  (date of last crawl)
   * 6. until (date of this current crawl)
   *
   * a example request will be
   * http://www.biomedcentral.com/oai/2.0/?verb=ListRecords&from=2004-09-05&
   *   until=2004-09-08&metadataPrefix=oai_dc&set=journal%3A3001
   *
   * @return a set of Url that is updated within the from and until time range.
   */
  protected Set getUrlsToFollow() {
    Set updatedUrls = new HashSet();
    OaiRequestData oaiRequestData = spec.getOaiRequestData();
    String baseUrl = oaiRequestData.getOaiRequestHandlerUrl();
    String setSpec = oaiRequestData.getAuSetSpec();
    String metadataPrefix = oaiRequestData.getMetadataPrefix();    
    String from = getFromTime();
    String until = getUntilTime();
    ListRecords listRecords = null;
    
    try {
      listRecords = new ListRecords(baseUrl, from, until, setSpec,
						metadataPrefix);
    } catch (IOException e){
      logger.error("IOException when doing new ListRecords()", e);
    } catch (ParserConfigurationException e) {
      logger.error("ParserConfigurationException when doing new ListRecords()", e);
    } catch (SAXException e) {
      logger.error("SAXException when doing new ListRecords()", e);
    } catch (TransformerException e) {
      logger.error("TransformerException in new ListRecords()", e);
    }

    while (listRecords != null) {
      try {
	//XXX we can handle error better
	NodeList errors = listRecords.getErrors();   
	if (errors != null && errors.getLength() > 0) {
	  logger.error("Found errors");
	  int length = errors.getLength();
	  for (int i=0; i<length; ++i) {
	    Node item = errors.item(i);
	    logger.debug3(item.toString());
	  }
	  logger.debug3("Error record: " + listRecords.toString());
	  //abort crawl if error encountered ?
	  crawlAborted = true;
	  break;
	}
      } catch (TransformerException e) {
	logger.error("TransformerException in getting nodeList of error from oai reply document", e);
	crawlAborted = true;
      } 
 
      //see what is inside the response
      logger.debug3("The content of listRecord : \n" + listRecords.toString());

      try {
	// xpath experssion that parse through the doc and extract all the links in <dc:identifier> 
	// XXX we can let the publisher design the path/metadataFormat in the future version
	String xpath = "//*[namespace-uri()='"+ oaiRequestData.getMetadataNamespaceUrl() + "' and local-name()='"+ oaiRequestData.getUrlContainerTagName() +"']";

	// Get the matching elements
        NodeList nodeList = listRecords.getNodeList(xpath);
	logger.debug3("nodeList length = " + nodeList.getLength());

	// Process the elements in the nodelist
        for (int i=0; i<nodeList.getLength(); i++) {
          // add the Urls to the updatedUrls set
	  Node node = nodeList.item(i);
	  if (node != null) {
	    XObject xObject = XPathAPI.eval(node, "string()");
	    String str = xObject.str();
	    updatedUrls.add(str);
	    //logger.debug3("node (" + i + ") value = " + str);
	  }
        }
      } catch (TransformerException e) {
	logger.error("Thrown TransformerException when getting the NodeList " + 
                     "of <dc:idenitifer> from oai reply document", e);
	crawlAborted = true;
      }

      try {
	String resumptionToken = listRecords.getResumptionToken();
	logger.debug3("resumptionToken: " + resumptionToken);
	if (resumptionToken == null || resumptionToken.length() == 0) {
	  listRecords = null;
	} else {
	  listRecords = new ListRecords(baseUrl, resumptionToken);
	}
      } catch (TransformerException e){
	logger.error("TransformerException when getting resumptionToken from oai reply document",e);
      } catch (IOException e){
	logger.error("IOException in listRecords.getResumptionToken() ", e);
      } catch (NoSuchFieldException e) {
	logger.error("NoSuchFieldException in listRecords.getResumptionToken()", e);
      } catch (ParserConfigurationException e) {
	logger.error("ParserConfigurationException in listRecords.getResumptionToken()", e);
      } catch (SAXException e) {
	logger.error("SAXException in new ListRecords()", e);
      }

    } // loop until there is no resumptionToken
    lvlCnt = 1;
    //testing purpose
    logger.debug3("Urls from Oai repository: \n" + updatedUrls);
    return updatedUrls;
  }

//   /**
//    * getting the namespace's url of the metadata format
//    * To Do:
//    * adding code in OaiCrawlSpec and plugin class to get the url from plugin writer
//    */
//   private String getNamespaceUrl(){
//     String ns = spec.getMetadataNamespaceUrl();
//     if (ns==null) {
//       // default namespace
//       ns = "http://purl.org/dc/elements/1.1/";
//     }
//     return ns;
//   }

//   /**
//    * getting the XML tag name whose content is the url of the page we want to crawl
//    * To Do:
//    * adding code in the OaiCrawlSpec and plugin class to get tag name from plugin writer
//    */
//   private String getTagName(){
//     String tagName = spec.getUrlContainerTagName();
//     if (tagName == null) {
//       // default tagName
//       tagName = "identifier";
//     }
//     return tagName;
//   }

//   /**
//    * getting the Oai request handler url from publisher/repository
//    * To Do:
//    * adding code in the OaiCrawlSpec and plugin class to get this Oai request handler url
//    */
//   private String getOaiHandlerUrl(){
//     //testing purposes
//     //String oaiHandlerUrl = "http://www.biomedcentral.com/oai/2.0/";

//     String oaiHandlerUrl = (String) spec.getStartingUrls().get(0);
//     return oaiHandlerUrl;
//   }

  /**
   * getting the last crawl time from AuState of the current AU
   * 
   * //XXX noted: OAI protocol just enforce day granularity, we still need
   * to check if-modified-since 
   */  
  private String getFromTime(){
    Date lastCrawlDate = new Date(aus.getLastCrawlTime());
    logger.debug3("from=" + lastCrawlDate.toString());
    String lastCrawlDateString = iso8601DateFormatter.format(lastCrawlDate);
    
    //String lastCrawlDateString = "2004-09-08";
    return lastCrawlDateString;
  }

  /**
   * getting the time until when we want all the changed page's 
   * url from the publisher/repository
   * 
   * //XXX is this the policy we want ? 
   * Now it will return the time when this method is called
   */
  private String getUntilTime(){
    Date currentDate = new Date();
    logger.debug3("until="+currentDate.toString());
    String currentDateString = iso8601DateFormatter.format(currentDate);
    
    //String currentDateString = "2004-09-14";
    return currentDateString;
  }

//   /**
//    * getting the AU name from AU/plugin class
//    * To Do:
//    * adding code in AU/plugin to return the AU name set by the plugin writer 
//    *
//    * //XXX do we want to use SetSpec to store AU name ? 
//    * SetSpec is implemented in OAI protocol for selective harvesting.
//    */
//   private String getSetSpec(){
//     String auSetSpec = spec.getAuSetSpec();
//     return auSetSpec;
//   }

//   /**
//    * getting the metadata format/prefix from plugin/AU 
//    * default as "oai_dc"
//    * 
//    * //XXX now it is assumed publisher/repository will have "oai_dc" as the metadata format
//    * for future, we can let the publisher to specify its own metadataPrefix
//    * provided that they also supply an namespace url and a tag name
//    */
//   private String getMetadataPrefix(){
//     String metadataPrefix = spec.getMetadataPrefix();
//     if (metadataPrefix == null) {
//       metadataPrefix = "oai_dc";
//     }
//     return metadataPrefix;
//   }
  
  protected boolean shouldFollowLink(){
    return spec.getFollowLinkFlag();
  }
}
