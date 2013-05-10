/*
 * $Id: AmericanInstituteOfPhysicsSourceMetadataExtractorFactory.java,v 1.2 2013-05-10 20:14:05 alexandraohlson Exp $
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

package org.lockss.plugin.americaninstituteofphysics;

import java.io.*;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
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

public class AmericanInstituteOfPhysicsSourceMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("AIPMetadataExtractorFactory");

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    return new AIPXmlMetadataExtractor();
  }

  public static class AIPXmlMetadataExtractor 
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
	    
	    /** Map of raw xpath key to node value function */
	    static private final Map<String,XPathValue> nodeMap = 
	        new HashMap<String,XPathValue>();
	    static {
	      // normal journal article schema
	      nodeMap.put("/article/front/titlegrp/title", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/article/front/authgrp/author", AUTHOR_VALUE);
	      nodeMap.put("/article/front/pubfront/journal",  XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/article/front/pubfront/journal/@issn", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/article/front/pubfront/volume", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/article/front/pubfront/history/published/@date", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/article/front/pubfront/issue", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/article/front/pubfront/doi", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/article/front/spin/jouidx/country", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/article/front/spin/jouidx/addidx", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/article/front/abstract", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/article/front/cpyrt/cpyrtdate/@date", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/article/front/pubfront/fpage", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/article/front/pubfront/lpage", XmlDomMetadataExtractor.TEXT_VALUE);
	    }

	    /** Map of raw xpath key to cooked MetadataField */
	    static private final MultiValueMap xpathMap = new MultiValueMap();
	    static {
	      // normal journal article schema
	      xpathMap.put("/article/front/titlegrp/title", MetadataField.FIELD_ARTICLE_TITLE);
		  xpathMap.put("/article/front/authgrp/author", MetadataField.FIELD_AUTHOR);
		  xpathMap.put("/article/front/pubfront/journal", MetadataField.FIELD_JOURNAL_TITLE);
		  xpathMap.put("/article/front/pubfront/journal/@issn", MetadataField.FIELD_ISSN);
		  xpathMap.put("/article/front/pubfront/volume", MetadataField.FIELD_VOLUME);
		  xpathMap.put("/article/front/pubfront/history/published/@date", MetadataField.FIELD_DATE);
		  xpathMap.put("/article/front/pubfront/issue", MetadataField.FIELD_ISSUE);
		  xpathMap.put("/article/front/pubfront/doi", MetadataField.FIELD_DOI);
		  xpathMap.put("/article/front/spin/jouidx/country", MetadataField.DC_FIELD_SOURCE);
		  xpathMap.put("/article/front/spin/jouidx/addidx", MetadataField.FIELD_KEYWORDS);
		  xpathMap.put("/article/front/abstract", MetadataField.DC_FIELD_DESCRIPTION);
		  xpathMap.put("/article/front/cpyrt/cpyrtdate/@date", MetadataField.DC_FIELD_RIGHTS);
		  xpathMap.put("/article/front/pubfront/fpage", MetadataField.FIELD_START_PAGE);
		  xpathMap.put("/article/front/pubfront/lpage", MetadataField.FIELD_END_PAGE);
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
	      log.debug3("The MetadataExtractor attempted to extract metadata from cu: "+cu);
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