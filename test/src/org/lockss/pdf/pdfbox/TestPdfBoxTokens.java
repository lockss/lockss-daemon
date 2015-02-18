/*
 * $Id$
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

package org.lockss.pdf.pdfbox;

import org.apache.pdfbox.util.*;
import org.lockss.pdf.*;
import org.lockss.pdf.pdfbox.PdfBoxTokens;
import org.lockss.test.LockssTestCase;

public class TestPdfBoxTokens extends LockssTestCase {

  public void testMakeOperator() throws Exception {
    PdfBoxTokens.cachedOperators.clear();
    int expected = 0;
    assertEquals(expected, PdfBoxTokens.cachedOperators.size());
    
    // Verify that typical operators are cached, e.g. 'BT'
    PDFOperator bt1 = PDFOperator.getOperator(PdfOpcodes.BEGIN_TEXT_OBJECT);
    PdfToken tok1 = PdfBoxTokens.makeOperator(bt1);
    ++expected; // Previous line should have cached one more operator
    assertTrue(tok1.isOperator());
    assertEquals(PdfOpcodes.BEGIN_TEXT_OBJECT, tok1.getOperator());
    assertEquals(expected, PdfBoxTokens.cachedOperators.size());
    PdfToken tok2 = PdfBoxTokens.makeOperator(bt1);
    assertSame(tok1, tok2); // Cached: should be identical object
    assertEquals(expected, PdfBoxTokens.cachedOperators.size());
    
    // Verify that 'BI' operators don't get cached
    assertEquals(expected, PdfBoxTokens.cachedOperators.size());
    PDFOperator bi1 = PDFOperator.getOperator(PdfOpcodes.BEGIN_IMAGE_OBJECT);
    bi1.setImageData("abc".getBytes());
    bi1.setImageParameters(new ImageParameters());
    PdfToken tok3 = PdfBoxTokens.makeOperator(bi1);
    assertTrue(tok3.isOperator());
    assertEquals(PdfOpcodes.BEGIN_IMAGE_OBJECT, tok3.getOperator());
    assertEquals(expected, PdfBoxTokens.cachedOperators.size());
    PdfToken tok4 = PdfBoxTokens.makeOperator(bi1);
    assertNotSame(tok3, tok4); // Not cached, should be brand new object
    assertEquals(expected, PdfBoxTokens.cachedOperators.size());

    // Same thing with 'ID' operators
    PDFOperator id1 = PDFOperator.getOperator(PdfOpcodes.BEGIN_IMAGE_DATA);
    bi1.setImageData("def".getBytes());
    bi1.setImageParameters(new ImageParameters());
    PdfToken tok5 = PdfBoxTokens.makeOperator(id1);
    assertEquals(expected, PdfBoxTokens.cachedOperators.size());
    PdfToken tok6 = PdfBoxTokens.makeOperator(id1);
    assertNotSame(tok5, tok6);
    assertEquals(expected, PdfBoxTokens.cachedOperators.size());
  }
  
}
