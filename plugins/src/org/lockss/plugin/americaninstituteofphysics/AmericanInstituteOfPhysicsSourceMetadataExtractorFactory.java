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

package org.lockss.plugin.americaninstituteofphysics;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
              nodeMap.put("/article/front/cpyrt/cpyrtholder", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/article/front/pubfront/fpage", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/article/front/pubfront/lpage", XmlDomMetadataExtractor.TEXT_VALUE);
	    }

	    /** Map of raw xpath key to cooked MetadataField */
	    static private final MultiValueMap xpathMap = new MultiValueMap();
	    static {
	      // normal journal article schema
	      xpathMap.put("/article/front/titlegrp/title", MetadataField.FIELD_ARTICLE_TITLE);
		  xpathMap.put("/article/front/authgrp/author", MetadataField.FIELD_AUTHOR);
// Hard code the journal title from a table of options and the file name
		  //xpathMap.put("/article/front/pubfront/journal", MetadataField.FIELD_JOURNAL_TITLE);
		  xpathMap.put("/article/front/pubfront/journal/@issn", MetadataField.FIELD_ISSN);
		  xpathMap.put("/article/front/pubfront/volume", MetadataField.FIELD_VOLUME);
		  xpathMap.put("/article/front/pubfront/history/published/@date", MetadataField.FIELD_DATE);
		  xpathMap.put("/article/front/pubfront/issue", MetadataField.FIELD_ISSUE);
		  xpathMap.put("/article/front/pubfront/doi", MetadataField.FIELD_DOI);
		  xpathMap.put("/article/front/spin/jouidx/country", MetadataField.DC_FIELD_SOURCE);
		  xpathMap.put("/article/front/spin/jouidx/addidx", MetadataField.FIELD_KEYWORDS);
		  xpathMap.put("/article/front/abstract", MetadataField.DC_FIELD_DESCRIPTION);
		  xpathMap.put("/article/front/cpyrt/cpyrtdate/@date", MetadataField.DC_FIELD_RIGHTS);
                  xpathMap.put("/article/front/cpyrt/cpyrtholder", MetadataField.FIELD_PUBLISHER);
		  xpathMap.put("/article/front/pubfront/fpage", MetadataField.FIELD_START_PAGE);
		  xpathMap.put("/article/front/pubfront/lpage", MetadataField.FIELD_END_PAGE);
	    }
	    
	    // Not making this static so that it could be expanded at runtime in the event that this
	    // becomes necessary with the addition of other journals
	    private  Map<String, String> codeMap = new HashMap<String, String>();
	     {
	       codeMap.put("APPLAB", "Applied Physics Letters");
	       codeMap.put("BIOMGB", "Biomicrofluidics");
	       codeMap.put("CHAOEH", "Chaos: An Interdisciplinary Journal of Nonlinear Science");
	       codeMap.put("JAPIAU", "Journal of Applied Physics");
	       codeMap.put("JCPSA6", "Journal of Chemical Physics");
	       codeMap.put("JMAPAQ", "Journal of Mathematical Physics");
	       codeMap.put("JPCRBU", "Journal of Physical and Chemical Reference Data");
	       codeMap.put("JRSEBH", "Journal of Renewable and Sustainable Energy");
	       codeMap.put("PHFLE6", "Physics of Fluids");
	       codeMap.put("PHPAEN", "Physics of Plasmas");
	       codeMap.put("RSINAK", "Review of Scientific Instruments");
	       codeMap.put("AAIDBI", "AIP Advances");
	       codeMap.put("CJCPA6", "Chinese Journal of Chemical Physics");
	       codeMap.put("JLAPEN", "Journal of Laser Applications");
	       codeMap.put("LTPHEG", "Low Temperature Physics");
	       codeMap.put("TAMPLBX", "Theoretical and Applied Mechanics Letters");
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
	          
	          /* Now pick up the journal title from the code in the URL
	          * * The URL will be of the form:
	            * http://clockss-ingest.lockss.org/sourcefiles/aip-released/2010/AIP_xml_1.tar.gz!/APPLAB/vol_96/iss_1/010401_1.xml
	            * where the APPLAB portion will vary and is the short form of the journal code.
	            * Translate this code in to the Journal Title and if it's not in the map, at least put in the short form of the code.
	            */
	          /* This is the same pattern (different groups) that the article iterator used */
	          Pattern PATTERN = Pattern.compile("/AIP_xml_\\d+\\.tar\\.gz!/([^/]+)/vol_\\d+/iss_\\d+/[^/]+_1.xml$", Pattern.CASE_INSENSITIVE);
	          String jtitle = null;
	          String jcode = null;
	          Matcher mat = PATTERN.matcher(cu.getUrl());
	          if (mat.find()) {
	            jcode = (mat.group(1)).toUpperCase(); //force upper case for matching in map
	            jtitle = codeMap.get(jcode);
	            if (!(jtitle == null)) {
	              /* we translated the code to a title, use that */
                      am.put(MetadataField.FIELD_JOURNAL_TITLE, jtitle );           
	            } else if (!(jcode == null)) { 
	              /* we didn't recognize this title, just put in the code */
	              am.put(MetadataField.FIELD_JOURNAL_TITLE, jcode );	              
	            } 
	            // It isn't possible to have NOTHING in as a directory name in the pattern or the 
	            // article iterator wouldn't have matched it
	          }	               
	          	          
	          if (!am.hasValidValue(MetadataField.FIELD_PUBLISHER)) {
	            am.put(MetadataField.FIELD_PUBLISHER, "American Institute of Physics");
	          }
	          return am;
	        } catch (XPathExpressionException ex) {
	          PluginException ex2 = new PluginException("Error parsing XPaths");
	          ex2.initCause(ex);
	          throw ex2;
	        }
	      }
	  }

}