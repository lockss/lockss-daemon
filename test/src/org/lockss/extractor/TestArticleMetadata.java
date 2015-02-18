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

package org.lockss.extractor;

import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.test.*;
import org.lockss.util.*;
import static org.lockss.extractor.MetadataField.*;
import static org.lockss.extractor.ArticleMetadata.InvalidValue;

public class TestArticleMetadata extends LockssTestCase {
  
  private static final Logger log = Logger.getLogger(TestArticleMetadata.class);

  static String RAW_KEY1 = "key1";
  static String RAW_KEY2 = "key2";
  static String RAW_KEY3 = "key3";

  ArticleMetadata am;

  public void setUp() throws Exception {
    super.setUp();
    am = new ArticleMetadata();
  }

  public void testSetLocale() {
    assertSame(MetadataUtil.getDefaultLocale(), am.getLocale());
    am.setLocale(Locale.ITALY);
    assertSame(Locale.ITALY, am.getLocale());
  }

  public void testIllSetLocale() {
    assertSame(MetadataUtil.getDefaultLocale(), am.getLocale());
    assertTrue(am.put(FIELD_JOURNAL_TITLE, "val1"));
    try {
      am.setLocale(Locale.ITALY);
      fail("Attempt to set Locale should throw IllegalStateException");
    } catch (IllegalStateException e) {
    }
    assertSame(MetadataUtil.getDefaultLocale(), am.getLocale());
  }

  // Raw

  public void testRaw() {
    String key = RAW_KEY1;
    assertNull(am.getRaw(key));
    assertEquals(0, am.rawSize());
    assertEmpty(am.rawKeySet());
    assertEmpty(am.rawEntrySet());
    am.putRaw(key, "val1");
    assertEquals("val1", am.getRaw(key));
    assertEquals(1, am.rawSize());
    assertEquals(ListUtil.list("val1"), am.getRawList(key));
    am.putRaw(key, "val2");
    assertEquals("val1", am.getRaw(key));
    assertEquals(ListUtil.list("val1", "val2"), am.getRawList(key));
    am.putRaw(key, "val2");
    assertEquals("val1", am.getRaw(key));
    assertEquals(ListUtil.list("val1", "val2", "val2"), am.getRawList(key));
    assertEquals(1, am.rawSize());

    assertEquals(SetUtil.set(key), am.rawKeySet());
    Set<Map.Entry<String,List<String>>> eset = am.rawEntrySet();
    assertEquals(1, eset.size());
    Map.Entry<String,List<String>> ent =
      (Map.Entry)CollectionUtil.getAnElement(eset);
    assertEquals(key, ent.getKey());
    assertEquals(ListUtil.list("val1", "val2", "val2"), ent.getValue());
  }

  public void testRawKeyCase() {
    String key = "Raw_Key1";
    assertNull(am.getRaw(key));
    am.putRaw(key, "val1");
    assertEquals("val1", am.getRaw(key));
    assertEquals("val1", am.getRaw(key.toLowerCase()));
    assertEquals("val1", am.getRaw(key.toUpperCase()));
    assertEquals(ListUtil.list("val1"), am.getRawList(key));
    am.putRaw(key.toLowerCase(), "val2");
    assertEquals("val1", am.getRaw(key));
    assertEquals("val1", am.getRaw(key.toLowerCase()));
    assertEquals("val1", am.getRaw(key.toUpperCase()));
    assertEquals(ListUtil.list("val1", "val2"), am.getRawList(key));
    am.putRaw(key.toUpperCase(), "val3");
    assertEquals("val1", am.getRaw(key));
    assertEquals("val1", am.getRaw(key.toLowerCase()));
    assertEquals("val1", am.getRaw(key.toUpperCase()));
    assertEquals(ListUtil.list("val1", "val2", "val3"), am.getRawList(key));
  }

  // Cooked

  static final String KEY_SINGLE = "one";
  static final MetadataField FIELD_SINGLE =
    new MetadataField(KEY_SINGLE, Cardinality.Single) {
      @Override
      public String validate(ArticleMetadata am, String val)
	  throws MetadataException.ValidationException {
	if (val.matches(".*ill.*")) {
	  throw new MetadataException.ValidationException("Invalid: " + val);
	}
	return val.toLowerCase();
      }};	

  static final String KEY_MULTI = "many";
  static final MetadataField FIELD_MULTI =
    new MetadataField(KEY_MULTI, Cardinality.Multi) {
      @Override
      public String validate(ArticleMetadata am, String val)
	  throws MetadataException.ValidationException {
	if (val.matches(".*ill.*")) {
	  throw new MetadataException.ValidationException("Invalid: " + val);
	}
	return val.toLowerCase();
      }};	

  static final MetadataField FIELD_SPLIT =
    new MetadataField(FIELD_KEYWORDS, MetadataField.splitAt(",", "\""));	

  static final MetadataField FIELD_SPLIT_REVERSE =
    new MetadataField("anotherkey", Cardinality.Multi,
		      MetadataField.splitAt(";")) {
      @Override
      public String validate(ArticleMetadata am, String val)
	  throws MetadataException.ValidationException {
	return StringUtils.reverse(val);
      }};	


  public void testPut() throws MetadataException {
    MetadataField field = FIELD_JOURNAL_TITLE;
    assertNull(am.get(field));
    assertEmpty(am.getList(field));
    assertFalse(am.hasValue(field));
    assertFalse(am.hasValidValue(field));
    assertFalse(am.hasInvalidValue(field));
    assertTrue(am.put(field, "val1"));
    assertEquals("val1", am.get(field));
    assertEquals(ListUtil.list("val1"), am.getList(field));
    assertTrue(am.hasValue(field));
    assertTrue(am.hasValidValue(field));
    assertFalse(am.hasInvalidValue(field));
    assertFalse(am.put(field, "val2"));
    assertEquals("val1", am.get(field));
    assertEquals(ListUtil.list("val1"), am.getList(field));
    // store redundant value is ok
    assertTrue(am.put(field, "val1"));
    assertEquals("val1", am.get(field));
    assertEquals(ListUtil.list("val1"), am.getList(field));
  }

  public void testPutValid() throws MetadataException {
    MetadataField field = FIELD_SINGLE;
    assertNull(am.get(field));
    assertEmpty(am.getList(field));
    assertFalse(am.hasValue(field));
    assertFalse(am.hasValidValue(field));
    assertFalse(am.hasInvalidValue(field));
    assertTrue(am.put(field, "valid1"));
    assertEquals("valid1", am.get(field));
    assertEquals(ListUtil.list("valid1"), am.getList(field));
    assertTrue(am.hasValue(field));
    assertTrue(am.hasValidValue(field));
    assertFalse(am.hasInvalidValue(field));
    assertNull(am.getInvalid(field));
    assertFalse(am.put(field, "valid2"));
    assertEquals("valid1", am.get(field));
    assertEquals(ListUtil.list("valid1"), am.getList(field));
    // store redundant value is ok
    assertTrue(am.put(field, "valid1"));
    assertEquals("valid1", am.get(field));
    assertEquals(ListUtil.list("valid1"), am.getList(field));
  }

  public void testPutNormalize() throws MetadataException {
    MetadataField field = FIELD_SINGLE;
    assertNull(am.get(field));
    assertEmpty(am.getList(field));
    assertFalse(am.hasValue(field));
    assertFalse(am.hasValidValue(field));
    assertFalse(am.hasInvalidValue(field));
    assertTrue(am.put(field, "VALID1"));
    assertEquals("valid1", am.get(field));
    assertEquals(ListUtil.list("valid1"), am.getList(field));
    assertTrue(am.hasValue(field));
    assertTrue(am.hasValidValue(field));
    assertFalse(am.hasInvalidValue(field));
    assertNull(am.getInvalid(field));
    assertFalse(am.put(field, "valid2"));
    assertEquals("valid1", am.get(field));
    assertEquals(ListUtil.list("valid1"), am.getList(field));
    // redundant normalized value is ok
    assertTrue(am.put(field, "valid1"));
    assertEquals("valid1", am.get(field));
    assertEquals(ListUtil.list("valid1"), am.getList(field));
  }

  public void testPutInvalid() throws MetadataException {
    MetadataField field = FIELD_SINGLE;
    assertNull(am.get(field));
    assertEmpty(am.getList(field));
    assertFalse(am.hasValue(field));
    assertFalse(am.hasValidValue(field));
    assertFalse(am.hasInvalidValue(field));
    assertFalse(am.put(field, "illegal"));
    assertNull(am.get(field));
    assertEmpty(am.getList(field));
    InvalidValue ival = (InvalidValue)am.getInvalid(field);
    assertEquals("illegal", ival.getRawValue());
    assertTrue(am.hasValue(field));
    assertFalse(am.hasValidValue(field));
    assertTrue(am.hasInvalidValue(field));

    // 2nd illegal value shouldn't replace first
    assertFalse(am.put(field, "illegaltoo"));
    assertNull(am.get(field));
    assertEmpty(am.getList(field));
    InvalidValue ival2 = (InvalidValue)am.getInvalid(field);
    assertEquals("illegal", ival2.getRawValue());
    assertTrue(am.hasValue(field));
    assertFalse(am.hasValidValue(field));
    assertTrue(am.hasInvalidValue(field));

    // legal value replaces
    assertTrue(am.put(field, "valid1"));
    assertEquals("valid1", am.get(field));
    assertEquals(ListUtil.list("valid1"), am.getList(field));
    assertTrue(am.hasValue(field));
    assertTrue(am.hasValidValue(field));
    assertFalse(am.hasInvalidValue(field));
    assertFalse(am.put(field, "valid2"));
    assertEquals("valid1", am.get(field));
    assertEquals(ListUtil.list("valid1"), am.getList(field));
    // store redundant value is ok
    assertTrue(am.put(field, "valid1"));
    assertEquals("valid1", am.get(field));
    assertEquals(ListUtil.list("valid1"), am.getList(field));
  }

  public void testPutInvalidThrow() throws MetadataException {
    MetadataField field = FIELD_SINGLE;
    assertNull(am.get(field));
    assertEmpty(am.getList(field));
    assertFalse(am.hasValue(field));
    assertFalse(am.hasValidValue(field));
    assertFalse(am.hasInvalidValue(field));
    try {
      am.putValid(field, "illegal");
      fail("Should throw ValidationException");
    } catch (MetadataException.ValidationException e) {
      assertEquals("illegal", e.getRawValue());
    }
    assertNull(am.get(field));
    assertEmpty(am.getList(field));
    InvalidValue ival = (InvalidValue)am.getInvalid(field);
    assertEquals("illegal", ival.getRawValue());
    assertTrue(am.hasValue(field));
    assertFalse(am.hasValidValue(field));
    assertTrue(am.hasInvalidValue(field));

    // 2nd illegal value shouldn't replace first
    try {
      am.putValid(field, "illegaltoo");
      fail("Should throw ValidationException");
    } catch (MetadataException.ValidationException e) {
      assertEquals("illegaltoo", e.getRawValue());
    }
    assertNull(am.get(field));
    assertEmpty(am.getList(field));
    InvalidValue ival2 = (InvalidValue)am.getInvalid(field);
    assertEquals("illegal", ival2.getRawValue());
    assertTrue(am.hasValue(field));
    assertFalse(am.hasValidValue(field));
    assertTrue(am.hasInvalidValue(field));

    // legal value replaces
    am.putValid(field, "valid1");
    assertEquals("valid1", am.get(field));
    assertEquals(ListUtil.list("valid1"), am.getList(field));
    assertTrue(am.hasValue(field));
    assertTrue(am.hasValidValue(field));
    assertFalse(am.hasInvalidValue(field));

    try {
      am.putValid(field, "valid2");
      fail("Should throw CardinalityException");
    } catch (MetadataException.CardinalityException e) {
      assertEquals("valid2", e.getRawValue());
    }
    assertEquals("valid1", am.get(field));
    assertEquals(ListUtil.list("valid1"), am.getList(field));
    // store redundant value is ok
    am.putValid(field, "valid1");
    assertEquals("valid1", am.get(field));
    assertEquals(ListUtil.list("valid1"), am.getList(field));
  }

  public void testPutMultiValid() throws MetadataException {
    MetadataField field = FIELD_MULTI;
    assertNull(am.get(field));
    assertEmpty(am.getList(field));
    assertFalse(am.hasValue(field));
    assertFalse(am.hasValidValue(field));
    assertFalse(am.hasInvalidValue(field));
    assertTrue(am.put(field, "valid1"));
    assertEquals("valid1", am.get(field));
    assertEquals(ListUtil.list("valid1"), am.getList(field));
    assertTrue(am.hasValue(field));
    assertTrue(am.hasValidValue(field));
    assertFalse(am.hasInvalidValue(field));
    assertNull(am.getInvalid(field));
    assertTrue(am.put(field, "valid2"));
    assertEquals("valid1", am.get(field));
    assertEquals(ListUtil.list("valid1", "valid2"), am.getList(field));
    assertTrue(am.put(field, "valid1"));
    assertEquals("valid1", am.get(field));
    assertEquals(ListUtil.list("valid1", "valid2", "valid1"),
		 am.getList(field));
  }

  public void testPutMultiNormalize() throws MetadataException {
    MetadataField field = FIELD_MULTI;
    assertNull(am.get(field));
    assertEmpty(am.getList(field));
    assertFalse(am.hasValue(field));
    assertFalse(am.hasValidValue(field));
    assertFalse(am.hasInvalidValue(field));
    assertTrue(am.put(field, "VALID1"));
    assertEquals("valid1", am.get(field));
    assertEquals(ListUtil.list("valid1"), am.getList(field));
    assertTrue(am.hasValue(field));
    assertTrue(am.hasValidValue(field));
    assertFalse(am.hasInvalidValue(field));
    assertNull(am.getInvalid(field));
    assertTrue(am.put(field, "Valid2"));
    assertEquals("valid1", am.get(field));
    assertEquals(ListUtil.list("valid1", "valid2"), am.getList(field));
    assertTrue(am.put(field, "valid1"));
    assertEquals("valid1", am.get(field));
    assertEquals(ListUtil.list("valid1", "valid2", "valid1"),
		 am.getList(field));
  }

  public void testPutMultiInvalid() throws MetadataException {
    MetadataField field = FIELD_MULTI;
    assertNull(am.get(field));
    assertEmpty(am.getList(field));
    assertFalse(am.hasValue(field));
    assertFalse(am.hasValidValue(field));
    assertFalse(am.hasInvalidValue(field));
    assertFalse(am.put(field, "illegal"));
    assertNull(am.get(field));
    assertEmpty(am.getList(field));
    assertFalse(am.hasValue(field));
    assertFalse(am.hasValidValue(field));
    assertFalse(am.hasInvalidValue(field));
    assertNull(am.getInvalid(field));

    assertTrue(am.put(field, "valid1"));
    assertEquals("valid1", am.get(field));
    assertEquals(ListUtil.list("valid1"), am.getList(field));
    assertTrue(am.hasValue(field));
    assertTrue(am.hasValidValue(field));
    assertFalse(am.hasInvalidValue(field));
    assertTrue(am.put(field, "valid2"));
    assertEquals("valid1", am.get(field));
    assertEquals(ListUtil.list("valid1", "valid2"), am.getList(field));
    assertTrue(am.put(field, "valid1"));
    assertEquals("valid1", am.get(field));
    assertEquals(ListUtil.list("valid1", "valid2", "valid1"),
		 am.getList(field));
  }

  public void testPutMultiInvalidThrow() throws MetadataException {
    MetadataField field = FIELD_MULTI;
    assertNull(am.get(field));
    assertEmpty(am.getList(field));
    assertFalse(am.hasValue(field));
    assertFalse(am.hasValidValue(field));
    assertFalse(am.hasInvalidValue(field));
    try {
      am.putValid(field, "illegal");
      fail("Should throw ValidationException");
    } catch (MetadataException.ValidationException e) {
      assertEquals("illegal", e.getRawValue());
    }
    assertNull(am.get(field));
    assertEmpty(am.getList(field));
    assertNull(am.getInvalid(field));
    assertFalse(am.hasValue(field));
    assertFalse(am.hasValidValue(field));
    assertFalse(am.hasInvalidValue(field));

    assertTrue(am.put(field, "valid1"));
    assertEquals("valid1", am.get(field));
    assertEquals(ListUtil.list("valid1"), am.getList(field));
    assertTrue(am.hasValue(field));
    assertTrue(am.hasValidValue(field));
    assertFalse(am.hasInvalidValue(field));
    assertTrue(am.put(field, "valid2"));
    assertEquals("valid1", am.get(field));
    assertEquals(ListUtil.list("valid1", "valid2"), am.getList(field));
    assertTrue(am.put(field, "valid1"));
    assertEquals("valid1", am.get(field));
    assertEquals(ListUtil.list("valid1", "valid2", "valid1"),
		 am.getList(field));
  }

  public void testPutIfBetter() {
    // Don't store null
    assertFalse(am.putIfBetter(MetadataField.FIELD_VOLUME, null));
    assertEquals(null, am.get(MetadataField.FIELD_VOLUME));
    // Store new val
    assertTrue(am.putIfBetter(MetadataField.FIELD_VOLUME, "17"));
    assertEquals("17", am.get(MetadataField.FIELD_VOLUME));
    // Don't replace valid val
    assertFalse(am.putIfBetter(MetadataField.FIELD_VOLUME, "42"));
    assertEquals("17", am.get(MetadataField.FIELD_VOLUME));
    assertFalse(am.putIfBetter(MetadataField.FIELD_VOLUME, null));
    assertEquals("17", am.get(MetadataField.FIELD_VOLUME));

    // add an invalid value
    assertFalse(am.put(FIELD_SINGLE, "illegal"));
    assertTrue(am.hasInvalidValue(FIELD_SINGLE));
    // Don't store null
    assertFalse(am.putIfBetter(FIELD_SINGLE, null));
    assertEquals(null, am.get(FIELD_SINGLE));
    assertEquals("illegal", am.getInvalid(FIELD_SINGLE).getRawValue());
    // New val replaces invalid value
    assertTrue(am.putIfBetter(FIELD_SINGLE, "71"));
    assertEquals("71", am.get(FIELD_SINGLE));
  }  

  public void testReplace() throws MetadataException {
    MetadataField field = FIELD_SINGLE;
    assertTrue(am.put(field, "valid1"));
    assertEquals("valid1", am.get(field));
    assertFalse(am.put(field, "valid2"));
    assertEquals("valid1", am.get(field));
    assertTrue(am.replace(field, "valid3"));
    assertEquals("valid3", am.get(field));
  }

  public void testSplit() throws MetadataException {
    MetadataField field = FIELD_SPLIT;
    assertNull(am.get(field));
    assertTrue(am.put(field, "val1,val2"));
    assertEquals("val1", am.get(field));
    assertEquals(ListUtil.list("val1", "val2"), am.getList(field));
    assertTrue(am.hasValue(field));
    assertTrue(am.hasValidValue(field));
    assertFalse(am.hasInvalidValue(field));
    assertTrue(am.put(field, "val3"));
    assertEquals("val1", am.get(field));
    assertEquals(ListUtil.list("val1", "val2", "val3"), am.getList(field));
    assertTrue(am.put(field, "\"delimv1,delimv3,delimv2\""));
    assertEquals("val1", am.get(field));
    assertEquals(ListUtil.list("val1", "val2", "val3",
			       "delimv1", "delimv3" ,"delimv2"),
		 am.getList(field));
  }

  public void testIllSplit() throws MetadataException {
    try {
      MetadataField field = new MetadataField("foo", Cardinality.Single,
					      MetadataField.splitAt(";"));
      fail("Shouldn't be able to create single-valued field with splitter");
    } catch (IllegalArgumentException e) {
    }
  }

  MultiMap multiMap(Map map) {
    MultiMap res = new MultiValueMap();
    res.putAll(map);
    return res;
  }

  public void testCook() throws MetadataException {
    am.putRaw("r1", "V1");
    am.putRaw("r2", "V2");
    am.putRaw("r3", "V3");
    am.putRaw("r3", "V4");
    am.putRaw("r4", "s1,s2,s4");
    am.putRaw("r4", "s3");
    am.putRaw("r5", "abc;def");
    Map map = MapUtil.map("r2", FIELD_ARTICLE_TITLE,
			  "r1", FIELD_VOLUME,
			  "r3", FIELD_AUTHOR,
			  "r4", FIELD_SPLIT);
    map.put("r5", FIELD_SPLIT_REVERSE);
    assertEmpty(am.cook(multiMap(map)));
    assertEquals("V1", am.get(FIELD_VOLUME));
    assertEquals("V2", am.get(FIELD_ARTICLE_TITLE));
    assertEquals("V3", am.get(FIELD_AUTHOR));
    assertEquals(ListUtil.list("V3", "V4"), am.getList(FIELD_AUTHOR));
    assertEquals(ListUtil.list("s1", "s2", "s4","s3"), am.getList(FIELD_SPLIT));
    assertEquals(ListUtil.list("cba", "fed"), am.getList(FIELD_SPLIT_REVERSE));
  }

  public void testCookError() throws MetadataException {
    am.putRaw("r1", "V1");
    am.putRaw("r2", "V2");
    am.putRaw("r3", "V3");
    am.putRaw("r3", "V4");
    Map map = MapUtil.map("r2", FIELD_DOI,
			  "r1", FIELD_VOLUME,
			  "r3", FIELD_AUTHOR);
    List<MetadataException> errs = am.cook(multiMap(map));

    assertEquals(1, errs.size());
    MetadataException ex1 = errs.get(0);
    assertEquals(FIELD_DOI, ex1.getField());
    assertEquals("V2", ex1.getRawValue());
    assertClass(MetadataException.ValidationException.class, ex1);

    assertEquals("V1", am.get(FIELD_VOLUME));
    assertEquals(null, am.get(FIELD_DOI));
    assertTrue(am.hasInvalidValue(FIELD_DOI));
    InvalidValue ival = am.getInvalid(FIELD_DOI);
    assertEquals("V2", ival.getRawValue());
    assertSame(ex1, ival.getException());
    assertEquals("V3", am.get(FIELD_AUTHOR));
    assertEquals(ListUtil.list("V3", "V4"), am.getList(FIELD_AUTHOR));
  }

  public void testCookList() throws MetadataException {
    am.putRaw("r1", "V1");
    am.putRaw("r2", "V2");
    am.putRaw("r3", "V3");
    am.putRaw("r3", "V4");
    MultiMap map = new MultiValueMap();
    map.put("r2", FIELD_ARTICLE_TITLE);
    map.put("r1", FIELD_VOLUME);
    map.put("r1", FIELD_ISSUE);
    map.put("r3", FIELD_AUTHOR);
    map.put("r3", FIELD_KEYWORDS);
    am.cook(map);
    assertEquals("V1", am.get(FIELD_VOLUME));
    assertEquals("V1", am.get(FIELD_ISSUE));
    assertEquals("V2", am.get(FIELD_ARTICLE_TITLE));
    assertEquals("V3", am.get(FIELD_AUTHOR));
    assertEquals(ListUtil.list("V3", "V4"), am.getList(FIELD_AUTHOR));
    assertEquals(ListUtil.list("V3", "V4"), am.getList(FIELD_KEYWORDS));
    assertEquals(3, am.rawSize());
    assertEquals(5, am.size());
  }

  public void testCookListError() throws MetadataException {
    am.putRaw("r1", "V1");
    am.putRaw("r2", "V2");
    am.putRaw("r3", "bill");
    am.putRaw("r3", "frank");
    am.putRaw("r3", "william");
    MultiMap map = new MultiValueMap();
    map.put("r2", FIELD_ARTICLE_TITLE);
    map.put("r1", FIELD_VOLUME);
    map.put("r1", FIELD_ISSUE);
    map.put("r3", FIELD_AUTHOR);
    map.put("r3", FIELD_MULTI);
    List<MetadataException> errs = am.cook(multiMap(map));

    assertEquals(2, errs.size());
    MetadataException ex1 = errs.get(0);
    MetadataException ex2 = errs.get(1);
    assertClass(MetadataException.ValidationException.class, ex1);
    assertEquals(FIELD_MULTI, ex1.getField());
    assertEquals("bill", ex1.getRawValue());
    assertClass(MetadataException.ValidationException.class, ex2);
    assertEquals(FIELD_MULTI, ex2.getField());
    assertEquals("william", ex2.getRawValue());

    assertEquals("V1", am.get(FIELD_VOLUME));
    assertEquals("V1", am.get(FIELD_ISSUE));
    assertEquals("V2", am.get(FIELD_ARTICLE_TITLE));
    assertEquals("bill", am.get(FIELD_AUTHOR));
    assertEquals(ListUtil.list("bill", "frank", "william"),
		 am.getList(FIELD_AUTHOR));
    assertEquals(ListUtil.list("frank"), am.getList(FIELD_MULTI));
    assertEquals(3, am.rawSize());
    assertEquals(5, am.size());
  }

  public void testMiscAccessors() throws MetadataException {
    MetadataField field = FIELD_KEYWORDS;
    assertTrue(am.isEmpty());
    assertEquals(0, am.size());
    assertEmpty(am.keySet());
    am.put(field, "val11");
    assertFalse(am.isEmpty());
    assertEquals(1, am.size());
    assertEquals(SetUtil.set(field.getKey()), am.keySet());
    am.put(field, "val12");
    assertFalse(am.isEmpty());
    assertEquals(1, am.size());
    assertEquals(SetUtil.set(field.getKey()), am.keySet());
  }
}
