package org.lockss.exporter.kbart;

import java.util.Iterator;
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
  private final Iterator<KbartTitle> titleIt;
  
  /**
   * Constructor additionally takes a reference to the exporter test. Kind of an unpleasant callback mechanism.
   * 
   * @param titles the titles which are to be exported
   */
  public KbartDummyExporter(List<KbartTitle> titles, OutputFormat format, TestKbartExporter exporterTest) {
    super(titles, format);
    this.exporterTest = exporterTest;
    this.titleIt = titles.iterator();
  }

  @Override
  protected void emitRecord(List<String> vals) {
    // Check we have a properly constructed title
    //exporterTest.checkTitle(title);
    
    // Check we have the right vals
    exporterTest.checkTitle(vals, titleIt.next());
  }

}
