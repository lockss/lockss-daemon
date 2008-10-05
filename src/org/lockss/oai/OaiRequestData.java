/*
 * $Id: OaiRequestData.java,v 1.9 2007-01-17 19:51:48 troberts Exp $
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

package org.lockss.oai;

/**
 * An Object that contains all the information need from the plugin to
 * issue an Oai Request
 */
public class OaiRequestData {

  private String oaiRequestHandlerUrl;
  private String metadataNamespaceUrl;
  private String urlContainerTagName;
  private String auSetSpec;
  private String metadataPrefix;


  /**
   * Constructor
   *
   * @param oaiRequestHandlerUrl Oai request handler URL of an Oai repository
   * @param namespaceUrl metadata name space that the Oai reponse record using
   * @param tagName XML tag name that contain an articules' URL
   * @param setSpec Set name an articles belongs to
   * @param prefix metadata prefix of Oai reponse record (e.g. oai_dc)
   */
  public OaiRequestData(String oaiRequestHandlerUrl,
			String namespaceUrl,
			String tagName,
			String setSpec,
			String prefix
			) {
    if (oaiRequestHandlerUrl == null) {
      throw new IllegalArgumentException("Called with null Oai request handler Url");
    } else if (namespaceUrl == null) {
      throw new IllegalArgumentException("Called with null metadata namespace Url");
    } else if (tagName == null) {
      throw new IllegalArgumentException("Called with null url container tag name");
    } else if (setSpec == null) {
      //       throw new IllegalArgumentException("Called with null url container tag name");
    } else if (prefix == null) {
      throw new IllegalArgumentException("Called with null metadata prefix");
    }
    this.oaiRequestHandlerUrl = oaiRequestHandlerUrl;
    this.metadataNamespaceUrl = namespaceUrl;
    this.urlContainerTagName = tagName;
    this.auSetSpec = setSpec;
    this.metadataPrefix = prefix;
  }

  /**
   * Constructor with the knowledge of metadata handler
   *
   * @param oaiRequestHandlerUrl Oai request handler URL of an Oai repository
   * @param setSpec Set name an articles belongs to
   * @param metadataHandler
   */
  public OaiRequestData(String oaiRequestHandlerUrl,
			String setSpec,
			OaiMetadataHandler metadataHandler) {
    if (oaiRequestHandlerUrl == null) {
      throw new IllegalArgumentException("Called with null Oai request handler Url");
    } else if (setSpec == null) {
//       throw new IllegalArgumentException("Called with null url container tag name");
    } else if (metadataHandler == null) {
      throw new IllegalArgumentException("Called with null metadataHandler");
    }
    this.oaiRequestHandlerUrl = oaiRequestHandlerUrl;
    this.auSetSpec = setSpec;
    this.metadataPrefix = metadataHandler.getMetadataPrefix();
    this.metadataNamespaceUrl = metadataHandler.getMetadataNamespaceUrl();
    this.urlContainerTagName = metadataHandler.getUrlContainerTagName();
  }

  /**
   * Gets the OaiRequestHandlerUrl
   *
   * @return oai request handler url of the oai repository
   */
  public String getOaiRequestHandlerUrl(){
    return oaiRequestHandlerUrl;
  }

  /**
   * Gets the Metadata Namespace's Url from plugin tools. It is needed
   * to execute xpath
   *
   * e.g. "http://purl.org/dc/elements/1.1/" for Dublin Core (dc)
   *
   * This inforamtion is needed for xpath to execute
   * @return the metadata namespace url
   */
  public String getMetadataNamespaceUrl(){
    return metadataNamespaceUrl;
  }

  /**
   * Gets the XML tag name that contains Urls need to be crawled
   * in the metadata. It is needed to execute xpath
   *
   * e.g. "identifier" in Dublin Core case
   * @return the XML tag name contains Url to be crawled
   */
  public String getUrlContainerTagName(){
    return urlContainerTagName;
  }

  /**
   * Gets the SetSpec of the urls belong to. It is needed to issue
   * an Oai request.
   * In LOCKSS, SetSpec equals to AU name. We should enforce publisher to
   * have articles (urls) that belongs to the same AU have the same SetSpec
   *
   * e.g. "journal:dec04"
   * @return the SetSpec (AU) the crawl is on
   */
  public String getAuSetSpec(){
    return auSetSpec;
  }

  /**
   * Gets the metadata format prefix that the crawl is interested.It
   * is needed to issue an Oai request.
   *
   * e.g. "oai_dc", this is the one that MUST be implements in a
   * Oai repository
   * @return metadata format prefix the crawl is interested.
   */
  public String getMetadataPrefix(){
    return metadataPrefix;
  }

}
