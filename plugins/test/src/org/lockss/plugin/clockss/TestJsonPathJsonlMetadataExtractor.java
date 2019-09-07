/*
 * Copyright (c) 2019 Board of Trustees of Leland Stanford Jr. University,
 * all rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Stanford University shall not
 * be used in advertising or otherwise to promote the sale, use or other dealings
 * in this Software without prior written authorization from Stanford University.
 */

package org.lockss.plugin.clockss;

import com.jayway.jsonpath.DocumentContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.FileMetadataListExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.JsonPathJsonMetadataParser.JsonPathValue;
import org.lockss.plugin.clockss.SourceJsonMetadataExtractorFactory.SourceJsonMetadataExtractor;
import org.lockss.util.Logger;

/*
 *  For now this is a limited test. Test that parsing and retrieving data from
 *  an JSON file works equivalently for ONE article in the
 *  json file when
 *      - no global map; article map with one article node
 *      - global map; no article map nor article node
 *  Set up two basic helpers and then use both and verify the results are the same
 */
public class TestJsonPathJsonlMetadataExtractor extends TestSourceJsonMetadataExtractor {
  private static final Logger log = Logger.getLogger(TestJsonPathJsonlMetadataExtractor.class);
  private static final Pattern schemaAPATTERN = Pattern.compile("schemaA\\.json$");

  private static String goodPublisher = "Publisher Co.";
  private static String goodJournalTitle = "A Journal Title";
  private static String goodTitle = "A Good Title";
  private static String goodDate = "1999";
  private static String goodIdentifier = "123";

  private static String dotArticleContent =
      "{\n"
          + "\t\"my.article\": {\n"
          + "\t\t\"meta\": {\n"
          + "\t\t\t\"my.publisher\": \"Publisher Co.\",\n"
          + "\t\t\t\"my.journaltitle\": \"A Journal Title\"\n"
          + "\t\t},\n"
          + "\t\t\"my.identifier\": \"123\",\n"
          + "\t\t\"my.title\": \"A Good Title\",\n"
          + "\t\t\"my.pubdate\": {\n"
          + "\t\t\t\"date\": \"1999\",\n"
          + "\t\t\t\"_type\": \"online\"\n"
          + "\t\t}\n"
          + "\t}\n"
          + "}";

  private static String singleArticleContent =
      "{\n"
          + "\t\"article\": {\n"
          + "\t\t\"meta\": {\n"
          + "\t\t\t\"publisher\": \"Publisher Co.\",\n"
          + "\t\t\t\"journaltitle\": \"A Journal Title\"\n"
          + "\t\t},\n"
          + "\t\t\"identifier\": \"123\",\n"
          + "\t\t\"title\": \"A Good Title\",\n"
          + "\t\t\"pubdate\": {\n"
          + "\t\t\t\"date\": \"1999\",\n"
          + "\t\t\t\"_type\": \"online\"\n"
          + "\t\t}\n"
          + "\t}\n"
          + "}";

  static final String article_id =  "$.article.identifier";
  static final String article_date = "$.article.pubdate.date";
  static final String article_title = "$.article.title";
  static final String meta_publisher = "$.article.meta.publisher";
  static final String meta_journal = "$.article.meta.journaltitle";

  private final Map<String,JsonPathValue> schemaMap =
      new HashMap<String,JsonPathValue>();
  {
    schemaMap.put(article_id, JsonPathJsonMetadataParser.STRING_VALUE);
    schemaMap.put(article_title, JsonPathJsonMetadataParser.STRING_VALUE);
    schemaMap.put(article_date, JsonPathJsonMetadataParser.STRING_VALUE);
    schemaMap.put(meta_publisher, JsonPathJsonMetadataParser.STRING_VALUE);
    schemaMap.put(meta_journal, JsonPathJsonMetadataParser.STRING_VALUE);
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

  static final String dotarticle_id =  "$[\"my.article\"][\"my.identifier\"]";
  static final String dotarticle_date = "$[\"my.article\"][\"my.pubdate\"][\"date\"]";
  static final String dotarticle_title = "$[\"my.article\"][\"my.title\"]";
  static final String dotmeta_publisher = "$[\"my.article\"][\"meta\"][\"my.publisher\"]";
  static final String dotmeta_journal = "$[\"my.article\"][\"meta\"][\"my.journaltitle\"]";
  private final Map<String,JsonPathValue> dotschemaMap =
      new HashMap<String,JsonPathValue>();
  {
    dotschemaMap.put(dotarticle_id, JsonPathJsonMetadataParser.STRING_VALUE);
    dotschemaMap.put(dotarticle_title, JsonPathJsonMetadataParser.STRING_VALUE);
    dotschemaMap.put(dotarticle_date, JsonPathJsonMetadataParser.STRING_VALUE);
    dotschemaMap.put(dotmeta_publisher, JsonPathJsonMetadataParser.STRING_VALUE);
    dotschemaMap.put(dotmeta_journal, JsonPathJsonMetadataParser.STRING_VALUE);
  }

  private final MultiValueMap dotcookMap = new MultiValueMap();
  {
    dotcookMap.put(dotarticle_id, MetadataField.FIELD_PROPRIETARY_IDENTIFIER);
    dotcookMap.put(dotarticle_date, MetadataField.FIELD_DATE);
    dotcookMap.put(dotmeta_publisher, MetadataField.FIELD_PUBLISHER);
    dotcookMap.put(dotmeta_journal, MetadataField.FIELD_PUBLICATION_TITLE);
    dotcookMap.put(dotarticle_title, MetadataField.FIELD_ARTICLE_TITLE);
  }


  // SchemaA takes in all the values from a globalMap
  public void testExtractJsonSchemaA() throws Exception {

    String json_url = getBaseUrl() + getYear() + "/schemaA.json";
    FileMetadataExtractor me = new MyJsonMetadataExtractor(schemaMap, cookMap);
    FileMetadataListExtractor mle =
        new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = extractFromContent(json_url, "application/json", singleArticleContent, mle);
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
  public void testExtractJsonSchemaB() throws Exception {
    String json_url = getBaseUrl() + getYear() + "/schemaB.json";
    FileMetadataExtractor me = new MyJsonMetadataExtractor(schemaMap, cookMap);
    FileMetadataListExtractor mle =
        new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = extractFromContent(json_url, "application/json", singleArticleContent, mle);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodIdentifier, md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
  }

  public void testDotExtractJsonSchemaA() throws Exception {

    String json_url = getBaseUrl() + getYear() + "/schemaA.json";
    FileMetadataExtractor me = new MyJsonMetadataExtractor(dotschemaMap, dotcookMap);
    FileMetadataListExtractor mle =
        new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = extractFromContent(json_url, "application/json", dotArticleContent, mle);
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
  public void testDotExtractJsonSchemaB() throws Exception {
    String json_url = getBaseUrl() + getYear() + "/schemaB.json";
    FileMetadataExtractor me = new MyJsonMetadataExtractor(dotschemaMap, dotcookMap);
    FileMetadataListExtractor mle =
        new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = extractFromContent(json_url, "application/json", dotArticleContent, mle);
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
  private class MyJsonMetadataExtractor extends SourceJsonMetadataExtractor {

    private Map<String,JsonPathValue> schema;
    private MultiValueMap cooker;

    MyJsonMetadataExtractor(Map<String,JsonPathValue> schema,
                            MultiValueMap cooker) {
      this.schema = schema;
      this.cooker = cooker;
    }
    // choose the schema based on the URL - for publishers that use > 1
    @Override
    protected SourceJsonSchemaHelper setUpSchema(CachedUrl cu,
                                                 DocumentContext doc) {
      // Once you have it created, just keep returning the same one. It won't change.
      Matcher AMat = schemaAPATTERN.matcher(cu.getUrl());
      if (AMat.find()) {
        log.debug3("using schemaA - globalMap only");
        return new schemaAJsonSchemaHelper(schema, cooker);
      }
      // Otherwise it's the B schema
      log.debug3("using schemaB - articleMap only");
      return new schemaBJsonSchemaHelper(schema, cooker);
    }

    // Make this do nothing so that we don't need to add actual content files
    // for testing
    @Override
    protected boolean preEmitCheck(SourceJsonSchemaHelper schemaHelper,
                                   CachedUrl cu, ArticleMetadata thisAM) {

      log.debug3("in preEmitCheck which always returns true");
      return true;
    }



    // Schema A will access the content solely through a globalMap
    // since there is only one article per file
    private class schemaAJsonSchemaHelper
        implements SourceJsonSchemaHelper {
      private Map<String,JsonPathValue> schema;
      private MultiValueMap cooker;

      schemaAJsonSchemaHelper(Map<String,JsonPathValue> schema,
                              MultiValueMap cooker) {
        this.schema = schema;
        this.cooker = cooker;
      }

      @Override
      public Map<String, JsonPathValue> getGlobalMetaMap() {
        return schema;
      }

      @Override
      public Map<String, JsonPathValue> getArticleMetaMap() {
        return null;
      }

      @Override
      public String getArticleNode() {
        return null;
      }

      @Override
      public MultiValueMap getCookMap() {
        return cooker;
      }

      @Override
      public String getDeDuplicationJsonKey() {
        return null;
      }

      @Override
      public String getConsolidationJsonKey() {
        return null;
      }

      @Override
      public String getFilenameJsonKey() {
        return null;
      }
    }

    // SchemaB will access the content through an article map where the article
    // node is the top <article> node and no global information
    private class schemaBJsonSchemaHelper implements SourceJsonSchemaHelper {
      private Map<String,JsonPathValue> schema;
      private MultiValueMap cooker;

      schemaBJsonSchemaHelper(Map<String,JsonPathValue> schema,
                              MultiValueMap cooker) {
        this.schema = schema;
        this.cooker = cooker;
      }
      @Override
      public Map<String, JsonPathValue> getGlobalMetaMap() {
        return null;
      }

      @Override
      public Map<String, JsonPathValue> getArticleMetaMap() {
        return schema;
      }

      // use the top of the document
      @Override
      public String getArticleNode() {
        return null;
      }

      @Override
      public MultiValueMap getCookMap() {
        return cooker;
      }

      @Override
      public String getDeDuplicationJsonKey() {
        return null;
      }

      @Override
      public String getConsolidationJsonKey() {
        return null;
      }

      @Override
      public String getFilenameJsonKey() {
        return null;
      }

    }
  }
}
