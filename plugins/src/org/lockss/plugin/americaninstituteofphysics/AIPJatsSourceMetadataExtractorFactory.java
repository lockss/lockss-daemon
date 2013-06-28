/* $Id: AIPJatsSourceMetadataExtractorFactory.java,v 1.1 2013-06-28 02:58:27 ldoan Exp $

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.map.*;
import javax.xml.xpath.XPathExpressionException;

/*
 * Extracts metadata for AIPJats Source Content, stored in <file name>.xml.
 * 
 * "http://clockss-ingest.lockss.org/sourcefiles/aipjats-released/
 *              2013/test_76_clockss_aip_2013-06-07_084326.zip!/
 *              JAP/v111/i11/112601_1/Markup/VOR_10.1063_1.4726155.xml"
 *
 * Test cases extracting metadata:
 *      1. the original xml file
 *      2. xml file with missing journal title
 *      3. xml file with missing journal title, journal id and issn
 *      4. xml file with missing journal title, journal id, issn and eissn
 */

public class AIPJatsSourceMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  
  static Logger log = 
      Logger.getLogger(AIPJatsSourceMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    return new AIPJatsSourceMetadataExtractor();
  }
  
  public static class AIPJatsSourceMetadataExtractor
    implements FileMetadataExtractor {
    
    private Map<String, String> journalTitleMap;

    private static Pattern JOURNAL_ID_PATTERN = 
        Pattern.compile("/aipjats-[^/]+/[0-9]{4}/[^/]+/([A-Z]+)/.+"); 

    public AIPJatsSourceMetadataExtractor() {
      journalTitleMap = new HashMap<String, String>();
    }
	  
    /** NodeValue for creating value of subfields from AuthorName tag */
    private static final NodeValue AUTHOR_VALUE = new NodeValue() {
      @Override
      public String getValue(Node node) {
        if (node == null) {
          return null;
        }
        
        NodeList nameNodes = node.getChildNodes();
        String givenName = "", familyName = "";
        for (int k = 0; k < nameNodes.getLength(); k++) {
          Node nameNode = nameNodes.item(k);
          if (nameNode.getNodeName().equals("given-names")) {
            givenName += nameNode.getTextContent();
          } else if (nameNode.getNodeName().equals("surname")) {
            familyName += nameNode.getTextContent();
          }
        }
        return familyName + ", " + givenName;
      }
    };
	    
    /** NodeValue for creating value of subfields from Date tag **/
    private static final NodeValue DATE_VALUE = new NodeValue() {
      @Override
      public String getValue(Node node) {
        if (node == null) {
          return null;
        }
        
        NodeList nameNodes = node.getChildNodes();
        String year = null, month = null, day = null;
        for (int k = 0; k < nameNodes.getLength(); k++) {
          Node nameNode = nameNodes.item(k);
          if (nameNode.getNodeName().equals("year")) {
            year = nameNode.getTextContent();
          } else if (nameNode.getNodeName().equals("month")) {
            month = nameNode.getTextContent();
          } else if (nameNode.getNodeName().equals("day")) {
            day = nameNode.getTextContent();
          }
        }
        
        return year + "-" + month + (day != null ? "-" + day : "");
      }
    };
	    
    /** Map of raw xpath key to node value function */
    private static final Map<String,XPathValue> nodeMap = 
        new HashMap<String,XPathValue>();
    static {
      // normal journal article schema
      
      // use hardwired publisher name
      // nodeMap.put("/article/front/publisher/publisher-name", 
      //        XmlDomMetadataExtractor.TEXT_VALUE);
      
      nodeMap.put("/article/front/journal-meta/"
                  + "journal-id[@journal-id-type='coden']", 
                  XmlDomMetadataExtractor.TEXT_VALUE);

      nodeMap.put("/article/front/journal-meta/journal-title-group/"
                  + "journal-title", XmlDomMetadataExtractor.TEXT_VALUE);

      nodeMap.put("/article/front/journal-meta/issn[@pub-type='ppub']", 
                  XmlDomMetadataExtractor.TEXT_VALUE);

      nodeMap.put("/article/front/journal-meta/issn[@pub-type='epub']", 
                  XmlDomMetadataExtractor.TEXT_VALUE);
      
      nodeMap.put("/article/front/article-meta/article-id[@pub-id-type='doi']", 
                  XmlDomMetadataExtractor.TEXT_VALUE);
      
      nodeMap.put("/article/front/article-meta/title-group/article-title", 
                  XmlDomMetadataExtractor.TEXT_VALUE);
      
      nodeMap.put("/article/front/article-meta/contrib-group/"
                  + "contrib/name[@name-style='western']", AUTHOR_VALUE);
      
      nodeMap.put("/article/front/article-meta/pub-date", DATE_VALUE);
      
      nodeMap.put("/article/front/article-meta/volume", 
                  XmlDomMetadataExtractor.TEXT_VALUE);
      
      nodeMap.put("/article/front/article-meta/issue", 
                  XmlDomMetadataExtractor.TEXT_VALUE);
      
      nodeMap.put("/article/front/article-meta/permissions/copyright-year", 
                  XmlDomMetadataExtractor.TEXT_VALUE);
      
      nodeMap.put("/article/front/article-meta/kwd-group/kwd", 
                  XmlDomMetadataExtractor.TEXT_VALUE);
      
      nodeMap.put("/article/front/article-meta/abstract/p", 
                  XmlDomMetadataExtractor.TEXT_VALUE);
    }

    /** Map of raw xpath key to cooked MetadataField */
    private static final MultiValueMap xpathMap = new MultiValueMap();
    static {
      // normal journal article schema
      
      // use hardwire publisher name
      // xpathMap.put("/article/front/publisher/publisher-name", 
      //              MetadataField.FIELD_PUBLISHER);
      
      xpathMap.put("/article/front/journal-meta/"
                   + "journal-id[@journal-id-type='coden']", 
                   MetadataField.FIELD_PROPRIETARY_IDENTIFIER);
      
      xpathMap.put("/article/front/journal-meta/journal-title-group/"
                   + "journal-title", MetadataField.FIELD_JOURNAL_TITLE);
      
      xpathMap.put("/article/front/journal-meta/issn[@pub-type='ppub']",
                   MetadataField.FIELD_ISSN);
      
      xpathMap.put("/article/front/journal-meta/issn[@pub-type='epub']", 
                   MetadataField.FIELD_EISSN);
      
      xpathMap.put("/article/front/article-meta/article-id[@pub-id-type='doi']", 
                   MetadataField.FIELD_DOI);
      
      xpathMap.put("/article/front/article-meta/title-group/article-title",
                   MetadataField.FIELD_ARTICLE_TITLE);
      
      xpathMap.put("/article/front/article-meta/contrib-group/"
                   + "contrib/name[@name-style='western']", 
                   MetadataField.FIELD_AUTHOR);
      
      xpathMap.put("/article/front/article-meta/pub-date", 
                   MetadataField.FIELD_DATE);
      
      xpathMap.put("/article/front/article-meta/volume", 
                   MetadataField.FIELD_VOLUME);
      
      xpathMap.put("/article/front/article-meta/issue", 
                   MetadataField.FIELD_ISSUE);
      
      xpathMap.put("/article/front/article-meta/permissions/copyright-year", 
                   MetadataField.DC_FIELD_RIGHTS);
      
      xpathMap.put("/article/front/article-meta/kwd-group/kwd", 
                   MetadataField.FIELD_KEYWORDS);
      
      xpathMap.put("/article/front/article-meta/abstract/p", 
                    MetadataField.DC_FIELD_DESCRIPTION);
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
      String journalId = getJournalId(url, am);
      String issn = am.get(MetadataField.FIELD_ISSN);
      String eissn = am.get(MetadataField.FIELD_EISSN);
      String journalTitle = am.get(MetadataField.FIELD_JOURNAL_TITLE);

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
      String genTitle = null; // generated title fron issn, eissn or journalID
      try {
        // try issn as key
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
     * @param emitter the emiter to output the resulting ArticleMetadata
     */
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException, PluginException {
      
      log.debug3("The MetadataExtractor attempted to extract metadata "
                 + "from cu: " + cu);
      
      ArticleMetadata am = do_extract(target, cu, emitter);
      
      // hardwired publisher for board report (look at imprints later)
      am.put(MetadataField.FIELD_PUBLISHER, "American Institute of Physics");
      
      // emit only if journal title exists, otherwise report site error
      String journalTitle = getJournalTitle(cu.getUrl(), am);
      if (!StringUtil.isNullString(journalTitle)) {
        emitter.emitMetadata(cu, am);
      } else {
        log.siteError("Missing journal title: " + cu.getUrl());
      }
    }
	    
    /**
     * Use XmlMetadataExtractor to extract raw metadata, map
     * to cooked fields, then extract extra tags by reading the file.
     * 
     * @param target the MetadataTarget
     * @param in the Xml input stream to parse
     * @param emitter the emitter to output the resulting ArticleMetadata
     */
    public ArticleMetadata do_extract(MetadataTarget target, 
                                      CachedUrl cu, Emitter emit)
                                          throws IOException, PluginException {
      try {
        ArticleMetadata am; 
        try {
          am = new XmlDomMetadataExtractor(nodeMap).extract(target, cu);
        } finally {
          AuUtil.safeRelease(cu);
        }
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
