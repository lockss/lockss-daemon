/*
 * $Id$
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

import org.lockss.extractor.MetadataField.Cardinality;
import org.lockss.test.*;
import org.lockss.util.*;

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
    assertField(KEY_ACCESS_URL, Cardinality.Single, FIELD_ACCESS_URL);
    assertField(KEY_ARTICLE_TITLE, Cardinality.Single, FIELD_ARTICLE_TITLE);
    assertField(KEY_AUTHOR, Cardinality.Multi, FIELD_AUTHOR);
    assertField(KEY_DATE, Cardinality.Single, FIELD_DATE);
    assertField(KEY_DOI, Cardinality.Single, FIELD_DOI);
    assertField(KEY_EISSN, Cardinality.Single, FIELD_EISSN);
    assertField(KEY_ISSUE, Cardinality.Single, FIELD_ISSUE);
    assertField(KEY_ISSN, Cardinality.Single, FIELD_ISSN);
    assertField(KEY_JOURNAL_TITLE, Cardinality.Single, FIELD_JOURNAL_TITLE);
    assertField(KEY_PUBLICATION_TITLE, Cardinality.Single, FIELD_PUBLICATION_TITLE);
    assertField(KEY_SERIES_TITLE, Cardinality.Single, FIELD_SERIES_TITLE);
    assertField(KEY_KEYWORDS, Cardinality.Multi, FIELD_KEYWORDS);
    assertField(KEY_PUBLISHER, Cardinality.Single, FIELD_PUBLISHER);
    assertField(KEY_START_PAGE, Cardinality.Single, FIELD_START_PAGE);
    assertField(KEY_VOLUME, Cardinality.Single, FIELD_VOLUME);
    assertField(KEY_LANGUAGE, Cardinality.Single, FIELD_LANGUAGE);
    assertField(KEY_FORMAT, Cardinality.Single, FIELD_FORMAT);


    assertField(DC_KEY_CITATION_CHAPTER, Cardinality.Single,
        DC_FIELD_CITATION_CHAPTER);
    assertField(DC_KEY_CITATION_EPAGE, Cardinality.Single,
        DC_FIELD_CITATION_EPAGE);
    assertField(DC_KEY_CITATION_ISSUE, Cardinality.Single,
        DC_FIELD_CITATION_ISSUE);
    assertField(DC_KEY_CITATION_SPAGE, Cardinality.Single,
        DC_FIELD_CITATION_SPAGE);
    assertField(DC_KEY_CITATION_VOLUME, Cardinality.Single,
        DC_FIELD_CITATION_VOLUME);
    assertField(DC_KEY_CONTRIBUTOR, Cardinality.Multi, DC_FIELD_CONTRIBUTOR);
    assertField(DC_KEY_DATE, Cardinality.Single, DC_FIELD_DATE);
    assertField(DC_KEY_COVERAGE, Cardinality.Single, DC_FIELD_COVERAGE);
    assertField(DC_KEY_DESCRIPTION, Cardinality.Single, DC_FIELD_DESCRIPTION);
    assertField(DC_KEY_FORMAT, Cardinality.Single, DC_FIELD_FORMAT);
    assertField(DC_KEY_IDENTIFIER, Cardinality.Multi, DC_FIELD_IDENTIFIER);
    assertField(DC_KEY_IDENTIFIER_ISSN, Cardinality.Single,
        DC_FIELD_IDENTIFIER_ISSN);
    assertField(DC_KEY_IDENTIFIER_EISSN, Cardinality.Single,
        DC_FIELD_IDENTIFIER_EISSN);
    assertField(DC_KEY_IDENTIFIER_ISSNL, Cardinality.Single,
        DC_FIELD_IDENTIFIER_ISSNL);
    assertField(DC_KEY_IDENTIFIER_ISBN, Cardinality.Single,
        DC_FIELD_IDENTIFIER_ISBN);
    assertField(DC_KEY_IDENTIFIER_EISBN, Cardinality.Single,
        DC_FIELD_IDENTIFIER_EISBN);
    assertField(DC_KEY_ISSUED, Cardinality.Single, DC_FIELD_ISSUED);
    assertField(DC_KEY_LANGUAGE, Cardinality.Single, DC_FIELD_LANGUAGE);
    assertField(DC_KEY_PUBLISHER, Cardinality.Single, DC_FIELD_PUBLISHER);
    assertField(DC_KEY_RELATION, Cardinality.Multi, DC_FIELD_RELATION);
    assertField(DC_KEY_RELATION_ISPARTOF, Cardinality.Single,
        DC_FIELD_RELATION_ISPARTOF);
    assertField(DC_KEY_RIGHTS, Cardinality.Single, DC_FIELD_RIGHTS);
    assertField(DC_KEY_RIGHTS, Cardinality.Single, DC_FIELD_RIGHTS);
    assertField(DC_KEY_SUBJECT, Cardinality.Single, DC_FIELD_SUBJECT);
    assertField(DC_KEY_TITLE, Cardinality.Single, DC_FIELD_TITLE);
  }

  public void testDefault() throws MetadataException.ValidationException {
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
    }
  };

  public void testDoi() throws MetadataException.ValidationException {
    MetadataField f1 = FIELD_DOI;
    assertEquals("10.1234/56", f1.validate(am, "10.1234/56"));
    assertEquals("10.1234/56", f1.validate(am, "doi:10.1234/56"));
    assertEquals("10.1234/56", f1.validate(am, "DOI:10.1234/56"));
    assertEquals("10.1234/56", f1.validate(am, "DOI.ORG:10.1234/56"));
    assertEquals("10.1234/56", f1.validate(am, "https://dx.doi.org/10.1234/56"));
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

  public void testIsbn() throws MetadataException.ValidationException {
    MetadataField f1 = FIELD_ISBN;
    assertEquals("978-1-58562-317-4", f1.validate(am, "978-1-58562-317-4"));
    assertEquals("978-1-58562-317-4", f1.validate(am, "isbn:978-1-58562-317-4"));
    assertEquals("978-1-58562-317-4", f1.validate(am, "ISBN:978-1-58562-317-4"));
    try {
      f1.validate(am, "not.a.isbn.1234");
      fail("Should throw ValidationException");
    } catch (MetadataException.ValidationException e) {
    }
    try {
      f1.validate(am, "isbn:978-1-58562-317-3"); // checksum error
    } catch (MetadataException.ValidationException e) {
      fail("Should not throw ValidationException");
    }

    MetadataField f2 = FIELD_EISBN;
    assertEquals("978-1-58562-317-4", f2.validate(am, "978-1-58562-317-4"));
    assertEquals("978-1-58562-317-4", f2.validate(am, "eisbn:978-1-58562-317-4"));
    assertEquals("978-1-58562-317-4", f2.validate(am, "EISBN:978-1-58562-317-4"));
    try {
      f2.validate(am, "not.a.isbn.1234");
      fail("Should throw ValidationException");
    } catch (MetadataException.ValidationException e) {
    }
    try {
      f2.validate(am, "eisbn:978-1-58562-317-3"); // checksum error
    } catch (MetadataException.ValidationException e) {
      fail("Should not throw ValidationException");
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
      f1.validate(am, "issn:1234-567X"); // checksum error
    } catch (MetadataException.ValidationException e) {
      fail("Should not throw ValidationException");
    }

    MetadataField f2 = FIELD_EISSN;
    assertEquals("1234-5679", f2.validate(am, "1234-5679"));
    assertEquals("1234-5679", f2.validate(am, "eissn:1234-5679"));
    assertEquals("1234-5679", f2.validate(am, "EISSN:1234-5679"));
    try {
      f2.validate(am, "not.a.eissn.1234");
      fail("Should throw ValidationException");
    } catch (MetadataException.ValidationException e) {
    }
    try {
      f2.validate(am, "eissn:1234-567X"); // checksum error
    } catch (MetadataException.ValidationException e) {
      fail("Should not throw ValidationException");
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
      new MetadataField("bar", Cardinality.Single, MetadataField.splitAt(";"));
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
    assertEquals(ListUtil.list("one", "two"), spl.split(am, null, "(one;two)"));
    assertEquals(ListUtil.list("one", "two"),
        spl.split(am, null, "  (one ; ; two)  "));
  }

  public void testFindField() {
    assertSame(FIELD_VOLUME, MetadataField.findField(KEY_VOLUME));
    assertNull(MetadataField.findField("nosuchfield"));
  }

  // public void testValidate() {
  // MetadataField field = new MetadataField(
  // assertSame(FIELD_VOLUME, MetadataField.findField(KEY_VOLUME));
  // assertNull(MetadataField.findField("nosuchfield"));
  // }

  public void testPutMulti() {
    assertSame(FIELD_VOLUME, MetadataField.findField(KEY_VOLUME));
    assertNull(MetadataField.findField("nosuchfield"));
  }
  
  
  public void testExtractor()throws MetadataException.ValidationException  {
   
    MetadataField f1 = FIELD_START_PAGE;
    assertEquals("pp. 23",f1.validate(am, "pp. 23"));
    assertEquals("p23",f1.validate(am, "p23"));
    assertEquals("pp. 23-45",f1.validate(am, "pp. 23-45"));

    //String testpagepattern2 = "[pP\\. ]*([^-]+)(?:-(.+)$h)?";
  
     String testpagepattern1 = "pp. (23)-(45)";
     String testpagepattern2 = "p(23)";
     String testpagepattern3 = "(23)-(45)";
     String testpagepattern4 = "P(23)";
     String testpagepattern5 = "([^-]+)(-(.+))?";
     
     String testhtmlfield = 
         "A review of the applications of the hydrofiber dressing with silver (Aquacel Ag<sup>&reg;</sup>) in wound care";
     String testhtmltextfield =
         "A review of the applications of the hydrofiber dressing with silver (Aquacel Ag\u00ae) in wound care";
    
    ArticleMetadata articleMetadata = new ArticleMetadata();
    MetadataField testvalf0 = new MetadataField(MetadataField.FIELD_START_PAGE,
        MetadataField.groupExtractor(testpagepattern1,1));
    MetadataField testvalf1 = new MetadataField(MetadataField.FIELD_START_PAGE,
        MetadataField.groupExtractor(testpagepattern2,1));
    MetadataField testvalf2 = new MetadataField(MetadataField.FIELD_START_PAGE,
        MetadataField.groupExtractor(testpagepattern3,1));
    MetadataField testvalf3 = new MetadataField(MetadataField.FIELD_START_PAGE,
        MetadataField.groupExtractor(testpagepattern4,1));
    MetadataField testvalf4 = new MetadataField(MetadataField.FIELD_END_PAGE,
        MetadataField.groupExtractor(testpagepattern1,2));
    MetadataField testvalf5 = new MetadataField(MetadataField.FIELD_START_PAGE,
        MetadataField.groupExtractor(testpagepattern5,1));
    MetadataField testvalf6 = new MetadataField(MetadataField.FIELD_END_PAGE,
        MetadataField.groupExtractor(testpagepattern5,3));
    MetadataField testvalf7 = new MetadataField(MetadataField.FIELD_ARTICLE_TITLE,
        MetadataField.htmlTextExtractor());
   
    //test for start page pattern 
    assertEquals("23", testvalf1.extract(articleMetadata,"p23"));
    assertEquals("23", testvalf0.extract(articleMetadata, "pp. 23-45"));
    assertEquals("23", testvalf2.extract(articleMetadata, "23-45"));
    assertEquals("23", testvalf3.extract(articleMetadata, "P23"));
    assertEquals("23", testvalf5.extract(articleMetadata, "23-45"));
    assertEquals("23", testvalf5.extract(articleMetadata, "23"));
    
   //test for end page pattern
    assertEquals("45", testvalf4.extract(articleMetadata, "pp. 23-45"));
    assertEquals("45", testvalf6.extract(articleMetadata, "23-45"));
    assertEquals(null, testvalf6.extract(articleMetadata, "23"));
    
    // test extraction of text from html
    assertEquals(testhtmltextfield, 
                 testvalf7.extract(articleMetadata, testhtmlfield));
    // make sure it works with no html tags or character entities
    assertEquals(testhtmltextfield, 
                 testvalf7.extract(articleMetadata, testhtmltextfield));
    assertEquals("", 
                 testvalf7.extract(articleMetadata, ""));

  }
  public void testAuthor()throws MetadataException.ValidationException {
    MetadataField f1 = FIELD_AUTHOR;
    try {
      f1.validate(am, ", ");
      fail("Should throw ValidationException");
    } catch (MetadataException.ValidationException e) {
    }
    try {
      f1.validate(am, ", , : /");
      fail("Should throw ValidationException");
    } catch (MetadataException.ValidationException e) {
    }
    try {
      f1.validate(am, ",");
      fail("Should throw ValidationException");
    } catch (MetadataException.ValidationException e) {
    }
 
    assertEquals("Herron, David;Haglund, Lotta", f1.validate(am, "Herron, David;Haglund, Lotta"));
      
    }
   
}
