package org.lockss.exporter.kbart;

import java.util.EnumSet;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.lockss.config.TdbTestUtil;
import org.lockss.exporter.kbart.KbartExportFilter.CustomFieldOrdering;
import org.lockss.exporter.kbart.KbartExportFilter.PredefinedFieldOrdering;
import org.lockss.exporter.kbart.KbartTitle.Field;
import org.lockss.test.LockssTestCase;
import org.lockss.util.CollectionUtil;

/**
 * Test various types of export filter. Note that the testing includes a level of randomness 
 * to produce custom orderings, so there may be obscure failures which are not immediately reproducible.
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
    this.title2 = title1.clone();
    title2.setField(Field.DATE_FIRST_ISSUE_ONLINE, FIRST_DATE_2);
    title2.setField(Field.DATE_LAST_ISSUE_ONLINE, LAST_DATE_2);
    
    this.titles = new ArrayList<KbartTitle>() {{
      add(title1);
      add(title2);
    }};

    // Setup a random custom ordering
    this.customOrder = getRandomFieldOrder();
    // Random choice of omit empty fields
    this.omitEmptyFields = rand.nextBoolean();
    System.out.printf("Randomised setup:\n ordering: %s \n omitEmptyFields: %s\n", customOrder, omitEmptyFields);
    // Setup filters
    this.identityFilter = KbartExportFilter.identityFilter(titles);
    this.filterIssnOnly = new KbartExportFilter(titles, PredefinedFieldOrdering.ISSN_ONLY, omitEmptyFields);
    this.customFilter = new KbartExportFilter(titles, new CustomFieldOrdering(customOrder), omitEmptyFields);
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
      EnumSet<Field> visFields = EnumSet.copyOf(filter.getFieldOrdering().getFields());
      if (filter.isOmitEmptyFields()) {
	//visFields.retainAll(filledFields);
	visFields.removeAll(emptyFields);
      }
    
      // Calculate the ordered fields (be careful to *copy* the ordering)
      List<Field> expectedVisibleFieldOrder = new Vector<Field>(filter.getFieldOrdering().getOrdering());
      expectedVisibleFieldOrder.retainAll(visFields);

      // Test the order
      assertEqualContent(expectedVisibleFieldOrder, filter.getVisibleFieldOrder());
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
      boolean notAllFields = filter.getFieldOrdering().getFields().size() != Field.getFieldSet().size();
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
      EnumSet<Field> flds = EnumSet.copyOf(filter.getFieldOrdering().getFields());
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
      List<Field> order = new Vector<Field>( filter.getFieldOrdering().getOrdering() );
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
      // Does this filter include a field which differs between the titles?
      boolean titlesDiffer = !CollectionUtil.isDisjoint(differingFields, filter.getVisibleFieldOrder());
      for (KbartTitle title : titles) {
	// Should output if it is first title or they differ under this filter
	boolean shouldOutput = title==title1 || titlesDiffer;
	String msg = "Title "+title+" should "+(shouldOutput?"":"not ")+"be output with the filter "+filter;
  	assertEquals(msg, shouldOutput, filter.isTitleForOutput(title));
      }
    }
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
