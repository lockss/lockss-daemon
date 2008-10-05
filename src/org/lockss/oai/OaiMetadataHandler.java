/*
 * $Id: OaiMetadataHandler.java,v 1.3 2005-10-11 05:45:13 tlipkis Exp $
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

import java.util.Set;
import org.w3c.dom.NodeList;

/**
 *  Interface for different Oai Metadata Handler.
 *
 */
public interface OaiMetadataHandler
{

  /**
   * Setup the metadataHandler by getting  a list of <metadata>...</metadata>
   * xml nodes. Then, process them in according to different implementation
   * of OaiMetadataHandler.
   * The least amount of implementation, it should collect urls from the
   * given nodeList
   *
   * @param metadataNodeList a nodelist with a bunch of <metadata>..</metadata> tag
   *
   */
  public void setupAndExecute(NodeList metadataNodeList);

  /**
   * Return the set of urls collected in the oai response
   *
   * @return the set of urls
   */
  public Set getArticleUrls();

  /**
   * Returns the metadata format prefix that the crawl is interested.
   *
   * e.g. "oai_dc", this is the format that a Oai repository must
   * support
   * @return metadata format prefix the crawl is interested.
   */
  public String getMetadataPrefix();

  /**
   * return the Metadata Namespace url
   *
   * e.g. "http://purl.org/dc/elements/1.1/" for Dublin Core (oai_dc)
   *
   * @return the metadata namespace url
   */
  public String getMetadataNamespaceUrl();

  /**
   * Returns the XML tag name that contains Urls need to be crawled
   * in the metadata. It is needed to execute xpath
   *
   * e.g. "identifier" in Dublin Core case,
   * <oai_dc:identifier>http://example.com</
   * @return the XML tag name contains Url to be crawled
   */
  public String getUrlContainerTagName();

}
