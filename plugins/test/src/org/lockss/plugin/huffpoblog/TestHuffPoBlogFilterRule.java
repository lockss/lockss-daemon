/*
 * $Id: TestHuffPoBlogFilterRule.java,v 1.1 2006-02-06 20:32:14 dshr Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.huffpoblog;

import java.io.*;
import org.lockss.util.*;
import org.lockss.test.LockssTestCase;

public class TestHuffPoBlogFilterRule extends LockssTestCase {
  private HuffPoBlogFilterRule rule;

  public void setUp() throws Exception {
    super.setUp();
    rule = new HuffPoBlogFilterRule();
  }

  private static final String inst1 =
 	"<center id=\"topad\">\n" +
  	"\n" +
	  "\n" +
	  "<!-- begin ad tag -->\n" +
	  "<iframe src=\"http://ad.doubleclick.net/adi/huffingtonpost/general;nickname=jeralyn-merritt;tile=1;dcopt=ist;sz=728x90;ord='+ ord +'?\" width=\"728\" height=\"90\" marginwidth=\"0\" marginheight=\"0\" frameborder=\"0\" scrolling=\"no\">\n" +
	  "<script src=\"http://ad.doubleclick.net/adj/huffingtonpost/general;nickname=jeralyn-merritt;tile=1;sz=728x90;abr=!ie;ord='+ ord +'?\" type=\"text/javascript\"></script>\n" +
	  "</iframe>\n" +
	  "<noscript><a href=\"http://ad.doubleclick.net/jump/huffingtonpost/general;nickname=jeralyn-merritt;tile=1;sz=728x90;abr=!ie4;abr=!ie5;abr=!ie6;ord=123456789?\" target=\"_blank\"><img src=\"http://ad.doubleclick.net/ad/huffingtonpost/general;nickname=jeralyn-merritt;tile=1;sz=728x90;abr=!ie4;abr=!ie5;abr=!ie6;ord=123456789?\" width=\"728\" height=\"90\" border=\"0\" alt=\"\" /></a></noscript>\n" +
	  "<!-- End ad tag -->\n" +
	  "\n" +
	  "\n" +
	  "</center>\n" +
	  "\n";

  private static final String inst2 =
 	"<center id=\"topad\">\n" +
  	"\n" +
	  "\n" +
	  "<!-- begin ad tag -->\n" +
	  "<iframe src=\"http://ad.doubleclick.net/adi/huffingtonpost/general;nickname=merritt-jeralyn;tile=1;dcopt=ist;sz=728x90;ord='+ ord +'?\" width=\"728\" height=\"90\" marginwidth=\"0\" marginheight=\"0\" frameborder=\"0\" scrolling=\"no\">\n" +
	  "<script src=\"http://ad.doubleclick.net/adj/huffingtonpost/general;nickname=merritt-jeralyn;tile=1;sz=728x90;abr=!ie;ord='+ ord +'?\" type=\"text/javascript\"></script>\n" +
	  "</iframe>\n" +
	  "<noscript><a href=\"http://ad.doubleclick.net/jump/huffingtonpost/general;nickname=merritt-jeralyn;tile=1;sz=728x90;abr=!ie4;abr=!ie5;abr=!ie6;ord=123456789?\" target=\"_blank\"><img src=\"http://ad.doubleclick.net/ad/huffingtonpost/general;nickname=merritt-jeralyn;tile=1;sz=728x90;abr=!ie4;abr=!ie5;abr=!ie6;ord=123456789?\" width=\"728\" height=\"90\" border=\"0\" alt=\"\" /></a></noscript>\n" +
	  "<!-- End ad tag -->\n" +
	  "\n" +
	  "\n" +
	  "</center>\n" +
	  "\n";

  private static final String inst3 =
 	"<center id=\"topad\">	\n" +
	  " <!-- begin ad tag -->\n" +
	  "<iframe src=\"http://ad.doubleclick.net/adi/huffingtonpost/general;nickname=merritt-jeralyn;tile=1;dcopt=ist;sz=728x90;ord='+ ord +'?\" width=\"728\" height=\"90\" marginwidth=\"0\" marginheight=\"0\" frameborder=\"0\" scrolling=\"no\">\n" +
	  "<script	src=\"http://ad.doubleclick.net/adj/huffingtonpost/general;nickname=merritt-jeralyn;tile=1;sz=728x90;abr=!ie;ord='+ ord +'?\" type=\"text/javascript\"></script>\n" +
	  "</iframe>\n" +
	  "<noscript><a href=\"http://ad.doubleclick.net/jump/huffingtonpost/general;nickname=merritt-jeralyn;tile=1;sz=728x90;abr=!ie4;abr=!ie5;abr=!ie6;ord=123456789?\" target=\"_blank\"><img src=\"http://ad.doubleclick.net/ad/huffingtonpost/general;nickname=merritt-jeralyn;tile=1;sz=728x90;abr=!ie4;abr=!ie5;abr=!ie6;ord=123456789?\" width=\"728\" height=\"90\" border=\"0\" alt=\"\" /></a></noscript>\n" +
	  "<!-- End ad tag -->\n" +
	  "\n" +
	  "\n" +
	  "</center>\n" +
	  "\n";

  public void testAdvertFiltering() throws IOException {
    Reader reader1 = rule.createFilteredReader(new StringReader(inst1));
    Reader reader2 = rule.createFilteredReader(new StringReader(inst2));
    assertEquals(StringUtil.fromReader(reader1),
		 StringUtil.fromReader(reader2));
  }
  public void testWhiteSpaceFiltering() throws IOException {
    Reader reader2 = rule.createFilteredReader(new StringReader(inst2));
    Reader reader3 = rule.createFilteredReader(new StringReader(inst3));
    assertEquals(StringUtil.fromReader(reader2),
		 StringUtil.fromReader(reader3));
  }
}
