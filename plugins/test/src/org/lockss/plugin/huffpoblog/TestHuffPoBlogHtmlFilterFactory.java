/*
 * $Id$
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
import org.lockss.test.*;

public class TestHuffPoBlogHtmlFilterFactory extends LockssTestCase {
  private HuffPoBlogHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new HuffPoBlogHtmlFilterFactory();
    mau = new MockArchivalUnit();
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

  private static final String inst4 =
	"<h3>Related News Stories</h3>\n" +
	  "<ul class=\"relatedposts\"><li><a href=\"/2006/02/09/cheney-authorized-leaki_n_15361.html\">Cheney \"Authorized\" Leaking Of Classified Information...</a></li>\n" +
	  "<li><a href=\"/2006/02/04/prosecutors-report-says-_n_15096.html\">Prosecutor\'s Report Says Cheney Aide In Broad Web Of Deception, Lied Repeatedly About CIA Outing...</a></li>\n" +
	  "<li><a href=\"/2006/02/05/cia-was-making-specific-_n_15133.html\">CIA \"Was Making Specific Efforts To Conceal\" Plame\'s Covert Status When Outed...</a></li>\n" +
	  "</ul>\n" +
	  "\n" +
	  "\n" +
	"<h3>Related Blog Posts</h3>\n" +
	  "<ul class=\"relatedposts\"><li><a href=\"/arianna-huffington/the-beginning-of-the-end_b_15389.html\"><b>Arianna Huffington</b>: The Beginning of the End?</a></li>\n" +
	  "<li><a href=\"/jane-hamsher/time-magazine-spikes-the-_b_15279.html\"><b>Jane Hamsher</b>: Time Magazine Spikes the Plame Story</a></li>\n" +
	  "<li><a href=\"/joe-fontaine/russert-still-hasnt-clar_b_15223.html\"><b>Joe Fontaine</b>: Russert Still Hasn\'t Clarified Plamegate Role</a></li>\n" +
	  "</ul>\n" +
	  "\n" +
	  "\n" +
	"<h3 id=\"comments\">\n" +
	  "Posted Comments \n" +
	"</h3>\n";

  private static final String inst5 =
	"<h3>Related News Stories</h3>\n" +
	  "<ul class=\"relatedposts\"><li><a href=\"/2006/02/09/cheney-ordered-leaki_n_15361.html\">Cheney \"Ordered\" Leaking Of Classified Information...</a></li>\n" +
	  "<li><a href=\"/2006/02/04/prosecutors-report-writes-_n_15096.html\">Prosecutor\'s Report Says Cheney In Broad Web Of Deception, Lied Repeatedly About CIA Outing...</a></li>\n" +
	  "<li><a href=\"/2006/02/05/cia-was-making-frantic-_n_15133.html\">CIA \"Was Making Frantic Efforts To Conceal\" Plame\'s Covert Status When Outed...</a></li>\n" +
	  "</ul>\n" +
	  "\n" +
	  "\n" +
	"<h3>Related Blog Posts</h3>\n" +
	  "<ul class=\"relatedposts\"><li><a href=\"/arianna-huffington/the-end-of-the-end_b_15389.html\"><b>Arianna Huffington</b>: The End of the End?</a></li>\n" +
	  "<li><a href=\"/jane-hamsher/time-magazine-discards-the-_b_15279.html\"><b>Jane Hamsher</b>: Time Magazine Discards the Plame Story</a></li>\n" +
	  "<li><a href=\"/joe-fontaine/russert-still-has-clar_b_15223.html\"><b>Joe Fontaine</b>: Russert Still Has Plamegate Role</a></li>\n" +
	  "</ul>\n" +
	  "\n" +
	  "\n" +
	"<h3 id=\"comments\">\n" +
	  "Posted Comments \n" +
	"</h3>\n";

  private static final String inst6 =
	  "Hello\n" +
	  "<div class=\"relatedcats\">\n" +
		"READ MORE: <a href=\"/news/iraq\">Iraq</a>, <a href=\"/news/dick-cheney\">Dick Cheney</a>, <a href=\"/news/scooter-libby\">Scooter Libby</a>, <a href=\"/news/patrick-fitzgerald\">Patrick Fitzgerald</a>, <a href=\"/news/investigations\">Investigations</a>, <a href=\"/news/george-w-bush\">George W. Bush</a>                  </div>\n" +
	  "Goodbye\n";

  private static final String inst7 =
	  "Hello\n" +
	  "<div class=\"relatedcats\">\n" +
		"READ MORE: <a href=\"/news/iran\">Iran</a>, <a href=\"/news/dick-cheney\">Dick Cheney</a>, <a href=\"/news/scooter-libby\">Scooter Libby</a>, <a href=\"/news/patrick-fitzgerald\">Patrick Fitzgerald</a>, <a href=\"/news/george-w-bush\">George W. Bush</a>                  </div>\n" +
	  "Goodbye\n";


  public void testAdvertFiltering() throws Exception {
    InputStream in1 =
        fact.createFilteredInputStream(mau, new StringInputStream(inst1),
            Constants.DEFAULT_ENCODING);
    InputStream in2 =
        fact.createFilteredInputStream(mau, new StringInputStream(inst2),
            Constants.DEFAULT_ENCODING);
    assertEquals(StringUtil.fromInputStream(in1),
        StringUtil.fromInputStream(in2));
  }

  public void testWhiteSpaceFiltering() throws Exception {
    InputStream in2 =
        fact.createFilteredInputStream(mau,
            new StringInputStream(inst2),
            Constants.DEFAULT_ENCODING);
    InputStream in3 =
        fact.createFilteredInputStream(mau,
            new StringInputStream(inst3),
            Constants.DEFAULT_ENCODING);
    assertEquals(StringUtil.fromInputStream(in2),
        StringUtil.fromInputStream(in3));
  }

  public void testRelatedPostFiltering() throws Exception {
    InputStream in4 =
        fact.createFilteredInputStream(mau,
            new StringInputStream(inst4),
            Constants.DEFAULT_ENCODING);
    InputStream in5 =
        fact.createFilteredInputStream(mau,
            new StringInputStream(inst5),
            Constants.DEFAULT_ENCODING);
    assertEquals(StringUtil.fromInputStream(in4),
        StringUtil.fromInputStream(in5));
  }
  public void testRelatedCatFiltering() throws Exception {
    InputStream in6 =
        fact.createFilteredInputStream(mau,
            new StringInputStream(inst6),
            Constants.DEFAULT_ENCODING);
    InputStream in7 =
        fact.createFilteredInputStream(mau,
            new StringInputStream(inst7),
            Constants.DEFAULT_ENCODING);
    assertEquals(StringUtil.fromInputStream(in6),
        StringUtil.fromInputStream(in7));
  }
}
