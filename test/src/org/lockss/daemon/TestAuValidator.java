/*
 * $Id$
 */

/*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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


package org.lockss.daemon;

import java.io.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.daemon.AuValidator.*;
import org.lockss.plugin.*;

public class TestAuValidator extends LockssTestCase {
  static Logger log = Logger.getLogger("TestAuValidator");

  static String STEM = "http://example.com/path/";

  MockArchivalUnit mau;
  AuValidator auv;


  public void setUp() throws Exception {
    super.setUp();
    mau = new MockArchivalUnit();
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    cus.setHashItSource(Collections.EMPTY_LIST);
    cus.setFlatItSource(Collections.EMPTY_LIST);

    auv = new AuValidator(mau);
//     mau.setStartUrls(STARTS);
//     mau.setPermissionUrls(PERMS);
  }

  List<CachedUrl> added = new ArrayList<CachedUrl>();

  void addCu(MockArchivalUnit au, String url, String content, String fail) {
    CIProperties props = new CIProperties();
    if (fail != null) {
      props.setProperty("fail", fail);
    }
    MockCachedUrl mcu = au.addUrl(url, true, true, props);
    mcu.setContent(content);
    added.add(mcu);
  }

  void setUpAu(MockArchivalUnit au, int num) throws Exception {
    for (int ix = 1; ix <= num; ix++) {
      addCu(au, STEM + ix, "content" + ix,
	    (ix % 3) == 0 ? ("validate fail " + ix) : null);
    }

    MockCachedUrlSet cus = (MockCachedUrlSet)au.getAuCachedUrlSet();
    cus.setHashItSource(added);
    cus.setFlatItSource(added);
  }

  public void testEmpty() throws Exception {
    Result res = auv.validateAu();
    assertFalse(res.hasValidationFailures());
    assertFalse(res.hasError());
    assertEmpty(res.getValidationFailures());
    assertEquals(0, res.numFiles());
    assertEquals(0, res.numValidations());
    assertEquals(0, res.numValidationFailures());
  }

  public void testNoValidator() throws Exception {
    setUpAu(mau, 2);
    Result res = auv.validateAu();
    assertFalse(res.hasValidationFailures());
    assertFalse(res.hasError());
    assertEmpty(res.getValidationFailures());
    assertEquals(2, res.numFiles());
    assertEquals(0, res.numValidations());
    assertEquals(0, res.numValidationFailures());
  }

  public void testNoFailures() throws Exception {
    setUpAu(mau, 2);
    mau.setContentValidatorFactory(new MyContentValidatorFactory());
    Result res = auv.validateAu();
    assertFalse(res.hasValidationFailures());
    assertFalse(res.hasError());
    assertEquals(2, res.numFiles());
    assertEquals(2, res.numValidations());
    assertEquals(0, res.numValidationFailures());
  }

  public void test1Failure() throws Exception {
    setUpAu(mau, 4);
    mau.setContentValidatorFactory(new MyContentValidatorFactory());
    Result res = auv.validateAu();
    assertTrue(res.hasValidationFailures());
    assertFalse(res.hasError());
    assertEquals(4, res.numFiles());
    assertEquals(4, res.numValidations());
    assertEquals(1, res.numValidationFailures());
    ValidationFailure vf = res.getValidationFailures().get(0);
    assertEquals(STEM + "3", vf.getUrl());
    assertMatchesRE("WrongLength.*validate fail 3", vf.getMessage());
  }


  static class MyContentValidatorFactory implements ContentValidatorFactory {
    public ContentValidator createContentValidator(ArchivalUnit au,
						   String contentType) {
      return new MyContentValidator(au, contentType);
    }
  }
    
  static class MyContentValidator implements ContentValidator {
    public MyContentValidator(ArchivalUnit au,
			      String contentType) {
    }

    public void validate(CachedUrl cu)
	throws ContentValidationException, PluginException, IOException {

      CIProperties props = cu.getProperties();
      if (props.containsKey("fail")) {
	throw new ContentValidationException.WrongLength(props.getProperty("fail"));
      }
    }
  }

}
