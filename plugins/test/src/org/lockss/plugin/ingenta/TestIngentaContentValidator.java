/*
 * $Id$
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

package org.lockss.plugin.ingenta;

import org.lockss.test.*;
import org.lockss.util.*;

import java.util.Properties;

import org.lockss.config.*;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.plugin.definable.DefinablePlugin;

public class TestIngentaContentValidator extends LockssTestCase {

  static Logger log = Logger.getLogger(TestIngentaContentValidator.class);


  private MockLockssDaemon theDaemon;

  static final String PLUGIN_ID = "org.lockss.plugin.ingenta.IngentaJournalPlugin";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String GRAPHICS_URL = "http://graphics.ingentaconnect.com/"; //this is not a real url
  static final String BASE_URL = "https://www.ingentaconnect.com/"; //this is not a real url
  static final String API_URL = "https://api.ingentaconnect.com/"; //this is not a real url

  private static final String SOMETEXT = "text goes here";
  private static final int LEN = SOMETEXT.length();

  private static final String GOOD_LANDING_TEXT = 
      "<!DOCTYPE html>" +
          "<html lang=\"en\">" +
          "<head>" +
          "<meta name=\"CRAWLER.fullTextHtmlLink\" content=\"https://api.ingentaconnect.com/content/aspt/sb/2018/00000043/00000001/art00001?crawler=true&mimetype=text/html\"/>" +
          "<meta name=\"CRAWLER.fullTextLink\" content=\"https://api.ingentaconnect.com/content/aspt/sb/2018/00000043/00000001/art00001?crawler=true\"/>" +
          "</head>" +
          "<body class=\"ingenta\">FOO </body></html>";
  private static final int GOODLEN = GOOD_LANDING_TEXT.length();

  private static final String BAD_LANDING_TEXT = 
      "<!DOCTYPE html>" +
          "<html lang=\"en\">" +
          "<head>" +
          "<meta name=\"DC.creator\" content=\"Foo, Blah\"/>" +
          "</head>" +
          "<body class=\"ingenta\">FOO </body></html>";
  private static final int BADLEN = BAD_LANDING_TEXT.length();


  private DefinableArchivalUnit ing_au;

  static final String htmlLandingUrls[] =
    {
    BASE_URL + "content/aspt/sb/2018/00000043/00000001/art00001",
    BASE_URL + "content/aspt/sb/2018/00000099/00000044/art00011",
    };
  static final String htmlNotLandingUrls[] =
    {
    BASE_URL + "content/aspt/sb/2018/00000043/00000001",
    API_URL + "content/aspt/sb/2018/00000043/00000003/art00023?crawler=true&mimetype=text/html",
    API_URL + "content/0363-6445?format=clockss&volume=43",
    BASE_URL + "content/0363-6445?format=clockss&volume=43",
    //text/plain
    BASE_URL + "content/aspt/sb/2018/00000043/00000001/art00001?format=bib",
    BASE_URL + "content/aspt/sb/2018/00000043/00000001?format=bib",
    };
  static final String pdfUrls[] =
    {
    API_URL + "content/aspt/sb/2018/00000043/00000003/art00023?crawler=true",
    };




  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();	

    Properties props = new Properties();
    props.setProperty("publisher_id", "aspt");
    props.setProperty(JID_KEY, "sb");
    props.setProperty(BASE_URL_KEY, BASE_URL);
    props.setProperty("api_url", API_URL);
    props.setProperty("graphics_url", GRAPHICS_URL);
    props.setProperty("volume_name", "43");
    props.setProperty("journal_issn", "0363-6445");
    Configuration config = ConfigurationUtil.fromProps(props);

    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(getMockLockssDaemon(),
        PLUGIN_ID);
    ing_au = (DefinableArchivalUnit)ap.createAu(config);
  }


  ContentValidator getValidator() {
    String mimeType = "text/html";
    ContentValidatorFactory fact =
        ing_au.getContentValidatorFactory(mimeType);
    return fact.createContentValidator(ing_au, mimeType);
  }    


  public void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Method that creates a simulated Cached URL with appropriate content-type 
   * @throws Exception
   */
  public void testHtmlContentType() throws Exception {
    MockCachedUrl cu;

    ContentValidator defValidator = getValidator();
    assertNotEquals(null, defValidator);
    for (String urlString : htmlLandingUrls) {
      cu = new MockCachedUrl(urlString, ing_au);
      cu.setContent(GOOD_LANDING_TEXT);
      cu.setContentSize(GOODLEN);
      cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);
      // should NOT throw
      defValidator.validate(cu);
      // SHOULD THROW
      cu.setContent(BAD_LANDING_TEXT);
      cu.setContentSize(BADLEN);
      try {
        defValidator.validate(cu);
        fail("Bad cu should throw exception " + cu.getUrl());
      } catch (Exception e) {
        // okay, fall-thru
      }    	
    }

    // Now check that the non-landing html pages don't fail
    for (String urlString : htmlNotLandingUrls) {
      cu = new MockCachedUrl(urlString, ing_au);
      cu.setContent(GOOD_LANDING_TEXT);
      cu.setContentSize(GOODLEN);
      cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);
      // should NOT throw
      defValidator.validate(cu);
      // should NOT THROW - we don't check content
      cu.setContent(BAD_LANDING_TEXT);
      cu.setContentSize(BADLEN);
      defValidator.validate(cu);
    }		

    // Now check that the non-landing html pages don't fail
    for (String urlString : pdfUrls) {
      cu = new MockCachedUrl(urlString, ing_au);
      cu.setContent(GOOD_LANDING_TEXT);
      cu.setContentSize(GOODLEN);
      cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);
      // SHOULD THROW even though good content because this should be a PDF mimetype
      try {
        defValidator.validate(cu);
        fail("PDF url pattern is html " + cu.getUrl());
      } catch (Exception e) {
        // okay, fall-thru
      }    
    } 
    
    // Now check that the non-landing html pages don't fail
    for (String urlString : pdfUrls) {
      cu = new MockCachedUrl(urlString, ing_au);
      cu.setContent(BAD_LANDING_TEXT);
      cu.setContentSize(BADLEN);
      cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);
      // doesn't matter that it's "bad" content - will fail because not PDF
      try {
        defValidator.validate(cu);
        fail("PDF url pattern is html " + cu.getUrl());
      } catch (Exception e) {
        // okay, fall-thru
      }    
    }      

  
  }
  
}
