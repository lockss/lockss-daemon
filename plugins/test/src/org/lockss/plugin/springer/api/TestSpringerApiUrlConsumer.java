/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.springer.api;

import java.util.regex.Pattern;

import org.lockss.test.LockssTestCase;

public class TestSpringerApiUrlConsumer extends LockssTestCase {

  public void testOrigPdfPattern() throws Exception {
    Pattern origPdfPat = SpringerApiUrlConsumer.makeOrigPdfPattern("http://www.example.com/");
    assertMatchesRE(origPdfPat, "http://www.example.com/content/pdf/10.1007%2Fs00125-013-3090-y.pdf");
    assertMatchesRE(origPdfPat, "http://www.example.com/content/pdf/10.1007/s00125-013-3090-y.pdf");
    assertMatchesRE(origPdfPat, "http://www.example.com/content/pdf/10.1007/a/b/c.pdf");
    assertMatchesRE(origPdfPat, "http://www.example.com/content/pdf/10.1007/a(1)b[2]c{3}d.pdf");
    assertMatchesRE(origPdfPat, "http://www.example.com/content/pdf/10.1007%2Fa%281%29b%5B2%5Dc%7B3%7Dd.pdf");
    assertNotMatchesRE(origPdfPat, "http://www.example.com/article/10.1007%2Fs00125-013-3090-y");
    assertNotMatchesRE(origPdfPat, "http://www.example.com/article/10.1007%2Fs00125-013-3090-y/fulltext.html");
    assertNotMatchesRE(origPdfPat, "http://www.example.com/esm/art%3A10.1007%2Fs00125-013-3090-y/MediaObjects/125_2013_3090_MOESM1_ESM.pdf");
    assertNotMatchesRE(origPdfPat, "http://static-content.example.com/esm/art%3A10.1007%2Fs00125-013-3090-y/MediaObjects/125_2013_3090_MOESM1_ESM.pdf");
    assertNotMatchesRE(origPdfPat, "http://download.example.com/static/pdf/371/art%253A10.1007%252Fs00125-013-3090-y.pdf?auth66=0000028291_00004acfc1773d7b4cf8393f4201ce39&ext=.pdf");
  }
  
  public void testDestPdfPattern() throws Exception {
    Pattern destPdfPat = SpringerApiUrlConsumer.makeDestPdfPattern("http://download.example.com/");
    assertMatchesRE(destPdfPat, "http://download.example.com/static/pdf/371/art%253A10.1007%252Fs00125-013-3090-y.pdf?auth123=0000028291_00004acfc1773d7b4cf8393f4201ce39&ext=.pdf");
    assertMatchesRE(destPdfPat, "http://download.example.com/static/pdf/371/art%253A10.1007%252Fs00125-013-3090-y.pdf?auth456=0000028291_00004acfc1773d7b4cf8393f4201ce39&ext=.pdf");
    assertMatchesRE(destPdfPat, "http://download.example.com/static/pdf/371/art%253A10.1007%252Fs00125-013-3090-y.pdf?auth=0000028291_00004acfc1773d7b4cf8393f4201ce39&ext=.pdf");
    assertMatchesRE(destPdfPat, "http://download.example.com/static/pdf/371/art%253A10.1007%252Fs00125-013-3090-y.pdf?auth123=0000028291_00004acfc1773d7b4cf8393f4201ce39");
    assertMatchesRE(destPdfPat, "http://download.example.com/static/pdf/other/path/to/fulltext.pdf?auth123=0000028291_00004acfc1773d7b4cf8393f4201ce39");
    assertNotMatchesRE(destPdfPat, "http://www.example.com/content/pdf/10.1007%2Fs00125-013-3090-y.pdf");
    assertNotMatchesRE(destPdfPat, "http://www.example.com/content/pdf/10.1007/s00125-013-3090-y.pdf");
    assertNotMatchesRE(destPdfPat, "http://www.example.com/article/10.1007%2Fs00125-013-3090-y");
    assertNotMatchesRE(destPdfPat, "http://www.example.com/article/10.1007%2Fs00125-013-3090-y/fulltext.html");
    assertNotMatchesRE(destPdfPat, "http://www.example.com/esm/art%3A10.1007%2Fs00125-013-3090-y/MediaObjects/125_2013_3090_MOESM1_ESM.pdf");
    assertNotMatchesRE(destPdfPat, "http://download.example.com/esm/art%3A10.1007%2Fs00125-013-3090-y/MediaObjects/125_2013_3090_MOESM1_ESM.pdf");
    assertNotMatchesRE(destPdfPat, "http://static-content.example.com/esm/art%3A10.1007%2Fs00125-013-3090-y/MediaObjects/125_2013_3090_MOESM1_ESM.pdf");
  }
  
}
