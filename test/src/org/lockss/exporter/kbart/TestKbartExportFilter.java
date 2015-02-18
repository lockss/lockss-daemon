/*
 * $Id$
 */

/*

Copyright (c) 2010-2011 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.EnumSet;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.lockss.config.TdbTestUtil;
import org.lockss.exporter.kbart.KbartExportFilter.CustomColumnOrdering;
import org.lockss.exporter.kbart.KbartTitle.Field;
import org.lockss.test.LockssTestCase;
import org.lockss.util.CollectionUtil;
import org.lockss.exporter.kbart.KbartExportFilter.PredefinedColumnOrdering;

/**
 * Test various types of export filter. Note that the testing includes a level
 * of randomness to produce custom orderings, so there may be obscure failures
 * which are not immediately reproducible.
 * 
 * 
 * @author Neil Mayo
 *
 */
public class TestKbartExportFilter extends LockssTestCase {

  Random rand = new Random();
  KbartExportFilter identityFilter;
  KbartExportFilter filterIssnOnly;
  KbartExportFilter customFilter;
  KbartExportFilter[] testFilters;
  List<KbartTitle>  titles;
  //FieldOrdering ordering;
  List<Field> customOrder;
  boolean omitEmptyFields;
  boolean omitHeader;
  boolean excludeNoIdTitles;
  boolean showHealthRatings;
 
  // Shared ID fields
  private static String TITLE_ID = TdbTestUtil.DEFAULT_TITLE_ID;
  private static String PRINT_ID = TdbTestUtil.DEFAULT_ISSN_1;
  private static String ONLINE_ID = TdbTestUtil.DEFAULT_EISSN_1;
  // Contrasting RANGE fields
  private static String FIRST_DATE_1 = "2000";
  private static String LAST_DATE_1 = "2001";
  private static String FIRST_DATE_2 = "1900";
  private static String LAST_DATE_2 = "1901";
  
  // Note that the following test titles are created with the same set of fields.
  KbartTitle title1, title2;
  
  // Title 1
  HashMap<KbartTitle.Field, String> title1props = new HashMap<KbartTitle.Field, String>() {{
    put(Field.TITLE_ID, TITLE_ID);
    put(Field.PRINT_IDENTIFIER, PRINT_ID);
    put(Field.ONLINE_IDENTIFIER, ONLINE_ID);
    // Set the varying fields
    put(Field.DATE_FIRST_ISSUE_ONLINE, FIRST_DATE_1);
    put(Field.DATE_LAST_ISSUE_ONLINE, LAST_DATE_1);
  }};
  // Title 2 is created with the same fields, but different range values
    
  private EnumSet<Field> filledFields = EnumSet.copyOf(title1props.keySet());
  private EnumSet<Field> emptyFields = EnumSet.complementOf(filledFields);
  private EnumSet<Field> differingFields = EnumSet.of(Field.DATE_FIRST_ISSUE_ONLINE, Field.DATE_LAST_ISSUE_ONLINE);
  
  @Override
  protected void setUp() throws Exception {
    //super.setUp();
    
    // Create the titles - title2 is created with the same fields, but different range values
    this.title1 = TestKbartTitle.createKbartTitle(title1props);
    this.title2 = title1.clone()
        .setField(Field.DATE_FIRST_ISSUE_ONLINE, FIRST_DATE_2)
        .setField(Field.DATE_LAST_ISSUE_ONLINE, LAST_DATE_2);
    
    this.titles = new ArrayList<KbartTitle>() {{
      add(title1);
      add(title2);
    }};

    // Setup a random custom ordering
    this.customOrder = getRandomFieldOrder();
    // Random choice of omit empty fields
    this.omitEmptyFields = rand.nextBoolean();
    this.omitHeader = rand.nextBoolean();
    this.excludeNoIdTitles = rand.nextBoolean();
    this.showHealthRatings = false;
    log.info(String.format(
        "Randomised setup:\n ordering: %s \n omitEmptyFields: %b\n omitHeader: %b\n excludeNoIdTitles: %b\n",
        customOrder, omitEmptyFields, omitHeader, excludeNoIdTitles
    ));
    // Setup filters
    this.identityFilter = KbartExportFilter.identityFilter(titles);
    this.filterIssnOnly = new KbartExportFilter(
        titles,
        KbartExportFilter.PredefinedColumnOrdering.ISSN_ONLY,
        omitEmptyFields,
        omitHeader,
        excludeNoIdTitles
    );
    this.customFilter = new KbartExportFilter(
        titles,
        CustomColumnOrdering.create(customOrder),
        omitEmptyFields,
        omitHeader,
        excludeNoIdTitles
    );
    // Add filters to array
    this.testFilters = new KbartExportFilter[] {identityFilter, filterIssnOnly, customFilter};
  }
  
  @Override
  protected void tearDown() throws Exception {
    //super.tearDown();
  }

  public void testGetVisibleFieldOrder() {
    for (KbartExportFilter filter: testFilters) {
      // Calculate the visible fields (be careful to *copy* the field set)
      // Visible fields
      EnumSet<Field> visFields = EnumSet.copyOf(filter.getColumnOrdering().getFields());
      if (filter.isOmitEmptyFields()) {
	//visFields.retainAll(filledFields);
	visFields.removeAll(emptyFields);
      }
    
      // Calculate the ordered fields (be careful to *copy* the ordering)
      List<Field> expectedVisibleFieldOrder = new Vector<Field>(filter.getColumnOrdering().getOrderedFields());
      expectedVisibleFieldOrder.retainAll(visFields);

      // Test the order
      assertEqualContent(expectedVisibleFieldOrder,
          filter.getVisibleColumnOrdering().getOrderedFields());
    }
  }

  public void testIsOmitEmptyFields() {
    assertFalse(identityFilter.isOmitEmptyFields());
    assertEquals(omitEmptyFields, filterIssnOnly.isOmitEmptyFields());
    assertEquals(omitEmptyFields, customFilter.isOmitEmptyFields());    
  }

  public void testGetEmptyFields() {
    for (KbartExportFilter filter: testFilters) {
      if (filter.isOmitEmptyFields()) {
	assertTrue(filter.getEmptyFields().containsAll(emptyFields));
      } else {
	assertTrue(filter.getEmptyFields().isEmpty());
      }
    }
  }

  public void testOmittedFieldsManually() {
    for (KbartExportFilter filter: testFilters) {
      boolean notAllFields = filter.getColumnOrdering().getFields().size() != Field.getFieldSet().size();
      assertEquals(notAllFields, filter.omittedFieldsManually());
    }
    // More specific tests
    assertFalse(identityFilter.omittedFieldsManually());
    assertTrue(filterIssnOnly.omittedFieldsManually());
    assertEquals(customOrder.size() < Field.values().length, customFilter.omittedFieldsManually());
  }

  public void testOmittedEmptyFields() {
    for (KbartExportFilter filter: testFilters) {
      // If the flag is not set, nothing should be omitted
      if (!filter.isOmitEmptyFields()) {
	assertFalse(filter.omittedEmptyFields());
	continue;
      }
      // If any of the filter's ordering fields are in the emptyFields set, it should omit them.
      EnumSet<Field> flds = EnumSet.copyOf(filter.getColumnOrdering().getFields());
      flds.retainAll(emptyFields);
      assertEquals(flds.size() > 0, filter.omittedEmptyFields());
    }
  }

  /**
   * The visible field values should be the values in field ordering of the filter,
   * minus any empty fields if <code>omitEmptyFields</code> is set. 
   */
  public void testGetVisibleFieldValues() {
    for (KbartExportFilter filter: testFilters) {
      List<String> vals = filter.getVisibleFieldValues(title1);
      // Expected order - that of the filter minus omittable fields
      List<Field> order = new Vector<Field>( filter.getColumnOrdering().getOrderedFields() );
      if (filter.isOmitEmptyFields()) order.removeAll(emptyFields);
      // Check sizes
      assertEquals(order.size(), vals.size());
      // Check contents
      // Get vals using the same method as in the code under test
      List<String> expectedVals = title1.fieldValues(order);
      assertEqualContent(expectedVals, vals);
      // Check vals manually as well
      for (int i=0; i<order.size(); i++) {
	assertEquals(title1.getField(order.get(i)), vals.get(i++));
      }
    }
  }

  /**
   * 
   */
  public void testIsTitleForOutput() {
    for (KbartExportFilter filter: testFilters) {
      for (KbartTitle title : titles) {
	boolean shouldOutput = shouldOutputTitle(title, filter);
	String msg = "Title "+title+" should "+(shouldOutput?"":"not ")+"be output with the filter "+filter;
  	assertEquals(msg, shouldOutput, filter.isTitleForOutput(title));
      }
    }
  }

  /**
   * Decide if the title should be shown under this filter. The title is shown
   * if any of the following hold:
   * <ul>
   *   <li>It is the first title.</li>
   *   <li>The filter includes range fields.</li>
   *   <li>The filter includes neither range nor id fields (meaning we can't decide if titles differ).</li>
   *   <li>The filter includes a field which differs between titles.</li>
   * </ul>
   * It is not shown if the filter is set to exclude identifier-less
   * titles, includes the title_id field, and the title has no identifier.
   * 
   * @param title the title under consideration for output
   * @param filter the output filter in effect
   * @return
   */
  private boolean shouldOutputTitle(KbartTitle title, KbartExportFilter filter) {
    EnumSet<Field> fields = filter.getVisibleColumnOrdering().getFields();
    // Does this filter include a field which differs between the titles?
    boolean titlesDiffer = !CollectionUtil.isDisjoint(differingFields, fields);
    // Are range field included in the output?
    boolean rangeFieldsIncluded = !CollectionUtil.isDisjoint(Field.rangeFields, fields);
    // Are id field included in the output?
    boolean idFieldsIncluded= !CollectionUtil.isDisjoint(Field.idFields, fields);
    boolean excludedForLackingId = filter.isExcludeNoIdTitles() &&
        filter.getColumnOrdering().getFields().contains(Field.TITLE_ID) &&
        !title.hasFieldValue(Field.TITLE_ID);
    return !excludedForLackingId && (
        title==title1  ||
            titlesDiffer ||
            rangeFieldsIncluded ||
            (!rangeFieldsIncluded && !idFieldsIncluded)
    );
  }
  
  
  /**
   * Generate a list containing a random ordering of fields.
   * The number of entries in the list will be between 0
   * and the number of fields, both inclusive.
   * 
   * @return a randomly ordered list of all fields
   */
  private List<Field> getRandomFieldOrder() {
    return getRandomFieldOrder(0);
  }
  
  /**
   * Generate a list containing a random ordering of fields, of
   * at least the minimum size specified.
   * 
   * @param minSize the minimum size of the list
   * @return a randomly ordered list of at least <code>minSize</code> fields
   */
  private List<Field> getRandomFieldOrder(int minSize) {
    //int n = minSize + rand.nextInt( Field.getFieldSet().size() - minSize );
    //return CollectionUtil.randomSelection(Field.getFieldSet(), n);
    // method needs parameterizing
    List<Field> list = new ArrayList<Field>(Field.getFieldSet());
    Collections.shuffle(list);
    int n = minSize + rand.nextInt( list.size() - minSize );
    return list.subList(0, n+1);
  }

  /**
   * Compare two lists to see if they contain the same elements
   * in the same order. Note that this uses <code>equals()</code>
   * so for predictable behaviour the list entries should 
   * implement {@link java.lang.Comparable}. 
   * 
   * @param l1 a list of items
   * @param l2 a list of items of the same type
   */
  public static <T> void assertEqualContent(List<T> l1, List<T> l2) {
    if (l1.size() != l2.size()) {
      fail(String.format("Lists are of different size %s and %s\n", l1.size(), l2.size()));
    }
    for (int i=0; i<l1.size(); i++) {
      T item1 = l1.get(i);
      T item2 = l2.get(i);
      if (!item1.equals(item2)) {
	String msg = String.format("Different elements at position %d: '%s' '%s'\n", i, item1.toString(), item2.toString());
	failNotEquals( msg, item1, item2 );
      }
    }
  }  
  
}
