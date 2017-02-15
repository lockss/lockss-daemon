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

package org.lockss.exporter.kbart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import static org.lockss.exporter.kbart.KbartExportFilter.*;
import org.lockss.exporter.kbart.KbartTitle.Field;
import static org.lockss.exporter.kbart.KbartTitle.Field.*;

import org.lockss.test.LockssTestCase;

public class TestKbartExportFilterCustomFieldOrdering extends LockssTestCase {

  // ------------------------------------------------
  // Some fields
  List<Field> someFields = Arrays.asList(
	Field.COVERAGE_NOTES, Field.PUBLISHER_NAME, Field.ONLINE_IDENTIFIER
  );

  // ------------------------------------------------
  // Some fields represented as strings, with a quoted constant column
  String someFieldsStr = StringUtils.join(
      someFields, CustomColumnOrdering.CUSTOM_ORDERING_FIELD_SEPARATOR
  ).toLowerCase();

  // ------------------------------------------------
  // Some strings and columns, with the same contents
  List<String> someStrings = Arrays.asList(
      ONLINE_IDENTIFIER.toString(),
      TITLE_URL.toString(),
      "\"QUOTED\""
  );
  List<ReportColumn> someColumns = ReportColumn.fromStrings(someStrings);

  CustomColumnOrdering cfo, cfoOneStr, cfoStr, cfoCol;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    cfo = CustomColumnOrdering.create(someFields) ;
    cfoOneStr = new CustomColumnOrdering(someFieldsStr);
    cfoStr = CustomColumnOrdering.createUnchecked(someStrings);
    cfoCol = new CustomColumnOrdering(someColumns, ReportColumn.getFields(someColumns));
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }


  public final void testCustomFieldOrderingListOfField() {
    // Check the fields by converting back
    assertEquals(someFields, cfo.getOrderedFields());
    assertSameElements(someFields, cfo.getFields());
  }

  public final void testCustomFieldOrderingListOfStrings() {
    // XXX cfoStr: Converting fields back to the string?
    //assertIsomorphic(someStrings, cfoStr.getOrderedColumns());
    assertIsomorphic(Field.getFields(someStrings), cfoStr.getOrderedFields());
    assertIsomorphic(someColumns, ReportColumn.fromStrings(cfoStr.getOrderedDescriptors()));
    // Check that the fields present in the labels match the set of fields
    assertSameElements(Field.getFields(cfoStr.getOrderedLabels()), cfoStr.getFields());
  }

  public final void testCustomFieldOrderingListOfColumns() {
    assertIsomorphic(someColumns, cfoCol.getOrderedColumns());
    assertIsomorphic(Field.getFields(someStrings), cfoCol.getOrderedFields());
    assertIsomorphic(someColumns, ReportColumn.fromStrings(cfoCol.getOrderedDescriptors()));
    // Check that the fields present in the labels match the set of fields
    assertSameElements(Field.getFields(cfoCol.getOrderedLabels()), cfoCol.getFields());
  }

  public final void testCustomFieldOrderingString() {
    // Check the fields by converting back
    List<Field> fs = cfoOneStr.getOrderedFields();
    assertEquals(someFields, new ArrayList<Field>(fs));
  }

  public final void testGetFields() {
    // Check that the orderings created in different ways contain 
    // the same fields in the same order.
    assertEquals(cfo.getFields(), cfoOneStr.getFields());
  }

  public final void testGetOrdering() {
    // Check that the orderings created in different ways contain 
    // the same fields in the same order.
    assertEquals(cfo.getOrderedFields(), cfoOneStr.getOrderedFields());
  }

  public final void testGetOrderedLabels() {
    // Check that the orderings created in different ways contain 
    // the same fields in the same order.
    assertEquals(cfo.getOrderedLabels(), cfoOneStr.getOrderedLabels());
  }

  public final void testToString() {
    // Check that the orderings created in different ways resolve to the same strings
    assertEquals(cfo.toString(), cfoOneStr.toString());
  }

}
