/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.laserinstituteofamerica;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.TextValue;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.*;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * This class implements a FileMetadataExtractorFactory for the
 * American Society of Civil Engineers..
 * <p>
 * Files used to write this class constructed from ASCE FTP archive:
 * ~/2010/ASCE_xml_9.tar.gz/ASCE_xml_9.tar/./APPLAB/vol_96/iss_1/
 */
public class LaserInstituteOfAmericaMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  static Logger log = 
    Logger.getLogger("LaserInstituteOfAmericaMetadataExtractorFactory");

  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    return new LaserInstituteOfAmericaMetadataExtractor();
  }

  /**
   * This class implements a FileMetadataExtractor for the
   * American Society of Civil Engineers.
   */
  public static class LaserInstituteOfAmericaMetadataExtractor 
    implements FileMetadataExtractor {
    
    /** NodeValue for creating value of subfields from author tag */
    static private final XPathValue AUTHOR_VALUE = new NodeValue() {
      @Override
      public String getValue(Node node) {
        if (node == null) {
          return null;
        }
        
        NodeList nameNodes = node.getChildNodes();
        String fname = null, mname = null, sname = null;
        for (int k = 0; k < nameNodes.getLength(); k++) {
          Node nameNode = nameNodes.item(k);
          if (nameNode.getNodeName().equals("fname")) {
            fname = nameNode.getTextContent();
          } else if (nameNode.getNodeName().equals("surname")) {
            sname = nameNode.getTextContent();
          } else if (nameNode.getNodeName().equals("middlename")) {
            mname = nameNode.getTextContent();
          }
        }
        // return name as [surname], [firstname] [middlename]
        return sname + ", " + fname + ((mname == null) ? "" : " " + mname);
      }
    };

    /** NodeValue for creating value of subfields from author tag */
    static private final XPathValue BOOKREVIEW_TITLE_VALUE = new TextValue() {
      @Override
      public String getValue(String s) {
        return "Review of: " + s;
      }
    };
    
    /** Map of raw xpath key to node value function */
    static private final Map<String,XPathValue> nodeMap = 
        new HashMap<String,XPathValue>();
    static {
      // normal journal article schema
      nodeMap.put("/article/front/titlegrp/title", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/article/front/pubfront/journal", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/article/front/pubfront/journal/@issn", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/article/front/pubfront/volume", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/article/front/pubfront/issue", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/article/front/pubfront/issue/@printdate", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/article/front/pubfront/fpage", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/article/front/pubfront/lpage", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/article/front/pubfront/doi", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/article/front/keywords/keyword", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/article/front/cpyrt/cpyrtholder", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/article/front/authgrp/author", AUTHOR_VALUE);

      // book review journal article schema
      nodeMap.put("/article/bookreview/booktitlegrp/booktitle", BOOKREVIEW_TITLE_VALUE);
      nodeMap.put("/article/bookreview/pubfront/journal", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/article/bookreview/pubfront/journal/@issn", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/article/bookreview/pubfront/volume", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/article/bookreview/pubfront/issue", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/article/bookreview/pubfront/issue/@printdate", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/article/bookreview/pubfront/fpage", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/article/bookreview/pubfront/lpage", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/article/bookreview/pubfront/doi", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/article/bookreview/cpyrt/cpyrtholder", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/article/bookreview/authgrp/author", AUTHOR_VALUE);

    }

    /** Map of raw xpath key to cooked MetadataField */
    static private final MultiValueMap xpathMap = new MultiValueMap();
    static {
      // normal journal article schema
      xpathMap.put("/article/front/titlegrp/title", MetadataField.FIELD_ARTICLE_TITLE);
      xpathMap.put("/article/front/pubfront/journal", MetadataField.FIELD_JOURNAL_TITLE);
      xpathMap.put("/article/front/pubfront/journal/@issn", MetadataField.FIELD_ISSN);
      xpathMap.put("/article/front/pubfront/volume", MetadataField.FIELD_VOLUME);
      xpathMap.put("/article/front/pubfront/issue", MetadataField.FIELD_ISSUE);
      xpathMap.put("/article/front/pubfront/issue/@printdate", MetadataField.FIELD_DATE);
      xpathMap.put("/article/front/pubfront/fpage", MetadataField.FIELD_START_PAGE);
      xpathMap.put("/article/front/pubfront/lpage", MetadataField.FIELD_END_PAGE);
      xpathMap.put("/article/front/pubfront/doi", MetadataField.FIELD_DOI);
      xpathMap.put("/article/front/keywords/keyword", MetadataField.FIELD_KEYWORDS);
      xpathMap.put("/article/front/cpyrt/cpyrtholder", MetadataField.FIELD_PUBLISHER);
      xpathMap.put("/article/front/authgrp/author", MetadataField.FIELD_AUTHOR);
      
      // book review jouranl article schema
      xpathMap.put("/article/bookreview/booktitlegrp/booktitle", MetadataField.FIELD_ARTICLE_TITLE);
      xpathMap.put("/article/bookreview/pubfront/journal", MetadataField.FIELD_JOURNAL_TITLE);
      xpathMap.put("/article/bookreview/pubfront/journal/@issn", MetadataField.FIELD_ISSN);
      xpathMap.put("/article/bookreview/pubfront/volume", MetadataField.FIELD_VOLUME);
      xpathMap.put("/article/bookreview/pubfront/issue", MetadataField.FIELD_ISSUE);
      xpathMap.put("/article/bookreview/pubfront/issue/@printdate", MetadataField.FIELD_DATE);
      xpathMap.put("/article/bookreview/pubfront/fpage", MetadataField.FIELD_START_PAGE);
      xpathMap.put("/article/bookreview/pubfront/lpage", MetadataField.FIELD_END_PAGE);
      xpathMap.put("/article/bookreview/pubfront/doi", MetadataField.FIELD_DOI);
      xpathMap.put("/article/bookreview/keywords/keyword", MetadataField.FIELD_KEYWORDS);
      xpathMap.put("/article/bookreview/cpyrt/cpyrtholder", MetadataField.FIELD_PUBLISHER);
      xpathMap.put("/article/bookreview/authgrp/author", MetadataField.FIELD_AUTHOR);
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
      log.debug3("Attempting to extract metadata from cu: "+cu);
      try {
      ArticleMetadata am = 
          new XmlDomMetadataExtractor(nodeMap).extract(target, cu);
        am.cook(xpathMap);
        emitter.emitMetadata(cu,  am);
      } catch (XPathExpressionException ex) {
        PluginException ex2 = new PluginException("Error parsing XPaths");
        ex2.initCause(ex);
        throw ex2;
      }
    }
  }
}