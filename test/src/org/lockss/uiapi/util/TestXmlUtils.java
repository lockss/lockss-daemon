/*
 * $Id: TestXmlUtils.java,v 1.4 2005-10-11 05:52:20 tlipkis Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.uiapi.util;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.*;

import org.apache.xerces.dom.*;
import org.apache.xml.serialize.*;

import junit.framework.TestCase;
import org.lockss.test.*;


public class TestXmlUtils extends LockssTestCase implements ApiParameters {

  final private static String LAST_TEXT     = "last element text";
  final private static String PLAIN_TEXT    = "text";
  final private static String ENCODED_TEXT  = "dGV4dA==";

  private String      xml;
  private Document    document;
  private XmlUtils    xmlUtils;

  public TestXmlUtils(String message) {
    super(message);
  }

  private static String tag(String name) {
    return (AP_NS_PREFIX + ":" + name);
  }

  private static Document parse(String xml) throws Exception {

    DocumentBuilderFactory factory;
    InputSource stringIn;

    stringIn = new InputSource(new StringReader(xml));
    factory  = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);

    return factory.newDocumentBuilder().parse(stringIn);
  }

  public void setUp() throws Exception {
    super.setUp();

    xml = "<" + AP_NS_PREFIX + ":top " + AP_NS_PREFIX + ":"
    +     COM_XML_VERSIONNAME + "=\"" + AP_XML_VERSION + "\" "
    +     "xmlns:" + AP_NS_PREFIX + "=\"" + AP_NS_URI + "\">"
    +     ENCODED_TEXT
    +     "<"   + AP_NS_PREFIX + ":second>2nd"
    +     "<"   + AP_NS_PREFIX + ":third>3rd"
    +     "</"  + AP_NS_PREFIX + ":third>"
    +     "</"  + AP_NS_PREFIX + ":second>"
    +     "<"   + AP_NS_PREFIX + ":last>"
    +     LAST_TEXT
    +     "</"  + AP_NS_PREFIX + ":last>"
    +     "<"   + AP_NS_PREFIX + ":last/>"
    +     "<"   + AP_NS_PREFIX + ":last/>"
    +     "</"  + AP_NS_PREFIX + ":top>";

    document = parse(xml);
    xmlUtils = new XmlUtils(AP_NS_PREFIX, AP_NS_URI, AP_XML_VERSION);
  }

  /*
   * createDocument(), createRoot()
   *
   * Success if the root element has the expected name and namespace URI,
   * and the correct document version number is in place
   */
  public void testCreateDocumentAndRoot() throws Exception {

    Element root;
    String  version;

    root = xmlUtils.createRoot(XmlUtils.createDocument(), "myroot");

    assertTrue(root.getNamespaceURI().equals(AP_NS_URI));
    assertEquals(root.getTagName(), tag("myroot"));

    version = root.getAttributeNS(AP_NS_URI, COM_XML_VERSIONNAME);
    assertTrue(version.equals(AP_XML_VERSION));
  }

  /*
   * parseXmlString()
   *
   * Success if the root element has the expected namespace URI and name
   */
  public void testParseXmlString() throws Exception {
    Document doc;
    Element  root;

    doc  = XmlUtils.parseXmlString(xml);
    root = doc.getDocumentElement();

    assertTrue(root.getNamespaceURI().equals(AP_NS_URI));
    assertEquals(root.getTagName(), tag("top"));
  }

  /*
   * parseXmlStream(InputStream)
   *
   * Success if the root element has the expected namespace URI and name
   */
  public void testParseXmlStream() throws Exception {

    Document doc;
    Element  root;

    doc  = XmlUtils.parseXmlStream(new ByteArrayInputStream(xml.getBytes()));
    root = doc.getDocumentElement();

    assertTrue(root.getNamespaceURI().equals(AP_NS_URI));
    assertEquals(root.getTagName(), tag("top"));
  }

  /*
   * parseXmlStream(Reader)
   *
   * Success if the root element has the expected namespace URI
   * and name
   */
  public void testParseXmlReader() throws Exception {

    Document doc;
    Element  root;

    doc = XmlUtils.parseXmlStream(new StringReader(xml));
    root = doc.getDocumentElement();

    assertTrue(root.getNamespaceURI().equals(AP_NS_URI));
    assertEquals(root.getTagName(), tag("top"));
  }

  /*
   * AddText()
   */
  public void testAddText() {
    Element   element;
    NodeList  nodeList;
    String    text;

    nodeList = xmlUtils.getList(document.getDocumentElement(), "last");
    assertEquals(nodeList.getLength(), 3);

    element = (Element) nodeList.item(2);
    XmlUtils.addText(element, PLAIN_TEXT);

    text = element.getFirstChild().getNodeValue();
    assertEquals(PLAIN_TEXT, text);
  }

  /*
   * AddEncodedText()
   */
  public void testAddEncodedText() {
    Element   element;
    NodeList  nodeList;
    String    text;

    nodeList = xmlUtils.getList(document.getDocumentElement(), "last");
    assertEquals(nodeList.getLength(), 3);

    element = (Element) nodeList.item(2);
    XmlUtils.addEncodedText(element, PLAIN_TEXT);

    text = element.getFirstChild().getNodeValue();
    assertEquals(ENCODED_TEXT, text);
  }

  /*
   * getText()
   */
  public void testGetText() throws Exception {
    String text = XmlUtils.getText(document.getDocumentElement());

    assertEquals(ENCODED_TEXT, text);
  }

  /*
   * getEncodedText()
   */
  public void testGetEncodedText() throws Exception {
    String text = XmlUtils.getEncodedText(document.getDocumentElement());

    assertEquals(PLAIN_TEXT, text);
  }

  /*
   * getList()
   *
   * Success if we find the anticipated number of matches on each node name
   */
  public void testGetList() {
    NodeList  nodeList;

    nodeList = xmlUtils.getList(document.getDocumentElement(), "xxxx");
    assertEquals(nodeList.getLength(), 0);

    nodeList = xmlUtils.getList(document.getDocumentElement(), "last");
    assertEquals(nodeList.getLength(), 3);
  }

  /*
   * getElement()
   *
   * Success if we find a match with the anticipated name and value
   */
  public void testGetElement() {
    Element element;
    element = xmlUtils.getElement(document.getDocumentElement(), "last");

    assertNotNull(element);
    assertEquals(element.getTagName(), tag("last"));
    assertEquals(element.getFirstChild().getNodeValue(), LAST_TEXT);
  }

  /*
   * getAttribute()
   *
   * Success if we find a match with the anticipated value
   */
  public void testGetAttribute() {
    String attribute;

    attribute = xmlUtils.getAttribute(document.getDocumentElement(),
                                      COM_XML_VERSIONNAME);
    assertTrue(attribute.length() > 0);
    assertEquals(attribute, AP_XML_VERSION);
  }

  /*
   * removeNode()
   *
   * Success if the node turns up missing after removal
   */
  public void testRemoveNode() {
    Element   element;
    NodeList  nodeList;
    String    text;

    nodeList = xmlUtils.getList(document.getDocumentElement(), "last");
    assertEquals(nodeList.getLength(), 3);

    element = (Element) nodeList.item(2);
    xmlUtils.removeNode(element);

    nodeList = xmlUtils.getList(document.getDocumentElement(), "last");
    assertEquals(nodeList.getLength(), 2);
  }

  /*
   * serialize() to OutputStream
   *
   * Success if we can serialze and parse the result to create a new DOM
   */
  public void testSerializeToStream() throws Exception {

    ByteArrayOutputStream	stream;
    Document newDoc;
    Element root;

    stream = new ByteArrayOutputStream();
    XmlUtils.serialize(document, stream);

    newDoc = parse(new String(stream.toByteArray()));
    root   = newDoc.getDocumentElement();

    assertTrue(root.getNamespaceURI().equals(AP_NS_URI));
    assertEquals(root.getTagName(), tag("top"));
  }

  /*
   * serialize() to Writer
   *
   * Success if we can serialze, re-parse, and find the expected
   * root and namespace URI
   */
  public void testSerializeToWriter() throws Exception {

    StringWriter writer;
    Document newDoc;
    Element  root;


    writer = new StringWriter();
    XmlUtils.serialize(document, writer);

    newDoc = parse(writer.toString());
    root   = newDoc.getDocumentElement();

    assertTrue(root.getNamespaceURI().equals(AP_NS_URI));
    assertEquals(root.getTagName(), tag("top"));
  }

  /*
   * serialize() to String
   *
   * Success if we can serialze, re-parse, and find the expected
   * root name and namespace URI
   */
  public void testSerializeToString() throws Exception {

    Document newDoc;
    Element  root;

    newDoc = parse(XmlUtils.serialize(document)); System.out.println("doc: " + newDoc);
    root   = newDoc.getDocumentElement();

    assertTrue(root.getNamespaceURI().equals(AP_NS_URI));
    assertEquals(root.getTagName(), tag("top"));
  }



  public static void main(String[] args) {
    String[] testCaseList = { TestXmlUtils.class.getName() };

    junit.swingui.TestRunner.main(testCaseList);
  }
}
