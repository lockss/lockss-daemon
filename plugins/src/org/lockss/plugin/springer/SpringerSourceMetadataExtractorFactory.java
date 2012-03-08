/*
 * $Id: SpringerSourceMetadataExtractorFactory.java,v 1.1 2012-03-08 01:54:09 dylanrhodes Exp $
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

package org.lockss.plugin.springer;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.map.*;
import javax.xml.xpath.XPathExpressionException;

/**
 * This implements a FileMetadataExtractor for Springer Source Content
 * 
 * Files used to write this class include:
 * ~/2010/ftp_PUB_10-05-17_06-11-02.zip/JOU=11864/VOL=2008.9/ISU=2-3/ART=2008_64/11864_2008_Article.xml.Meta
 */

public class SpringerSourceMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("SpringerSourceMetadataExtractorFactory");

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    return new SpringerSourceMetadataExtractor();
  }
  
  

  public static class SpringerSourceMetadataExtractor 
    implements FileMetadataExtractor {
	  
	  /** NodeValue for creating value of subfields from AuthorName tag */
	    static private final NodeValue AUTHOR_VALUE = new NodeValue() {
	      @Override
	      public String getValue(Node node) {
	        if (node == null) {
	          return null;
	        }
	        	        
	        NodeList nameNodes = node.getChildNodes();
	        String givenName = "", familyName = "";
	        for (int k = 0; k < nameNodes.getLength(); k++) {
	          Node nameNode = nameNodes.item(k);
	          if (nameNode.getNodeName().equals("GivenName")) {
	            givenName += " "+nameNode.getTextContent();
	          } else if (nameNode.getNodeName().equals("FamilyName")) {
	            familyName += nameNode.getTextContent();
	          }
	        }
	        
	        return familyName + ", " + givenName;
	      }
	    };
	    
	    /** NodeValue for creating value of subfields from OnlineDate tag **/
	      static private final NodeValue DATE_VALUE = new NodeValue() {
		      @Override
		      public String getValue(Node node) {
		        if (node == null) {
		          return null;
		        }
		        	        
		        NodeList nameNodes = node.getChildNodes();
		        String year = null, month = null, day = null;
		        for (int k = 0; k < nameNodes.getLength(); k++) {
		          Node nameNode = nameNodes.item(k);
		          if (nameNode.getNodeName().equals("Year")) {
		            year = nameNode.getTextContent();
		          } else if (nameNode.getNodeName().equals("Month")) {
		            month = nameNode.getTextContent();
		          } else if (nameNode.getNodeName().equals("Day")) {
			        day = nameNode.getTextContent();
			      }
		        }
		        
		        return month + (day != null ? "-" + day : "") + "-" + year;
		      }
		    };
	    
	    /** Map of raw xpath key to node value function */
	    static private final Map<String,XPathValue> nodeMap = 
	        new HashMap<String,XPathValue>();
	    static {
	      // normal journal article schema
	      nodeMap.put("/Publisher/PublisherInfo/PublisherName", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/Publisher/Journal/JournalPrintISSN", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/Publisher/Journal/JournalElectronicISSN", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/Publisher/Journal/JournalTitle", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/Publisher/Journal/Volume/VolumeInfo/VolumeIDStart", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/Publisher/Journal/Volume/Issue/IssueInfo/IssueIDStart", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/Publisher/Journal/Volume/Issue/Article/ArticleInfo/OnlineDate", DATE_VALUE);
	      nodeMap.put("/Publisher/Journal/Volume/Issue/IssueInfo/IssueCopyright/CopyrightYear", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/Publisher/Journal/Volume/Issue/Article/ArticleInfo/ArticleDOI", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/Publisher/Journal/Volume/Issue/Article/ArticleInfo/ArticleTitle/@Language", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/Publisher/Journal/Volume/Issue/Article/ArticleInfo/ArticleTitle", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/Publisher/Journal/Volume/Issue/Article/ArticleInfo/ArticleFirstPage", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/Publisher/Journal/Volume/Issue/Article/ArticleInfo/ArticleLastPage", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/Publisher/Journal/Volume/Issue/Article/ArticleHeader/AuthorGroup/Author/AuthorName", AUTHOR_VALUE);
	      nodeMap.put("/Publisher/Journal/Volume/Issue/Article/ArticleHeader/Abstract/Para", XmlDomMetadataExtractor.TEXT_VALUE);
	    }

	    /** Map of raw xpath key to cooked MetadataField */
	    static private final MultiValueMap xpathMap = new MultiValueMap();
	    static {
	      // normal journal article schema
	      xpathMap.put("/Publisher/PublisherInfo/PublisherName", MetadataField.FIELD_PUBLISHER);
	      xpathMap.put("/Publisher/Journal/JournalPrintISSN", MetadataField.FIELD_ISSN);
	      xpathMap.put("/Publisher/Journal/JournalElectronicISSN", MetadataField.FIELD_EISSN);
	      xpathMap.put("/Publisher/Journal/JournalTitle", MetadataField.FIELD_JOURNAL_TITLE);
	      xpathMap.put("/Publisher/Journal/Volume/VolumeInfo/VolumeIDStart", MetadataField.FIELD_VOLUME);
	      xpathMap.put("/Publisher/Journal/Volume/Issue/IssueInfo/IssueIDStart", MetadataField.FIELD_ISSUE);
	      xpathMap.put("/Publisher/Journal/Volume/Issue/Article/ArticleInfo/OnlineDate", MetadataField.FIELD_DATE);
	      xpathMap.put("/Publisher/Journal/Volume/Issue/IssueInfo/IssueCopyright/CopyrightYear", MetadataField.DC_FIELD_RIGHTS);
	      xpathMap.put("/Publisher/Journal/Volume/Issue/Article/ArticleInfo/ArticleDOI", MetadataField.FIELD_DOI);
	      xpathMap.put("/Publisher/Journal/Volume/Issue/Article/ArticleInfo/ArticleTitle/@Language", MetadataField.DC_FIELD_LANGUAGE);
	      xpathMap.put("/Publisher/Journal/Volume/Issue/Article/ArticleInfo/ArticleTitle", MetadataField.FIELD_ARTICLE_TITLE);
	      xpathMap.put("/Publisher/Journal/Volume/Issue/Article/ArticleInfo/ArticleFirstPage", MetadataField.FIELD_START_PAGE);
	      xpathMap.put("/Publisher/Journal/Volume/Issue/Article/ArticleInfo/ArticleLastPage", MetadataField.FIELD_END_PAGE);
	      xpathMap.put("/Publisher/Journal/Volume/Issue/Article/ArticleHeader/AuthorGroup/Author/AuthorName", MetadataField.FIELD_AUTHOR);
	      xpathMap.put("/Publisher/Journal/Volume/Issue/Article/ArticleHeader/Abstract/Para", MetadataField.DC_FIELD_DESCRIPTION);
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
	     * Uses a CachedUrl assumed to be in the directory of the article files to
	     * generate the Url of the current article
	     * @param cu - pathname of a sibling of the current article
	     * @return the current article's pathname
	     */
	    private CachedUrl getMetadataOf(CachedUrl cu)
	    {
	        Pattern pattern = Pattern.compile("(/[^/]+\\.zip!/JOU=[\\d]+/VOL=[\\d]+\\.[\\d]+/ISU=[\\d]+/ART=[^/]+/)(BodyRef/PDF/)([^/]+)(\\.pdf)$", Pattern.CASE_INSENSITIVE);
	        Matcher matcher = pattern.matcher(cu.getUrl());
	        return cu.getArchivalUnit().makeCachedUrl(matcher.replaceFirst("$1$3.xml.Meta"));
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
	            new XmlDomMetadataExtractor(nodeMap).extract(target, getMetadataOf(cu));
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