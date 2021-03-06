/*
 * $Id:$
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

package org.lockss.plugin.pub2web;

import java.util.Set;
import java.util.regex.Pattern;

import org.lockss.plugin.pub2web.Pub2WebUrlConsumerFactory.Pub2WebUrlConsumer;
import org.lockss.test.LockssTestCase;
import org.lockss.util.SetUtil;

public class TestPub2WebUrlConsumer extends LockssTestCase {
  Set<String> originatingUrls = SetUtil.set(
      "http://jmm.microbiologyresearch.org/deliver/fulltext/jmm/64/2/000003a.pdf?itemId=/content/suppdata/jmm/10.1099/jmm.0.000003-1&mimeType=pdf&isFastTrackArticle=",
      "http://jmm.microbiologyresearch.org/deliver/fulltext/jmm/64/2/000003b.mov?itemId=/content/suppdata/jmm/10.1099/jmm.0.000003-2&mimeType=quicktime&isFastTrackArticle=",
      "http://jmm.microbiologyresearch.org/deliver/fulltext/jmm/64/2/164_jmm000003.pdf?itemId=/content/journal/jmm/10.1099/jmm.0.000003&mimeType=pdf&isFastTrackArticle=",
      "http://jmm.microbiologyresearch.org/deliver/fulltext/jmm/64/10/000143-S2.xlsx?itemId=/content/suppdata/jmm/10.1099/jmm.0.000143-2&mimeType=xlsx&isFastTrackArticle=",
      "http://www.asmscience.org/deliver/fulltext/microbiolspec/2/6/AID-0022-2014.pdf?itemId=/content/journal/microbiolspec/10.1128/microbiolspec.AID-0022-2014&mimeType=pdf",
      "http://www.asmscience.org/deliver/fulltext/microbiolspec/3/2/PLAS_0039_2014_supp.xlsx?itemId=/content/suppdata/microbiolspec/10.1128/microbiolspec.PLAS-0039-2014-1&mimeType=vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      "https://jmmcr.microbiologyresearch.org/deliver/fulltext/supplementary-figures_jmmcr.0.000015.pdf?itemId=/content/jmmcr.0.000015&mimeType=pdf",
      "https://digital-library.theiet.org/deliver/fulltext/iet-syb/11/3/SYB.2016.0052.R1.SUP1.docx?itemId=/content/journals/10.1049/iet-syb.2016.0052/sup1&mimeType=docx",
      "https://digital-library.theiet.org/deliver/fulltext/iet-syb/11/6/SYB.2017.0013.R2.SUP1.docx?itemId=/content/journals/10.1049/iet-syb.2017.0013/sup1&mimeType=docx"
      );

  Set<String> originatingUrls2 = SetUtil.set(
          "https://mic.microbiologyresearch.org/content/journal/micro/10.1099/00221287-139-1-49?crawler=true&mimetype=application/pdf",
          "https://mic.microbiologyresearch.org/content/journal/micro/10.1099/00221287-139-9-1979?crawler=true&mimetype=application/pdf",
          "https://mic.microbiologyresearch.org/content/journal/micro/10.1099/00221287-139-9-1987?crawler=true&mimetype=application/pdf",
          "https://mic.microbiologyresearch.org/content/journal/micro/10.1099/00221287-139-9-1995?crawler=true&mimetype=application/pdf"
  );

  Set<String> destinationUrls = SetUtil.set(
      "http://www.microbiologyresearch.org/docserver/fulltext/jmm/64/2/000003b.mov?expires=1462410081&id=id&accname=guest&checksum=BAE31918F398930F23AA6FF787ADEA8",
      "http://www.microbiologyresearch.org/docserver/fulltext/jmm/64/2/000003c.mov?expires=1462410087&id=id&accname=guest&checksum=26350595A02A4F467F1160DEA4379A88",
      "http://www.microbiologyresearch.org/docserver/fulltext/jmm/64/2/000003d.mov?expires=1462410093&id=id&accname=guest&checksum=A08CAEA24F33D984D2843F8A14980366",
      "http://www.microbiologyresearch.org/docserver/fulltext/jmm/64/10/1216_jmm000143.pdf?expires=1462991102&id=id&accname=sgid025717&checksum=8533A66F9933B18C28BD5042672B66E4",
      "http://www.microbiologyresearch.org/docserver/fulltext/mgen/1/1/000001.pdf?expires=1472240373&id=id&accname=guest&checksum=BB2B5F904726B8D614BF63E8898665F1",
      "https://www.microbiologyresearch.org/docserver/fulltext/supplementary-figures_jmmcr.0.000015.pdf?expires=1472241881&id=id&accname=guest&checksum=B525FA14B2C3EAE641F814853E58678E",
      "http://digital-library.theiet.org/docserver/fulltext/iet-syb/11/3/SYB.2016.0052.R1.SUP1.docx?expires=1549647549&id=id&accname=325104&checksum=83B36F2CEC253D2DAF8EE20CBB4C880F",
      "http://digital-library.theiet.org/docserver/fulltext/iet-syb/11/6/SYB.2017.0013.R2.SUP1.docx?expires=1549646776&id=id&accname=guest&checksum=B011065F87CACCD30A572764F3E7D924",
      "https://www.microbiologyresearch.org/docserver/fulltext/micro/139/1/mic-139-1-49.pdf?expires=1558562688&id=id&accname=guest&checksum=C5016D69F74F8B015CB2A6A62134C4F9"
      );


  public void testOrigPdfPattern() throws Exception {
    Pattern origFullTextPat = Pattern.compile(Pub2WebUrlConsumer.ORIG_FULLTEXT_STRING, Pattern.CASE_INSENSITIVE);
    for (String url : originatingUrls) {
      assertMatchesRE(origFullTextPat, url);
    }
  }

  public void testOrigPdfPattern2() throws Exception {
    Pattern origFullTextPat = Pattern.compile(Pub2WebUrlConsumer.ORIG_FULLTEXT_STRING2, Pattern.CASE_INSENSITIVE);
    for (String url : originatingUrls2) {
      assertMatchesRE(origFullTextPat, url);
    }
  }

  public void testDestPdfPattern() throws Exception {
    Pattern destFullTextPat = Pattern.compile(Pub2WebUrlConsumer.DEST_FULLTEXT_STRING, Pattern.CASE_INSENSITIVE);
    for (String url : destinationUrls) {
      assertMatchesRE(destFullTextPat, url);
    }

  }
  
}
