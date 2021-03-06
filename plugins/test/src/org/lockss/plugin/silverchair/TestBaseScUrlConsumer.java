/*
 * $Id: $
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair;

import java.util.regex.Pattern;

import org.lockss.test.LockssTestCase;

public class TestBaseScUrlConsumer extends LockssTestCase {
  private static final BaseScUrlConsumerFactory ucf = new BaseScUrlConsumerFactory();

  public void testOrigPdfPattern() throws Exception {
    Pattern origPdfPat = ucf.getOrigPdfPattern();
    assertTrue(isMatchRe("https://academic.oup.com/database/article-pdf/doi/10.1093/database/bau125/7298457/bau125.pdf", origPdfPat));
    assertTrue(isMatchRe("https://academic.oup.com/bja/article-pdf/119/suppl_1/i72/22923220/aex383.pdf", origPdfPat));
    assertTrue(isMatchRe("https://read.dukeupress.edu/jclc/article-pdf/4/1/160/432125/160Wang.pdf", origPdfPat));
  }

  public void testDestPdfPattern() throws Exception {
    Pattern destPdfPat = ucf.getDestPdfPattern();
    assertTrue(isMatchRe("https://oup.silverchair-cdn.com/oup/backfile/Content_public/Journal/database/2015/10.1093_database_bau125/2/bau125.pdf?Expires=1487698032&Signature=BWuVCx0YtGloIS2kNqvnbnZB3iw4bCQA0PDHeHFgtds2tVZLPBjVDI~HclCpaiTByPNPNRdnqfzri2IDiBQJU2EII1f5S2Gh5h8J7uSPsqysOkSOkA6L227iLZUl3X30xPNUk88jMeZmQHYQRZsZOy4RBXCjCIsyt35IK47Ld5wn4b6rayV8LDphu8h77th05ky4jCIyjCKgcC4hC~244j982eGL9p4-j-lhD0wp0lBGFsEhNBp4-rAfYZs8QTlF~Mm57u1becRE9flX0oO~ozDwAZCMFMNEJQvkkMrd~1n5FyooR93zf8z6P0A1hKNNiDrmSkurF1hSj381FGNLtg__&Key-Pair-Id=APKAIUCZBIA4LVPAVW3Q", destPdfPat));
    assertTrue(isMatchRe("https://oup.silverchair-cdn.com/oup/backfile/content_public/journal/database/2015/10.1093_database_bau125/2/bau125.pdf?Expires=1487698032&Signature=BWuVCx0YtGloIS2kNqvnbnZB3iw4bCQA0PDHeHFgtds2tVZLPBjVDI~HclCpaiTByPNPNRdnqfzri2IDiBQJU2EII1f5S2Gh5h8J7uSPsqysOkSOkA6L227iLZUl3X30xPNUk88jMeZmQHYQRZsZOy4RBXCjCIsyt35IK47Ld5wn4b6rayV8LDphu8h77th05ky4jCIyjCKgcC4hC~244j982eGL9p4-j-lhD0wp0lBGFsEhNBp4-rAfYZs8QTlF~Mm57u1becRE9flX0oO~ozDwAZCMFMNEJQvkkMrd~1n5FyooR93zf8z6P0A1hKNNiDrmSkurF1hSj381FGNLtg__&Key-Pair-Id=APKAIUCZBIA4LVPAVW3Q", destPdfPat));
    assertTrue(isMatchRe("https://dup.silverchair-cdn.com/dup/Content_public/Journal/jclc/1/1-2/10.1215_23290048-2749359/4/1.pdf?Expires=1562199683&Signature=fIhPLCXoE2wIzx-p2pXNsDKtisJEl7tz98Ea3l1QSKBnzYhc8a3TXinElJWJElQOWKalmqiG0gtKIVrt3Vu3bQvnc~Po27TK5gzBMui9-gGVZOEoDN7uktnx6poUfe6wqeZuQMymH8Gpdvj2EMNPH702eAvWaSPeYsbMS9I~4tFrqb1m~HLCXBr29N7BsZi4yNwUkCikZc16w9ktMOycTBRaYYYqSzw2y-PfDttejI4BlUtF~zYZMZuLBWVrhND4DwONRiu2JkZrUUiCeiiclhbkH4FLIfnvDr3l9492ZW61z8yORlptOYVuaLPvv4xKAqY4DMQro~QhmR47Zd6NfQ__&Key-Pair-Id=APKAIE5G5CRDK6RD3PGA", destPdfPat));
  }

  public void testWmarkPdfPattern() throws Exception {
    Pattern wMarkPat = ucf.getWaterMarkPattern();
    assertTrue(isMatchRe("https://watermark.silverchair.com/548.pdf?token=AQECAHi208BE49Ofoo",wMarkPat));
  }

  public void testPatterns() throws Exception {
    Pattern origPdfPat = ucf.getOrigPdfPattern();
    Pattern destPdfPat = ucf.getDestPdfPattern();
    Pattern wMarkPat = ucf.getWaterMarkPattern();
    assertEquals(origPdfPat.pattern(), "/(issue|article)-pdf/");
    assertEquals(destPdfPat.pattern(), "(/backfile)?/Content_public/Journal/[^?]+" + "\\?Expires=[^&]+&Signature=[^&]+&Key-Pair-Id=.+$");
    assertEquals(wMarkPat.pattern(), "watermark[.]silverchair[.]com/[^?]+");
  }

}
