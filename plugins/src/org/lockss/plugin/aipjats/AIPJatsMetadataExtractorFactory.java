/* $Id: AIPJatsMetadataExtractorFactory.java,v 1.1 2013-01-23 23:03:19 ldoan Exp $
 
 Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

 
package org.lockss.plugin.aipjats;

import java.io.*;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.*;
import org.apache.commons.collections.map.*;
import javax.xml.xpath.XPathExpressionException;

/*
 * This file implements a FileMetadataExtractor for American Institute Of
 * Physics Source content (JATS format).
 * 
 * Files used to write this class constructed from AIP JATS Source FTP archive:
 * ./074101_1-testnobodyback.xml from
 * http://clockss-ingest.lockss.org/sourcefiles/aip-dev/2012/
 */

public class AIPJatsMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
    
  static Logger log = Logger.getLogger("AIPJatsMetadataExtractorFactory");

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
	throws PluginException {
    
    return new AIPJatsMetadataExtractor();
    
  } // FileMetadataExtractor

  public static class AIPJatsMetadataExtractor 
     implements FileMetadataExtractor {
	 
    // NodeValue for creating value of authors from contrib tag
    static private final NodeValue CONTRIB_VALUE = new NodeValue() {
      @Override
      public String getValue(Node node) { // node name is "name"
        
        if (node == null) {
          return null;
        }
        
        NodeList childNodeList = node.getChildNodes();
        String fname = null, sname = null;
          
        for (int i = 0; i < childNodeList.getLength(); i++) {
          Node childNode = childNodeList.item(i);
                    
          if (childNode.getNodeName().equals("surname")) {
            sname = childNode.getTextContent();
          }
          else if (childNode.getNodeName().equals("given-names")) {
            fname = childNode.getTextContent();
          }
        } // for
        
        // return name as [surname], [firstname]
        return (sname + ", " + fname) ;
      }
      
    }; // CONTRIB_VALUE
    
    
    // For each child c of node n, if c is an instance of CDATASection
    //   remove child c from parent
    // else call recursively with parameter c
    static public void deleteCDATANode(Node n) {
      
      if (n == null) {
        return;
      }
      
      NodeList childNodeList = n.getChildNodes();
      for (int i = 0; i < childNodeList.getLength(); i++) {
        Node childNode = childNodeList.item(i);
        
        if (childNode instanceof CDATASection) {
          childNode.getParentNode().removeChild(childNode);
          log.info("Found CDATASection node - deleted");
        } else {
          deleteCDATANode(childNode); // calling recursively
        }
      } // for
      
    } // deleteCDATANode
    
    // NodeValue for creating value of article title from article-title tag
    static private final NodeValue ARTICLE_TITLE_VALUE = new NodeValue() {
      @Override
      public String getValue(Node node) { // node name is "article-title"
        
        if (node == null) {
          return null;
        }
        
        deleteCDATANode(node);
        
        return (node.getTextContent());
      }; // getValue
      
    }; // ARTICLE_TITLE_VALUE
	    
    // Map of raw xpath key to node value function
    static private final Map<String,XPathValue> nodeMap = 
        new HashMap<String,XPathValue>();
    
    static {
      // normal journal article schema
      nodeMap.put("/article/front/journal-meta/journal-title-group/journal-title",
          XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/article/front/journal-meta/issn",
          XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/article/front/article-meta/contrib-group/contrib/name",
          CONTRIB_VALUE);
      nodeMap.put("/article/front/article-meta/volume",
          XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/article/front/article-meta/history/date/string-date",
          XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/article/front/article-meta/issue",
          XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/article/front/article-meta/article-id",
          XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/article/front/article-meta/title-group/article-title",
          ARTICLE_TITLE_VALUE);
    }

    // Map of raw xpath key to cooked MetadataField
    static private final MultiValueMap xpathMap = new MultiValueMap();
    
    static {
      // normal journal article schema
      xpathMap.put("/article/front/journal-meta/journal-title-group/journal-title",
          MetadataField.FIELD_JOURNAL_TITLE);
      xpathMap.put("/article/front/journal-meta/issn",
          MetadataField.FIELD_ISSN);
      xpathMap.put("/article/front/article-meta/contrib-group/contrib/name",
          MetadataField.FIELD_AUTHOR);
      xpathMap.put("/article/front/article-meta/volume",
          MetadataField.FIELD_VOLUME);
      xpathMap.put("/article/front/article-meta/history/date/string-date", 
          MetadataField.FIELD_DATE);
      xpathMap.put("/article/front/article-meta/issue",
          MetadataField.FIELD_ISSUE);
      xpathMap.put("/article/front/article-meta/article-id",
          MetadataField.FIELD_DOI);
      xpathMap.put("/article/front/article-meta/title-group/article-title",
          MetadataField.FIELD_ARTICLE_TITLE);
    }

    
    // Use XmlMetadataExtractor to extract raw metadata, map
    // to cooked fields, then extract extra tags by reading the file.
    //
    // @param target the MetadataTarget
    // @param cu the CachedUrl from which to read input
    // @param emitter the emiter to output the resulting ArticleMetadata
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException, PluginException {
	      
      log.debug3("The MetadataExtractor attempted to extract metadata from cu: "+cu);
        
      ArticleMetadata am = do_extract(target, cu, emitter);
      emitter.emitMetadata(cu,  am);
      
    } // extract

    // Called by method extract()
    public ArticleMetadata do_extract(MetadataTarget target, CachedUrl cu, Emitter emit)
        throws IOException, PluginException {
          
      try {
        ArticleMetadata am = 
            new XmlDomMetadataExtractor(nodeMap).extract(target, cu);
        am.cook(xpathMap);
        return am;
      } catch (XPathExpressionException ex) {
        PluginException ex2 = new PluginException("Error parsing XPaths");
        ex2.initCause(ex);
        throw ex2;
      }
    } // do_extract
      
  } // AIPJatsMetadataExtractor
  
} // AIPJatsMetadataExtractorFactory
