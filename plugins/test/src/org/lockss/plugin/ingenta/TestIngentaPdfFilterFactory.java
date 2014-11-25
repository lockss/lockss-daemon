/*
 * $Id: TestIngentaPdfFilterFactory.java,v 1.3 2014-11-25 22:50:26 thib_gc Exp $
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

package org.lockss.plugin.ingenta;

import java.util.Arrays;

import org.lockss.pdf.*;
import org.lockss.plugin.ingenta.IngentaPdfFilterFactory.ManeyPublishingPdfFilterFactory.ManeyPublishingWorker;
import org.lockss.plugin.ingenta.IngentaPdfFilterFactory.WhiteHorsePressPdfFilterFactory.WhiteHorsePressWorker;
import org.lockss.test.LockssTestCase;

public class TestIngentaPdfFilterFactory extends LockssTestCase {

  protected PdfTokenFactory tf;

  @Override
  protected void setUp() throws Exception {
    this.tf = new MockPdfTokenFactory();
  }
  
  public void testManeyPublishingWorker() throws Exception {
    ManeyPublishingWorker worker = new ManeyPublishingWorker();
    worker.process(Arrays.asList(/* 00 */ tf.makeString("This is irrelevant."),
                                 /* 01 */ tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 /* 02 */ tf.makeOperator(PdfOpcodes.BEGIN_TEXT_OBJECT),
                                 /* 03 */ tf.makeString("This is irrelevant."),
                                 /* 04 */ tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 /* 05 */ tf.makeString("Published by Maney Publishing."),
                                 /* 06 */ tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 /* 07 */ tf.makeString("This is irrelevant."),
                                 /* 08 */ tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 /* 09 */ tf.makeOperator(PdfOpcodes.END_TEXT_OBJECT),
                                 /* 10 */ tf.makeString("This is irrelevant."),
                                 /* 11 */ tf.makeOperator(PdfOpcodes.SHOW_TEXT)),
                   tf);
    assertTrue(worker.result);
    assertEquals(2, worker.beginIndex); // inclusive
    assertEquals(9, worker.endIndex); // inclusive
    worker.process(Arrays.asList(tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.BEGIN_TEXT_OBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("Not the right string."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.END_TEXT_OBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT)),
                   tf);
    assertFalse(worker.result);
    worker.process(Arrays.asList(tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.BEGIN_TEXT_OBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("Published by Maney Publishing."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.END_TEXT_OBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.BEGIN_TEXT_OBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.END_TEXT_OBJECT)),
                   tf);
    assertFalse(worker.result);
  }
  
  public void testWhiteHorsePressWorker() throws Exception {
    WhiteHorsePressWorker worker = new WhiteHorsePressWorker();
    worker.process(Arrays.asList(/* 00 */ tf.makeString("This is irrelevant."),
                                 /* 01 */ tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 /* 02 */ tf.makeOperator(PdfOpcodes.BEGIN_TEXT_OBJECT),
                                 /* 03 */ tf.makeString("This is irrelevant."),
                                 /* 04 */ tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 /* 05 */ tf.makeString("<some IP address> = IP address"),
                                 /* 06 */ tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 /* 07 */ tf.makeString("This is irrelevant."),
                                 /* 08 */ tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 /* 09 */ tf.makeString("<some timestamp> = Date & Time"),
                                 /* 10 */ tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 /* 11 */ tf.makeString("This is irrelevant."),
                                 /* 12 */ tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 /* 13 */ tf.makeOperator(PdfOpcodes.END_TEXT_OBJECT),
                                 /* 14 */ tf.makeString("This is irrelevant."),
                                 /* 15 */ tf.makeOperator(PdfOpcodes.SHOW_TEXT)),
                   tf);
    assertTrue(worker.result);
    assertEquals(2, worker.beginIndex); // inclusive
    assertEquals(13, worker.endIndex); // inclusive
    worker.process(Arrays.asList(tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.BEGIN_TEXT_OBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("<some timestamp> = Date & Time"),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.END_TEXT_OBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT)),
                   tf);
    assertFalse(worker.result);
    worker.process(Arrays.asList(tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.BEGIN_TEXT_OBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.END_TEXT_OBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT)),
                   tf);
    assertFalse(worker.result);
    worker.process(Arrays.asList(tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.BEGIN_TEXT_OBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("<some IP address> = IP address"),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("<some timestamp> = Date & Time"),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.END_TEXT_OBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.BEGIN_TEXT_OBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.END_TEXT_OBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT)),
                   tf);
    assertFalse(worker.result);
  }
  
//  public void testPacificAffairsWorker() throws Exception {
//    PacificAffairsWorker worker = new PacificAffairsWorker();
//    worker.process(Arrays.asList(tf.makeOperator(PdfOpcodes.SAVE_GRAPHICS_STATE),
//                                 tf.makeString("This is irrelevant."),
//                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
//                                 tf.makeOperator(PdfOpcodes.RESTORE_GRAPHICS_STATE),
//                                 tf.makeOperator(PdfOpcodes.SAVE_GRAPHICS_STATE),
//                                 tf.makeString("This is irrelevant."),
//                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
//                                 tf.makeString("   \t\t\t   Delivered by <something something> to: <something something>   \t\t\t   "),
//                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
//                                 tf.makeString("This is irrelevant."),
//                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
//                                 tf.makeOperator(PdfOpcodes.RESTORE_GRAPHICS_STATE),
//                                 tf.makeOperator(PdfOpcodes.SAVE_GRAPHICS_STATE),
//                                 tf.makeString("This is irrelevant."),
//                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
//                                 tf.makeOperator(PdfOpcodes.RESTORE_GRAPHICS_STATE)),
//                   tf);
//    assertTrue(worker.result);
//  }
  
}
