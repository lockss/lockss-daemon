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

package org.lockss.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.*;
import com.jayway.jsonpath.Configuration.ConfigurationBuilder;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

public class JsonPathMetadataExtractor extends SimpleFileMetadataExtractor {

  static Logger log = Logger.getLogger("JsonPathMetadataExtractor");

  public enum JSON_TYPE {STRING_TYPE, NUMBER_TYPE, OBJECT_TYPE, ARRAY_TYPE, BOOLEAN_TYPE, NULL_TYPE}

  public /**/ static final TypeRef<List<String>> STRING_LIST_TYPE = new TypeRef<List<String>>() {
  };

  /**
   * The json map to use for extracting
   */
  final protected JsonPath[] jsonPathExprs;
  final protected String[] jsonPathKeys;
  final protected JsonPathValue[] pathValues;
  final protected Configuration defConfig = defaultConfig();


  static abstract public class JsonPathValue {

    /**
     * Override this method to specify a different type
     */
    public abstract JSON_TYPE getType();

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
    public JSON_TYPE getType() {
      return JSON_TYPE.STRING_TYPE;
    }
  }

  static final public JsonPathValue STRING_VALUE = new StringValue();

  /**
   * JsonPathValue for a single number value
   */
  static public class NumberValue extends JsonPathValue {

    @Override
    public JSON_TYPE getType() {
      return JSON_TYPE.NUMBER_TYPE;
    }
  }

  static final public JsonPathValue NUMBER_VALUE = new NumberValue();

  /**
   * JsonPathValue for a boolean value
   */
  static public class BooleanValue extends JsonPathValue {

    @Override
    public JSON_TYPE getType() {
      return JSON_TYPE.BOOLEAN_TYPE;
    }
  }

  static final public JsonPathValue BOOLEAN_VALUE = new BooleanValue();

  /**
   * JsonPathValue for an untyped object
   */
  static public class ObjectValue extends JsonPathValue {

    @Override
    public JSON_TYPE getType() {
      return JSON_TYPE.OBJECT_TYPE;
    }
  }

  static final public JsonPathValue OBJECT_VALUE = new ObjectValue();

  /**
   * JsonPathValue for an untyped array
   */
  static public class ArrayValue extends JsonPathValue {

    @Override
    public JSON_TYPE getType() {
      return JSON_TYPE.ARRAY_TYPE;
    }
  }

  static final public JsonPathValue ARRAY_VALUE = new ArrayValue();

  /**
   * JsonPathValue for an untyped array
   */
  static public class NullValue extends JsonPathValue {

    @Override
    public JSON_TYPE getType() {
      return JSON_TYPE.NULL_TYPE;
    }
  }

  static final public JsonPathValue NULL_VALUE = new NullValue();

  /**
   * Setup the default configuration
   */
  static protected Configuration defaultConfig() {
    ConfigurationBuilder builder = Configuration.builder();
    builder.jsonProvider(new JacksonJsonProvider())
      .mappingProvider(new JacksonMappingProvider())
      .options(Option.SUPPRESS_EXCEPTIONS, Option.DEFAULT_PATH_LEAF_TO_NULL,
        Option.ALWAYS_RETURN_LIST);
    return builder.build();
  }

  /**
   * Create an extractor based on the required size.
   *
   * @param size the required size.
   */
  protected JsonPathMetadataExtractor(int size) {
    jsonPathExprs = new JsonPath[size];
    jsonPathKeys = new String[size];
    pathValues = new JsonPathValue[size];
  }

  /**
   * Create an extractor that will extract the textContent of the
   * nodes specified by the XPath expressions.
   *
   * @param jsonPaths the collection of jsonPath expressions whose
   * text content to extract.
   */
  public JsonPathMetadataExtractor(Collection<String> jsonPaths) {
    this(jsonPaths.size());
    int i = 0;
    for (String path : jsonPaths) {
      jsonPathKeys[i] = path;
      jsonPathExprs[i] = JsonPath.compile(path);
      pathValues[i] = STRING_VALUE;
      i++;
    }
  }

  /**
   * Create an extractor that will extract the textContent of the
   * nodes specified by the XPath expressions by applying the
   * corresponding NodeValues.
   *
   * @param jsonPathMap the map of jsonpath expressions whose value to extract
   * by applying the corresponding NodeValues.
   */
  public JsonPathMetadataExtractor(Map<String, JsonPathValue> jsonPathMap) {
    this(jsonPathMap.size());

    int i = 0;
    for (Map.Entry<String, JsonPathValue> entry : jsonPathMap.entrySet()) {
      jsonPathKeys[i] = entry.getKey();
      jsonPathExprs[i] = JsonPath.compile(entry.getKey());
      pathValues[i] = entry.getValue();
      i++;
    }
  }

  /**
   * Parse content on CachedUrl,  Return a Metadata object describing it
   *
   * @param target the purpose for which metadata is being extracted
   * @param cu the CachedUrl to extract from
   */
  @Override
  public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
    throws IOException, PluginException {
    if (cu == null) {
      throw new IllegalArgumentException("null CachedUrl");
    }
    ArticleMetadata am = new ArticleMetadata();
    if (!cu.hasContent()) {
      return am;
    }
    BufferedReader bReader = new BufferedReader(cu.openForReading());
    DocumentContext doc;
    try {
      doc = JsonPath.parse(bReader, defConfig);
    }
    finally {
      IOUtil.safeClose(bReader);
    }

    for (int i = 0; i < jsonPathKeys.length; i++) {
      JSON_TYPE type = pathValues[i].getType();
      JsonPath expr = jsonPathExprs[i];
      String key = jsonPathKeys[i];
      String value = null;

      switch (type) {
        case STRING_TYPE:
          value = doc.read(expr, String.class);
          break;
        case NUMBER_TYPE:
          Number num = doc.read(expr, Number.class);
          value = (num == null) ? null : num.toString();
          break;
        case OBJECT_TYPE:
          Object obj = doc.read(expr);
          value = (obj == null) ? null : obj.toString();
          break;
        case ARRAY_TYPE:
          List<String> vals = doc.read(expr, STRING_LIST_TYPE);
          for (String val : vals) {
            if (!StringUtil.isNullString(val)) {
              am.putRaw(key, val);
            }
          }
          break;
        case BOOLEAN_TYPE:
          Boolean b = doc.read(expr, Boolean.class);
          value = (b == null) ? null : b.toString();
          break;
        case NULL_TYPE:
          value = null;
          break;
        default:
          log.debug("Unsupported type: " + type.toString());
          break;
      }
      if (!StringUtil.isNullString(value)) {
        am.putRaw(key, value);
      }
    }
    return am;
  }
}
