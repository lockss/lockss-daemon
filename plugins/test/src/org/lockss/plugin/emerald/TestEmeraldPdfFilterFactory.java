/*
 * $Id: TestEmeraldPdfFilterFactory.java,v 1.4 2014-11-25 02:11:33 thib_gc Exp $
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

package org.lockss.plugin.emerald;

import java.util.Arrays;

import org.lockss.pdf.*;
import org.lockss.plugin.emerald.EmeraldPdfFilterFactory.FrontPageWorker;
import org.lockss.test.LockssTestCase;

public class TestEmeraldPdfFilterFactory extends LockssTestCase {

  protected PdfTokenFactory tf;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    this.tf = new MockPdfTokenFactory();
  }

  public void testFrontPageWorker() throws Exception {
    FrontPageWorker worker = new FrontPageWorker();
    worker.process(Arrays.asList(tf.makeOperator(PdfOpcodes.SAVE_GRAPHICS_STATE),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeName("Name1"),
                                 tf.makeOperator(PdfOpcodes.INVOKE_XOBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.RESTORE_GRAPHICS_STATE),
                                 tf.makeOperator(PdfOpcodes.SAVE_GRAPHICS_STATE),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeName("Name2"),
                                 tf.makeOperator(PdfOpcodes.INVOKE_XOBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.RESTORE_GRAPHICS_STATE),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.BEGIN_TEXT_OBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("Downloaded on: <something>"),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.END_TEXT_OBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.BEGIN_TEXT_OBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("Access to this document was granted through an Emerald subscription provided by <something>"),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.END_TEXT_OBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT)),
                   tf);
    assertTrue(worker.result);
    worker.process(Arrays.asList(tf.makeOperator(PdfOpcodes.SAVE_GRAPHICS_STATE),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeName("Name1"),
                                 tf.makeOperator(PdfOpcodes.INVOKE_XOBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.RESTORE_GRAPHICS_STATE),
                                 tf.makeOperator(PdfOpcodes.SAVE_GRAPHICS_STATE),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeName("Name2"),
                                 tf.makeOperator(PdfOpcodes.INVOKE_XOBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.RESTORE_GRAPHICS_STATE),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.BEGIN_TEXT_OBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("Downloaded on: <something>"),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.END_TEXT_OBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.BEGIN_TEXT_OBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("Not the right string."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.END_TEXT_OBJECT)),
                   tf);
    assertFalse(worker.result);
    worker.process(Arrays.asList(tf.makeOperator(PdfOpcodes.SAVE_GRAPHICS_STATE),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeName("Name1"),
                                 tf.makeOperator(PdfOpcodes.INVOKE_XOBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.RESTORE_GRAPHICS_STATE),
                                 tf.makeOperator(PdfOpcodes.SAVE_GRAPHICS_STATE),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeName("Name2"),
                                 tf.makeOperator(PdfOpcodes.INVOKE_XOBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.RESTORE_GRAPHICS_STATE),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.BEGIN_TEXT_OBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("Not the right string."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.END_TEXT_OBJECT)),
                   tf);
    assertFalse(worker.result);
    worker.process(Arrays.asList(tf.makeOperator(PdfOpcodes.SAVE_GRAPHICS_STATE),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeName("Name1"),
                                 tf.makeOperator(PdfOpcodes.INVOKE_XOBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.RESTORE_GRAPHICS_STATE),
                                 tf.makeOperator(PdfOpcodes.SAVE_GRAPHICS_STATE),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.RESTORE_GRAPHICS_STATE)),
                   tf);
    assertFalse(worker.result);
    worker.process(Arrays.asList(tf.makeOperator(PdfOpcodes.SAVE_GRAPHICS_STATE),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.RESTORE_GRAPHICS_STATE)),
                   tf);
    assertFalse(worker.result);
  }
  
}
