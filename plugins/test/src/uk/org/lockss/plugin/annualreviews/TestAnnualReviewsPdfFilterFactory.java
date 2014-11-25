/*
 * $Id: TestAnnualReviewsPdfFilterFactory.java,v 1.2 2014-11-25 02:11:33 thib_gc Exp $
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

package uk.org.lockss.plugin.annualreviews;

import java.util.Arrays;

import org.lockss.pdf.*;
import org.lockss.test.LockssTestCase;

import uk.org.lockss.plugin.annualreviews.AnnualReviewsPdfFilterFactory.DownloadedFromWorker;
import uk.org.lockss.plugin.annualreviews.AnnualReviewsPdfFilterFactory.ForPersonalUseWorker;

public class TestAnnualReviewsPdfFilterFactory extends LockssTestCase {

  protected PdfTokenFactory tf;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    this.tf = new MockPdfTokenFactory();
  }
  
  public void testDownloadedFromWorker() throws Exception {
    DownloadedFromWorker worker = new DownloadedFromWorker();
    for (String goodString : Arrays.asList("Downloaded from http://www.example.com/",
                                           "Downloaded from http://www.example.com",
                                           "Downloaded from www.example.com")) {
      worker.process(Arrays.asList(tf.makeOperator(PdfOpcodes.BEGIN_TEXT_OBJECT),
                                   tf.makeString("This is irrelevant."),
                                   tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                   tf.makeInteger(1),
                                   tf.makeInteger(2),
                                   tf.makeInteger(3),
                                   tf.makeInteger(4),
                                   tf.makeInteger(5),
                                   tf.makeInteger(6),
                                   tf.makeOperator(PdfOpcodes.SET_TEXT_MATRIX),
                                   tf.makeString("This is irrelevant."),
                                   tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                   tf.makeString(goodString),
                                   tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                   tf.makeString("This is irrelevant."),
                                   tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                   tf.makeOperator(PdfOpcodes.END_TEXT_OBJECT)),
                     tf);
      assertTrue(worker.result);
    }
    worker.process(Arrays.asList(tf.makeOperator(PdfOpcodes.BEGIN_TEXT_OBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeInteger(1),
                                 tf.makeInteger(2),
                                 tf.makeInteger(3),
                                 tf.makeInteger(4),
                                 tf.makeInteger(5),
                                 tf.makeInteger(6),
                                 tf.makeOperator(PdfOpcodes.SET_TEXT_MATRIX),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("Downloaded from www.example.com"),
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
                                 tf.makeInteger(1),
                                 tf.makeInteger(2),
                                 tf.makeInteger(3),
                                 tf.makeInteger(4),
                                 tf.makeInteger(5),
                                 tf.makeInteger(6),
                                 tf.makeOperator(PdfOpcodes.SET_TEXT_MATRIX),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("Downloaded from www.example.com"),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.END_TEXT_OBJECT)),
                   tf);
    assertFalse(worker.result);
    worker.process(Arrays.asList(tf.makeOperator(PdfOpcodes.BEGIN_TEXT_OBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeInteger(1),
                                 tf.makeInteger(2),
                                 tf.makeInteger(3),
                                 tf.makeInteger(4),
                                 tf.makeInteger(5),
                                 tf.makeInteger(6),
                                 tf.makeOperator(PdfOpcodes.SET_TEXT_MATRIX),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("Not the right string."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.END_TEXT_OBJECT)),
                   tf);
    assertFalse(worker.result);
    worker.process(Arrays.asList(tf.makeOperator(PdfOpcodes.BEGIN_TEXT_OBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("Downloaded from www.example.com"),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.END_TEXT_OBJECT)),
                   tf);
    assertFalse(worker.result);
  }

  public void testForPersonalUseWorker() throws Exception {
    ForPersonalUseWorker worker = new ForPersonalUseWorker();
    for (String goodString : Arrays.asList("by <something something> on 01/02/03. For personal use only.",
                                           "by <something something> on 99/99/99. For personal use only.",
                                           "   \t\t\t   by <something something> on 01/03/03. For personal use only.   \t\t\t")) {
      worker.process(Arrays.asList(tf.makeOperator(PdfOpcodes.BEGIN_TEXT_OBJECT),
                                   tf.makeString("This is irrelevant."),
                                   tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                   tf.makeInteger(1),
                                   tf.makeInteger(2),
                                   tf.makeInteger(3),
                                   tf.makeInteger(4),
                                   tf.makeInteger(5),
                                   tf.makeInteger(6),
                                   tf.makeOperator(PdfOpcodes.SET_TEXT_MATRIX),
                                   tf.makeString("This is irrelevant."),
                                   tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                   tf.makeString(goodString),
                                   tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                   tf.makeString("This is irrelevant."),
                                   tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                   tf.makeOperator(PdfOpcodes.END_TEXT_OBJECT)),
                     tf);
      assertTrue(worker.result);
    }
    worker.process(Arrays.asList(tf.makeOperator(PdfOpcodes.BEGIN_TEXT_OBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeInteger(1),
                                 tf.makeInteger(2),
                                 tf.makeInteger(3),
                                 tf.makeInteger(4),
                                 tf.makeInteger(5),
                                 tf.makeInteger(6),
                                 tf.makeOperator(PdfOpcodes.SET_TEXT_MATRIX),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("by <something something> on 01/02/03. For personal use only."),
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
                                 tf.makeInteger(1),
                                 tf.makeInteger(2),
                                 tf.makeInteger(3),
                                 tf.makeInteger(4),
                                 tf.makeInteger(5),
                                 tf.makeInteger(6),
                                 tf.makeOperator(PdfOpcodes.SET_TEXT_MATRIX),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("by <something something> on 01/02/03. For personal use only."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.END_TEXT_OBJECT)),
                   tf);
    assertFalse(worker.result);
    worker.process(Arrays.asList(tf.makeOperator(PdfOpcodes.BEGIN_TEXT_OBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeInteger(1),
                                 tf.makeInteger(2),
                                 tf.makeInteger(3),
                                 tf.makeInteger(4),
                                 tf.makeInteger(5),
                                 tf.makeInteger(6),
                                 tf.makeOperator(PdfOpcodes.SET_TEXT_MATRIX),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("by <not the right string> on 12/11/2010. For personal use only."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.END_TEXT_OBJECT)),
                   tf);
    assertFalse(worker.result);
    worker.process(Arrays.asList(tf.makeOperator(PdfOpcodes.BEGIN_TEXT_OBJECT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("by <something something> on 01/02/03. For personal use only."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeString("This is irrelevant."),
                                 tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                                 tf.makeOperator(PdfOpcodes.END_TEXT_OBJECT)),
                   tf);
    assertFalse(worker.result);
  }
  
}
