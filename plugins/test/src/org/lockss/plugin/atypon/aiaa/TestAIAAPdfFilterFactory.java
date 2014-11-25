/*
 * $Id: TestAIAAPdfFilterFactory.java,v 1.2 2014-11-25 02:11:33 thib_gc Exp $
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

package org.lockss.plugin.atypon.aiaa;

import java.util.*;

import org.lockss.pdf.*;
import org.lockss.plugin.atypon.aiaa.AIAAPdfFilterFactory.CitedByWorker;
import org.lockss.test.LockssTestCase;

public class TestAIAAPdfFilterFactory extends LockssTestCase {

  protected PdfTokenFactory tf;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    this.tf = new MockPdfTokenFactory();
  }

  public void testCitedByWorker() throws Exception {
    CitedByWorker worker = new CitedByWorker();
    worker.process(Arrays.asList(tf.makeArray(Arrays.asList(tf.makeString("This article "),
                                                            tf.makeFloat(1.0f),
                                                            tf.makeString("has been "),
                                                            tf.makeFloat(2.0f),
                                                            tf.makeString("cited by:"))),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT_GLYPH_POSITIONING),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT)),
                   tf);
    assertTrue(worker.result);
    worker.process(Arrays.asList(tf.makeArray(Arrays.asList(tf.makeString("This is"),
                                                            tf.makeFloat(1.0f),
                                                            tf.makeString("not the "),
                                                            tf.makeFloat(2.0f),
                                                            tf.makeString("right string."))),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT_GLYPH_POSITIONING),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT)),
                   tf);
    assertFalse(worker.result);
    worker.process(Arrays.asList(tf.makeString("This article has been cited by:"),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT)),
                   tf);
    assertFalse(worker.result);
  }

}
