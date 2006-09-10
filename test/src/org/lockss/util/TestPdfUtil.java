/*
 * $Id: TestPdfUtil.java,v 1.1 2006-09-10 07:50:50 thib_gc Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;

import org.lockss.test.*;
import org.lockss.util.PdfUtil.*;
import org.pdfbox.cos.*;

public class TestPdfUtil extends LockssTestCase {

  public void testIdentityDocumentTransform() throws Exception {
    assertEquals(true, new IdentityDocumentTransform().transform(new MockPdfDocument()));
    assertEquals(true, new IdentityDocumentTransform(true).transform(new MockPdfDocument()));
    assertEquals(false, new IdentityDocumentTransform(false).transform(new MockPdfDocument()));
  }

  public void testIdentityPageTransform() throws Exception {
    assertEquals(true, new IdentityPageTransform().transform(new MockPdfPage()));
    assertEquals(true, new IdentityPageTransform(true).transform(new MockPdfPage()));
    assertEquals(false, new IdentityPageTransform(false).transform(new MockPdfPage()));
  }

  public void testPdfString() throws Exception {
    assertFalse(PdfUtil.isPdfString(new COSFloat(1.0f)));
    assertFalse(PdfUtil.isPdfString(new COSInteger(1)));
    assertFalse(PdfUtil.isPdfString("Java string"));
    COSString cosString = new COSString("foo");
    assertTrue(PdfUtil.isPdfString(cosString));
    assertEquals(cosString.getString(), PdfUtil.getPdfString(cosString));
  }

  public void testPdfInteger() throws Exception {
    assertFalse(PdfUtil.isPdfInteger(new COSFloat(1.0f)));
    assertFalse(PdfUtil.isPdfInteger(new COSString("foo")));
    assertFalse(PdfUtil.isPdfInteger(new Integer(1)));
    COSInteger cosInteger = new COSInteger(123);
    assertTrue(PdfUtil.isPdfInteger(cosInteger));
    assertEquals(cosInteger.intValue(), PdfUtil.getPdfInteger(cosInteger));
  }

  public void testPdfFloat() throws Exception {
    assertFalse(PdfUtil.isPdfFloat(new COSInteger(1)));
    assertFalse(PdfUtil.isPdfFloat(new COSString("foo")));
    assertFalse(PdfUtil.isPdfFloat(new Float(1.0f)));
    COSFloat cosFloat = new COSFloat(12.34f);
    assertTrue(PdfUtil.isPdfFloat(cosFloat));
    assertEquals(cosFloat.floatValue(), PdfUtil.getPdfFloat(cosFloat), 0.0f);
  }

}
