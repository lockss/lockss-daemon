/*
 * $Id: BaseOaiMetadataHandler.java,v 1.1 2005-01-12 02:21:41 dcfok Exp $
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

public abstract class BaseOaiMetadataHandler implements OaiMetadataHandler {

    protected static Logger logger = Logger.getLogger("BaseOaiMetadataHandler");
    protected String metadataPrefix;
    protected String metadataNamespaceUrl;
    protected String urlContainerTagName;
    protected NodeList metadataNodeList;
    protected Set extractedUrls;

    public BaseOaiMetadataHandler() {
        metadataNodeList = null;
    }

    public BaseOaiMetadataHandler(String metadataPrefix, 
				  String metadataNamespaceUrl, 
				  String urlContainerTagName) {
        metadataNodeList = null;
        this.metadataPrefix = metadataPrefix;
        this.metadataNamespaceUrl = metadataNamespaceUrl;
        this.urlContainerTagName = urlContainerTagName;
    }

    public void setupAndExecute(NodeList metadataNodeList) {
        this.metadataNodeList = metadataNodeList;
        extractedUrls = collectArticleUrls(metadataNodeList);
    }

    protected Set collectArticleUrls(NodeList nodeList) {
        Set extractedUrls = new HashSet();
        logger.debug3("Processing Metadata nodes");
        for(int i = 0; i < nodeList.getLength(); i++)
        {
            Node node = nodeList.item(i);
            if(node != null)
            {
                NodeList list = 
		  ((Element)node).getElementsByTagNameNS(getMetadataNamespaceUrl(), 
							 getUrlContainerTagName());
                String str = list.item(0).getFirstChild().getNodeValue();
                extractedUrls.add(str);
                logger.debug3("node (" + i + ") value = " + str);
            }
        }

        return extractedUrls;
    }

    public Set getArticleUrls() {
        return extractedUrls;
    }

    public String getMetadataPrefix() {
        return metadataPrefix;
    }

    public String getMetadataNamespaceUrl() {
        return metadataNamespaceUrl;
    }

    public String getUrlContainerTagName() {
        return urlContainerTagName;
    }

}
