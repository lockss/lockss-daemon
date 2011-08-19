package org.lockss.exporter.kbart;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.lockss.config.TdbTestUtil;
import org.lockss.exporter.kbart.KbartTitle.Field;
import org.lockss.test.LockssTestCase;
import org.lockss.util.StringUtil;

import junit.framework.TestCase;

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
