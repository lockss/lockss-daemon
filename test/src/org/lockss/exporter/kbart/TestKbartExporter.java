package org.lockss.exporter.kbart;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.commons.io.output.NullOutputStream;
import org.lockss.config.TdbTestUtil;
import org.lockss.exporter.kbart.KbartExportFilter.PredefinedFieldOrdering;
import org.lockss.exporter.kbart.KbartExportFilter.CustomFieldOrdering;
import org.lockss.exporter.kbart.KbartExporter.OutputFormat;
import org.lockss.exporter.kbart.KbartTitle.Field;
import org.lockss.test.LockssTestCase;
import org.lockss.util.StringUtil;

public class TestKbartExporter extends LockssTestCase {

  Field EMPTY_FIELD = Field.COVERAGE_DEPTH;
  List<KbartTitle> titles;
  KbartExporter kb;
  KbartExporter basicKb;
  KbartExportFilter filter;
  boolean omitEmptyFields = false;
  
  protected void setUp() throws Exception {
    super.setUp();
    this.titles = new Vector<KbartTitle>() {{
      // Add a basic title
      add(TestKbartTitle.createKbartTitle(new HashMap<Field, String>() {{
	put(Field.TITLE_ID, TdbTestUtil.DEFAULT_TITLE_ID);
	put(Field.PRINT_IDENTIFIER, TdbTestUtil.DEFAULT_ISSN_1);
	put(Field.ONLINE_IDENTIFIER, TdbTestUtil.DEFAULT_EISSN_1);
      }}));
    }};

    this.basicKb = new KbartDummyExporter(titles, KbartExporter.OutputFormat.KBART_HTML, this);
    basicKb.setFilter( KbartExportFilter.identityFilter(titles) );
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * This does not actually make any assertions, but involves the KbartDummyExporter,
   * which tests the records it receives.
   */
  public final void testExport() {
    // Test basic export
    this.kb = new KbartDummyExporter(titles, KbartExporter.OutputFormat.KBART_HTML, this);
    this.omitEmptyFields = false;
    this.filter = KbartExportFilter.identityFilter(titles);
    kb.setFilter(filter);
    kb.setCompress(false);
    kb.export(new NullOutputStream());
    assertEquals("Number of titles does not match export count.", titles.size(), kb.exportCount);
    
    // Test predefined custom export
    this.kb = new KbartDummyExporter(titles, KbartExporter.OutputFormat.KBART_HTML, this);
    this.omitEmptyFields = false;
    this.filter = new KbartExportFilter(titles, PredefinedFieldOrdering.ISSN_ONLY, omitEmptyFields);
    kb.setFilter(filter);
    kb.export(new NullOutputStream());
    assertEquals("Number of titles does not match export count.", titles.size(), kb.exportCount);

    // Test custom export
    List<Field> ordering = new ArrayList<Field>() {{
      add(EMPTY_FIELD);
      add(Field.FIRST_AUTHOR);
      add(Field.ONLINE_IDENTIFIER);
      add(Field.TITLE_ID);
    }};
    this.kb = new KbartDummyExporter(titles, KbartExporter.OutputFormat.KBART_HTML, this);
    this.omitEmptyFields = true;
    this.filter = new KbartExportFilter(titles, new CustomFieldOrdering(ordering), omitEmptyFields);
    kb.setFilter(filter);
    kb.export(new NullOutputStream());
    assertEquals("Number of titles does not match export count.", titles.size(), kb.exportCount);
}

  public final void testGetFilename() {
    assertNotNull(basicKb.getFilename());
  }

  public final void testGetHostName() {
    assertNotNull(basicKb.getHostName());
    assertFalse(basicKb.getHostName()=="");
  }

  /**
   * A callback for the exporter instance. The testing is done here. We
   * check that the title is valid - not null, and with some sensible 
   * values for essential fields title id, issn, eissn. 
   * Then we check that none of the other fields are null.
   * 
   * @param title
   */
  protected void checkTitle(KbartTitle title) {
    assertNotNull(title);
    // Check it has essential fields
    assertFalse(title.getField(Field.TITLE_ID).equals(""));
    assertNotNull(title.getField(Field.PRINT_IDENTIFIER));
    assertFalse(title.getField(Field.PRINT_IDENTIFIER).equals(""));
    assertNotNull(title.getField(Field.ONLINE_IDENTIFIER));
    assertFalse(title.getField(Field.ONLINE_IDENTIFIER).equals(""));
    // No field should be null, though they may be empty
    for (Field f : KbartTitle.Field.values()) {
      assertNotNull(title.getField(f));
    }
  }

  /**
   * A callback for the exporter instance. The testing is done here. We
   * check that the list of values is valid - not null, and with some sensible 
   * values for essential fields title id, issn, eissn. 
   * Then we check that none of the other fields are null.
   * 
   * @param vals list of values from the title for the exporter's fields
   */
  protected void checkTitle(List<String> vals, KbartTitle title) {
    assertNotNull(vals);
    assertNotNull(title);

    // No field should be empty if omitEmptyFields is on
    if (omitEmptyFields) {
      for (String s : vals) {
	assertFalse(StringUtil.isNullString(s));
      }
    }

    // Should have same num of values as the ordering without the empties, if omitEmptyFields is on
    EnumSet<Field> flds = EnumSet.copyOf(filter.getFieldOrdering().getFields());
    flds.removeAll(filter.getEmptyFields());
    assertEquals(flds.size(), vals.size());
    
    // Check the field values in the order specified in the filter
    int i = 0;
    for (Field f : filter.getFieldOrdering().getOrdering()) {
      String s = title.getField(f);
      if (omitEmptyFields && StringUtil.isNullString(s)) continue;
      assertEquals(vals.get(i++), s);
    }
  }

}
