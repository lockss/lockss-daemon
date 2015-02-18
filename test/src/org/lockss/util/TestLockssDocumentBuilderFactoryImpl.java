/*
 * $Id$
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.lockss.util.*;
import org.lockss.test.*;

/**
 * Test class for org.lockss.util.LockssDocumentBuilderFactoryImpl
 */
public class TestLockssDocumentBuilderFactoryImpl extends LockssTestCase {

  static String SERVICE_FILE =
    "/META-INF/services/javax.xml.parsers.DocumentBuilderFactory";

  public void testGetFeature() throws Exception {
    LockssDocumentBuilderFactoryImpl fact =
      new LockssDocumentBuilderFactoryImpl();
    try {
      fact.getFeature("random_feature");
      // unlikely, but it's ok if it succeeds
    } catch (ParserConfigurationException e) {
      // expected if getFeature is supported
    } catch (RuntimeException e) {
      // expected if getFeature is not supported
      if (!(e.getCause() instanceof NoSuchMethodException)) {
	throw e;
      }
    }
  }

  public void testSetFeature() throws Exception {
    LockssDocumentBuilderFactoryImpl fact =
      new LockssDocumentBuilderFactoryImpl();
    try {
      fact.setFeature("random_feature", true);
      // unlikely, but it's ok if it succeeds
    } catch (ParserConfigurationException e) {
      // expected if getFeature is supported
    } catch (RuntimeException e) {
      // expected if getFeature is not supported
      if (!(e.getCause() instanceof NoSuchMethodException)) {
	throw e;
      }
    }
  }

  // ensure proper services definition file is found on classpath
  public void testServiceFile() {
    URL res = getResource(SERVICE_FILE);
    assertMatchesRE("/lockss.jar", res.toString());
  }

  // ensure proper DocumentBuilderFactory is loaded
  public void testCorrectFactory() {
    DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
    assertTrue("expected a LockssDocumentBuilderFactoryImpl, was " + fact,
	       fact instanceof LockssDocumentBuilderFactoryImpl);
  }

  public void assertContainsMatchForRe(String expRe, Collection coll) {
    for (Iterator iter = coll.iterator(); iter.hasNext(); ) {
      String str = (String)iter.next();
      if (isMatchRe(str, expRe)) {
	return;
      }
    }
    fail("No match for " + expRe + " in " + coll);
  }

  public void testLog() throws Exception {
    Logger l =
      Logger.getLogger(LockssDocumentBuilderFactoryImpl.ERROR_LOGGER_NAME);
    MockLogTarget target = new MockLogTarget();
    Logger.setTarget(target);

    DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = fact.newDocumentBuilder();
    try {
      builder.parse(new StringInputStream(""));
      fail("Expected parse of empty string to throw SAXParseException");
    } catch (SAXParseException e) {
      // this string depends on parser, may need to be changed
      assertContainsMatchForRe("Premature end of file", target.getMessages());
    }
  }
}
