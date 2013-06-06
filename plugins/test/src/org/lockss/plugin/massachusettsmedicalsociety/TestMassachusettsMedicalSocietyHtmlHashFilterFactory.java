/*
 * $Id: TestMassachusettsMedicalSocietyHtmlHashFilterFactory.java,v 1.1 2013-06-06 22:38:40 janicecheng Exp $
 */

package org.lockss.plugin.massachusettsmedicalsociety;

import java.io.*;

import org.htmlparser.filters.TagNameFilter;
import org.lockss.util.*;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.test.*;

public class TestMassachusettsMedicalSocietyHtmlHashFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private MassachusettsMedicalSocietyHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new MassachusettsMedicalSocietyHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }

  String test[] = {
		  "<html><div id = \"related\">abc</div></html>",
		  "<html><div id = \"trendsBox\">abc</div></html>",
	      "<html><div id = \"topAdBar\">abc</div></html>",
	      "<html><div class = \"Topbanner CM8\">abc</div></html>",
	      "<html><div class = \"audioTitle\">abc</div></html>",
	      "<html><div class = \"topLeftAniv\">abc</div></html>",
	      "<html><div class = \"ad\">abc</div></html>",
	      "<html><div id = \"rightRailAd\">abc</div></html>",
	      "<html><div class = \"rightAd\">abc</div></html>",
	      "<html><div id = \"rightAd\">abc</div></html>",
	      "<html><div class = \"toolsAd\">abc</div></html>",
	      "<html><div id = \"bottomAdBar\">abc</div></html>",
	      "<html><div class = \"bottomAd\">abc</div></html>",
	      "<html><div class = \"bannerAdTower\">abc</div></html>",
	      "<html><dd id = \"comments\">abc</dd></html>",
	      "<html><dd id = \"letters\">abc</dd></html>",
	      "<html><dd id = \"citedby\">abc</dd></html>",
	      "<html><div class = \"articleCorrection\">abc</div></html>",
	      "<html><div id = \"galleryContent\">abc</div></html>",
	      "<html><div class = \"discussion\">abc</div></html>",
	      "<html><dt id = \"citedbyTab\">abc</dt></html>",
	      "<html><div class = \"articleActivity\">abc</div></html>",
	      "<html><div id = \"institutionBox\">abc</div></html>",
	      "<html><div id = \"copyright\">abc</div></html>",
	      "<html><a class = \"issueArchive-recentIssue\">abc</a></html>",
	      "<html><div id = \"moreIn\">abc</div></html>",
	      "<html><div id = \"layerPlayer\">abc</div></html>",
	      "<html><li class = \"submitLetter\">abc</div></html>",
	      "<html><p class = \"openUntilInfo\">abc</div></html>",
	      "<html><div class = \"poll\">abc</div></html>"
		  
  };
  
  public void testFiltering() throws Exception {
    InputStream in;
    
    for (String t : test){
    	in = fact.createFilteredInputStream(mau, new StringInputStream(t),
    	        ENC);
    	assertEquals("<html></html>",StringUtil.fromInputStream(in));
    }
  }
}