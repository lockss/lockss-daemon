/*
 * $Id$
 */

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair.oup;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.CacheException;

import java.util.Properties;

import org.lockss.config.*;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.*;
//import org.lockss.plugin.base.MimeTypeContentValidator;
//import org.lockss.plugin.base.MimeTypeContentValidatorFactory;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.plugin.definable.DefinablePlugin;

// super class for this plugin - variants defined within it
public class TestOupScContentValidator extends LockssTestCase {

	static Logger log = Logger.getLogger(TestOupScContentValidator.class);

	private MockLockssDaemon theDaemon;

	static final String PLUGIN_ID = "org.lockss.plugin.silverchair.oup.ClockssOupSilverchairPlugin";
	static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
    static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
    static final String JRNL_ID_KEY = "journal_id";
    public static final String BASE_URL = "http://www.example.com/";

    private static final String SOMETEXT1 = "----------------------text goes here--------------------------------";
    private static final String SOMETEXT2 = "--Sorry for the inconvenience, we are performing some maintenance at the moment. We will be back online shortly--";
    private static final String SOMETEXT3 = "-----48_3_cover.png?Expires=2147483647&amp;Signature=---------------";
    private static final String SOMETEXT4 = "-----48_3_cover.png?Expires=9876543210&amp;Signature=---------------";
    private static final int LEN = SOMETEXT1.length();

    private DefinableArchivalUnit defAu;

    static final String htmlUrls[] =
      {
          BASE_URL + "journals/jid/article-abstract/00418",
          BASE_URL + "journals/jid/fullarticle/00418",
          BASE_URL + "journals/jid/fullarticle/00418",
          BASE_URL + "view-large/figure/44950976/lfv07701.jpeg",
      };
    static final String pdfUrls[] =
      {
          BASE_URL + "journals/jid/articlepdf/00417/evp1500002.pdf",
          BASE_URL + "doi/pdf/10.1108/XYZ-01-2017-00418.pdf",
      };
    static final String imageUrls[] =
      {
          BASE_URL + "UI/app/img/favicon-32x32.png",
          BASE_URL + "journals/jid/article/00417/art001.jpeg",
          BASE_URL + "journals/jid/article/00417/art001.jpg",
          BASE_URL + "journals/jid/article/00417/art001.png",
      };
    static final String unknownUrls[] =
      {
          BASE_URL + "toc/foo/1/12",
          BASE_URL + "doi/pdfx/10.1108/XYZ-01-2017-00418",
          BASE_URL + "action/downloadCitation?doi=10.3362%2F9780855988692&format=ris&include=cit",
          BASE_URL + "na101/home/literatum/publisher/blah/foo.jpg",
      };



    public void setUp() throws Exception {
      super.setUp();
      theDaemon = getMockLockssDaemon();

      Properties props = new Properties();
      props.setProperty(YEAR_KEY, "2016");
      props.setProperty(JRNL_ID_KEY, "jid");
      props.setProperty(BASE_URL_KEY, BASE_URL);
      Configuration config = ConfigurationUtil.fromProps(props);

      DefinablePlugin ap = new DefinablePlugin();
      ap.initPlugin(getMockLockssDaemon(), PLUGIN_ID);
      defAu = (DefinableArchivalUnit)ap.createAu(config);
    }

    ContentValidator getValidator(String mimeType) {
      ContentValidatorFactory fact =
          defAu.getContentValidatorFactory(mimeType);
      return fact.createContentValidator(defAu, mimeType);
    }

    public void testPluginSetup() throws Exception {
      // make sure a valid map is specified in the plugin
      PatternStringMap urlMimeValidationMap = defAu.makeUrlMimeValidationMap();
      if (urlMimeValidationMap.isEmpty())
        log.warning("au_url_mime_validation_map not set up correctly");

      // make sure that a default mime type validator factory gets set up for all mime types
      String typeList[] = {"text/html", "application/pdf", "foo/blah", };
      for (String theType : typeList) {
        ContentValidatorFactory deffact = defAu.getContentValidatorFactory(theType);
        assertNotNull("plugin's default Mime Type Validator Factory was null for type " + theType, deffact);
        ContentValidator defValidator = deffact.createContentValidator(defAu, theType);
        assertNotNull("plugin's default Mime Type Validator was null", defValidator);
        log.warning(defValidator.getClass().getSimpleName());
      }
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

      ContentValidator defValidator = getValidator("text/html");
      //cu = new MockCachedUrl(htmlUrls[0], defAu);
      for (String urlString : htmlUrls) {
        cu = new MockCachedUrl(urlString, defAu);
        cu.setContentSize(LEN);
        cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);
        cu.setContent(SOMETEXT1);
        // should NOT throw
        defValidator.validate(cu);
        cu.setContent(SOMETEXT2);
        // SHOULD throw
        try {
          defValidator.validate(cu);
          fail("Bad cu should throw exception " + cu.getUrl());
        } catch (Exception e) {
          log.warning(urlString, e);
          // okay, fall-thru
        }
        cu.setContent(SOMETEXT3);
        // should NOT throw
        defValidator.validate(cu);
        cu.setContent(SOMETEXT4);
        // maybe SHOULD throw XXX
        try {
          defValidator.validate(cu);
          // XXX fail("Bad cu should throw exception " + cu.getUrl());
        } catch (Exception e) {
          log.warning(urlString, e);
          // okay, fall-thru
        }
      }
    }

    public void testPdfUrls() throws Exception {
      MockCachedUrl cu;

      ContentValidator textValidator = getValidator("text/*");
      ContentValidator pdfValidator = getValidator("application/pdf");
      for (String urlString : pdfUrls) {
        cu = new MockCachedUrl(urlString, defAu);
        cu.setContent(SOMETEXT1);
        cu.setContentSize(LEN);
        // SHOULD throw
        cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);
        try {
          textValidator.validate(cu);
          fail("Bad cu should throw exception " + cu.getUrl());
        } catch (Exception e) {
          log.warning(urlString, e);
          // okay, fall-thru
        }
        // should NOT throw
        cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_PDF);
        pdfValidator.validate(cu);
      }
    }

    public void testOtherContentType() throws Exception {
      MockCachedUrl cu;

      ContentValidator defValidator = getValidator("*/*");
      // we know nothing about these patterns - allow any mime type at all
      for (String urlString : unknownUrls) {
        cu = new MockCachedUrl(urlString, defAu);
        cu.setContent(SOMETEXT1);
        cu.setContentSize(LEN);
        cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);
        // should NOT throw
        defValidator.validate(cu);
        cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_PDF);
        // should NOT throw
        defValidator.validate(cu);
        cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_XML);
        // should NOT throw
        defValidator.validate(cu);
      }
    }

    public void testImageContentType() throws Exception {
      MockCachedUrl cu;

      ContentValidator defValidator = getValidator("image/png");
      for (String urlString : imageUrls) {
        cu = new MockCachedUrl(urlString, defAu);
        cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "image/png");
        // should NOT throw
        defValidator.validate(cu);
        cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);
        try {
          defValidator.validate(cu);
          log.warning("No Exception for:" + urlString);
        } catch (ContentValidationException e) {
          // ought to be MimeType exception
          assertClass(ContentValidationException.LogOnly.class, e);
        } catch (CacheException e) {
          // XXX for possible future testing
          assertClass(CacheException.RetryableException.class, e);
        } catch (Exception e) {
          log.warning(urlString, e);
          // okay, fall-thru
        }
      }
    }
}

