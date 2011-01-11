/*
 * $Id: TestArticleMetadata.java,v 1.2 2011-01-11 05:39:07 tlipkis Exp $
 */

/*

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

package org.lockss.extractor;

import java.io.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import static org.lockss.extractor.MetadataField.*;
import static org.lockss.extractor.MetadataException.*;
import static org.lockss.extractor.ArticleMetadata.InvalidValue;
import static java.util.AbstractMap.SimpleEntry;

public class TestArticleMetadata extends LockssTestCase {
  static Logger log = Logger.getLogger("TestArticleMetadata");

  static String RAW_KEY1 = "key1";
  static String RAW_KEY2 = "key2";
  static String RAW_KEY3 = "key3";

  ArticleMetadata am;

  public void setUp() throws Exception {
    super.setUp();
    am = new ArticleMetadata();
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
      public String validate(String val)
	  throws MetadataException.ValidationException {
	log.info("validate("+val+")");
	if (val.matches(".*ill.*")) {
	  throw new MetadataException.ValidationException("Invalid: " + val);
	}
	return val.toLowerCase();
      }};	

  static final String KEY_MULTI = "many";
  static final MetadataField FIELD_MULTI =
    new MetadataField(KEY_MULTI, Cardinality.Multi) {
      public String validate(String val)
	  throws MetadataException.ValidationException {
	log.info("validate("+val+")");
	if (val.matches(".*ill.*")) {
	  throw new MetadataException.ValidationException("Invalid: " + val);
	}
	return val.toLowerCase();
      }};	


  public void testPut() throws MetadataException {
    MetadataField field = FIELD_JOURNAL_TITLE;
    assertNull(am.get(field));
    assertNull(am.getList(field));
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
    assertNull(am.getList(field));
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
    assertNull(am.getList(field));
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
    assertNull(am.getList(field));
    assertFalse(am.hasValue(field));
    assertFalse(am.hasValidValue(field));
    assertFalse(am.hasInvalidValue(field));
    assertFalse(am.put(field, "illegal"));
    assertNull(am.get(field));
    assertNull(am.getList(field));
    InvalidValue ival = (InvalidValue)am.getInvalid(field);
    assertEquals("illegal", ival.getRawValue());
    assertTrue(am.hasValue(field));
    assertFalse(am.hasValidValue(field));
    assertTrue(am.hasInvalidValue(field));

    // 2nd illegal value shouldn't replace first
    assertFalse(am.put(field, "illegaltoo"));
    assertNull(am.get(field));
    assertNull(am.getList(field));
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
    assertNull(am.getList(field));
    assertFalse(am.hasValue(field));
    assertFalse(am.hasValidValue(field));
    assertFalse(am.hasInvalidValue(field));
    try {
      am.putValid(field, "illegal");
    } catch (MetadataException.ValidationException e) {
      assertEquals("illegal", e.getRawValue());
    }
    assertNull(am.get(field));
    assertNull(am.getList(field));
    InvalidValue ival = (InvalidValue)am.getInvalid(field);
    assertEquals("illegal", ival.getRawValue());
    assertTrue(am.hasValue(field));
    assertFalse(am.hasValidValue(field));
    assertTrue(am.hasInvalidValue(field));

    // 2nd illegal value shouldn't replace first
    try {
      am.putValid(field, "illegaltoo");
    } catch (MetadataException.ValidationException e) {
      assertEquals("illegaltoo", e.getRawValue());
    }
    assertNull(am.get(field));
    assertNull(am.getList(field));
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
    assertNull(am.getList(field));
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
    assertNull(am.getList(field));
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
    assertNull(am.getList(field));
    assertFalse(am.hasValue(field));
    assertFalse(am.hasValidValue(field));
    assertFalse(am.hasInvalidValue(field));
    assertFalse(am.put(field, "illegal"));
    assertNull(am.get(field));
    assertNull(am.getList(field));
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
    assertNull(am.getList(field));
    assertFalse(am.hasValue(field));
    assertFalse(am.hasValidValue(field));
    assertFalse(am.hasInvalidValue(field));
    try {
      am.putValid(field, "illegal");
    } catch (MetadataException.ValidationException e) {
      assertEquals("illegal", e.getRawValue());
    }
    assertNull(am.get(field));
    assertNull(am.getList(field));
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

  public void testCook() throws MetadataException {
    am.putRaw("r1", "V1");
    am.putRaw("r2", "V2");
    am.putRaw("r3", "V3");
    am.putRaw("r3", "V4");
    Map map = MapUtil.map("r2", FIELD_ARTICLE_TITLE,
			  "r1", FIELD_VOLUME,
			  "r3", FIELD_AUTHOR);
    am.cook(map);
    assertEquals("V1", am.get(FIELD_VOLUME));
    assertEquals("V2", am.get(FIELD_ARTICLE_TITLE));
    assertEquals("V3", am.get(FIELD_AUTHOR));
    assertEquals(ListUtil.list("V3", "V4"), am.getList(FIELD_AUTHOR));
  }

  public void testCookList() throws MetadataException {
    am.putRaw("r1", "V1");
    am.putRaw("r2", "V2");
    am.putRaw("r3", "V3");
    am.putRaw("r3", "V4");
    Map map = MapUtil.map("r2", FIELD_ARTICLE_TITLE,
			  "r1", ListUtil.list(FIELD_VOLUME, FIELD_ISSUE),
			  "r3", ListUtil.list(FIELD_AUTHOR, FIELD_KEYWORDS));
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



  public void testMiscAccessors() throws MetadataException {
    MetadataField field = FIELD_KEYWORDS;
    assertTrue(am.isEmpty());
    assertEquals(0, am.size());
    assertEmpty(am.keySet());
//     assertEmpty(am.entrySet());
    am.put(field, "val11");
    assertFalse(am.isEmpty());
    assertEquals(1, am.size());
    assertEquals(SetUtil.set(field.getKey()), am.keySet());
//     assertEquals(SetUtil.set(new SimpleEntry(field.getKey(),
// 					     ListUtil.list("val11"))),
// 		 am.entrySet());
    am.put(field, "val12");
    assertFalse(am.isEmpty());
    assertEquals(1, am.size());
    assertEquals(SetUtil.set(field.getKey()), am.keySet());
//     assertEquals(SetUtil.set(new SimpleEntry(field.getKey(),
// 					     ListUtil.list("val11", "val12"))),
// 		 am.entrySet());
  }
}
