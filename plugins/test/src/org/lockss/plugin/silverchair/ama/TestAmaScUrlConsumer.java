/*
 * $Id: $
 */

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair.ama;

import java.util.regex.Pattern;

import org.lockss.plugin.silverchair.ama.AmaScUrlConsumerFactory;
import org.lockss.test.LockssTestCase;

public class TestAmaScUrlConsumer extends LockssTestCase {

  public void testOrigPdfPattern() throws Exception {
    Pattern origPdfPat = AmaScUrlConsumerFactory.getOrigPdfPattern();
    assertTrue(isMatchRe("http://www.example.com/journals/jama/data/journals/jama/935316/iic160017.pdf", origPdfPat));
    assertTrue(isMatchRe("https://www.example.com/journals/jama/data/journals/jama/935316/iic160017.pdf", origPdfPat));
    assertTrue(isMatchRe("http://www.example.com/journals/jama/data/Journals/INTEMED/935149/IOI150114supp1_prod.pdf", origPdfPat));
    assertFalse(isMatchRe("http://www.example.com/journals/jama/data/journals/jama/935316/iic160017.pdf.gif", origPdfPat));
    assertFalse(isMatchRe("http://www.example.com/journals/jama/fullarticle/2521823", origPdfPat));
  }
  
  public void testDestPdfPattern() throws Exception {
    Pattern destPdfPat = AmaScUrlConsumerFactory.getDestPdfPattern();
    assertTrue(isMatchRe("http://www.example.com/pdfaccess.ashx?url=/data/journals/intemed/935149/io123456.pdf&routename=jamainternalmedicine", destPdfPat));
    assertTrue(isMatchRe("http://www.example.com/pdfaccess.ashx?url=/data/journals/foo/935149/io123456.pdf&routename=jama", destPdfPat));
    assertTrue(isMatchRe("http://www.example.com/pdfaccess.ashx?url=/data/journals/foo/935149/io123456.pdf", destPdfPat));
    assertTrue(isMatchRe("http://www.example.com/pdfaccess.ashx?url=/data/journals/foo/9f/io123456.pdf", destPdfPat));
    assertTrue(isMatchRe("http://www.example.com/pdfaccess.ashx?url=/data/journals/foo/9f/io123456.pdf", destPdfPat));
    assertFalse(isMatchRe("http://www.example.com/pdfaccess.ashx?url=/data/journals/foo/935149/io123456.pdf&routename=", destPdfPat));
    assertFalse(isMatchRe("http://www.example.com/journals/jama/downloadcitation/2530290?format=", destPdfPat));
  }
  
}
