/*
 * $Id: TestIOPScienceHtmlHashFilterFactory.java,v 1.2 2013-07-25 22:07:48 alexandraohlson Exp $
 */

package org.lockss.plugin.iop;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestIOPScienceHtmlHashFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private IOPScienceHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new IOPScienceHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }

  // test removal of tags by the hash filter
  private static final String tagsHtmlHash =
      "<div>" +
      "<div class=\"alsoRead\"><br></div>" +
      "<div class=\"tabs javascripted\"><br></div>" +
      "<div id=\"banner\"><br></div>" +
      "<div id=\"footer\"><br></div>" +
      "<link type=\"text/css\"/>" +
      "<script type=\"javascript\"/>var x=0;</script>" +
      "<form action=\"foo?jsessionId=bar\"><br></form>" +
      "<div id=\"tacticalBanners\"><br></div>" +
      "</div>";
  // only outer div should remain
  private static final String tagsHtmlHashFiltered =
      "<div></div>";
  
 
  private static final String WhiteSpace1 = "\n  <li><a href=\"/content/pdf/1477-7525-8-103.pdf\">PDF</a>\n (543KB)\n </li>";
  
  private static final String WhiteSpace2 = "\n\n      <li><a href=\"/content/pdf/1477-7525-8-103.pdf\">PDF</a>\n       (543KB)\n      </li>";
 
  
  private static final String mathJaxHtml =
      "<div class=\"mathJaxControls\" style=\"display:none\">" +
          "<!-- add mathjax logo here and hide mathjax text -->" +
          "<a class=\"mjbadge\" href=\"http://www.mathjax.org/\">Mathjax</a>" +
          "<a href=\"#\" id=\"mathJaxOn\">On</a> | <a href=\"#\" class=\"selectedMathJaxOption\" id=\"mathJaxOff\">Off</a>" +
          "</div>" +
          "<br clear=\"all\"/>";
  private static final String mathJaxHtmlFiltered =
          "<br clear=\"all\"/>";
 
  private static final String rightColHtml =
      "<div id=\"rightCol\">" +
          "<ul class=\"accordion\">" +
          "    <li><h5>Contents</h5>" +
          "<ol class=\"accordion open\">" +
          "<li><a href=\"#artAbst\">Abstract</a></li>" +
          "    <li>" +
          "        <h5>Related Articles</h5>" +
          "        <ol class=\"accordion\">" +
          "                <li>" +
          "                    <a href=\"/xx?rel=sem&amp;relno=1\"" +
          "                       title=\"Semicond. Sci. Technol., xx, yy\"" +
          "                       >" +
          "                            1. title here" +
          "                    </a>" +
          "                </li>" +
          "                <li>" +
          "                    <a href=\"/zz?rel=sem&amp;relno=2\"" +
          "                       title=\"Semicond. Sci. Technol., zz, qq\"" +
          "                       >" +
          "                            2. another title here" +
          "                    </a>" +
          "                </li>" +
          "        </ol>" +
          "    </li>" +
          "    <li>" +
          "        <h5>Related Review Articles</h5>" +
          "        <ol class=\"accordion\">" +
          "                <li>" +
          "                    <a href=\"/pp?rel=rev&amp;relno=1\"" +
          "                       title=\"Semicond. Sci. Technol., pp, ss\"" +
          "                       >" +
          "                            1. title three" +
          "                    </a>" +
          "                </li>" +
          "        </ol>" +
          "    </li>" +
          "<li>" +
          "    <h5>Journal links</h5>" +
          "    <ul class=\"accordion\">" +
          "    <li><a href=\"/nn\" title=\"Journal home\">Journal home</a></li>" +
          "    <li><a href=\"/nn/page/Scope\">Scope</a></li>" +
          "    </ul>" +
          "</li>" +
          "</ul>" +
          "<div id=\"tabStop\">&nbsp;</div>" +
          "</div>" +
          "<br clear=\"all\"/>";
  private static final String rightColHtmlFiltered =
          "<br clear=\"all\"/>";


  public void testFiltering() throws Exception {
    InputStream inA;
    InputStream inB;
    
    /* impactFactor test */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(tagsHtmlHash),
        ENC);

    assertEquals(tagsHtmlHashFiltered,StringUtil.fromInputStream(inA));

    /* whiteSpace test */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(WhiteSpace1),
        ENC);
    
    inB = fact.createFilteredInputStream(mau, new StringInputStream(WhiteSpace2),
        ENC);

    assertEquals(StringUtil.fromInputStream(inA),StringUtil.fromInputStream(inB));
    
    /* rightCol test */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(rightColHtml),
        ENC);
    assertEquals(rightColHtmlFiltered,StringUtil.fromInputStream(inA));
    
    /* mathjax text */
    inA = fact.createFilteredInputStream(mau, new StringInputStream(mathJaxHtml),
        ENC);
    assertEquals(mathJaxHtmlFiltered,StringUtil.fromInputStream(inA));
        
    
  }
}