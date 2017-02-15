/*
 * $Id$
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


public class TestParseUtils extends LockssTestCase
                            implements ApiParameters, ClusterControlParameters {

  final private static String ELEMENT_1     = "element_1";
  final private static String ELEMENT_2     = "element_2";
  final private static String TEXT_1        = "First element text";
  final private static String TEXT_2        = "Last element text";


  final private static String METADATA      = "metadata";
  final private static String DYNAMIC_N1    = "dynamic_name_1";
  final private static String DYNAMIC_N2    = "dynamic_name_2";
  final private static String DYNAMIC_V1    = "First dynamic value";
  final private static String DYNAMIC_V2    = "Second dynamic value";

  final private static String COMMAND       = "noop";
  final private static String DATE          = "30-Jun-2003 13:23:38";
  final private static String SYSTEM        = "my.system.name";

  final private static String COMMENT_TEXT  = "comment comment comment";
  final private static String DETAIL_TEXT   = "detail detail detail";
  final private static String STATUS_TEXT   = "status status status";

  final private static String DETAIL_N1     = "detail_n1";
  final private static String DETAIL_V1     = "detail_v1";
  final private static String DETAIL_N2     = "detail_n2";
  final private static String DETAIL_V2     = "detail_v2";

  final private static String STATUS_N1     = "status_n1";
  final private static String STATUS_V1     = "status_v1";
  final private static String STATUS_N2     = "status_n2";
  final private static String STATUS_V2     = "status_v2";

  private String      apixml;
  private String      badxml;
  private String      ccxml;
  private String      clientxml;
  private String      shortxml;

  private Document    document;
  private XmlUtils    xmlUtils;


  public TestParseUtils(String message) {
    super(message);
  }

  private static String apiTag(String name) {
    return (AP_NS_PREFIX + ":" + name);
  }

  private static String clusterTag(String name) {
    return (CCP_NS_PREFIX + ":" + name);
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

    apixml    =   startApiResponse()
              +   statusBlock()
              +   detailBlock()
              +   commentBlock()
              +   elementBlock(ELEMENT_1, TEXT_1)
              +   elementBlock(ELEMENT_2, TEXT_2)
              +   elementBlock(METADATA, DYNAMIC_N1)
              +   elementBlock(DYNAMIC_N1, DYNAMIC_V1)
              +   elementBlock(METADATA, DYNAMIC_N2)
              +   elementBlock(DYNAMIC_N2, DYNAMIC_V2)
              +   endApiResponse();

    shortxml  =   startApiResponse()
              +   shortStatusBlock()
              +   shortDetailBlock()
              +   endApiResponse();

    ccxml     =   startClusterResponse()
              +   endClusterResponse();

    badxml    =   startApiResponse()
              +   endApiResponse();

    clientxml =   startClientRequest()
              +   "<"  + apiTag(COMMAND) + ">"
              +   "</" + apiTag(COMMAND) + ">"
              +   endClientRequest();

  }

  private String startClientRequest() {

    return  "<" + apiTag(AP_REQUESTROOT) + " "
    +       apiTag(COM_XML_VERSIONNAME)   + "=\"" + AP_XML_VERSION  + "\" "
    +       "xmlns:" + AP_NS_PREFIX       + "=\"" + AP_NS_URI       + "\">";
  }

  private String endClientRequest() {
    return "</" + apiTag(AP_REQUESTROOT) + ">";
  }

  private String startApiResponse() {

    return  "<" + apiTag(AP_RESPONSEROOT) + " "
    +       apiTag(COM_XML_VERSIONNAME)   + "=\"" + AP_XML_VERSION  + "\" "
    +       "xmlns:" + AP_NS_PREFIX       + "=\"" + AP_NS_URI       + "\">";
  }

  private String endApiResponse() {
    return "</" + apiTag(AP_RESPONSEROOT) + ">";
  }

  private String startClusterResponse() {

    return  "<" + clusterTag(CCP_E_RESPONSE)  + " "
    +       clusterTag(COM_XML_VERSIONNAME)   + "=\"" + CCP_XML_VERSION + "\" "
    +       "xmlns:" + CCP_NS_PREFIX          + "=\"" + CCP_NS_URI      + "\">";
  }

  private String endClusterResponse() {
    return "</" + clusterTag(CCP_E_RESPONSE) + ">";
  }

  private String statusBlock() {

    return  "<"   + apiTag(AP_E_STATUS)
      +     " "   + apiTag(AP_A_SUCCESS)
      +     "=\"" + AP_FALSE + "\">"
      +     "<"   + apiTag(AP_E_MESSAGE) + ">"
      +     STATUS_TEXT
      +     "</"  + apiTag(AP_E_MESSAGE) + ">"
      +     "<"   + apiTag(AP_E_OPTION)
      +     " "   + apiTag(AP_A_NAME)    + "=\"" + STATUS_N1 + "\""
      +     " "   + apiTag(AP_A_VALUE)   + "=\"" + STATUS_V1 + "\"/>"
      +     "<"   + apiTag(AP_E_OPTION)
      +     " "   + apiTag(AP_A_NAME)    + "=\"" + STATUS_N2 + "\""
      +     " "   + apiTag(AP_A_VALUE)   + "=\"" + STATUS_V2 + "\"/>"
      +     "</"  + apiTag(AP_E_STATUS)  + ">";
  }

  private String shortStatusBlock() {

    return  "<"   + apiTag(AP_E_STATUS)
      +     " "   + apiTag(AP_A_SUCCESS)
      +     "=\"" + AP_FALSE + "\">"
      +     "<"   + apiTag(AP_E_MESSAGE) + ">"
      +     "</"  + apiTag(AP_E_MESSAGE) + ">"
      +     "</"  + apiTag(AP_E_STATUS)  + ">";
  }

  private String commentBlock() {

    return  "<"   + apiTag(AP_E_COMMENT) + ">"
      +     COMMENT_TEXT
      +     "</"  + apiTag(AP_E_COMMENT) + ">";
  }

  private String detailBlock() {

    return  "<"   + apiTag(AP_E_DETAIL)
      +     " "   + apiTag(AP_A_COMMAND) + "=\"" + COMMAND + "\""
      +     " "   + apiTag(AP_A_DATE)    + "=\"" + DATE    + "\""
      +     " "   + apiTag(AP_A_SYSTEM)  + "=\"" + SYSTEM  + "\""
      +     ">"
      +     "<"   + apiTag(AP_E_MESSAGE) + ">"
      +     DETAIL_TEXT
      +     "</"  + apiTag(AP_E_MESSAGE) + ">"
      +     "<"   + apiTag(AP_E_OPTION)
      +     " "   + apiTag(AP_A_NAME)    + "=\"" + DETAIL_N1 + "\""
      +     " "   + apiTag(AP_A_VALUE)   + "=\"" + DETAIL_V1 + "\"/>"
      +     "<"   + apiTag(AP_E_OPTION)
      +     " "   + apiTag(AP_A_NAME)    + "=\"" + DETAIL_N2 + "\""
      +     " "   + apiTag(AP_A_VALUE)   + "=\"" + DETAIL_V2 + "\"/>"
      +     "</"  + apiTag(AP_E_DETAIL)  + ">";
  }

  private String shortDetailBlock() {

    return  "<"   + apiTag(AP_E_DETAIL)
      +     " "   + apiTag(AP_A_COMMAND) + "=\"" + COMMAND + "\""
      +     " "   + apiTag(AP_A_DATE)    + "=\"" + DATE    + "\""
      +     " "   + apiTag(AP_A_SYSTEM)  + "=\"" + SYSTEM  + "\""
      +     ">"
      +     "<"   + apiTag(AP_E_MESSAGE) + ">"
      +     "</"  + apiTag(AP_E_MESSAGE) + ">"
      +     "</"  + apiTag(AP_E_DETAIL)  + ">";
  }

  private String elementBlock(String element, String text) {
    String value = text == null ? "" : text;

    return "<" + apiTag(element) + ">" + value + "</" + apiTag(element) + ">";
  }

  /*
   * getApiXmlUtils()
   *
   * Success if we get the expected namespace URI and prefix:root pair
   */
  public void testGetApiXmlUtils() throws Exception {

    Element   root;

    xmlUtils  = ParseUtils.getApiXmlUtils();
    document  = parse(apixml);
    root      = document.getDocumentElement();

    assertTrue(root.getNamespaceURI().equals(AP_NS_URI));
    assertEquals(apiTag(AP_RESPONSEROOT), root.getTagName());
  }

  /*
   * getClusterXmlUtils()
   *
   * Success if we get the expected namespace URI and prefix:root element pair
   */
  public void testGetClusterXmlUtils() throws Exception {

    Element   root;

    xmlUtils  = ParseUtils.getClusterXmlUtils();
    document  = parse(ccxml);
    root      = document.getDocumentElement();

    assertTrue(root.getNamespaceURI().equals(CCP_NS_URI));
    assertEquals(clusterTag(CCP_E_RESPONSE), root.getTagName());
  }

  /*
   * Verify that we fail on malformed documents
   */
  public void testVerification() throws Exception {

    boolean seen;

    xmlUtils  = new XmlUtils(AP_NS_PREFIX, AP_NS_URI, AP_XML_VERSION);
    document  = parse(badxml);

    seen = false;
    try {
      ParseUtils.verifyMandatoryElement(null, "NoSuchElement");
    } catch (Exception ignore) {
      seen = true;
    }
    if (!seen) {
      fail("Should have failed on missing element");
    }

    try {
      ParseUtils.verifyMandatoryElement(document.getDocumentElement(), "Root");
    } catch (Exception ignore) {
      fail("Root element present - test should not have failed");
    }

    seen = false;
    try {
      boolean status = ParseUtils.getStatus(xmlUtils, document.getDocumentElement());
    } catch (Exception ignore) {
      seen = true;
    }
    if (!seen) {
      fail("Should have failed on missing status element");
    }

    seen = false;
    try {
      String text = ParseUtils.getStatusMessage(xmlUtils, document.getDocumentElement());
    } catch (Exception ignore) {
      seen = true;
    }
    if (!seen) {
      fail("Should have failed on missing status element");
    }

    seen = false;
    try {
      String text = ParseUtils.getServerName(xmlUtils, document.getDocumentElement());
    } catch (Exception ignore) {
      seen = true;
    }
    if (!seen) {
      fail("Should have failed on missing detail element");
    }
  }

  /*
   * getText()
   *
   * Success if we locate existing text and fail on non-existent text
   */
  public void testGetText() throws Exception {

    String  text;

    xmlUtils  = new XmlUtils(AP_NS_PREFIX, AP_NS_URI, AP_XML_VERSION);
    document  = parse(apixml);

    text = ParseUtils.getText(xmlUtils,
                              document.getDocumentElement(),
                              ELEMENT_1);
    assertEquals(TEXT_1, text);

    text = ParseUtils.getText(xmlUtils,
                              document.getDocumentElement(),
                              "NoSuchElement");
    assertNull(text);
  }

  /*
   * getStatus()
   *
   * Success if we fetch the anticipated status
   */
  public void testGetStatus() throws Exception {

    boolean status;
    String  text;

    xmlUtils  = new XmlUtils(AP_NS_PREFIX, AP_NS_URI, AP_XML_VERSION);
    document  = parse(apixml);
    status    = ParseUtils.getStatus(xmlUtils, document.getDocumentElement());

    assertFalse(status);
  }

  /*
   * getStatusOption()
   *
   * Success if we can fetch the proper value by name
   */
  public void testGetStatusOption() throws Exception {
    String value;

    xmlUtils  = new XmlUtils(AP_NS_PREFIX, AP_NS_URI, AP_XML_VERSION);
    document  = parse(apixml);

    value     = ParseUtils.getStatusOption(xmlUtils,
                                           document.getDocumentElement(),
                                           STATUS_N2);
    assertEquals(STATUS_V2, value);

    value     = ParseUtils.getStatusOption(xmlUtils,
                                           document.getDocumentElement(),
                                           "no_such_name");
    assertNull(value);
  }

  /*
   * getStatusOptions()
   *
   * Success if we can lookup the expected HashMap name/value pairs
   */
  public void testGetStatusOptions() throws Exception {
    HashMap hashMap;
    String  value;

    xmlUtils  = new XmlUtils(AP_NS_PREFIX, AP_NS_URI, AP_XML_VERSION);

    document  = parse(shortxml);
    hashMap   = ParseUtils.getStatusOptions(xmlUtils,
                                            document.getDocumentElement());
    assertNull(hashMap);

    document  = parse(apixml);
    hashMap   = ParseUtils.getStatusOptions(xmlUtils,
                                            document.getDocumentElement());
    assertNotNull(hashMap);
    assertEquals(2, hashMap.size());

    value = (String) hashMap.get(STATUS_N1);
    assertEquals(STATUS_V1, value);

    value = (String) hashMap.get("no_such_name");
    assertNull(value);
  }

  /*
   * getStatusMessage()
   *
   * Success if we fetch the anticipated status text
   */
  public void testGetStatusMessage() throws Exception {

    String  message;

    xmlUtils  = new XmlUtils(AP_NS_PREFIX, AP_NS_URI, AP_XML_VERSION);
    document  = parse(apixml);
    message   = ParseUtils.getStatusMessage(xmlUtils, document.getDocumentElement());

    assertEquals(STATUS_TEXT, message);
  }

  /*
   * getDetailOption()
   *
   * Success if we can fetch the proper value by name
   */
  public void testGetDetailOption() throws Exception {
    String value;

    xmlUtils  = new XmlUtils(AP_NS_PREFIX, AP_NS_URI, AP_XML_VERSION);
    document  = parse(apixml);

    value     = ParseUtils.getDetailOption(xmlUtils,
                                           document.getDocumentElement(),
                                           DETAIL_N1);
    assertEquals(DETAIL_V1, value);

    value     = ParseUtils.getStatusOption(xmlUtils,
                                           document.getDocumentElement(),
                                           "no_such_name");
    assertNull(value);
  }

  /*
   * getDetailOptions()
   *
   * Success if:
   *
   * o Null is returned for "short" XML doument (no OPTION elements)
   * o We find the expected name/value pairs in a "good document"
   * o We fail to find a name that isn't in the table
   *
   */
  public void testGetDetailOptions() throws Exception {
    HashMap hashMap;
    String  value;

    xmlUtils  = new XmlUtils(AP_NS_PREFIX, AP_NS_URI, AP_XML_VERSION);

    document  = parse(shortxml);
    hashMap   = ParseUtils.getDetailOptions(xmlUtils,
                                            document.getDocumentElement());
    assertNull(hashMap);

    document  = parse(apixml);
    hashMap   = ParseUtils.getDetailOptions(xmlUtils,
                                            document.getDocumentElement());
    assertNotNull(hashMap);
    assertEquals(2, hashMap.size());

    value = (String) hashMap.get(DETAIL_N2);
    assertEquals(DETAIL_V2, value);

    value = (String) hashMap.get("no_such_name");
    assertNull(value);
  }

  /*
   * getDetailMessage()
   *
   * Success if we fetch the anticipated status text
   */
  public void testGetDetailMessage() throws Exception {

    String  message;

    xmlUtils  = new XmlUtils(AP_NS_PREFIX, AP_NS_URI, AP_XML_VERSION);
    document  = parse(apixml);
    message   = ParseUtils.getDetailMessage(xmlUtils, document.getDocumentElement());

    assertEquals(DETAIL_TEXT, message);
  }

  /*
   * getCommandName()
   *
   * Success if we fetch the anticipated command name
   */
  public void testGetCommandName() throws Exception {

    String  command;

    xmlUtils  = new XmlUtils(AP_NS_PREFIX, AP_NS_URI, AP_XML_VERSION);
    document  = parse(apixml);
    command   = ParseUtils.getCommandName(xmlUtils, document.getDocumentElement());

    assertEquals(COMMAND, command);
  }

  /*
   * getServerName()
   *
   * Success if we fetch the anticipated server name
   */
  public void testGetServerName() throws Exception {

    String  server;

    xmlUtils  = new XmlUtils(AP_NS_PREFIX, AP_NS_URI, AP_XML_VERSION);
    document  = parse(apixml);
    server    = ParseUtils.getServerName(xmlUtils, document.getDocumentElement());

    assertEquals(SYSTEM, server);
  }

  /*
   * getDynamicFields()
   *
   * Success if resulting list has correct number of entries, expected content
   */
  public void testGetDynamicFields() throws Exception {
    KeyedList list;

    xmlUtils  = new XmlUtils(AP_NS_PREFIX, AP_NS_URI, AP_XML_VERSION);
    document  = parse(apixml);
    list      = ParseUtils.getDynamicFields(xmlUtils, document, METADATA);

    assertEquals(2, list.size());

    assertEquals(DYNAMIC_V1, (String) list.get(DYNAMIC_N1));
    assertEquals(DYNAMIC_V2, (String) list.get(DYNAMIC_N2));

    assertNull((String) list.get("no_such_name"));
  }

  /*
   * getClientCommand()
   *
   * Success if the "command" element is what we expected to find
   */
  public void testGetClientCommand() throws Exception {

    Element element;

    xmlUtils  = new XmlUtils(AP_NS_PREFIX, AP_NS_URI, AP_XML_VERSION);
    document  = parse(clientxml);

    assertEquals(COMMAND, ParseUtils.getClientCommand(xmlUtils, document));
    assertNotEquals("Bad", ParseUtils.getClientCommand(xmlUtils, document));
  }

  /*
   * addSetupRequest()
   *
   * Success if we add a "setup request" element to a client request document
   */
  public void testAddSetupRequest() throws Exception {

    Element element;

    xmlUtils  = new XmlUtils(AP_NS_PREFIX, AP_NS_URI, AP_XML_VERSION);
    document  = parse(clientxml);

    element = xmlUtils.getElement(document.getDocumentElement(), COMMAND);
    assertNotNull(element);
    element = xmlUtils.getElement(element, AP_E_SETUP);
    assertNull(element);

    ParseUtils.addSetupRequest(xmlUtils, document, COMMAND);

    element = xmlUtils.getElement(document.getDocumentElement(), COMMAND);
    assertNotNull(element);
    element = xmlUtils.getElement(element, AP_E_SETUP);
    assertNotNull(element);
  }

  /*
   * isSetupRequest()
   *
   * Success if we see a "setup request" element in a client request
   */
  public void testIsSetupRequest() throws Exception {

    Element element;

    xmlUtils  = new XmlUtils(AP_NS_PREFIX, AP_NS_URI, AP_XML_VERSION);
    document  = parse(clientxml);

    assertFalse(ParseUtils.isSetupRequest(xmlUtils, document, COMMAND));
    ParseUtils.addSetupRequest(xmlUtils, document, COMMAND);
    assertTrue(ParseUtils.isSetupRequest(xmlUtils, document, COMMAND));
  }

  public static void main(String[] args) {
    String[] testCaseList = { TestParseUtils.class.getName() };

    junit.swingui.TestRunner.main(testCaseList);
  }
}
