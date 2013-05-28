/*
 * $Id:
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


/**
 * This class implements a FileMetadataExtractorFactory for Wiley content
 * Files used to write this class constructed from Wiley FTP archive:
 * http://clockss-ingest.lockss.org/sourcefiles/wiley-dev/2011/A/AEN49.1.zip
 */
  public class WileyMetadataExtractorFactory
    implements FileMetadataExtractorFactory {
    static Logger log = 
      Logger.getLogger("WileyMetadataExtractorFactory");

  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    return new WileyMetadataExtractor();
  }

/**
 * This class implements a FileMetadataExtractor for Wiley content.
 */
  public static class WileyMetadataExtractor 
    implements FileMetadataExtractor {
    
    private Map<String, String> journalTitleMap;
    
    // http://clockss-ingest.lockss.org/sourcefiles/wiley-released/2012/A/AAB102.1.zip!/j.1744-7348.1983.tb02660.x.pdf
    private Pattern JOURNAL_ID_PATTERN = Pattern.compile("/wiley-[^/]+/[0-9]{4}/[A-Z]/([A-Z]+)");
            
    public WileyMetadataExtractor() {
      journalTitleMap = new HashMap<String, String>();
    }
	    
    /** NodeValue for creating value of subfields from author tag */
    static private final XPathValue AUTHOR_VALUE = new NodeValue() {
      @Override
      public String getValue(Node node) {
        if (node == null) {
          return null;
        }

        // Wiley stores author names in two tags: givenNames and familyName
        // Middle initials are stored as suffixes of givenNames with no period (e.g. "Todd F")
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

    // matches issue number ranges of integers separated by a unicode dash
    static final Pattern issuePat = Pattern.compile("(^[0-9]+)[^0-9]+([0-9]+)$");
    static private final XPathValue ISSUE_VALUE = new TextValue() {
      @Override
      public String getValue(String s) {
        return StringUtil.isNullString(s) 
            ? null : issuePat.matcher(s).replaceFirst("$1-$2");
      }
    };
    
    // hard code overwriting publisher name (temp solution for board report)
//    static private final XPathValue PUBLISHER_VALUE = new TextValue() {
//      @Override
//      public String getValue(String s) {
//        return "John Wiley & Sons, Inc.";
//      }
//    };
    
    /** Map of raw xpath key to node value function */
    
    static private final Map<String,XPathValue> nodeMap = 
        new HashMap<String,XPathValue>();
    static {
      // Journal article schema
      nodeMap.put("/component/header/contentMeta/titleGroup/title", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/component/header/publicationMeta[@level='product']/titleGroup/title", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/component/header/publicationMeta[@level='product']/issn[@type='print']", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/component/header/publicationMeta[@level='product']/issn[@type='electronic']", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/component/header/publicationMeta[@level='product']/idGroup/id[@type='product']/@value", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/component/header/publicationMeta[@level='part']/numberingGroup/numbering[@type='journalVolume']", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/component/header/publicationMeta[@level='part']/numberingGroup/numbering[@type='journalIssue']", ISSUE_VALUE);
      nodeMap.put("/component/header/publicationMeta[@level='part']/coverDate/@startDate", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/component/header/publicationMeta[@level='unit']/numberingGroup/numbering[@type='pageFirst']", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/component/header/publicationMeta[@level='unit']/numberingGroup/numbering[@type='pageLast']", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/component/header/publicationMeta[@level='unit']/doi", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("/component/header/contentMeta/keywordGroup/keyword", XmlDomMetadataExtractor.TEXT_VALUE);
      //nodeMap.put("/component/header/publicationMeta[@level='product']/publisherInfo/publisherName", XmlDomMetadataExtractor.TEXT_VALUE);
      // hard code overwriting publisher name (temp solution for board report)
//      nodeMap.put("/component/header/publicationMeta[@level='product']/publisherInfo/publisherName", PUBLISHER_VALUE);
      nodeMap.put("/component/header/contentMeta/creators/creator[@creatorRole='author']/personName", AUTHOR_VALUE);
    }

    /** Map of raw xpath key to cooked MetadataField */
    
    static private final MultiValueMap xpathMap = new MultiValueMap();
    static {
      // Journal article schema
      xpathMap.put("/component/header/contentMeta/titleGroup/title", MetadataField.FIELD_ARTICLE_TITLE);
      xpathMap.put("/component/header/publicationMeta[@level='product']/titleGroup/title", MetadataField.FIELD_JOURNAL_TITLE);
      xpathMap.put("/component/header/publicationMeta[@level='product']/issn[@type='print']", MetadataField.FIELD_ISSN);
      xpathMap.put("/component/header/publicationMeta[@level='product']/issn[@type='electronic']", MetadataField.FIELD_EISSN);
      xpathMap.put("/component/header/publicationMeta[@level='product']/idGroup/id[@type='product']/@value", MetadataField.FIELD_PROPRIETARY_IDENTIFIER);
      xpathMap.put("/component/header/publicationMeta[@level='part']/numberingGroup/numbering[@type='journalVolume']", MetadataField.FIELD_VOLUME);
      xpathMap.put("/component/header/publicationMeta[@level='part']/numberingGroup/numbering[@type='journalIssue']", MetadataField.FIELD_ISSUE);
      xpathMap.put("/component/header/publicationMeta[@level='part']/coverDate/@startDate", MetadataField.FIELD_DATE);
      xpathMap.put("/component/header/publicationMeta[@level='unit']/numberingGroup/numbering[@type='pageFirst']", MetadataField.FIELD_START_PAGE);
      xpathMap.put("/component/header/publicationMeta[@level='unit']/numberingGroup/numbering[@type='pageLast']", MetadataField.FIELD_END_PAGE);
      xpathMap.put("/component/header/publicationMeta[@level='unit']/doi", MetadataField.FIELD_DOI);
      xpathMap.put("/component/header/contentMeta/keywordGroup/keyword", MetadataField.FIELD_KEYWORDS);
//      xpathMap.put("/component/header/publicationMeta[@level='product']/publisherInfo/publisherName", MetadataField.FIELD_PUBLISHER);
      xpathMap.put("/component/header/contentMeta/creators/creator[@creatorRole='author']/personName", MetadataField.FIELD_AUTHOR);
    }
    
    /**
     * Get the journal ID from from the article metadata. If not set, gets
     * journal id from the URL and adds it to the article metadata.
     *  
     * @param url the URL of the article
     * @param am the article metadata of the article
     * @return the journalID or null if not available
     */
    // extract journal id from cached url.
    // if not found (url is opaque), then assign a default value.
    private String getJournalId(String url, ArticleMetadata am) {
      String journalId = am.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER);
      if (StringUtil.isNullString(journalId)) {
        // http://localhost/~lydoan/wiley-released/2012/A/AAB102.1.zip!/j.1744-7348.1983.tb02660.x.pdf
        // Pattern PATTERN = Pattern.compile("/wiley-[^/]+/[0-9]{4}/[A-Z]/([A-Z]+)[0-9]+.+");
        Matcher mat = JOURNAL_ID_PATTERN.matcher(url);
        if (mat.find()) {
          journalId = mat.group(1);
          am.put(MetadataField.FIELD_PROPRIETARY_IDENTIFIER, journalId);
        }
      }
      log.debug3("getJournalIdl() journalId: " + journalId);
      return (journalId);
    }

    /**
     * Get the journal title for the specified ArticleMetadata. If not set,
     * looks up cached value using issn, eissn, or journalID. If not cached,
     * creates one from the issn, eissn or journalID.  Adds journalID to
     * the article metadata if not present. 
     * 
     * @param url the URL of the article
     * @param am the article metadata of the article
     * @return the journal title or null if not available
     */
    private String getJournalTitle(String url, ArticleMetadata am) {
      String journalTitle = am.get(MetadataField.FIELD_JOURNAL_TITLE);
      String journalId = getJournalId(url, am);
      String issn = am.get(MetadataField.FIELD_ISSN);
      String eissn = am.get(MetadataField.FIELD_EISSN);

      // journal has title -- cache using issn, eissn, and journalID
      // in case journal title is missing from later records
      if (!StringUtil.isNullString(journalTitle)) {
        if (!StringUtil.isNullString(issn)) {
          journalTitleMap.put(issn, journalTitle);
        }
        if (!StringUtil.isNullString(eissn)) {
          journalTitleMap.put(eissn, journalTitle);
        }
        if (!StringUtil.isNullString(journalId)) {
          journalTitleMap.put(journalId, journalTitle);
        }
        return journalTitle;
      }
      // journal has no title -- find it using issn, eissn, and jouranalID,
      // or generate a title using one of these properties otherwise
      String genTitle = null;  // generated title fron issn, eissn or journalID
      try {
        // try ISSN as key
        if (!StringUtil.isNullString(issn)) {
          // use cached journal title for journalId
          journalTitle = journalTitleMap.get(issn);
          if (!StringUtil.isNullString(journalTitle)) {
            return journalTitle;
          }
          if (genTitle == null) {
            // generate title with issn for preference
            genTitle = "UNKNOWN_TITLE/issn=" + issn;
          }
        }
        
        // try eissn as key
        if (!StringUtil.isNullString(eissn)) {
          // use cached journal title for journalId
          journalTitle = journalTitleMap.get(eissn);
          if (!StringUtil.isNullString(journalTitle)) {
            return journalTitle;
          }
          if (genTitle == null) {
            // generate title with eissn if issn not available
          genTitle = "UNKNOWN_TITLE/eissn=" + eissn;
          }
        }
        
        // try journalId as key
        if (!StringUtil.isNullString(journalId)) {
          // use cached journal title for journalId
          journalTitle = journalTitleMap.get(journalId);
          if (!StringUtil.isNullString(journalTitle)) {
            return journalTitle;
          }
          if (genTitle == null) {
            // generate title with journalID if issn and eissn not available
            genTitle = "UNKNOWN_TITLE/journalId=" + journalId;
          }
        }
        
      } finally {
        if (StringUtil.isNullString(journalTitle)) {
          journalTitle = genTitle;
        }
        if (!StringUtil.isNullString(journalTitle)) {
          am.put(MetadataField.FIELD_JOURNAL_TITLE, journalTitle);
        }
        log.debug3("getJournalTitle() journalTitle: " + journalTitle);
      }
      return journalTitle;
    }

    /**
     * Use XmlMetadataExtractor to extract raw metadata, map
     * to cooked fields, then extract extra tags by reading the file.
     * 
     * @param target the MetadataTarget
     * @param cu the CachedUrl from which to read input
     * @param emitter the emitter to output the resulting ArticleMetadata
     */
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException, PluginException {
      log.debug3("Attempting to extract metadata from cu: "+cu);
      String xmlUrl = cu.getUrl().replaceFirst("\\.pdf$", ".wml.xml");
      ArchivalUnit au = cu.getArchivalUnit();
      CachedUrl xmlCu = au.makeCachedUrl(xmlUrl);
      try {
        if (xmlCu.hasContent()) {
          ArticleMetadata am;
          try {
            am = new XmlDomMetadataExtractor(nodeMap).extract(target, xmlCu);
            am.cook(xpathMap);
          } catch (XPathExpressionException ex) {
            PluginException ex2 = new PluginException("Error parsing XPaths");
            ex2.initCause(ex);
            throw ex2;
          }
            
          // hardwire publisher for board report (look at imprints later)
          am.put(MetadataField.FIELD_PUBLISHER, "John Wiley & Sons, Inc.");
  
          // emit only if journal title exists, otherwise report site error
          String journalTitle = getJournalTitle(xmlUrl, am);
          if (!StringUtil.isNullString(journalTitle)) {
            emitter.emitMetadata(cu, am);
          } else {
            log.siteError("Missing journal title: " + xmlUrl);
          }
  
        } else {
          log.siteError("Missing XML file: " + xmlUrl);
        }
        
      } finally {
        AuUtil.safeRelease(xmlCu);
      }
    }
  }
}