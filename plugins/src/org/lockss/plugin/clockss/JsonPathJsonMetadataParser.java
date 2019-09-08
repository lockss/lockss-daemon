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


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Configuration.ConfigurationBuilder;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

public class JsonPathJsonMetadataParser {

  static abstract public class JsonPathValue {

    /**
     * Override this method to specify a different type
     */
    public abstract JsonNodeType getType();

    /**
     * Override this method to handle a node-valued
     * property or attribute. The default implementation
     * returns the text content of the node.
     *
     * @param node the node value
     * @return the text value of the node
     */
    public String getValue(JsonNode node) {
      return (node == null) ? null : node.textValue();
    }

    /**
     * Override this method to handle a text-valued
     * property or attribute. The default implementation
     * returns the input string.
     *
     * @param s the text value
     * @return the text value
     */
    public String getValue(String s) {
      return s;
    }

    /**
     * Override this method to handle a boolean-valued
     * property or attribute.  The default implementation
     * returns the string value of the input boolean.
     *
     * @param b the boolean value
     * @return the string value of the boolean
     */
    public String getValue(Boolean b) {
      return (b == null) ? null : b.toString();
    }

    /**
     * Override this method to handle a number-valued
     * property or attribute.  The default implementation
     * returns the string value of the input number.
     *
     * @param n the number value
     * @return the string value of the number
     */
    public String getValue(Number n) {
      return (n == null) ? null : n.toString();
    }
  }

  /**
   * JsonPathValue value for a single String value
   */
  static public class StringValue extends JsonPathValue {

    @Override
    public JsonNodeType getType() {
      return JsonNodeType.STRING;
    }
  }

  static final public JsonPathValue STRING_VALUE = new StringValue();

  /**
   * JsonPathValue for a single number value
   */
  static public class NumberValue extends JsonPathValue {

    @Override
    public JsonNodeType getType() {
      return JsonNodeType.NUMBER;
    }
  }

  static final public JsonPathValue NUMBER_VALUE = new NumberValue();

  /**
   * JsonPathValue for a boolean value
   */
  static public class BooleanValue extends JsonPathValue {

    @Override
    public JsonNodeType getType() {
      return JsonNodeType.BOOLEAN;
    }
  }

  static final public JsonPathValue BOOLEAN_VALUE = new BooleanValue();

  /**
   * JsonPathValue for an untyped object
   */
  static public class ObjectValue extends JsonPathValue {

    @Override
    public JsonNodeType getType() {
      return JsonNodeType.OBJECT;
    }
  }

  static final public JsonPathValue OBJECT_VALUE = new ObjectValue();

  /**
   * JsonPathValue for an untyped array
   */
  static public class ArrayValue extends JsonPathValue {

    @Override
    public JsonNodeType getType() {
      return JsonNodeType.ARRAY;
    }
  }

  static final public JsonPathValue ARRAY_VALUE = new ArrayValue();

  /**
   * JsonPathValue for an untyped array
   */
  static public class NullValue extends JsonPathValue {

    @Override
    public JsonNodeType getType() {
      return JsonNodeType.NULL;
    }
  }

  static final public JsonPathValue NULL_VALUE = new NullValue();
  static final TypeRef<List<String>> STRING_LIST = new TypeRef<List<String>>() {};

  private static Logger log = Logger.getLogger(JsonPathJsonMetadataParser.class);
  protected JsonPathInfo[] gJPathList;
  protected JsonPathInfo[] aJPathList;
  protected JsonPath articlePath;
  protected boolean doJsonFiltering;
  protected Configuration config = makeJsonPathConfiguration();

  /*
   * Bare bones constructor that allows for delayed setting of the schema
   */
  public JsonPathJsonMetadataParser() {
    gJPathList = null;
    aJPathList = null;
    articlePath = null;
    doJsonFiltering = false; // default behavior
  }


  /*
   * Bare bones constructor that allows for delayed setting of the schema
   * but sets up the document filtering
   */
  public JsonPathJsonMetadataParser(boolean doJsonFiltering) {
    this();
    setDoJsonFiltering(doJsonFiltering);
  }

  /*
   * Convenience constructor (legacy) when only one schema is used across the plugin
   *
   * @param globalMap xPaths for data that should be applied across entire XML
   * @param articleNode defines a path to the top of an individual article node
   * @param articleMap path relative to articleNode to apply to each article
   * @throws XPathExpressionException
   */
  public JsonPathJsonMetadataParser(Map<String, JsonPathValue> globalMap,
                                    String articleNode,
                                    Map<String, JsonPathValue> articleMap) {
    setJsonParsingSchema(globalMap, articleNode, articleMap);
  }

  /*
   *  A constructor that allows for the xml filtering of the input stream
   *  This is the convenience (legacy) constructor for when there is only one
   *  schema every used by the plugin
   *
   * @param globalMap JsonPath for data that should be applied across entire XML
   * @param articleNode defines a path to the top of an individual article node
   * @param articleMap path relative to articleNode to apply to each article
   * @param doJsonFiltering whether to pre-filter the xml at Document creation
   * @throws IOException
   */
  public JsonPathJsonMetadataParser(Map<String, JsonPathValue> globalMap,
                                    String articleNode,
                                    Map<String, JsonPathValue> articleMap,
                                    boolean doJsonFiltering) {
    this(globalMap, articleNode, articleMap);
    setDoJsonFiltering(doJsonFiltering);
  }

  /* a convenience to ensure we don't dereference null - used by
   * constructor for this class
   */
  private static int getMapSize(Map<String, JsonPathValue> jpathMap) {
    return ((jpathMap != null) ? jpathMap.size() : 0);
  }

  public void setJsonParsingSchema(Map<String, JsonPathValue> globalMap,
                                   String articleNode, Map<String, JsonPathValue> articleMap) {
    if (gJPathList != null) {
      log.warning("Resetting the global XPath list for this file extraction");
    }
    if (aJPathList != null) {
      log.warning("Resetting the article XPath list for this file extraction");
    }
    gJPathList = new JsonPathInfo[getMapSize(globalMap)];
    if (globalMap != null) {
      int i = 0;
      for (Map.Entry<String, JsonPathValue> entry : globalMap.entrySet()) {
        gJPathList[i] = new JsonPathInfo(entry.getKey(),
            JsonPath.compile(entry.getKey()),
            entry.getValue());
        i++;
      }
    }

    aJPathList = new JsonPathInfo[getMapSize(articleMap)];
    if (articleMap != null) {
      int i = 0;
      for (Map.Entry<String, JsonPathValue> entry : articleMap.entrySet()) {
        aJPathList[i] = new JsonPathInfo(entry.getKey(),
            JsonPath.compile(entry.getKey()),
            entry.getValue());
        i++;
      }
    }

    if (articleNode != null) {
      articlePath = JsonPath.compile(articleNode);
    }
    log.debug3("globalMap: " + globalMap);
    log.debug3("articleNode: " + articleNode);
    log.debug3("articleMap: " + articleMap);
  }

  /*
   * getter/setter for the switch to do xml filtering of input stream
   */

  /**
   * <p>
   * Determines if XML pre-filtering with {@link XmlFilteringInputStream} has
   * been requested for this instance.
   * </p>
   *
   * @return True if JSON pre-filtering has been requested.
   * @since 1.66
   */
  public boolean isDoJsonFiltering() {
    return doJsonFiltering;
  }

  public void setDoJsonFiltering(boolean doJsonFiltering) {
    this.doJsonFiltering = doJsonFiltering;
  }

  /**
   * Extract metadata from the Json Document
   * using the constructor-set jsonPath definitions.
   *
   * @return list of ArticleMetadata objects; one per record in the Json
   */
  public List<ArticleMetadata> extractMetadataFromDocument(MetadataTarget target,
                                                           DocumentContext context) {
    if ((gJPathList == null) || (aJPathList == null)) {
      log.warning("The XML schema was not set for this XML Document");
      return null; // no articles extacted
    }
    List<ArticleMetadata> amList = makeNewAMList();
    ArticleMetadata globalAM = null;

    // no exception thrown but the document wasn't succesfully created
    if (context == null) {
      return amList; // return empty list
    }
    /* GLOBAL - If global data map exists, collect it and put it in a temporary AM */
    if (gJPathList.length > 0) {
      log.debug3("extracting global metadata");
      globalAM = extractDataFromNode(context, gJPathList);
    }
    if (aJPathList.length > 0) {
      /* ARTICLE - If there is no definition of an article path, collect article data from entire tree */
      if (articlePath == null) {
        log.debug3("extract article data from entire document");
        ArticleMetadata oneAM = extractDataFromNode(context, aJPathList);
        addGlobalToArticleAM(globalAM, oneAM);
        amList.add(oneAM);
      }
      else {
        /* Get a list of articles */
        log.debug3("extracting article data from each article path:" + articlePath);
        List<String> articleList = context.read(articlePath,STRING_LIST);
        log.debug3("Looping through article list of " + articleList.size() + " items");
        long startTime = 0;
        long endTime = 0;
        int a_count = 0;
        for (String article : articleList) {
          a_count++;
          if (log.isDebug3()) {
            startTime = System.currentTimeMillis();
            log.debug3("Article node " + a_count);
          }
          if (StringUtil.isNullString(article)) {
            log.debug3("NULL article");
            continue;
          }
          else {
            DocumentContext doc = JsonPath.parse(article);
            ArticleMetadata singleAM = extractDataFromNode(doc, aJPathList);
            addGlobalToArticleAM(globalAM, singleAM);
            amList.add(singleAM); // before going on to the next individual item
          }
          if (log.isDebug3()) {
            endTime = System.currentTimeMillis();
            log.debug3("#" + a_count + " node eval: " + ((endTime - startTime)) + " millisecs");
          }
        }
      }
    }
    else {
      /* No article map, but if there was a global map, use that */
      if (globalAM != null) {
        amList.add(globalAM);
      }
    }
    return amList;
  }

  /**
   * Extract metadata from the XML source specified by the input stream using
   * the constructor-set xPath definitions.
   *
   * @param cu the CachedUrl for the XML source file
   * @return list of ArticleMetadata objects; one per record in the XML
   */
  public List<ArticleMetadata> extractMetadataFromCu(MetadataTarget target, CachedUrl cu)
      throws IOException {
    if (cu == null) {
      throw new IllegalArgumentException("Null CachedUrl");
    }
    if (!cu.hasContent()) {
      throw new IllegalArgumentException("CachedUrl has no content: " + cu.getUrl());
    }

    // this could throw IO or SAX exception - handled  upstream
    DocumentContext doc = createDocument(cu);
    return extractMetadataFromDocument(target, doc);
  }

  /*
   *  from a given node, using a set of jsonPath expressions
   */
  private ArticleMetadata extractDataFromNode(DocumentContext doc,
                                              JsonPathInfo[] jPathList) {

    ArticleMetadata returnAM = makeNewArticleMetadata();

    for (JsonPathInfo info : jPathList) {
      log.debug3("evaluate jsonPath: " + info.jKey);
      JsonNodeType type = info.jVal.getType();
      JsonPath expr = info.jExpr;
      String key = info.jKey;
      String value = null;
      switch (type) {
        case STRING:
          value = doc.read(expr, String.class);
          break;
        case NUMBER:
          Number num = doc.read(expr, Number.class);
          value = (num == null) ? null : num.toString();
          break;
        case OBJECT:
          Object obj = doc.read(expr);
          value = (obj == null) ? null : obj.toString();
          break;
        case ARRAY:
          List<String> vals = doc.read(expr, STRING_LIST);
          for (String val : vals) {
            if (!StringUtil.isNullString(val)) {
              returnAM.putRaw(key, val);
            }
          }
          break;
        case BOOLEAN:
          Boolean b = doc.read(expr, Boolean.class);
          value = (b == null) ? null : b.toString();
          break;
        case NULL:
          value = null;
          break;
        default:
          log.debug("Unsupported type: " + type.toString());
          break;
      }
      if (!StringUtil.isNullString(value)) {
        returnAM.putRaw(key, value);
      }
    }
    return returnAM;
  }

  /*
   * If the globalAM isn't null, take any values from the globalAM and put them
   * in to the singleAM as raw values
   */
  private void addGlobalToArticleAM(ArticleMetadata globalAM, ArticleMetadata singleAM) {
    if (globalAM == null) {
      return; // possible, just ignore
    }
    if (singleAM == null) {
      log.debug3("Null article AM passed in to addGlobalToArticleAM"); // an error
      return;
    }

    // loop over the keys in the global raw map and put their values in to the single raw map
    // don't check for key already in single map - relative v. absolute xpath makes it unlikely
    // and put won't overwrite anyway
    if (globalAM.rawSize() > 0) {
      for (String gKey : globalAM.rawKeySet()) {
        singleAM.putRaw(gKey, globalAM.getRaw(gKey));
      }
    }
  }

  protected Configuration makeJsonPathConfiguration() {
    ConfigurationBuilder builder = Configuration.builder();
    builder.jsonProvider(new JacksonJsonNodeJsonProvider())
        .mappingProvider(new JacksonMappingProvider());
    return builder.build();
  }

  /**
   * Given a CU for an XML file, load and return the XML as a Document "tree".
   *
   * @param cu to the XML file
   * @return Document for the loaded XML file
   */
  protected DocumentContext createDocument(CachedUrl cu) throws IOException {
    InputStream bis = new BufferedInputStream(getInputStreamFromCU(cu));
    DocumentContext doc;
    try {
      doc = JsonPath.parse(bis, config);
    }
    finally {
      IOUtil.safeClose(bis);
    }
    return doc;
  }

  protected InputStream getInputStreamFromCU(CachedUrl cu) {
    return cu.getUnfilteredInputStream();
  }

  /**
   * A wrapper around ArticleMetadata creation to allow for override
   *
   * @return newly created ArticleMetadata object
   */
  protected ArticleMetadata makeNewArticleMetadata() {
    return new ArticleMetadata();
  }

  /**
   * A wrapper around ArticleMetadata list creation to allow for override
   *
   * @return newly created list of ArticleMetadata objects
   */
  protected List<ArticleMetadata> makeNewAMList() {
    return new ArrayList<>();
  }

  /**
   * A class to hold the information we need to use an XPath<br>
   * xKey - the string that defines the path of the XPath
   * xExpr - the compiled XPath expression
   * xVal - the evaluator to get a string return value
   *
   * @author alexohlson
   */
  static protected class JsonPathInfo {

    String jKey;
    JsonPath jExpr;
    JsonPathValue jVal;

    public JsonPathInfo(String keyVal,
                        JsonPath exprVal,
                        JsonPathValue evalVal) {
      jKey = keyVal;
      jExpr = exprVal;
      jVal = evalVal;
    }
  }

}
