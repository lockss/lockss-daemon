/*
 * $Id$
 */

/*

Copyright (c) 2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.royalsocietyofchemistry;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.*;

// super class for this plugin - variants defined within it
public class TestRSCContentValidator extends LockssTestCase {
  
  static Logger log = Logger.getLogger(TestRSCContentValidator.class);
  
//  private MockLockssDaemon theDaemon;
  
  private static ContentValidator contentValidator;
  
  private static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  private static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private static final String BASE_URL = "http://www.example.com/";
  private static final String TEXT = "text";
  private static final int LEN = TEXT.length();
  private static final String TEXT_CONTENT_TYPE = "text/html; charset=utf-8";
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
      BASE_URL_KEY, BASE_URL,
      VOLUME_NAME_KEY, "1");
  
  private MockArchivalUnit mau;
  
  private String urlStr1;
  private String urlStr2;
  private String urlStr3;
  
  public void setUp() throws Exception {
    super.setUp();
    
    mau = new MockArchivalUnit();
    mau.setConfiguration(AU_CONFIG);
    
    ContentValidatorFactory cvfact = new RSCContentValidator.Factory();
    contentValidator = cvfact.createContentValidator(mau, "text/html");
    if (contentValidator == null) 
      fail("contentValidator == null");
    
    urlStr1 = "http://xlink.rsc.org/?doi=c3gc90050a";
    urlStr2 = BASE_URL + "en/content/articlelanding/2014/gc/c3gc90050a";
    urlStr3 = BASE_URL + "en/content/articlepdf/2014/gc/c3gc90050a";
  }
  
  public void tearDown() throws Exception {
    super.tearDown();
  }
  
  
  /**
   * Method that creates a simulated Cached URL with appropriate content-type 
   * @throws Exception
   */
  public void testForTextContent() throws Exception {
    MockCachedUrl cu;
    
    setUp();
    cu = new MockCachedUrl(urlStr1, mau);
    cu.setContent(TEXT);
    cu.setContentSize(LEN);
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, TEXT_CONTENT_TYPE);
    contentValidator.validate(cu);
    cu = new MockCachedUrl(urlStr2, mau);
    cu.setContent(TEXT);
    cu.setContentSize(LEN);
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, TEXT_CONTENT_TYPE);
    contentValidator.validate(cu);
    
    cu = new MockCachedUrl(urlStr3, mau);
    cu.setContent(TEXT);
    cu.setContentSize(LEN);
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, TEXT_CONTENT_TYPE);
    try {
      contentValidator.validate(cu);
      fail("Bad cu should throw exception " + cu.getUrl());
    } catch (Exception e) {
      // okay, fall-thru
    }
    
  }
  
}
