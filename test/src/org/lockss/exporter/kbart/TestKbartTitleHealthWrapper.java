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

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

import org.lockss.config.TdbTestUtil;
import org.lockss.exporter.kbart.KbartTitle.Field;
import org.lockss.test.LockssTestCase;
import org.lockss.util.StringUtil;

public class TestKbartTitleHealthWrapper extends LockssTestCase {

  static final double HEALTH = 0.5;
  
  static final HashMap<Field, String> props = new HashMap<Field, String>() {{
    put(Field.TITLE_ID, TdbTestUtil.DEFAULT_TITLE_ID);
    put(Field.PRINT_IDENTIFIER, TdbTestUtil.DEFAULT_ISSN_1);
    put(Field.ONLINE_IDENTIFIER, TdbTestUtil.DEFAULT_EISSN_1);
  }};
  
  final KbartTitle title = TestKbartTitle.createKbartTitle(props);
  
  final KbartTitleHealthWrapper wrapper = new KbartTitleHealthWrapper(title, HEALTH);

  
  public final void testFieldValues() {
    assertEquals(title.fieldValues().size()+1, wrapper.fieldValues().size());
  }

  public final void testFieldValuesListOfField() {
    // Test property fields
    assertEquals(
	title.fieldValues(new ArrayList<Field>(props.keySet())).size()+1, 
	nonEmptyValues(wrapper.fieldValues()).size()
    );
    // Test each field individually 
    for (final Field f : Field.getFieldSet()) {
      List<Field> lf = new ArrayList<Field>() {{add(f);}};
      assertEquals(
	  title.fieldValues(lf).size()+1, 
	  wrapper.fieldValues(lf).size()
      );
    }
  }

  public final void testKbartTitleHealthWrapper() {
    //fail("Not yet implemented");
  }

  public final void testGetHealth() {
    assertEquals(HEALTH, wrapper.getHealth());
  }

  public final void testEqualsObject() {
    // A pair of KBTs should be equal if all their fields have the same values
    assertTrue(wrapper.equals(title));
  }
 
  /**
   * Remove empty values from a list. The fieldValues() method returns an entry 
   * for every Field, even if the list of fields on display is a subset.
   * 
   * @param l a list 
   * @return a list of the non-empty values of the source list
   */
  private List<String> nonEmptyValues(List<String> l) {
    List<String> res = new ArrayList();
    for (String s : l) {
      if (!StringUtil.isNullString(s)) res.add(s);
    }
    return res;
  }
  
}
