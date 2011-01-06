package org.lockss.exporter.kbart;

import java.util.List;

/**
 * A test implementation of the KbartExporter. This can be placed in the full pipeline 
 * and will just check/count records and perform sanity checks without outputting anything.  
 * 
 * See ListHoldings.doExport for setup and process....
 * 
 * @author Neil Mayo
 *
 */
public class KbartDummyExporter extends KbartExporter {

  private final TestKbartExporter exporterTest;
  
  /**
   * Constructor additionally takes a reference to the exporter test. Kind of an unpleasant callback mechanism.
   * 
   * @param titles the titles which are to be exported
   */
  public KbartDummyExporter(List<KbartTitle> titles, OutputFormat format, TestKbartExporter exporterTest) {
    super(titles, format);
    this.exporterTest = exporterTest;
  }

  @Override
  protected void emitRecord(KbartTitle title) {
    // Check we have a properly constructed title
    exporterTest.checkTitle(title);
  }

}
