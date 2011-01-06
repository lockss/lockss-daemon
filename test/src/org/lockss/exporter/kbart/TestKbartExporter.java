package org.lockss.exporter.kbart;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.commons.io.output.NullOutputStream;
import org.lockss.config.TdbTestUtil;
import org.lockss.exporter.kbart.KbartExporter.OutputFormat;
import org.lockss.exporter.kbart.KbartTitle.Field;
import org.lockss.test.LockssTestCase;

public class TestKbartExporter extends LockssTestCase {

  List<KbartTitle> titles;
  OutputFormat format;
  KbartExporter kb;
  
  protected void setUp() throws Exception {
    super.setUp();
    this.titles = new Vector<KbartTitle>() {{
      // Add a basic title
      add(TestKbartTitle.createKbartTitle(new HashMap<KbartTitle.Field, String>() {{
	put(KbartTitle.Field.TITLE_ID, TdbTestUtil.DEFAULT_TITLE_ID);
	put(KbartTitle.Field.PRINT_IDENTIFIER, TdbTestUtil.DEFAULT_ISSN_1);
	put(KbartTitle.Field.ONLINE_IDENTIFIER, TdbTestUtil.DEFAULT_EISSN_1);
      }}));
    }};
    this.format = KbartExporter.OutputFormat.KBART_HTML;
    this.kb = new KbartDummyExporter(titles, format, this);
    //KbartConverter kbconv = new KbartConverter(tdb);
    //titles = kbconv.extractAllTitles();
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * This does not actually make any assertions, but involves the KbartDummyExporter,
   * which tests the records it receives.
   */
  public final void testExport() {
    //kb.setTdbTitleTotal(tdb.getTdbTitleCount());
    kb.setCompress(false);
    kb.export(new NullOutputStream());
  }

  public final void testGetFilename() {
    assertNotNull(kb.getFilename());
  }

  public final void testGetHostName() {
    assertNotNull(kb.getHostName());
    assertFalse(kb.getHostName()=="");
  }

  /**
   * A callback for the exporter instance. The testing is done here. We
   * check that the title is valid - not null, and with some sensible 
   * values for essential fields title id, issn, eissn. The rest of the 
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

}
