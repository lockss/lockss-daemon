/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;

import org.lockss.test.LockssTestCase;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

public class TestXPathUtil extends LockssTestCase {

  public static final String XML =
      "<?xml version=\"1.0\"?>" +
      "<myroot>" +
      "  <myboolean>unicorn</myboolean>" +
      "  <mynode>Node 1</mynode>" +
      "  <mynodeset>Nodeset 1</mynodeset>" +
      "  <mynodeset>Nodeset 2</mynodeset>" +
      "  <mynodeset>Nodeset 3</mynodeset>" +
      "  <mynumber>0.8675309</mynumber>" +
      "  <mystring>Hello world</mystring>" +
      "</myroot>";
  
  protected XPath xpath;
  
  public void setUp() throws Exception {
    super.setUp();
    xpath = XPathFactory.newInstance().newXPath();
  }
  
  public void testEachType() throws Exception {    
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(XML)));
    assertFalse(XPathUtil.evaluateBoolean(xpath.compile("/myroot/unknown"), doc));
    assertTrue(XPathUtil.evaluateBoolean(xpath.compile("/myroot/myboolean"), doc));
    assertTrue(XPathUtil.evaluateBoolean(xpath.compile("/myroot/myboolean[text()='unicorn']"), doc));
    assertTrue(XPathUtil.evaluateBoolean(xpath.compile("/myroot/myboolean[text()!='mermaid']"), doc));
    assertTrue(XPathUtil.evaluateBoolean(xpath.compile("/myroot/mynodeset[text()='Nodeset 2']"), doc));
    assertTrue(XPathUtil.evaluateBoolean(xpath.compile("/myroot/mynodeset"), doc));
    assertNull(XPathUtil.evaluateNode(xpath.compile("/myroot/unknown"), doc));
    assertEquals("Node 1", XPathUtil.evaluateNode(xpath.compile("/myroot/mynode"), doc).getTextContent());
    assertEquals(0, XPathUtil.evaluateNodeSet(xpath.compile("/myroot/unknown"), doc).getLength());
    assertEquals(1, XPathUtil.evaluateNodeSet(xpath.compile("/myroot/mynode"), doc).getLength());
    assertEquals(3, XPathUtil.evaluateNodeSet(xpath.compile("/myroot/mynodeset"), doc).getLength());
    assertEquals(0.8675309, XPathUtil.evaluateNumber(xpath.compile("/myroot/mynumber"), doc));
    assertEquals(0, XPathUtil.evaluateNumber(xpath.compile("/myroot/mynumber"), doc).intValue());
    assertTrue(XPathUtil.evaluateNumber(xpath.compile("/myroot/myboolean"), doc).isNaN());
    assertTrue(XPathUtil.evaluateNumber(xpath.compile("/myroot/unknown"), doc).isNaN());
    assertEquals("Hello world", XPathUtil.evaluateString(xpath.compile("/myroot/mystring"), doc));
    assertEquals("Nodeset 1", XPathUtil.evaluateString(xpath.compile("/myroot/mynodeset"), doc));
    assertEquals("", XPathUtil.evaluateString(xpath.compile("/myroot/unknown"), doc));
  }
  
}
