/*
 * $Id: BaseOaiMetadataHandler.java,v 1.7 2007-03-26 20:48:54 troberts Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.oai;

import java.util.HashSet;
import java.util.Set;
import org.lockss.util.Logger;
import org.w3c.dom.*;

/**
 * Base implementation of OaiMetadataHandler
 */
public class BaseOaiMetadataHandler implements OaiMetadataHandler{

  protected static Logger logger = Logger.getLogger("BaseOaiMetadataHandler");
  protected String metadataPrefix;
  protected String metadataNamespaceUrl;
  protected String urlContainerTagName;
  protected NodeList metadataNodeList;
  protected Set extractedUrls;

//   /**
//    * Constructor with no arguments
//    */
//   public BaseOaiMetadataHandler() {
//     metadataNodeList = null;
//   }

  /**
   * Constructor with some arguments, which is useful if we want
   * to support plugin definable OaiMetadataHandler.
   *
   * @param metadataPrefix the metadata prefix string
   * @param metadataNamespaceUrl the url that describe the metadata namespace
   * @param urlContainerTagName the tag name where a url can be found
   */
  public BaseOaiMetadataHandler(String metadataPrefix,
				String metadataNamespaceUrl,
				String urlContainerTagName)
    throws NullPointerException {
      if (metadataPrefix == null){
	  throw new NullPointerException("metadataPrefix is null");
      }
      if (metadataNamespaceUrl == null){
	  throw new NullPointerException("metadataNamespaceUrl is null");
      }
      if (urlContainerTagName == null){
	  throw new NullPointerException("urlContainerTagName is null");
      }

      metadataNodeList = null;
      this.metadataPrefix = metadataPrefix;
      this.metadataNamespaceUrl = metadataNamespaceUrl;
      this.urlContainerTagName = urlContainerTagName;
  }

  /**
   * Extracted urls from the given  list of  <metadata>...</metadata> xml nodes.
   * Different metadata implementation can override this method by adding
   * more behaviors w.r.t. different metadata.
   *
   * @param metadataNodeList a nodelist with a bunch of <metadata>..</metadata> tag
   */
  public void setupAndExecute(NodeList metadataNodeList) {
    this.metadataNodeList = metadataNodeList;
    extractedUrls = collectArticleUrls();
  }

  /**
   * Collect Urls within a specific xml tag name under a particular
   * metadata namespace from node list
   */
  protected Set collectArticleUrls() {
    Set articleUrls = new HashSet();
    logger.debug3("Processing Metadata nodes");
    for(int i = 0; i < metadataNodeList.getLength(); i++) {
	Node node = metadataNodeList.item(i);
	if(node != null) {
   	  //logger.debug3("metadataNodeList ("+i+") = " + OaiHandler.displayXML(node) );
	  NodeList list =
	    ((Element)node).getElementsByTagNameNS(metadataNamespaceUrl, urlContainerTagName);
	  if (list.getLength() > 0) {
	    String str = list.item(0).getFirstChild().getNodeValue();
	    articleUrls.add(str);
	    logger.debug3("node (" + i + ") value = " + str);
	  } else {
	    logger.siteError("No XML elements with the tag name : "+urlContainerTagName+
	                     " in the namespace : "+metadataNamespaceUrl);
	  }
	}
    }

    return articleUrls;
  }

  /**
   * Return the set of urls collected in the oai response
   *
   * @return the set of urls
   */
  public Set getArticleUrls() {
    return extractedUrls;
  }

  /**
   * Returns the metadata format prefix that the crawl is interested.
   *
   * e.g. "oai_dc", this is the format that a Oai repository must
   * support
   * @return metadata format prefix the crawl is interested.
   */
  public String getMetadataPrefix() {
        return metadataPrefix;
  }

  /**
   * return the Metadata Namespace url
   *
   * e.g. "http://purl.org/dc/elements/1.1/" for Dublin Core (oai_dc)
   *
   * @return the metadata namespace url
   */
  public String getMetadataNamespaceUrl() {
        return metadataNamespaceUrl;
  }

  /**
   * Returns the XML tag name that contains Urls need to be crawled
   * in the metadata. It is needed to execute xpath
   *
   * e.g. "identifier" in Dublin Core case,
   * <oai_dc:identifier>http://example.com</
   * @return the XML tag name contains Url to be crawled
   */
  public String getUrlContainerTagName() {
    return urlContainerTagName;
  }

}
