/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss;

import java.util.*;
import java.util.regex.*;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.*;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory.SourceXmlMetadataExtractor;
import org.lockss.test.*;
import org.lockss.util.Logger;


/*
 *  For now this is a limited test. Test that parsing and retrieving data from 
 *  an XML file works equivalently for ONE article in the 
 *  xml file when
 *      - no global map; article map with one article node
 *      - global map; no article map nor article node
 *  Set up two basic helpers and then use both and verify the results are the same
 */
public class TestXpathXmlMetadataExtractor
extends SourceXmlMetadataExtractorTest {
  
  private static Logger log = Logger.getLogger(TestXpathXmlMetadataExtractor.class);

  private static final Pattern schemaAPATTERN = Pattern.compile("schemaA\\.xml$");

  private static String goodPublisher = "Publisher Co.";
  private static String goodJournalTitle = "A Journal Title";
  private static String goodTitle = "A Good Title";
  private static String goodDate = "1999";
  private static String goodIdentifier = "123";
  
  private static String singleArticleContent = 
  "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
  "<article>" +
    "<meta>" +
      "<publisher>" + goodPublisher + "</publisher>" +
      "<journaltitle>" + goodJournalTitle + "</journaltitle>" +
    "</meta>" +
    "<identifier>" + goodIdentifier + "</identifier>" +
    "<title>" + goodTitle + "</title>" +
    "<pubdate type=\"online\"><date>" + goodDate + "</date></pubdate>" +
  "</article>";
  
  
  // SchemaA takes in all the values from a globalMap
  public void testExtractXmlSchemaA() throws Exception {

    String xml_url = getBaseUrl() + getYear() + "/schemaA.xml";
    FileMetadataExtractor me = new MyXmlMetadataExtractor();
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = extractFromContent(xml_url, "text/xml", singleArticleContent, mle);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodIdentifier, md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
  }
  
  // SchemaB takes in all the values from a articleMap
  public void testExtractXmlSchemaB() throws Exception {
    String xml_url = getBaseUrl() + getYear() + "/schemaB.xml";
    FileMetadataExtractor me = new MyXmlMetadataExtractor();
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = extractFromContent(xml_url, "text/xml", singleArticleContent, mle);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodIdentifier, md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
  }
  
  
  // Set up a test version of a source extractor in order to define/control
  // a basic schema for testing
  private class MyXmlMetadataExtractor extends SourceXmlMetadataExtractor {


    @Override
    protected SourceXmlSchemaHelper setUpSchema() {
      // Once you have it, just keep returning the same one. It won't change.
      return new schemaAXmlSchemaHelper();
    }

    // choose the schema based on the URL - for publishers that use > 1
    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it created, just keep returning the same one. It won't change.
      Matcher AMat = schemaAPATTERN.matcher(cu.getUrl()); 
      if (AMat.find()) {
        log.debug3("using schemaA - globalMap only");
        return new schemaAXmlSchemaHelper();
      }         
      // Otherwise it's the B schema
      log.debug3("using schemaB - articleMap only");
      return new schemaBXmlSchemaHelper();
    }
    
    // Make this do nothing so that we don't need to add actual content files
    // for testing
    @Override
    protected boolean preEmitCheck(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {

      log.debug3("in preEmitCheck which always returns true");
      return true;
    }

    
     static final String article_id =  "article/identifier";
     static final String article_date =  "article/pubdate/date";
     static final String article_title = "article/title";
     static final String meta_publisher = "article/meta/publisher";
     static final String meta_journal = "article/meta/journaltitle";
     
     private final Map<String,XPathValue> schemaMap = 
         new HashMap<String,XPathValue>();
      {
       schemaMap.put(article_id, XmlDomMetadataExtractor.TEXT_VALUE); 
       schemaMap.put(article_title, XmlDomMetadataExtractor.TEXT_VALUE); 
       schemaMap.put(article_date, XmlDomMetadataExtractor.TEXT_VALUE); 
       schemaMap.put(meta_publisher, XmlDomMetadataExtractor.TEXT_VALUE);
       schemaMap.put(meta_journal, XmlDomMetadataExtractor.TEXT_VALUE);
     }
      
      private final MultiValueMap cookMap = new MultiValueMap();
       {
        // do NOT cook publisher_name; get from TDB file for consistency
        cookMap.put(article_id, MetadataField.FIELD_PROPRIETARY_IDENTIFIER); 
        cookMap.put(article_date, MetadataField.FIELD_DATE); 
        cookMap.put(meta_publisher, MetadataField.FIELD_PUBLISHER);
        cookMap.put(meta_journal, MetadataField.FIELD_PUBLICATION_TITLE);
        cookMap.put(article_title, MetadataField.FIELD_ARTICLE_TITLE);
      }

    // Schema A will access the content solely through a globalMap 
    // since there is only one article per file
    private class schemaAXmlSchemaHelper
    implements SourceXmlSchemaHelper {
      
      @Override
      public Map<String, XPathValue> getGlobalMetaMap() {
        return schemaMap;
      }

      @Override
      public Map<String, XPathValue> getArticleMetaMap() {
        return null;
      }

      @Override
      public String getArticleNode() {
        return null;
      }

      @Override
      public MultiValueMap getCookMap() {
        return cookMap;
      }

      @Override
      public String getDeDuplicationXPathKey() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public String getConsolidationXPathKey() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public String getFilenameXPathKey() {
        // TODO Auto-generated method stub
        return null;
      }
      
    }
    
    // SchemaB will access the content through an article map where the article
    // node is the top <article> node and no global information
    private class schemaBXmlSchemaHelper implements SourceXmlSchemaHelper {

      @Override
      public Map<String, XPathValue> getGlobalMetaMap() {
        return null;
      }

      @Override
      public Map<String, XPathValue> getArticleMetaMap() {
        return schemaMap;
      }

      // use the top of the document
      @Override
      public String getArticleNode() {
        return null;
      }

      @Override
      public MultiValueMap getCookMap() {
        return cookMap;
      }

      @Override
      public String getDeDuplicationXPathKey() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public String getConsolidationXPathKey() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public String getFilenameXPathKey() {
        // TODO Auto-generated method stub
        return null;
      }
      
    }
  }
}
