/*
 * $Id: WileyMetadataExtractorFactory.java,v 1.10 2013-09-13 22:40:16 ldoan Exp $
 */

/*

 Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.wiley;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.TextValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.*;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/*
 * Implements a FileMetadataExtractorFactory for Wiley source content
 * Files used to write this class constructed from Wiley FTP archive:
 *      <base_url>/<year>/A/ADMA23.16.zip 
 *           
 * Metadata found in all xmls (full-text and abstract).
 * 
 * Full-text xml:
 *      <base_url>/<year>/A/AAB102.1.zip!/j.1744-7348.1983.tb02660.x.wml.xml
 *      <base_url>/<year>/A/ADMA23.16!/1810_ftp.wml.xml
 *      
 * Abstract xml:
 *      <base_url>/<year>/A/1803_hdp.wml.xml
 */
  public class WileyMetadataExtractorFactory
    implements FileMetadataExtractorFactory {
    
    static Logger log = Logger.getLogger(WileyMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    return new WileyMetadataExtractor();
  }
  
  public static class WileyMetadataExtractor 
    implements FileMetadataExtractor {
    
    static private final Pattern JOURNAL_PATTERN = Pattern.compile(
                    "/wiley-[^/]+/[0-9]{4}/[A-Z]/([A-Z]+)([0-9]+)\\.([0-9]+)");
    
    // xpath contants
    static private final String XPATH_ARTICLE_TITLE =
        "/component/header/contentMeta/titleGroup/title";
    static private final String XPATH_JOURNAL_TITLE = 
        "/component/header/publicationMeta[@level='product']" +
        "/titleGroup/title";
    static private final String XPATH_ISSN = 
        "/component/header/publicationMeta[@level='product']" +
        "/issn[@type='print']";
    static private final String XPATH_EISSN = 
        "/component/header/publicationMeta[@level='product']" +
        "/issn[@type='electronic']";
    static private final String XPATH_PROPRIETARY_IDENTIFIER = 
          "/component/header/publicationMeta[@level='product']" +
          "/idGroup/id[@type='product']/@value";
    static private final String XPATH_VOLUME = 
        "/component/header/publicationMeta[@level='part']" +
        "/numberingGroup/numbering[@type='journalVolume']"; 
    static private final String XPATH_ISSUE = 
        "/component/header/publicationMeta[@level='part']" +
        "/numberingGroup/numbering[@type='journalIssue']"; 
    static private final String XPATH_DATE = 
        "/component/header/publicationMeta[@level='part']" +
        "/coverDate/@startDate";
    static private final String XPATH_START_PAGE = 
        "/component/header/publicationMeta[@level='unit']" +
        "/numberingGroup/numbering[@type='pageFirst']"; 
    static private final String XPATH_END_PAGE = 
        "/component/header/publicationMeta[@level='unit']" +
        "/numberingGroup/numbering[@type='pageLast']"; 
    static private final String XPATH_DOI = 
        "/component/header/publicationMeta[@level='unit']/doi";
    static private final String XPATH_KEYWORDS = 
        "/component/header/contentMeta/keywordGroup/keyword"; 
    static private final String XPATH_AUTHOR = 
        "/component/header/contentMeta/creators" +
        "/creator[@creatorRole='author']/personName"; 
    static private final String XPATH_BODY_SECTION_TITLE = 
        "/component/body/section/title"; 
                  
    // NodeValue for creating value of subfields from author tag
    static private final XPathValue AUTHOR_VALUE = new NodeValue() {
      @Override
      public String getValue(Node node) {
        if (node == null) {
          return null;
        }
        // Wiley stores author names in two tags: givenNames and familyName
        NodeList nameNodes = node.getChildNodes();
        String givenName = null, familyName = null;
        for (int k = 0; k < nameNodes.getLength(); k++) {
          Node nameNode = nameNodes.item(k);
          if (nameNode.getNodeName().equals("givenNames")) {
            givenName = nameNode.getTextContent();
          } else if (nameNode.getNodeName().equals("familyName")) {
            familyName = nameNode.getTextContent();
          }
        }
        return familyName + ", " + givenName;
      }
    };

    // Matches issue number ranges of integers separated by a unicode dash
    // There are variety of dashes. To match a dash, we just match a non-digit
    static final Pattern issuePat = 
                                  Pattern.compile("(^[0-9]+)[^0-9]+([0-9]+)$");
    static private final XPathValue ISSUE_VALUE = new TextValue() {
      @Override
      public String getValue(String s) {
        if (StringUtil.isNullString(s)) {
          return null;
        }
        return (issuePat.matcher(s).replaceFirst("$1-$2"));
      }
    };   

    // Map of raw xpath key to node value function
    static private final Map<String,XPathValue> nodeMap = 
        new HashMap<String, XPathValue>();
    static {
      // Journal article schema
      nodeMap.put(XPATH_ARTICLE_TITLE, XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put(XPATH_JOURNAL_TITLE, XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put(XPATH_ISSN, XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put(XPATH_EISSN, XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put(XPATH_PROPRIETARY_IDENTIFIER, 
                  XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put(XPATH_VOLUME, XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put(XPATH_ISSUE, ISSUE_VALUE);
      nodeMap.put(XPATH_DATE, XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put(XPATH_START_PAGE, XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put(XPATH_END_PAGE, XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put(XPATH_DOI, XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put(XPATH_KEYWORDS, XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put(XPATH_AUTHOR, AUTHOR_VALUE);
      // if no <body> found, then the xml is an abstract
      nodeMap.put(XPATH_BODY_SECTION_TITLE, 
                  XmlDomMetadataExtractor.TEXT_VALUE);
    }

    // Map of raw xpath key to cooked MetadataField
    // am.cook() method requires MultiValueMap     
    static private final MultiValueMap xpathMap = new MultiValueMap();
    static {
      // Journal article schema
      xpathMap.put(XPATH_ARTICLE_TITLE, MetadataField.FIELD_ARTICLE_TITLE);
      xpathMap.put(XPATH_JOURNAL_TITLE, MetadataField.FIELD_JOURNAL_TITLE);
      xpathMap.put(XPATH_ISSN, MetadataField.FIELD_ISSN);
      xpathMap.put(XPATH_EISSN, MetadataField.FIELD_EISSN);
      xpathMap.put(XPATH_PROPRIETARY_IDENTIFIER, 
      		   MetadataField.FIELD_PROPRIETARY_IDENTIFIER);
      xpathMap.put(XPATH_VOLUME, MetadataField.FIELD_VOLUME);
      xpathMap.put(XPATH_ISSUE, MetadataField.FIELD_ISSUE);
      xpathMap.put(XPATH_DATE, MetadataField.FIELD_DATE);
      xpathMap.put(XPATH_START_PAGE, MetadataField.FIELD_START_PAGE);
      xpathMap.put(XPATH_END_PAGE, MetadataField.FIELD_END_PAGE);
      xpathMap.put(XPATH_DOI, MetadataField.FIELD_DOI);
      xpathMap.put(XPATH_KEYWORDS, MetadataField.FIELD_KEYWORDS);
      xpathMap.put(XPATH_AUTHOR, MetadataField.FIELD_AUTHOR);
    }
    
    // Use XmlDomMetadataExtractor to extract raw metadata, map
    // to cooked fields, then extract extra tags by reading the file.
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException, PluginException {
      log.debug3("Attempting to extract metadata from cu: " + cu);
      String xmlUrl = cu.getUrl();
      ArchivalUnit au = cu.getArchivalUnit();
      CachedUrl xmlCu = au.makeCachedUrl(xmlUrl);
      try {
        if (xmlCu.hasContent()) {
          ArticleMetadata am;
          try {
            am = new XmlDomMetadataExtractor(nodeMap).extract(target, xmlCu);
            am.cook(xpathMap);
          } catch (XPathExpressionException ex) { // syntactically incorrect
            throw (new PluginException("Error parsing XPaths", ex));
          } catch (IOException ioex) {
            // let it fall through since we want 
            // LOCKSS synthesized metadata to be filled in.
            log.siteWarning("Error in XmlDomMetadataExtractor", ioex);
            am = new ArticleMetadata();
          }
          
          // assign abstract for xml with no <body> tag
          // also check with the file name with "_hdp" to avoid 
          // corrupted xml such as 1829_ftp.wml.xml from 2011
          if (am.getRaw(XPATH_BODY_SECTION_TITLE) == null
              && xmlUrl.contains("_hdp")) {
            am.put(MetadataField.FIELD_COVERAGE, "abstract");
          }
            
          // hardwire publisher for board report (look at imprints later)
          am.put(MetadataField.FIELD_PUBLISHER, "John Wiley & Sons, Inc.");
          
          // get journal id, volume and issue from xml url
          Matcher mat = JOURNAL_PATTERN.matcher(xmlUrl);
          if (mat.find()) {
            am.putIfBetter(MetadataField.FIELD_PROPRIETARY_IDENTIFIER, 
                           mat.group(1));
            am.putIfBetter(MetadataField.FIELD_VOLUME, mat.group(2));
            am.putIfBetter(MetadataField.FIELD_ISSUE, mat.group(3));
          }
                      
          emitter.emitMetadata(cu, am);
            
        } else {
          log.siteError("Missing XML file: " + xmlUrl);
        }
        
      } finally {
        AuUtil.safeRelease(xmlCu);
      }
    }
  }
}