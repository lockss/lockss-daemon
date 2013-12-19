/* $Id: AIPJatsSourceXmlMetadataExtractorFactory.java,v 1.1 2013-12-19 00:08:59 aishizaki Exp $

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
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.io.FilenameUtils;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.*;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
import java.util.Map.Entry;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.XPathXmlMetadataParser;

import org.apache.commons.collections.map.*;
import javax.xml.xpath.XPathExpressionException;

/*
 * Extracts metadata for AIPJats Source Content, stored in <file name>.xml.
 * ex: <base_url>/JAP/v111/i11/112601_1/Markup/VOR_10.1063_1.4726155.xml
 * Test cases extracting metadata from:
 *      1. the original xml file from the publisher
 *      2. xml file with missing journal title
 *      3. xml file with missing journal title, journal id and issn
 *      4. xml file with missing journal title, journal id, issn and eissn

 * ~/2010/ftp_PUB_10-05-17_06-11-02.zip/JOU=11864/VOL=2008.9/ISU=2-3/
 * ART=2008_64/11864_2008_Article.xml.Meta
 */
/*
 * An XML extractor that handles XML extraction for AIPJats format
 * This class is a subclass of the ClockssSourceXMLMetadataExtractor
 * It defines xpath definitions specific to AIPJATS
 * It defines evaluation methods for nodes in an AIPJATS tree
 * It defines a cooking map specific to AIPJATS
 * Then it creates a ClockssXMLSourceXMLMetadataExtractor using these definitions
 * 
 * extract() will consolidate all AM information based on the ProductIdentifer ISBN13
 *   and will cook and emit if a file exists of the name <isbn13>.(pdf|epub)
 * extractNoEmit() will return a consolidated cooked list of AM records if the plugin needs to do
 *   some other sort of validation check before emitting (eg. different filename)
 */

public class AIPJatsSourceXmlMetadataExtractorFactory
implements FileMetadataExtractorFactory {

  static Logger log = 
    Logger.getLogger(AIPJatsSourceXmlMetadataExtractorFactory.class);

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    //log.setLevel("debug3");
    return new AIPJatsSourceXmlMetadataExtractor();

  }

  public static class AIPJatsSourceXmlMetadataExtractor
  implements FileMetadataExtractor {

    /*
     * AUTHOR information
     * NODE=<Contrib-group>  
     *  contrib/
     *  name/
     *  "\n\nLastName\nFirstName\n\n1\n"
     */
    static private final NodeValue AIPJATS_AUTHOR_VALUE = new NodeValue() {
      @Override
      public String getValue(Node node) {
        if (node == null) {
          return null;
        }
        log.debug3("getValue of AIPJATS contributor");
        String name = null;
        NodeList childNodes = node.getChildNodes(); 
        StringBuilder names = new StringBuilder();
        for (int m = 0; m < childNodes.getLength(); m++) {
          Node infoNode = childNodes.item(m);
          String nodeName = infoNode.getNodeName();
          if("#text".equals(nodeName)) {
            continue;
          }
          if ("contrib".equals(nodeName)) {
            NodeList cNodes = infoNode.getChildNodes();
            for (int n = 0; n < cNodes.getLength(); n++) {
              Node iNode = cNodes.item(n);
              String nName = iNode.getNodeName();
              if("#text".equals(nName)) {
                continue;
              }
              if ("name".equals(nName)) {
                name = infoNode.getTextContent();
                if (!name.equals(null)) {
                  // \n\nLastName\nFirstName\n\n1\n
                  // remove leading, trailing '\n'
                  // remove trailing number
                  // replace center '\n' with ", "
                  name = name.replace("\n\n", "");
                  name = name.replaceFirst("\\d+\n*$", "");
                  name = name.replace("\n", ", ");
                  if (names.length() == 0) {
                    names.append(name);
                  } else {
                    names.append("; " + name);
                  }
                }
              } 
            }
          }
        }
        if (names.length() == 0) {
          log.debug3("no contributor found");
          return null;
        } else {
          return names.toString();
        }
      }
    };

    /* 
     * PUBLISHING DATE - could be under one of two nodes
     * NODE=<pub-date/>
     *   <month=>
     *   <day=>
     *   <year=>
     */
    static private final NodeValue AIPJATS_DATE_VALUE = new NodeValue() {
      @Override
      public String getValue(Node node) {
        log.debug3("getValue of AIPJATS date");
        NodeList childNodes = node.getChildNodes();
        if (childNodes == null) return null;
        String pubType = null;
        String year = null;
        String month = null;
        String day = null;
        String datetype = null;
        String dDate = null;

        if (node.getNodeName().equals("pub-date")) {
          for (int m = 0; m < childNodes.getLength(); m++) {
            Node childNode = childNodes.item(m);
            if ("pub-type".equals(childNode.getNodeName())) {
              pubType = childNode.getTextContent();
            } else if (childNode.getNodeName().equals("day")) {
              day = childNode.getTextContent();
            } else if (childNode.getNodeName().equals("month")) {
              month = childNode.getTextContent();
            } else if (childNode.getNodeName().equals("year")) {
              year = childNode.getTextContent();
            }
          }
        } 

        // make it W3C format instead of YYYYMMDD
        StringBuilder dBuilder = new StringBuilder();
        dBuilder.append(year); //YYYY
        dBuilder.append("-");
        dBuilder.append(month); //MM
        dBuilder.append("-");
        dBuilder.append(day); //DD
        return dBuilder.toString();

      }
    };
      
      /* 
       *  AIPJATS specific XPATH key definitions that we care about
       */

      /* Under an item node, the interesting bits live at these relative locations */
      private static String AIPJATS_JMETA = "journal-meta";

      private static String AIPJATS_issn = AIPJATS_JMETA + "/issn"; //issn[@pub-type = 'ppub']
      private static String AIPJATS_issntype_ppub = AIPJATS_issn + "[@pub-type = 'ppub']";
      private static String AIPJATS_issntype_epub = AIPJATS_issn + "[@pub-type= 'epub']";
      /* journal id */
      private static String AIPJATS_journal_id = AIPJATS_JMETA + "/journal-id[@journal-id-type = 'coden']";
      private static String AIPJATS_journal_title = AIPJATS_JMETA + "/journal-title-group/journal-title";

      /* components under Publisher */
      private static String AIPJATS_publisher = AIPJATS_JMETA + "/publisher";  
      private static String AIPJATS_publisher_name =
        AIPJATS_publisher + "/publisher-name";

      private static String AIPJATS_AMETA = "article-meta";
      /* article title */
      private static String AIPJATS_article_title = AIPJATS_AMETA + "/title-group/article-title";
      /* article id */
      private static String AIPJATS_article_id = AIPJATS_AMETA + "/article-id";
      private static String AIPJATS_doi = AIPJATS_article_id + "[@pub-id-type='doi']";
      /* vol, issue */
      private static String AIPJATS_issue = AIPJATS_AMETA + "/issue";
      private static String AIPJATS_vol = AIPJATS_AMETA + "/volume";
      /* copyright */
      private static String AIPJATS_copyright = AIPJATS_AMETA + "/permissions/copyright-year";
      /* keywords */
      private static String AIPJATS_keywords = AIPJATS_AMETA + "/kwd-group/kwd";
      /* abstract */
      private static String AIPJATS_abstract = AIPJATS_AMETA + "/abstract/p";
      /* published date */
      private static String AIPJATS_pubdate = AIPJATS_AMETA + "/pub-date";

      /* author */
      private static String AIPJATS_contrib = AIPJATS_AMETA + "/contrib-group";
      private static String AIPJATS_author = AIPJATS_contrib;

      static private final NodeValue AIPJATS_ARTICLE_TITLE_VALUE = new NodeValue() {
        @Override
        public String getValue(Node node) {
          if (node == null) {
            return null;
          }
          log.debug3("getValue of AIPJATS ARTICLE TITLE");
          String titleVal = null;
          String nodeName = null;
          NodeList childNodes = node.getChildNodes();
          for (int m = 0; m < childNodes.getLength(); m++) {
            Node infoNode = childNodes.item(m); 
            nodeName = infoNode.getNodeName();
            titleVal = infoNode.getTextContent();
            break;
          }
          if (titleVal != null)  {
            return titleVal;
          } else {
            log.debug3("no value in this article title");
            return null;
          }
        }
      };

      /*
       *  The following 3 variables are needed to use the XPathXmlMetadataParser
       */

      /* 1.  MAP associating xpath & value type definition or evaluator */
      static private final Map<String,XPathValue> AIPJATS_articleMap = 
        new HashMap<String,XPathValue>();
      static {
        AIPJATS_articleMap.put(AIPJATS_issntype_ppub, XmlDomMetadataExtractor.TEXT_VALUE); 
        AIPJATS_articleMap.put(AIPJATS_issntype_epub, XmlDomMetadataExtractor.TEXT_VALUE); 
        AIPJATS_articleMap.put(AIPJATS_publisher_name, XmlDomMetadataExtractor.TEXT_VALUE); 
        AIPJATS_articleMap.put(AIPJATS_journal_title, XmlDomMetadataExtractor.TEXT_VALUE); 
        AIPJATS_articleMap.put(AIPJATS_doi, XmlDomMetadataExtractor.TEXT_VALUE); 
        AIPJATS_articleMap.put(AIPJATS_article_title, AIPJATS_ARTICLE_TITLE_VALUE); 
        AIPJATS_articleMap.put(AIPJATS_issue, XmlDomMetadataExtractor.TEXT_VALUE);
        AIPJATS_articleMap.put(AIPJATS_vol, XmlDomMetadataExtractor.TEXT_VALUE);
        AIPJATS_articleMap.put(AIPJATS_contrib, AIPJATS_AUTHOR_VALUE);
        //AIPJATS_articleMap.put(AIPJATS_keywords, XmlDomMetadataExtractor.TEXT_VALUE);
        //AIPJATS_articleMap.put(AIPJATS_abstract, XmlDomMetadataExtractor.TEXT_VALUE);
        AIPJATS_articleMap.put(AIPJATS_journal_id, XmlDomMetadataExtractor.TEXT_VALUE);
        AIPJATS_articleMap.put(AIPJATS_pubdate, AIPJATS_DATE_VALUE);
      }
      
      /* 2. Each article has its own subNode */
      static private final String AIPJATS_articleNode = "/article/front"; 

      /* 3. in AIPJATS,  global information is repeated per article */ 
      static private final Map<String,XPathValue> AIPJATS_globalMap = null;
      
      /*
       * The emitter will need a map to know how to cook AIPJATS raw values
       */
      static private final MultiValueMap cookMap = new MultiValueMap();
      static {
        // normal journal article schema
        cookMap.put(AIPJATS_issntype_ppub, MetadataField.FIELD_ISSN);
        cookMap.put(AIPJATS_issntype_epub, MetadataField.FIELD_EISSN);
        cookMap.put(AIPJATS_doi, MetadataField.FIELD_DOI);
        cookMap.put(AIPJATS_vol, MetadataField.FIELD_VOLUME);
        cookMap.put(AIPJATS_issue, MetadataField.FIELD_ISSUE);
        cookMap.put(AIPJATS_journal_title, MetadataField.FIELD_JOURNAL_TITLE);
        cookMap.put(AIPJATS_article_title, MetadataField.FIELD_ARTICLE_TITLE);
        cookMap.put(AIPJATS_contrib, MetadataField.FIELD_AUTHOR);
        cookMap.put(AIPJATS_publisher_name, MetadataField.FIELD_PUBLISHER);
        cookMap.put(AIPJATS_pubdate, MetadataField.FIELD_DATE);
        cookMap.put(AIPJATS_journal_id, MetadataField.FIELD_PROPRIETARY_IDENTIFIER);
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
        try {
          Map<String,ArticleMetadata> uniqueRecordMap = new HashMap<String,ArticleMetadata>();
          
          // 1. Gather all the metadata in to a list of AM records
          List<ArticleMetadata> amList = 
              new XPathXmlMetadataParser(AIPJATS_globalMap, AIPJATS_articleNode, AIPJATS_articleMap).extractMetadata(target, cu);

          // 2. Consolidate identical isbn13 AM records in the list
          for ( ArticleMetadata oneAM : amList) {
            //updateRecordMap could be overridden by a child with different id usage

            updateRecordMap(uniqueRecordMap, oneAM);
          }

          if ( (uniqueRecordMap == null) || (uniqueRecordMap.isEmpty()) ) return;

          // 3. Cook & Emit all the records in the unique AM list that have files that exist
          Iterator<Entry<String, ArticleMetadata>> it = uniqueRecordMap.entrySet().iterator();
          while (it.hasNext()) {
            ArticleMetadata nextAM = (ArticleMetadata)(it.next().getValue());
            // pre-emit check could be overridden by a child with different layout/naming
            //if (preEmitCheck(cu,nextAM)) {
              nextAM.cook(cookMap);
              emitter.emitMetadata(cu,nextAM);
            //} 
          }
        } catch (XPathExpressionException e) {
          log.debug3("Xpath expression exception:" + e.getMessage());
        }

      }
      /*
       *  can have multiple <Product/> records for the same item, because
       *  each format might get its own record (eg epub, pdf, etc.)
       *  We only want to emit metadata once per item, so this 
       *  consolidates AM records for items that have the same product id
       *     limitation - if two versions of the same item are in two different XML files, we won't catch it
       *  NOTE: a child might override this in order to use a different type of
       *     identifier or to eliminate consolidation
       */
      protected void updateRecordMap(Map<String, ArticleMetadata> uniqueRecordMap,
          ArticleMetadata nextAM) {
        String nextID = nextAM.getRaw(AIPJATS_issntype_ppub);
        log.debug3("updateRecordMap nextID = " + nextID);

        ArticleMetadata prevAM = uniqueRecordMap.get(nextID);
        if (prevAM == null) {
          log.debug3("no record already existed with that id");
          uniqueRecordMap.put(nextID,  nextAM);
        } else {
          log.debug3("combining two AM records under that id");
          // prevAM.putRaw(formDetailKey, nextAM.getRaw(formDetailKey));
          // assume for now that the metadata is the same, just add to the product form information
          //TODO: Once support is implemented, we'll need to update the FIELD_FACET_URL_MAP information
          // so we can tell the database what format files are available.
        }
      }

      /*
       * Verify that a content file exists that matches this metadata before
       * cooking and emitting
       * NOTE: a child  might want to override this function
       * For example - if the <filename>.pdf wasn't <isbn13>.pdf they would need
       * to use a different validation check for the existence of a content file
       */
      protected boolean preEmitCheck(CachedUrl cu, ArticleMetadata thisAM) {
        String cuBase = FilenameUtils.getFullPath(cu.getUrl());
        ArchivalUnit B_au = cu.getArchivalUnit();
        CachedUrl fileCu;

        // associated pdf file name is not in xml file, but seems to be for
        // 2013/JAP/v114/i18/183704_1/Markup/VOR_10.1063_1.4829703.xml
        // 2013/JAP/v114/i18/183704_1/Page_Renditions/online.pdf
        log.debug3("in AIPJATS preEmitCheckcheck");
        
        String filename = "online.pdf";
        cuBase = cuBase.replace("Markup", "Page_Renditions");

        //first check for a pdf version
        fileCu = B_au.makeCachedUrl(cuBase + filename);
        log.debug3("does "+ cuBase + filename + " exist?");
        if(fileCu == null || !(fileCu.hasContent())) {
            log.debug3(filename + " does not exist in this AU");
            return false;
        }
        
        // Set a cooked value for an access file. Otherwise it would get set to xml file
        thisAM.put(MetadataField.FIELD_ACCESS_URL, fileCu.getUrl());
        thisAM.cook(cookMap);
        AuUtil.safeRelease(fileCu);
        return true;
      }
  }
}




