/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.clockss;

import java.util.*;
import java.util.regex.*;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.*;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory.SourceXmlMetadataExtractor;
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
  
  private static final Logger log = Logger.getLogger(TestXpathXmlMetadataExtractor.class);

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
