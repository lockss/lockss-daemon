/*
 * $Id$
 */
package org.lockss.plugin.ojs3;

import java.io.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestOjs3HtmlCrawlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private Ojs3HtmlCrawlFilterFactory fact;
  private MockArchivalUnit mau;
  
  public void setUp() throws Exception {
    super.setUp();
    fact = new Ojs3HtmlCrawlFilterFactory();
    mau = new MockArchivalUnit();
  }
  
 
  private static final String sectionArtDetails =
  		"<section class=\"article-more-details\">\n" + 
  		" <h2 class=\"sr-only\">Article Details</h2>\n" + 
  		"</section>FOO";
  
  private static final String byAuthor =
		"FOO" +
  		"<div id=\"articlesBySameAuthorList\">\n" + 
  		"  <h3>Most read articles by the same author(s)</h3>\n" + 
  		"  <ul>\n" + 
  		"    <li>\n" + 
  		"   <a href=\"https://journalprivacyconfidentiality.org/index.php/jpc/article/view/123\"></a>\n" + 
  		"    </li>" +
  		" </ul>" +
  		" </div>";
  
  private static final String aside =
		  "FOO" +
		  "<aside id=\"sidebar\" class=\"pkp_structure_sidebar\">\n" + 
		  "	<div class=\"content\">\n" + 
		  "   <a href=\"https://journalprivacyconfidentiality.org/index.php/jpc/about/submissions\">" + 
		  "			Make a Submission\n" + 
		  " </a>" +
		  " </div>" +
		  "</aside>";
  
  private static final String footer = 
		  "<footer class=\"footer\" role=\"contentinfo\">BLAH</footer>FOO";
		  
  private static final String header = 
		  "<header class=\"navbar navbar-default\">BLAH</header>FOO";
  
  private static final String filtered = "FOO";
  
  public void testFiltering() throws Exception {
    InputStream inA;
    
    inA = fact.createFilteredInputStream(mau, new StringInputStream(sectionArtDetails),
			 Constants.DEFAULT_ENCODING);
    assertEquals(filtered, StringUtil.fromInputStream(inA));
    
    inA = fact.createFilteredInputStream(mau, new StringInputStream(byAuthor),
			 Constants.DEFAULT_ENCODING);
    assertEquals(filtered, StringUtil.fromInputStream(inA));

    inA = fact.createFilteredInputStream(mau, new StringInputStream(aside),
			 Constants.DEFAULT_ENCODING);
    assertEquals(filtered, StringUtil.fromInputStream(inA));
    
    inA = fact.createFilteredInputStream(mau, new StringInputStream(header),
			 Constants.DEFAULT_ENCODING);
    assertEquals(filtered, StringUtil.fromInputStream(inA));
    
    inA = fact.createFilteredInputStream(mau, new StringInputStream(footer),
			 Constants.DEFAULT_ENCODING);
    assertEquals(filtered, StringUtil.fromInputStream(inA));

  }

}
