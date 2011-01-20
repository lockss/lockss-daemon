/*
 * $Id: TestMetadataField.java,v 1.2 2011-01-20 08:37:43 tlipkis Exp $
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
import java.net.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import static org.lockss.extractor.MetadataField.*;

public class TestMetadataField extends LockssTestCase {

  ArticleMetadata am;

  public void setUp() throws Exception {
    super.setUp();
    am = new ArticleMetadata();
  }

  void assertField(String expKey, Cardinality expCard, MetadataField field) {
    assertEquals(expKey, field.getKey());
    assertEquals(expCard, field.getCardinality());
  }

  public void testPredefined() {
    assertField(KEY_DOI, Cardinality.Single, FIELD_DOI);
    assertField(KEY_ISSN, Cardinality.Single, FIELD_ISSN);
    assertField(KEY_EISSN, Cardinality.Single, FIELD_EISSN);
    assertField(KEY_VOLUME, Cardinality.Single, FIELD_VOLUME);
    assertField(KEY_ISSUE, Cardinality.Single, FIELD_ISSUE);
    assertField(KEY_START_PAGE, Cardinality.Single, FIELD_START_PAGE);
    assertField(KEY_DATE, Cardinality.Single, FIELD_DATE);
    assertField(KEY_ARTICLE_TITLE, Cardinality.Single, FIELD_ARTICLE_TITLE);
    assertField(KEY_JOURNAL_TITLE, Cardinality.Single, FIELD_JOURNAL_TITLE);
    assertField(KEY_AUTHOR, Cardinality.Multi, FIELD_AUTHOR);
    assertField(KEY_ACCESS_URL, Cardinality.Single, FIELD_ACCESS_URL);
    assertField(KEY_KEYWORDS, Cardinality.Multi, FIELD_KEYWORDS);
    assertField(DC_KEY_IDENTIFIER, Cardinality.Single, DC_FIELD_IDENTIFIER);
    assertField(DC_KEY_DATE, Cardinality.Single, DC_FIELD_DATE);
    assertField(DC_KEY_CONTRIBUTOR, Cardinality.Single, DC_FIELD_CONTRIBUTOR);
  }

  public void testDefault() throws MetadataException. ValidationException {
    MetadataField f1 = new MetadataField.Default("key1");
    assertField("key1", Cardinality.Single, f1);    
    String val = "value";
    assertSame(val, f1.validate(am, val));
  }

  static class TestValidator implements MetadataField.Validator {
    @Override
    public String validate(ArticleMetadata am, MetadataField field, String value)
	throws MetadataException.ValidationException {
      if (value.matches(".*ill.*")) {
	throw new MetadataException.ValidationException("Invalid: " + value);
      }
      return value.toLowerCase();
    }};	

  public void testDoi() throws MetadataException.ValidationException {
    MetadataField f1 = FIELD_DOI;
    assertEquals("10.1234/56", f1.validate(am, "10.1234/56"));
    assertEquals("10.1234/56", f1.validate(am, "doi:10.1234/56"));
    assertEquals("10.1234/56", f1.validate(am, "DOI:10.1234/56"));
    try {
      f1.validate(am, "not.a.doi.1234");
      fail("Should throw ValidationException");
    } catch (MetadataException.ValidationException e) {
    }
    try {
      f1.validate(am, "dio:10.1234/56");
      fail("Should throw ValidationException");
    } catch (MetadataException.ValidationException e) {
    }
  }

  public void testIssn() throws MetadataException.ValidationException {
    MetadataField f1 = FIELD_ISSN;
    assertEquals("1234-5679", f1.validate(am, "1234-5679"));
    assertEquals("1234-5679", f1.validate(am, "issn:1234-5679"));
    assertEquals("1234-5679", f1.validate(am, "ISSN:1234-5679"));
    try {
      f1.validate(am, "not.a.issn.1234");
      fail("Should throw ValidationException");
    } catch (MetadataException.ValidationException e) {
    }
    try {
      f1.validate(am, "eissn:1234-5679");
      fail("Should throw ValidationException");
    } catch (MetadataException.ValidationException e) {
    }
  }

  public void testEissn() throws MetadataException.ValidationException {
    MetadataField f1 = FIELD_EISSN;
    assertEquals("1234-5679", f1.validate(am, "1234-5679"));
    assertEquals("1234-5679", f1.validate(am, "eissn:1234-5679"));
    assertEquals("1234-5679", f1.validate(am, "EISSN:1234-5679"));
    try {
      f1.validate(am, "not.a.eissn.1234");
      fail("Should throw ValidationException");
    } catch (MetadataException.ValidationException e) {
    }
    try {
      f1.validate(am, "issn:1234-5679");
      fail("Should throw ValidationException");
    } catch (MetadataException.ValidationException e) {
    }
  }

  public void testConstructors() throws MetadataException.ValidationException {
    MetadataField f1;
    f1 = new MetadataField("foo");
    assertField("foo", Cardinality.Single, f1);    
    String val = "value";
    assertSame(val, f1.validate(am, val));
    assertFalse(f1.hasSplitter());

    f1 = new MetadataField("bar", Cardinality.Multi);
    assertField("bar", Cardinality.Multi, f1);    
    assertSame(val, f1.validate(am, val));
    assertFalse(f1.hasSplitter());
    assertEquals(ListUtil.list("aval;bval"), f1.split(am, "aval;bval"));

    f1 = new MetadataField("bar", Cardinality.Single, new TestValidator());
    assertField("bar", Cardinality.Single, f1);    
    assertFalse(f1.hasSplitter());
    assertEquals(ListUtil.list("aval;bval"), f1.split(am, "aval;bval"));
    assertEquals("foo", f1.validate(am, "Foo"));
    try {
      f1.validate(am, "illness");
      fail("Should throw ValidationException");
    } catch (MetadataException.ValidationException e) {
    }

    try {
      new MetadataField("bar", Cardinality.Single,
			MetadataField.splitAt(";"));
      fail("Should throw IllegalArgumentException");
    } catch (IllegalArgumentException e) {
    }      
    f1 = new MetadataField("keym", Cardinality.Multi,
			   MetadataField.splitAt(";"));
    assertField("keym", Cardinality.Multi, f1);    
    assertTrue(f1.hasSplitter());
    assertEquals("illness", f1.validate(am, "illness"));
    assertEquals(ListUtil.list("one", "two", "four"),
		 f1.split(am, "one;two;four"));
  }

  public void testSplitter1() {
    MetadataField.Splitter spl = MetadataField.splitAt(";");
    assertEquals(ListUtil.list("one", "two", "four"),
		 spl.split(am, null, "one;two;four"));
    assertEquals(ListUtil.list("one", "two", "four"),
		 spl.split(am, null, "  one;two  ;  four  "));
    assertEquals(ListUtil.list("\"one", "two\""),
		 spl.split(am, null, "\"one;;two\""));
  }

  public void testSplitter2() {
    MetadataField.Splitter spl = MetadataField.splitAt(";", "\"");
    assertEquals(ListUtil.list("one", "two", "four"),
		 spl.split(am, null, "one;two;four"));
    assertEquals(ListUtil.list("one", "two"),
		 spl.split(am, null, "\"one;two\""));
    assertEquals(ListUtil.list("one", "two"),
		 spl.split(am, null, " \"one; two\"  "));
  }

  public void testSplitter3() {
    MetadataField.Splitter spl = MetadataField.splitAt(";", "(", ")");
    assertEquals(ListUtil.list("one", "two", "four"),
		 spl.split(am, null, "one;two;four"));
    assertEquals(ListUtil.list("one", "two"),
		 spl.split(am, null, "(one;two)"));
    assertEquals(ListUtil.list("one", "two"),
		 spl.split(am, null, "  (one ; ; two)  "));
  }

  public void testFindField() {
    assertSame(FIELD_VOLUME, MetadataField.findField(KEY_VOLUME));
    assertNull(MetadataField.findField("nosuchfield"));
  }

//   public void testValidate() {
//     MetadataField field = new MetadataField(
//     assertSame(FIELD_VOLUME, MetadataField.findField(KEY_VOLUME));
//     assertNull(MetadataField.findField("nosuchfield"));
//   }



  public void testPutMulti() {
    assertSame(FIELD_VOLUME, MetadataField.findField(KEY_VOLUME));
    assertNull(MetadataField.findField("nosuchfield"));
  }
}
