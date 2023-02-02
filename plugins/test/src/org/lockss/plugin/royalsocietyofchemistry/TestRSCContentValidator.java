/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
