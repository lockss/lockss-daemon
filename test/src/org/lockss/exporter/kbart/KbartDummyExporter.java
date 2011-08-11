/*
 * $Id: KbartDummyExporter.java,v 1.3 2011-08-11 16:52:38 easyonthemayo Exp $
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
