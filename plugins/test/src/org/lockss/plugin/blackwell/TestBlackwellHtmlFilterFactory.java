/*
 * $Id: TestBlackwellHtmlFilterFactory.java,v 1.3 2007-05-01 22:36:44 troberts Exp $
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

package org.lockss.plugin.blackwell;

import java.io.*;

import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.test.*;

public class TestBlackwellHtmlFilterFactory extends LockssTestCase {

  private BlackwellHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new BlackwellHtmlFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String in1 =
    "Test content<center><span class=\"bannerstyle\">\n" +
    "<!-- Institution/Society Banners -->Licensed by Stanford\n" +
    "<!-- Ad Placeholder Id 1017 --><img src=\"/sda/8432/JOPY_synergy.jpg\" width=\"445\" height=\"60\" border=\"0\">\n" +
    "<!-- Ad Placeholder Id 1016 -->\n" +
    "</span></center>\n" +
    "<!-- END REGION 2 -->\n";

  private static final String in2 =
    "<b>Test content</b><center><span class=\"bannerstyle\">\n" +
    "<!-- Institution/Society Banners -->Licensed by Generic U.\n" +
    "<!-- Ad Placeholder Id 1017 --><img src=\"/sda/9999/JOPY_synergy.jpg\" width=\"445\" height=\"60\" border=\"0\">\n" +
    "<!-- Ad Placeholder Id 1016 -->\n" +
    "</span></center>\n" +
    "<!-- END REGION 2 -->\n";

  private static final String out = "Test content ";
//     "<center><span class=\"bannerstyle\">\n" +
//     "</span></center>\n" +
//     "<!-- END REGION 2 -->\n";

  private static final String noCitation =
    "Blah, random content" +
    "Blah, more random content";

  private static final String oneCitation =
    "Blah, random content" +
    "<h3 id=\"CitedBy\">This article is cited by:</h3></div>" +
    "<ul class=\"citedBy\">" +
    "<li>Skirmantas Janu&amp;#x0161;onis, George M. Anderson, Ilya Shifrovich and Pasko Rakic.  (2006) Ontogeny of brain and blood serotonin levels in 5-HT1A receptor knockout mice: potential relevance to the neurobiology of autism. <i>Journal of Neurochemistry</i>&nbsp;<span class=\"volume\">99</span>:3,  1019&#8211;1031<div class=\"CbLinks\"><a href=\"/servlet/linkout?suffix=citedby&amp;doi=10.1111/j.1651-2227.2005.tb01779.x&amp;doi2=10.1111/j.1471-4159.2006.04150.x&amp;url=/doi/abs/10.1111/j.1471-4159.2006.04150.x\">Abstract</a>&nbsp;<a href=\"/servlet/linkout?suffix=citedby&amp;doi=10.1111/j.1651-2227.2005.tb01779.x&amp;doi2=10.1111/j.1471-4159.2006.04150.x&amp;url=/doi/ref/10.1111/j.1471-4159.2006.04150.x\">Abstract and References</a>&nbsp;<a href=\"/servlet/linkout?suffix=citedby&amp;doi=10.1111/j.1651-2227.2005.tb01779.x&amp;doi2=10.1111/j.1471-4159.2006.04150.x&amp;url=/doi/full/10.1111/j.1471-4159.2006.04150.x\">Full Text Article</a>&nbsp;<a href=\"/doi/pdf/10.1111/j.1471-4159.2006.04150.x\" onclick=\"newWindow(this.href);return false\">Full Article PDF</a></div></li>" +
    "</li>" +
    "</ul>" +
    "Blah, more random content";

  private static final String twoCitations =
    "Blah, random content" +
    "<h3 id=\"CitedBy\">This article is cited by:</h3></div>" +
    "<ul class=\"citedBy\">" +
    "<li>Skirmantas Janu&amp;#x0161;onis, George M. Anderson, Ilya Shifrovich and Pasko Rakic.  (2006) Ontogeny of brain and blood serotonin levels in 5-HT1A receptor knockout mice: potential relevance to the neurobiology of autism. <i>Journal of Neurochemistry</i>&nbsp;<span class=\"volume\">99</span>:3,  1019&#8211;1031<div class=\"CbLinks\"><a href=\"/servlet/linkout?suffix=citedby&amp;doi=10.1111/j.1651-2227.2005.tb01779.x&amp;doi2=10.1111/j.1471-4159.2006.04150.x&amp;url=/doi/abs/10.1111/j.1471-4159.2006.04150.x\">Abstract</a>&nbsp;<a href=\"/servlet/linkout?suffix=citedby&amp;doi=10.1111/j.1651-2227.2005.tb01779.x&amp;doi2=10.1111/j.1471-4159.2006.04150.x&amp;url=/doi/ref/10.1111/j.1471-4159.2006.04150.x\">Abstract and References</a>&nbsp;<a href=\"/servlet/linkout?suffix=citedby&amp;doi=10.1111/j.1651-2227.2005.tb01779.x&amp;doi2=10.1111/j.1471-4159.2006.04150.x&amp;url=/doi/full/10.1111/j.1471-4159.2006.04150.x\">Full Text Article</a>&nbsp;<a href=\"/doi/pdf/10.1111/j.1471-4159.2006.04150.x\" onclick=\"newWindow(this.href);return false\">Full Article PDF</a></div></li>" +
    "<li>Magnus Landgren, Marita Andersson Gr&amp;ouml;nlund, Per-Olof Elfstrand, Jan-Erik Simonsson, Leif Svensson &amp;amp; Kerstin Str&amp;ouml;mland.  (2006) Health before and after adoption from Eastern Europe. <i>Acta Paediatrica</i>&nbsp;<span class=\"volume\">95</span>:6,  720&#8211;725<div class=\"CbLinks\"><a href=\"/servlet/linkout?suffix=citedby&amp;doi=10.1111/j.1651-2227.2005.tb01779.x&amp;doi2=10.1111/j.1651-2227.2006.tb02321.x&amp;url=/doi/abs/10.1111/j.1651-2227.2006.tb02321.x\">Abstract</a>&nbsp;<a href=\"/servlet/linkout?suffix=citedby&amp;doi=10.1111/j.1651-2227.2005.tb01779.x&amp;doi2=10.1111/j.1651-2227.2006.tb02321.x&amp;url=/doi/ref/10.1111/j.1651-2227.2006.tb02321.x\">Abstract and References</a>&nbsp;<a href=\"/servlet/linkout?suffix=citedby&amp;doi=10.1111/j.1651-2227.2005.tb01779.x&amp;doi2=10.1111/j.1651-2227.2006.tb02321.x&amp;url=/doi/full/10.1111/j.1651-2227.2006.tb02321.x\">Full Text Article</a>&nbsp;<a href=\"/doi/pdf/10.1111/j.1651-2227.2006.tb02321.x\" onclick=\"newWindow(this.href);return false\">Full Article PDF</a></div>" +
    "</li>" +
    "</ul>" +
    "Blah, more random content";

  public void testFilter() throws IOException {
    InputStream inA;
    InputStream inB;

    inA = fact.createFilteredInputStream(mau, new StringInputStream(in1),
					 Constants.DEFAULT_ENCODING);
    assertInputStreamMatchesString(out, inA);
    inB = fact.createFilteredInputStream(mau, new StringInputStream(in2),
					 Constants.DEFAULT_ENCODING);
    assertInputStreamMatchesString(out, inB);

    assertFilterToSame(in1, in2);

    assertFilterToSame(noCitation, oneCitation);
    assertFilterToSame(oneCitation, twoCitations);
  }

  private void assertFilterToSame(String str1, String Str2) throws IOException {
    InputStream inA = fact.createFilteredInputStream(mau, new StringInputStream(str1),
					 Constants.DEFAULT_ENCODING);
    InputStream inB = fact.createFilteredInputStream(mau, new StringInputStream(Str2),
					 Constants.DEFAULT_ENCODING);
    assertEquals(StringUtil.fromInputStream(inA),
                 StringUtil.fromInputStream(inB));

  }

}
