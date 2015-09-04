/*
 * $Id$
 */

/*

Copyright (c) 2010-2015 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.exporter.kbart;

import java.util.*;

import org.lockss.config.TdbTestUtil;
import static org.lockss.exporter.kbart.KbartTitle.Field;

import org.lockss.exporter.biblio.BibliographicUtil;
import org.lockss.test.LockssTestCase;

public class TestKbartTitle extends LockssTestCase {

  private static final String DEFAULT_TEST_STRING = "A_big_string_of_test"; 
  private static final String DEFAULT_TITLE = "KBART KSIMPSON"; 
  private static final String DEFAULT_TITLE_LESS = "JBART"; 
  private static final String DEFAULT_TITLE_GREATER = "LBART"; 
  private static final Field EMPTY_FIELD = Field.COVERAGE_DEPTH; 
  private static final String TAB = "	";
  private static final String SPACE = " ";
  
  private KbartTitle testTitle;
  
  protected void setUp() throws Exception {
    super.setUp();
    this.testTitle = createKbartTitle(new HashMap<Field, String>() {{
      put(Field.TITLE_ID, TdbTestUtil.DEFAULT_TITLE_ID);
      put(Field.PRINT_IDENTIFIER, TdbTestUtil.DEFAULT_ISSN_1);
      put(Field.ONLINE_IDENTIFIER, TdbTestUtil.DEFAULT_EISSN_1);
      put(Field.PUBLICATION_TITLE, DEFAULT_TITLE);
      put(Field.PUBLISHER_NAME, TdbTestUtil.DEFAULT_PUBLISHER);
      put(Field.TITLE_URL, TdbTestUtil.DEFAULT_URL);

      put(Field.DATE_FIRST_ISSUE_ONLINE, TdbTestUtil.RANGE_1_START);
      put(Field.DATE_LAST_ISSUE_ONLINE, TdbTestUtil.RANGE_1_END);
      put(Field.NUM_FIRST_VOL_ONLINE, TdbTestUtil.RANGE_1_START_VOL);
      put(Field.NUM_LAST_VOL_ONLINE, TdbTestUtil.RANGE_1_END_VOL);
      //put(Field.NUM_FIRST_ISSUE_ONLINE, TdbTestUtil.RANGE_1_START);
      //put(Field.NUM_LAST_ISSUE_ONLINE, TdbTestUtil.RANGE_1_END);
    }});
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    this.testTitle = null;
  }

  /**
   * Make a clone and check that it is equal but not identical. 
   */
  public final void testClone() {
    KbartTitle clone = testTitle.clone();
    // The clone should have all the same values but be a different object
    assertEquals(testTitle, clone);
    assertNotSame(testTitle, clone);
    for (Field f : Field.values()) {
      assertEquals("Clone differs on "+f.getLabel(), testTitle.getField(f), clone.getField(f)); 
    }
  }

  /**
   * Try setting various values on a field.
   */
  public final void testSetField() {
    // Set a field value (note we use an otherwise unused field)
    testTitle.setField(Field.COVERAGE_DEPTH, DEFAULT_TEST_STRING);
    assertEquals(testTitle.getField(Field.COVERAGE_DEPTH), DEFAULT_TEST_STRING);
    // Try setting a null field value
    testTitle.setField(null, "not likely");
    assertEquals(testTitle.getField(Field.COVERAGE_DEPTH), DEFAULT_TEST_STRING);
    // Try setting a field to a null value
    testTitle.setField(Field.COVERAGE_DEPTH, null);
    assertEquals(testTitle.getField(Field.COVERAGE_DEPTH), "");
  }

  /**
   * Try getting a field value, and passing null values.
   */
  public final void testGetField() {
    boolean isLast = testTitle.isLast();
    testTitle.setLast(true);
    // Set a field value (note we use an otherwise unused field)
    testTitle.setField(Field.COVERAGE_DEPTH, DEFAULT_TEST_STRING);
    String s = testTitle.getField(Field.COVERAGE_DEPTH);
    assertNotNull(s);
    assertEquals(s, DEFAULT_TEST_STRING);
    // Test that empty strings are returned where necessary
    assertNotNull(testTitle.getField(null));
    assertEquals(testTitle.getField(null), "");

    // Test that end fields return empty string if set to current year or higher
    String thisYear = ""+BibliographicUtil.getThisYear();
    String laterYear = ""+(BibliographicUtil.getThisYear()+10);
    for (Field f : KbartTitle.blankIfCoverageToPresent) {
      for (String yr : new String[]{thisYear, laterYear}) {
        testTitle.setField(f, yr);
        s = testTitle.getField(f);
        assertNotNull(s);
        assertNotEquals(s, yr);
        if (f != Field.NUM_LAST_VOL_ONLINE) {
          assertEquals(s, "");
        }
      }
    }
    testTitle.setLast(isLast);
  }


  /**
   * Try getting a field value, and passing null values.
   * Make sure that values for this year are still returned.
   */
  public final void testGetFieldValue() {
    String thisYear = ""+ BibliographicUtil.getThisYear();
    // Set each field value to this year
    for (Field f : Field.values()) {
      testTitle.setField(f, thisYear);
      String s = testTitle.getFieldValue(f);
      assertNotNull(s);
      assertEquals(s, thisYear);
      // Test that empty strings are returned where necessary
      assertNotNull(testTitle.getFieldValue(null));
      assertEquals(testTitle.getFieldValue(null), "");
    }
  }


  /**
   * Check the field values.
   */
  public final void testNormalise() {
    String s = "%s These days a browser has tabs. So %s does this sentence. %s";
    String test1 = formatTestString(s, TAB, TAB, TAB);
    String test2 = formatTestString(s, SPACE, SPACE, SPACE);
    assertEquals(KbartTitle.normalise(test1), test2);
  }
  
  private final String formatTestString(String s, String... strings) {
    return String.format(s, (Object[])strings); 
  }
  
  /**
   * Check the field values.
   */
  public final void testFieldValues() {
    // Check that all fields are represented, and in the appropriate order
    List<String> vals = new Vector<String>(testTitle.fieldValues());
    Collection<String> labs = KbartTitle.Field.getLabels();
    // The full range of KbartTitle fields:
    EnumSet<Field> fields = EnumSet.allOf(Field.class);
    // Check every field has a value
    assertEquals(fields.size(), vals.size());
    
    // Check the content of every field
    Iterator<String> i1 = vals.iterator();
    Iterator<Field> i2 = fields.iterator();
    while (i1.hasNext()) {
      String s1 = i1.next();
      Field f = i2.next();
      String s2 = testTitle.getField(f);
      assertEquals("Field "+f.getLabel()+" differs", s1, s2);      
    }
  }

  /**
   * Check the field values.
   * Return the values of the fields listed in the argument, in the same order.
   */
  public final void testFieldValuesList() {
    List<Field> fields = new ArrayList<Field>() {{
      add(EMPTY_FIELD);
      add(Field.PUBLICATION_TITLE);
      add(Field.TITLE_ID);
    }};
    List<String> expectedValues = new ArrayList<String>() {{
      add("");
      add(DEFAULT_TITLE);
      add(TdbTestUtil.DEFAULT_TITLE_ID);
    }};
    
    List<String> vals = new Vector<String>(testTitle.fieldValues(fields));
    // Check size first
    assertEquals(vals.size(), expectedValues.size());
    for (int i=0; i<expectedValues.size(); i++) {
    	assertEquals(expectedValues.get(i), vals.get(i));
    }
  }
  
  /**
   * Check whether the title has a value for the given field.
   * @return whether the field has a non-empty value
   */
  public final void testHasFieldValue() {
    assertTrue(testTitle.hasFieldValue(Field.PUBLICATION_TITLE));
    assertTrue(testTitle.hasFieldValue(Field.PRINT_IDENTIFIER));
    assertFalse(testTitle.hasFieldValue(EMPTY_FIELD));
  }
  
  /**
   * The KbartTitles are compared based on their publication titles; test titles that are equal, less and greater.
   */
  public final void testCompareTo() {
    // Create a second test title which is a clone of the first
    KbartTitle testTitle2 = testTitle.clone();
    assertTrue(testTitle.compareTo(testTitle2)==0);
    // Change title to a lesser one 
    testTitle2.setField(Field.PUBLICATION_TITLE, DEFAULT_TITLE_LESS);
    assertTrue(testTitle.compareTo(testTitle2)>0);
    // Change title to a greater one
    testTitle2.setField(Field.PUBLICATION_TITLE, DEFAULT_TITLE_GREATER);
    assertTrue(testTitle.compareTo(testTitle2)<0);
  }

  /**
   * Create a KbartTitle with fields specified by the supplied properties hash.
   *  
   * @param props as hash of properties to be added to the new title, using the constant field names as keys
   * @return
   */
  protected static KbartTitle createKbartTitle(Map<Field, String> props) {
    KbartTitle kbt = new KbartTitle();
    for (Field f : props.keySet()) {
      kbt.setField(f, props.get(f));
    }
    return kbt;
  }
  
}
