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
import javax.xml.parsers.DocumentBuilder;

import org.lockss.test.LockssTestCase;

import org.w3c.dom.*;
import org.apache.xml.serialize.*;

public class TestXmlDomBuilder extends LockssTestCase {
  XmlDomBuilder builder;
  Document document;
  Element rootElem;

  public void setUp() throws Exception {
    super.setUp();

    builder = new XmlDomBuilder();
    document = XmlDomBuilder.createDocument();
    rootElem = builder.createRoot(document, "testroot");
  }

  public void testGetDocumentBuilder() throws Exception {
    DocumentBuilder domBuilder = XmlDomBuilder.getDocumentBuilder();
    assertNotNull(domBuilder);
  }

  public void testCreateRoot() {
    assertEquals("testroot", rootElem.getLocalName());
    assertEquals(builder.makeTag("testroot"), rootElem.getNodeName());
    assertEquals(document.getDocumentElement(), rootElem);
  }

  public void testCreateElement() {
    Element childElem = builder.createElement(rootElem, "child");
    assertEquals("child", childElem.getLocalName());
    assertEquals(rootElem.getFirstChild(), childElem);
  }

  public void testAddText() {
    XmlDomBuilder.addText(rootElem, "test text string");
    Node childNode = rootElem.getFirstChild();
    assertEquals(Element.TEXT_NODE, childNode.getNodeType());
    assertEquals("test text string", childNode.getNodeValue());
  }

  public void testGetText() {
    XmlDomBuilder.addText(rootElem, "test text string");
    assertEquals("test text string", XmlDomBuilder.getText(rootElem));
  }

  public void testCopyDocument() throws Exception {
    Document doc2 = XmlDomBuilder.createDocument();
    Element root2 = builder.createRoot(doc2, "root2");
    assertNull(root2.getFirstChild());
    XmlDomBuilder.copyDocument(document, doc2);
    assertEquals(rootElem.getNodeName(), root2.getFirstChild().getNodeName());
  }

  public void testAttributes() {
    assertEquals("", rootElem.getAttribute("attr1"));
    builder.setAttribute(rootElem, "attr1", "value1");
    assertEquals("value1", rootElem.getAttribute(builder.makeTag("attr1")));
    assertEquals("value1", builder.getAttribute(rootElem, "attr1"));
  }

  public void testGetElements() {
    builder.createElement(rootElem, "elem1");
    builder.createElement(rootElem, "elem2");
    builder.createElement(rootElem, "elem1");
    NodeList elemList = builder.getElementList(rootElem, "elem1");
    assertEquals(2, elemList.getLength());
    Element elem = builder.getElement(rootElem, "elem2");
    assertEquals("elem2", elem.getLocalName());
  }

  public void testRemoveNode() {
    Element child = builder.createElement(rootElem, "elem1");
    assertEquals("elem1", rootElem.getFirstChild().getLocalName());
    builder.removeNode(child);
    assertNull(rootElem.getFirstChild());
  }

  public void testGetFormatter() {
    OutputFormat formatter = XmlDomBuilder.getFormatter();
    assertEquals(XmlDomBuilder.ENCODING, formatter.getEncoding());
    assertEquals(Method.XML, formatter.getMethod());
  }

  public void testSerializeStream() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    XmlDomBuilder.serialize(document, baos);

    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    Document doc2 = XmlDomBuilder.parseXmlStream(bais);
    assertEquals(rootElem.getNodeName(),
                 doc2.getDocumentElement().getNodeName());
  }

  public void testSerializeWriter() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Writer writer = new OutputStreamWriter(baos);
    XmlDomBuilder.serialize(document, writer);

    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    Reader reader = new InputStreamReader(bais);
    Document doc2 = XmlDomBuilder.parseXmlStream(reader);
    assertEquals(rootElem.getNodeName(),
                 doc2.getDocumentElement().getNodeName());
  }

  public void testSerializeFile() throws Exception {
    String tempDirFile = getTempDir() + File.separator + "serfile";
    XmlDomBuilder.serialize(document, tempDirFile);

    Document doc2 = XmlDomBuilder.parseXmlFile(tempDirFile);
    assertEquals(rootElem.getNodeName(),
                 doc2.getDocumentElement().getNodeName());
  }

  public void testSerializeString() throws Exception {
    String serStr = XmlDomBuilder.serialize(document);

    Document doc2 = XmlDomBuilder.parseXmlString(serStr);
    assertEquals(rootElem.getNodeName(),
                 doc2.getDocumentElement().getNodeName());
  }

}
