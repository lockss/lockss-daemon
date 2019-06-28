/*
 * $Id:$
 */

/*

 Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.pkp;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang3.StringUtils;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *  A helper class that defines a schema for the Issue xml files delivered to PKP
 *  all articles in the issue are identified in the XML
 *  the PDF content is encoded in the XML
 */
public class PkpIssueXmlSchemaHelper implements SourceXmlSchemaHelper {
  
  private static final Logger log = Logger.getLogger(PkpIssueXmlSchemaHelper.class);
  

  /* 
   * AUTHOR INFORMATION - we're at an 'Author' node. 
   *     there could be more than one author
      <author primary_contact="true">
        <firstname>Richard</firstname>
        <lastname>Smith</lastname>
        <email>foo@blah</email>
      </author>
   * 
   * There can be multiple authors
   */
  static private final NodeValue AUTHOR_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      log.debug3("getValue of author");
      
      String firstname = null;
      String lastname = null;
      String suffix = null;
      NodeList childNodes = node.getChildNodes(); 
      for (int m = 0; m < childNodes.getLength(); m++) {
        Node infoNode = childNodes.item(m);
        String nodeName = infoNode.getNodeName();
        if ("firstname".equals(nodeName)) {
          firstname = StringUtils.strip(infoNode.getTextContent());
        } else if ("lastname".equals(nodeName)) {
          lastname = StringUtils.strip(infoNode.getTextContent());
        }
      }
      if (firstname == null && lastname == null) {
        log.debug3("No recognizable author schema in this author");
        return null;
      }
      // names are either in "givenName" in their entirety, or spread
      // across "given" + "surname" + "suffix"
      StringBuilder valbuilder = new StringBuilder();
      valbuilder.append(firstname);
      if (lastname != null) {
        valbuilder.append(" " + lastname);
      }
      return valbuilder.toString();
    }
  };
  
  
  /*
   * Pages are one node
   * <pages>517–519</pages>
   * split on the hyphen if it's there, could be other non-numeric char
   */
  static private final Pattern numPat = Pattern.compile("^([0-9]+)");
  static private final NodeValue PAGE_VALUE = new NodeValue() {
	    @Override
	    public String getValue(Node node) {
	      
	    	String pages = node.getTextContent();
	    	if (pages == null) {
	    		return null;
	    	}
	    	Matcher numMat = numPat.matcher(pages);
	    	if (numMat.find()) { 
	    		return numMat.group(1);
	    	}
	    	return pages;
	    }
	  };
  
  /* 
   *  Specific XPATH key definitions that we care about
   *  There is only one article per xml file and the 
   *  filename.xml == filename.pdf == Article PII (internal ID)
   */
  
  private static String issue_node = "/issue";
  // dtd seems to say that article will also sit below a section node
  // might need to become //article to capture any level if that assumption is wrong
  private static String article_node = issue_node + "/section/article";
  
  // article level
  private static String doi =  article_node + "/id[@type = 'doi']";
  // unclear if locale will always be set - might need to be or'd with " | /title[not(@locale)]" to get one with no locale set
  private static String title = article_node + "/title[@locale = 'en_US']";
  private static String fpage = article_node + "/pages";
  private static String author = article_node + "/author"; 
  private static String pub_year = issue_node + "/year";
  // unclear if locale will always be set - might need to be or'd with " | /title[not(@locale)]" to get one with no locale set
  // no e-issn information?? at least none in this sample
  private static String pub_title =  issue_node + "/title[@locale='en_US']";
  private static String pub_volume =  issue_node + "/volume";
  private static String pub_issue =  issue_node + "/number";

  
  /*
   *  The following 3 variables are needed to construct the XPathXmlMetadataParser
   */
  
  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String,XPathValue> articleMap = 
      new HashMap<String,XPathValue>();
  static {
   articleMap.put(doi, XmlDomMetadataExtractor.TEXT_VALUE);
   articleMap.put(title, XmlDomMetadataExtractor.TEXT_VALUE);
   articleMap.put(fpage, PAGE_VALUE);
   articleMap.put(author, AUTHOR_VALUE);
  }
  
  /*
   * The emitter will need a map to know how to cook raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // do NOT cook publisher_name; get from TDB file for consistency
    // XXX should also get PROVIDER from the TDB file??? XXX
    // cookMap.put(DA_source, MetadataField.FIELD_PROVIDER);
    cookMap.put(pub_title, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(title, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(doi, MetadataField.FIELD_DOI);
    cookMap.put(fpage, MetadataField.FIELD_START_PAGE);
    cookMap.put(author, MetadataField.FIELD_AUTHOR);
    cookMap.put(pub_year, MetadataField.FIELD_DATE);
    cookMap.put(pub_volume, MetadataField.FIELD_VOLUME);
    cookMap.put(pub_issue, MetadataField.FIELD_ISSUE);
    
  }

  
  /*
   * A map of information that will be collected once and applied to each article
   * These live at the top of the XML file and apply to all articles contained
   */
  static private final Map<String,XPathValue> pubMap = 
	      new HashMap<String,XPathValue>();  
  static {
	   pubMap.put(pub_year, XmlDomMetadataExtractor.TEXT_VALUE);
	   pubMap.put(pub_title, XmlDomMetadataExtractor.TEXT_VALUE);
	   pubMap.put(pub_volume, XmlDomMetadataExtractor.TEXT_VALUE);
	   pubMap.put(pub_issue, XmlDomMetadataExtractor.TEXT_VALUE);
	  }


  /**
   * global information outside of article records
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return pubMap;
  }

  /**
   * return article map to identify xpaths of interest
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return articleMap;
  }

  /**
   * Return the article node path
   * There is only one article per xml file so the top of the document is the
   * article and all paths are relative do document.
   */
  @Override
  public String getArticleNode() {
    return article_node;
  }

  /**
   * Return a map to translate raw values to cooked values
   */
  @Override
  public MultiValueMap getCookMap() {
    return cookMap;
  }

  /**
   * No duplicate data 
   */
  @Override
  public String getDeDuplicationXPathKey() {
    return null;
  }

  /**
   * No consolidation required
   */
  @Override
  public String getConsolidationXPathKey() {
    return null;
  }

  /**
   * No need to check for filenames
   */
  @Override
  public String getFilenameXPathKey() {
    return null;
  }

}
