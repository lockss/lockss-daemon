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

package org.lockss.plugin.atypon;

import org.lockss.test.*;
import org.lockss.util.*;

import java.util.Properties;

import org.lockss.config.*;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.*;
import org.lockss.plugin.base.MimeTypeContentValidator;
import org.lockss.plugin.base.MimeTypeContentValidatorFactory;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.plugin.definable.DefinablePlugin;

// super class for this plugin - variants defined within it
public class TestBaseAtyponContentValidator extends LockssTestCase {

	static Logger log = Logger.getLogger(TestBaseAtyponContentValidator.class);


	private MockLockssDaemon theDaemon;

	static final String PLUGIN_ID = "org.lockss.plugin.atypon.BaseAtyponPlugin";
	static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
	static final String JID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
	static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
	static final String ROOT_URL = "http://www.BaseAtypon.com/"; //this is not a real url

	private static final String SOMETEXT = "text goes here";
	private static final int LEN = SOMETEXT.length();

	private DefinableArchivalUnit BA_au;

	static final String htmlUrls[] =
		{
				ROOT_URL + "doi/abs/10.1108/XYZ-01-2017-00418",
				ROOT_URL + "doi/full/10.1108/XYZ-01-2017-00418",
		};
	static final String pdfUrls[] =
		{
				ROOT_URL + "doi/pdfplus/10.1108/XYZ-01-2017-00418",
				ROOT_URL + "doi/pdf/10.1108/XYZ-01-2017-00418",
				ROOT_URL + "doi/pdf/10.1108/XYZ-01-2017-00418.pdf",
		};
	static final String unknownUrls[] =
		{
				ROOT_URL + "toc/foo/1/12",
				ROOT_URL + "doi/pdfx/10.1108/XYZ-01-2017-00418",
				ROOT_URL + "action/downloadCitation?doi=10.3362%2F9780855988692&format=ris&include=cit",
				ROOT_URL + "na101/home/literatum/publisher/blah/foo.jpg",
		};



	public void setUp() throws Exception {
		super.setUp();
		setUpDiskSpace();
		theDaemon = getMockLockssDaemon();
		theDaemon.getHashService();	

		Properties props = new Properties();
		props.setProperty(VOL_KEY, "1");
		props.setProperty(JID_KEY, "foo");
		props.setProperty(BASE_URL_KEY, ROOT_URL);
		Configuration config = ConfigurationUtil.fromProps(props);

		DefinablePlugin ap = new DefinablePlugin();
		ap.initPlugin(getMockLockssDaemon(),
				PLUGIN_ID);
		BA_au = (DefinableArchivalUnit)ap.createAu(config);
	}


  ContentValidator getValidator() {
    String mimeType = "*/*";
    ContentValidatorFactory fact =
      BA_au.getContentValidatorFactory(mimeType);
    return fact.createContentValidator(BA_au, mimeType);
  }    

	public void testPluginSetup() throws Exception {	
		// make sure a valid map is specified in the plugin
		PatternStringMap urlMimeValidationMap = BA_au.makeUrlMimeValidationMap();
		
		if (urlMimeValidationMap.isEmpty())
			fail("au_url_mime_validation_map not set up correctly");

		// make sure that a default mime type validator factory gets set up for all mime types
		String typeList[] = {"text/html", "application/pdf", "foo/blah", };
		for (String theType : typeList) {
			ContentValidatorFactory deffact = BA_au.getContentValidatorFactory(theType);
			assertNotNull("plugin's default Mime Type Validator Factory was null for type " + theType, deffact);
			ContentValidator defValidator =
			  deffact.createContentValidator(BA_au, theType);
			assertNotNull("plugin's default Mime Type Validator was null", defValidator);
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

		ContentValidator defValidator = getValidator();
		for (String urlString : htmlUrls) {
			cu = new MockCachedUrl(urlString, BA_au);
			cu.setContent(SOMETEXT);
			cu.setContentSize(LEN);
			cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);
			// should NOT throw
			defValidator.validate(cu);
			// SHOULD throw
			cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_PDF);
			try {
				defValidator.validate(cu);
				fail("Bad cu should throw exception " + cu.getUrl());
			} catch (Exception e) {
				// okay, fall-thru
			}    
			cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_XML);
			try {
				defValidator.validate(cu);
				fail("Bad cu should throw exception " + cu.getUrl());
			} catch (Exception e) {
				// okay, fall-thru
			}    
		}
	}

	public void testPdfContentType() throws Exception {
		MockCachedUrl cu;

		ContentValidator defValidator = getValidator();
		for (String urlString : pdfUrls) {
			cu = new MockCachedUrl(urlString, BA_au);
			cu.setContent(SOMETEXT);
			cu.setContentSize(LEN);
			cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_PDF);
			// should NOT throw
			defValidator.validate(cu);
			// SHOULD throw
			cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);
			try {
				defValidator.validate(cu);
				fail("Bad cu should throw exception " + cu.getUrl());
			} catch (Exception e) {
				// okay, fall-thru
			}    
			cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_RIS);
			try {
				defValidator.validate(cu);
				fail("Bad cu should throw exception " + cu.getUrl());
			} catch (Exception e) {
				// okay, fall-thru
			}    
		}
	}

	public void testOtherContentType() throws Exception {
		MockCachedUrl cu;

		ContentValidator defValidator = getValidator();
		// we know nothing about these patterns - allow any mime type at all
		for (String urlString : unknownUrls) {
			cu = new MockCachedUrl(urlString, BA_au);
			cu.setContent(SOMETEXT);
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


}
