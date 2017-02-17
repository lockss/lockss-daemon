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

package org.lockss.plugin.silverchair.oup;

import java.util.regex.Pattern;

import org.lockss.plugin.silverchair.ama.AmaScUrlConsumerFactory;
import org.lockss.test.LockssTestCase;

public class TestOupScUrlConsumer extends LockssTestCase {

  public void testOrigPdfPattern() throws Exception {
    Pattern origPdfPat = OupScUrlConsumerFactory.getOrigPdfPattern();
    assertTrue(isMatchRe("https://academic.oup.com/database/article-pdf/doi/10.1093/database/bau125/7298457/bau125.pdf", origPdfPat));
  }
  
  public void testDestPdfPattern() throws Exception {
    Pattern destPdfPat = OupScUrlConsumerFactory.getDestPdfPattern();
    assertTrue(isMatchRe("https://oup.silverchair-cdn.com/oup/backfile/Content_public/Journal/database/2015/10.1093_database_bau125/2/bau125.pdf?Expires=1487698032&Signature=BWuVCx0YtGloIS2kNqvnbnZB3iw4bCQA0PDHeHFgtds2tVZLPBjVDI~HclCpaiTByPNPNRdnqfzri2IDiBQJU2EII1f5S2Gh5h8J7uSPsqysOkSOkA6L227iLZUl3X30xPNUk88jMeZmQHYQRZsZOy4RBXCjCIsyt35IK47Ld5wn4b6rayV8LDphu8h77th05ky4jCIyjCKgcC4hC~244j982eGL9p4-j-lhD0wp0lBGFsEhNBp4-rAfYZs8QTlF~Mm57u1becRE9flX0oO~ozDwAZCMFMNEJQvkkMrd~1n5FyooR93zf8z6P0A1hKNNiDrmSkurF1hSj381FGNLtg__&Key-Pair-Id=APKAIUCZBIA4LVPAVW3Q", destPdfPat));
  }
  
}
