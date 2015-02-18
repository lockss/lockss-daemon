/*
 * $Id$
 */

/*

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

package org.lockss.plugin.sage;

import java.io.*;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.TextValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.*;
import org.apache.commons.collections.map.*;
import javax.xml.xpath.XPathExpressionException;

/**
 * This file implements a FileMetadataExtractor for American Institute Of
 * Physics Source content.
 * 
 * Files used to write this class constructed from AIP FTP archive:
 * ~/2010/AIP_xml_9.tar.gz/AIP_xml_9.tar/./APPLAB/vol_96/iss_1/
 */

public class SageTriggeredContentMetadataExtractorFactory
implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("SageTriggeredContentMetadataExtractorFactory");
  
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new SageXmlMetadataExtractor();
  }
  
  public static class SageXmlMetadataExtractor 
  implements FileMetadataExtractor {
    
    /** NodeValue for creating value of subfields from author tag */
    static private final NodeValue AUTHOR_VALUE = new NodeValue() {
      @Override
      public String getValue(Node node) {
        if (node == null) {
          return null;
        }
        
        NodeList nameNodes = node.getChildNodes();
        String fname = null, mname = null, sname = null;
        for (int k = 0; k < nameNodes.getLength(); k++) {
          Node nameNode = nameNodes.item(k);
          if (nameNode.getNodeName().equals("fn")) {
            fname = nameNode.getTextContent();
          } else if (nameNode.getNodeName().equals("ln")) {
            sname = nameNode.getTextContent();
          } else if (nameNode.getNodeName().equals("mn")) {
            mname = nameNode.getTextContent();
          }
        }
        // return name as [surname], [firstname] [middlename]
        return sname + ", " + fname + ((mname == null) ? "" : " " + mname);
      }
    };
    
    /** NodeValue for creating value of subfields from date tag */
    static private final NodeValue DATE_VALUE = new NodeValue() {
      @Override
      public String getValue(Node node) {
        if (node == null) {
          return null;
        }
        
        NodeList dateNodes = node.getChildNodes();
        String year = "", month = null;
        for (int k = 0; k < dateNodes.getLength(); k++) {
          Node dateNode = dateNodes.item(k);
          if (dateNode.getNodeName().equals("yy")) {
            year = dateNode.getTextContent();
          } else if (dateNode.getNodeName().equals("mm")) {
            month = dateNode.getTextContent();
          }
        }
        // return name as [month]/[year]
        return ((month == null) ? "" : month + "/") + year;
      }
    };
    
    // extend XPathValue, needed because some titles have extra CR chars
    static private final XPathValue TITLE_VALUE = new TextValue() {
      @Override
      public String getValue(String s) {
        return (s == null) ? s : s.replace("\n", " ");
      }
    };
    
    /** Map of raw xpath key to node value function */
    static private final Map<String,XPathValue> nodeMap = 
        new HashMap<String,XPathValue>();
    static {
      // normal journal article schema
      nodeMap.put("/SAGEmeta/@doi", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/SAGEmeta/header/jrn_info/jrn_title", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/SAGEmeta/header/jrn_info/ISSN", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/SAGEmeta/header/jrn_info/vol", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/SAGEmeta/header/jrn_info/iss", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/SAGEmeta/header/jrn_info/date", DATE_VALUE);
      nodeMap.put("/SAGEmeta/header/jrn_info/pub_info/pub_name", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/SAGEmeta/header/art_info/art_title", TITLE_VALUE);
      nodeMap.put("/SAGEmeta/header/art_info/art_author/per_aut", AUTHOR_VALUE);
      nodeMap.put("/SAGEmeta/header/art_info/spn", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/SAGEmeta/header/art_info/epn", XmlDomMetadataExtractor.TEXT_VALUE);
    }
    
    /** Map of raw xpath key to cooked MetadataField */
    static private final MultiValueMap xpathMap = new MultiValueMap();
    static {
      // normal journal article schema
      xpathMap.put("/SAGEmeta/@doi", MetadataField.FIELD_DOI);
      xpathMap.put("/SAGEmeta/header/jrn_info/jrn_title", MetadataField.FIELD_JOURNAL_TITLE);
      xpathMap.put("/SAGEmeta/header/jrn_info/ISSN", MetadataField.FIELD_ISSN);
      xpathMap.put("/SAGEmeta/header/jrn_info/vol", MetadataField.FIELD_VOLUME);
      xpathMap.put("/SAGEmeta/header/jrn_info/iss", MetadataField.FIELD_ISSUE);
      xpathMap.put("/SAGEmeta/header/jrn_info/date", MetadataField.FIELD_DATE);
      xpathMap.put("/SAGEmeta/header/jrn_info/pub_info/pub_name", MetadataField.FIELD_PUBLISHER);
      xpathMap.put("/SAGEmeta/header/art_info/art_title", MetadataField.FIELD_ARTICLE_TITLE);
      xpathMap.put("/SAGEmeta/header/art_info/art_author/per_aut", MetadataField.FIELD_AUTHOR);
      xpathMap.put("/SAGEmeta/header/art_info/spn", MetadataField.FIELD_START_PAGE);
      xpathMap.put("/SAGEmeta/header/art_info/epn", MetadataField.FIELD_END_PAGE);
    }
    
    /**
     * Use XmlMetadataExtractor to extract raw metadata, map
     * to cooked fields, then extract extra tags by reading the file.
     * 
     * @param target the MetadataTarget
     * @param cu the CachedUrl from which to read input
     * @param emitter the emiter to output the resulting ArticleMetadata
     */
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException, PluginException {
      ArticleMetadata am = do_extract(target, cu, emitter);
      emitter.emitMetadata(cu,  am);
    }
    
    /**
     * Use XmlMetadataExtractor to extract raw metadata, map
     * to cooked fields, then extract extra tags by reading the file.
     * 
     * @param target the MetadataTarget
     * @param in the Xml input stream to parse
     * @param emitter the emitter to output the resulting ArticleMetadata
     */
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
    }
  }
}