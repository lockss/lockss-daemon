/*
 * $Id: TestKbartExportFilterCustomFieldOrdering.java,v 1.2 2012-10-16 11:55:26 easyonthemayo Exp $
 */

/*

Copyright (c) 2011 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.EnumSet;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.lockss.exporter.kbart.KbartExportFilter.CustomFieldOrdering;
import org.lockss.exporter.kbart.KbartTitle.Field;

import junit.framework.TestCase;

public class TestKbartExportFilterCustomFieldOrdering extends TestCase {

  List<Field> someFields = Arrays.asList(
	Field.COVERAGE_NOTES, Field.PUBLISHER_NAME, Field.ONLINE_IDENTIFIER
  );
  String someFieldsStr = StringUtils.join(
      someFields, CustomFieldOrdering.CUSTOM_ORDERING_FIELD_SEPARATOR
  ).toLowerCase();
  
  CustomFieldOrdering cfo, cfoStr;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    cfo = new CustomFieldOrdering(someFields);
    cfoStr = new CustomFieldOrdering(someFieldsStr);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  
  public final void testCustomFieldOrderingListOfField() {
    // Check the fields by converting back
    assertEquals(someFields, cfo.getOrdering());
  }

  public final void testCustomFieldOrderingString() {
    // Check the fields by converting back
    List<Field> fs = cfoStr.getOrdering();
    assertEquals(someFields, new ArrayList<Field>(fs));
    assertEquals(someFieldsStr, 
	StringUtils.join(fs, CustomFieldOrdering.CUSTOM_ORDERING_FIELD_SEPARATOR).toLowerCase()
    );
  }

  public final void testGetFields() {
    // Check that the orderings created in different ways contain 
    // the same fields in the same order.
    assertEquals(cfo.getFields(), cfoStr.getFields());
  }

  public final void testGetOrdering() {
    // Check that the orderings created in different ways contain 
    // the same fields in the same order.
    assertEquals(cfo.getOrdering(), cfoStr.getOrdering());
  }

  public final void testGetOrderedLabels() {
    // Check that the orderings created in different ways contain 
    // the same fields in the same order.
    assertEquals(cfo.getOrderedLabels(), cfoStr.getOrderedLabels());    
  }

  public final void testToString() {
    // Check that the orderings created in different ways resolve to the same strings
    assertEquals(cfo.toString(), cfoStr.toString());
  }

}
