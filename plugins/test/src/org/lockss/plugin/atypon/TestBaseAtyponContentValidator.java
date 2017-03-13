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
import org.lockss.config.*;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.*;

// super class for this plugin - variants defined within it
public class TestBaseAtyponContentValidator extends LockssTestCase {
  
  static Logger log = Logger.getLogger(TestBaseAtyponContentValidator.class);
  
//  private MockLockssDaemon theDaemon;
  
  private static BaseAtyponContentValidator.TextTypeValidator contentValidator;
  
  static final String PLUGIN_NAME = "org.lockss.plugin.atypon.BaseAtyponPlugin";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String ROOT_URL = "http://www.BaseAtypon.com/"; //this is not a real url
  static final String ROOT_HOST = "www.BaseAtypon.com"; //this is not a real url
 
  private static final String TEXT = "text";
  private static final int LEN = TEXT.length();
  private static final String TEXT_CONTENT_TYPE = "text/html; charset=utf-8";
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
      BASE_URL_KEY, ROOT_URL,
      VOL_KEY, "1");
  
  private MockArchivalUnit mau;
  
  private String urlStr1;
  private String urlStr2;
  private String urlStr3;
  private String urlStr4;
  private String urlStr5;
  
  public void setUp() throws Exception {
    super.setUp();
    
    mau = new MockArchivalUnit();
    mau.setConfiguration(AU_CONFIG);
    
    ContentValidatorFactory cvfact = new BaseAtyponContentValidator.Factory();
    contentValidator = (BaseAtyponContentValidator.TextTypeValidator) cvfact.createContentValidator(mau, "text/html");
    if (contentValidator == null) 
      fail("contentValidator == null");
    
    urlStr1 = ROOT_URL + "doi/full/10.1108/XYZ-01-2017-00418";
    urlStr2 = ROOT_URL + "doi/pdfx/10.1108/XYZ-01-2017-00418";
    urlStr3 = ROOT_URL + "doi/pdfplus/10.1108/XYZ-01-2017-00418";
    urlStr4 = ROOT_URL + "doi/pdf/10.1108/XYZ-01-2017-00418";
    urlStr5 = ROOT_URL + "doi/pdf/10.1108/XYZ-01-2017-00418.pdf";
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
    
    cu = new MockCachedUrl(urlStr4, mau);
    cu.setContent(TEXT);
    cu.setContentSize(LEN);
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, TEXT_CONTENT_TYPE);
    try {
      contentValidator.validate(cu);
      fail("Bad cu should throw exception " + cu.getUrl());
    } catch (Exception e) {
      // okay, fall-thru
    }
    cu = new MockCachedUrl(urlStr5, mau);
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
