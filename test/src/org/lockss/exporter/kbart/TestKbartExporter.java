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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.commons.io.output.NullOutputStream;
import org.lockss.config.TdbTestUtil;
import org.lockss.exporter.kbart.KbartTitle.Field;
import org.lockss.test.LockssTestCase;
import org.lockss.util.StringUtil;

/**
 * Tests for KbartExporter
 * 
 */
public class TestKbartExporter extends LockssTestCase {

  Field EMPTY_FIELD = Field.COVERAGE_DEPTH;
  List<KbartTitle> titles;
  KbartExporter kb;
  KbartExporter basicKb;
  KbartExportFilter filter;
  boolean omitEmptyFields = false;
  boolean omitHeader = false;
  boolean excludeNoIdTitles = false;
  boolean showHealthRatings = false;
  
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

    this.basicKb = new KbartDummyExporter(titles, KbartExporter.OutputFormat.HTML, this);
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
    this.kb = new KbartDummyExporter(titles, KbartExporter.OutputFormat.HTML, this);
    this.omitEmptyFields = false;
    this.showHealthRatings = false;
    this.filter = KbartExportFilter.identityFilter(titles);
    kb.setFilter(filter);
    kb.setCompress(false);
    kb.export(new NullOutputStream());
    assertEquals("Number of titles does not match export count.", 
	titles.size(), kb.exportCount);
    
    // Test predefined custom export
    this.kb = new KbartDummyExporter(titles, KbartExporter.OutputFormat.HTML, this);
    this.omitEmptyFields = false;
    this.showHealthRatings = false;
    this.filter = new KbartExportFilter(titles, 
	KbartExportFilter.PredefinedColumnOrdering.ISSN_ONLY,
        omitEmptyFields,
        omitHeader,
        excludeNoIdTitles
    );
    kb.setFilter(filter);
    kb.export(new NullOutputStream());
    assertEquals("Number of titles does not match export count.", 
	titles.size(), kb.exportCount);

    // Test custom export
    List<Field> ordering = new ArrayList<Field>() {{
      add(EMPTY_FIELD);
      add(Field.FIRST_AUTHOR);
      add(Field.ONLINE_IDENTIFIER);
      add(Field.TITLE_ID);
    }};
    this.kb = new KbartDummyExporter(titles, KbartExporter.OutputFormat.HTML, this);
    this.omitEmptyFields = true;
    this.showHealthRatings = false;
    this.filter = new KbartExportFilter(titles, 
        KbartExportFilter.CustomColumnOrdering.create(ordering),
        omitEmptyFields,
        omitHeader,
        excludeNoIdTitles
    );
    kb.setFilter(filter);
    kb.export(new NullOutputStream());
    assertEquals("Number of titles does not match export count.", 
	titles.size(), kb.exportCount);
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

    // Should have same num of values as the ordering without the empties,
    // if omitEmptyFields is on
    EnumSet<Field> flds = EnumSet.copyOf(filter.getColumnOrdering().getFields());
    if (omitEmptyFields) flds.removeAll(filter.getEmptyFields());
    assertEquals(showHealthRatings ? flds.size()+1 : flds.size(), vals.size());
    
    // Check the field values in the order specified in the filter
    int i = 0;
    for (Field f : filter.getColumnOrdering().getOrderedFields()) {
      String s = title.getField(f);
      if (omitEmptyFields && StringUtil.isNullString(s)) continue;
      assertEquals(vals.get(i++), s);
    }
  }

}
