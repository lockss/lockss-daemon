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

package org.lockss.uiapi.util;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.*;

import org.lockss.daemon.status.*;
import org.lockss.util.*;


public class ParseUtils implements ApiParameters, ClusterControlParameters {
  private static Logger log = Logger.getLogger("ParseUtils");

  /**
   * Get a new XML utility object in the API name space
   * @return XmlUtils object configured for the API name space
   */
  public static XmlUtils getApiXmlUtils() {
    return new XmlUtils(AP_NS_PREFIX, AP_NS_URI, AP_XML_VERSION);
  }

  /**
   * Get a new XML utility object in the Cluster name space
   * @return XmlUtils object configured for the Cluster Control name space
   */
  public static XmlUtils getClusterXmlUtils() {
    return new XmlUtils(CCP_NS_PREFIX, CCP_NS_URI, CCP_XML_VERSION);
  }

  /**
   * Get a new XML utility object in the (daemon) Status name space
   * @return XmlUtils object configured for the Cluster Control name space
   */
  public static XmlUtils getStatusXmlUtils() {
    return new XmlUtils(XmlStatusConstants.NS_PREFIX,
                        XmlStatusConstants.NS_URI,
                        XmlDomBuilder.XML_VERSIONNAME);
  }

  /**
   * Get the API version
   * @param xmlUtils XML utilities object
   * @param document Document to examine
   * @return Version text (empty string if none)
   */
  public static String getXmlVersion(XmlUtils xmlUtils, Document document) {
    return xmlUtils.getAttribute(document.getDocumentElement(),
                                 COM_XML_VERSIONNAME);
  }

  /**
   * Get the status of the last transaction
   * @param xmlUtils XML utilities object
   * @param root Root element for the search (typically the document root)
   * @return True if transaction successful
   */
  public static boolean getStatus(XmlUtils xmlUtils, Element root) {

    Element element;
    boolean status;

    element = xmlUtils.getElement(root, COM_E_STATUS);
    verifyMandatoryElement(element, COM_E_STATUS);

    status = xmlUtils.getAttribute
                    (element, COM_A_SUCCESS).equalsIgnoreCase(AP_TRUE);
    return status;
  }

  /**
   * Get a message (status or detail)
   * @param xmlUtils XML utilities object
   * @param root Root element for the search (typically the document root)
   * @param type The "base" element for this message (for example, AP_E_STATUS)
   * @param subType The name of the child element that has the actual text
   * @return Message text (null if none)
   */
  private static String getMessage(XmlUtils xmlUtils, Element root,
	                                 String type, String subType)
                        throws XmlException {

    String  message = null;
    Element element;

    element = xmlUtils.getElement(root, type);
    verifyMandatoryElement(element, type);

    element = xmlUtils.getElement(element, subType);
    if (element != null) {
      message = XmlUtils.getText(element);
    }
    return message;
  }

  /**
   * Get any detail text associated with the last transaction
   * @param xmlUtils XML utilities object
   * @param root Root element for the search (typically the document root)
   * @return Message text (null if none)
   */
  public static String getDetailMessage(XmlUtils xmlUtils, Element root)
                throws XmlException {

    return getMessage(xmlUtils, root, COM_E_DETAIL, COM_E_MESSAGE);
  }

  /**
   * Get any error text associated with the last transaction
   * @param xmlUtils XML utilities object
   * @param root Root element for the search (typically the document root)
   * @return Message text (null if none)
   */
  public static String getStatusMessage(XmlUtils xmlUtils, Element root)
                throws XmlException {

    return getMessage(xmlUtils, root, COM_E_STATUS, COM_E_MESSAGE);
  }

  /**
   * Get option value (status or detail block)
   * @param xmlUtils XML utilities object
   * @param root Root element for the search (typically the document root)
   * @param type The "base" element for options (for example, AP_E_STATUS)
   * @return Option value (null if none)
   */
  private static String getOption(XmlUtils xmlUtils, Element root,
                                  String type, String name)
                                  throws XmlException {
    NodeList  nodeList;
    int       listLength;
    Element   element;

    element = xmlUtils.getElement(root, type);
    verifyMandatoryElement(element, type);

    nodeList    = xmlUtils.getList(element, AP_E_OPTION);
    listLength  = nodeList.getLength();

    for (int i = 0; i < listLength; i++) {
      element = (Element) nodeList.item(i);

      if (name.equals(xmlUtils.getAttribute(element, AP_A_NAME))) {
        return xmlUtils.getAttribute(element, AP_A_VALUE);
      }
    }
    return null;
  }

  /**
   * Get a specified detail option
   * @param xmlUtils XML utilities object
   * @param root Root element for the search (typically the document root)
   * @return Option value (null if none)
   */
  public static String getDetailOption(XmlUtils xmlUtils, Element root,
                                       String name)
                                       throws XmlException {

    return getOption(xmlUtils, root, COM_E_DETAIL, name);
  }

  /**
   * Get a specified status option
   * @param xmlUtils XML utilities object
   * @param root Root element for the search (typically the document root)
   * @return Option value (null if none)
   */
  public static String getStatusOption(XmlUtils xmlUtils, Element root,
                                       String name)
                                       throws XmlException {

    return getOption(xmlUtils, root, COM_E_STATUS, name);
  }

  /**
   * Get a HashMap of options associated with this command (status or detail)
   * @param xmlUtils XML utilities object
   * @param root Root element for the search (typically the document root)
   * @param type The "base" element for options (for example, AP_E_STATUS)
   * @return HashMap of options (name=value pairs, null if none)
   */
  private static HashMap getOptions(XmlUtils xmlUtils, Element root,
	                                  String type) throws XmlException {

    HashMap   hashMap;
    NodeList  nodeList;
    int       listLength;
    Element   element;

    element = xmlUtils.getElement(root, type);
    verifyMandatoryElement(element, type);

    nodeList    = xmlUtils.getList(element, AP_E_OPTION);
    listLength  = nodeList.getLength();

    hashMap = null;

    if (listLength != 0) {
      hashMap = new HashMap(listLength);

      for (int i = 0; i < listLength; i++) {
        element = (Element) nodeList.item(i);

        hashMap.put(xmlUtils.getAttribute(element, AP_A_NAME),
                    xmlUtils.getAttribute(element, AP_A_VALUE));
      }
    }
    return hashMap;
  }

  /**
   * Get any detail options associated with the last transaction
   * @param xmlUtils XML utilities object
   * @param root Root element for the search (typically the document root)
   * @return Options HashMap (null if none)
   */
  public static HashMap getDetailOptions(XmlUtils xmlUtils, Element root)
                throws XmlException {

    return getOptions(xmlUtils, root, COM_E_DETAIL);
  }

  /**
   * Get any status options associated with the last transaction
   * @param xmlUtils XML utilities object
   * @param root Root element for the search (typically the document root)
   * @return Options HashMap (null if none)
   */
  public static HashMap getStatusOptions(XmlUtils xmlUtils, Element root)
                throws XmlException {

    return getOptions(xmlUtils, root, COM_E_STATUS);
  }

  /**
   * Get server host name
   * @param xmlUtils XML utilities object
   * @param root Root element for the search (typically the document root)
   * @return Server host name ("unknown" if none available)
   */
  public static String getServerName(XmlUtils xmlUtils, Element root)
                throws XmlException {

    return getDetailAttribute(xmlUtils, root, COM_A_SYSTEM);
  }

  /**
   * Get command name
   * @param xmlUtils XML utilities object
   * @param root Root element for the search (typically the document root)
   * @return Server host name ("unknown" if none available)
   */
  public static String getCommandName(XmlUtils xmlUtils, Element root)
                throws XmlException {

    return getDetailAttribute(xmlUtils, root, COM_A_COMMAND);
  }

  /**
   * Get an attribute from the command response detail block
   * @param xmlUtils XML utilities object
   * @param root Root element for the search (typically the document root)
   * @return Attribute value ("unknown" if none available)
   */
  public static String getDetailAttribute(XmlUtils xmlUtils, Element root,
                                          String attribute)
                throws XmlException {

    Element element;
    String  value;

    element = xmlUtils.getElement(root, COM_E_DETAIL);
    verifyMandatoryElement(element, COM_E_DETAIL);

    value = xmlUtils.getAttribute(element, attribute);
    if (value.length() == 0) {
      value = COM_VALUE_UNKNOWN;
    }
    return value;
  }

  /**
   * Get any text associated with the named element
   * @param xmlUtils XML utilities object
   * @param root Root element for the search (typically the document root)
   * @param name Element where text is found
   * @return Text (null if none)
   */
  public static String getText(XmlUtils xmlUtils, Element root, String name) {

    String  message   = null;
    Element element   = xmlUtils.getElement(root, name);

    if (element != null) {
      message = XmlUtils.getText(element);
    }
    return message;
  }

  /**
   * Build up a <code>KeyedList</code> of "dynamic page input field data"
   * <p>
   * The XML is composed of "metadata elements" - each of these is coupled with
   * a unique "parameter" element.  This provides a mechanism for handling
   * dynamic data associated with some daemon structures - archival unit
   * editable key fields are an example.  The XML looks something like:
   *
   * <code>
   *    <metadata>dynamic_element_name_1</metadata>
   *    <dynamic_element_name_1>value_1</dynamic_element_name_1>
   *
   *    <metadata>dynamic_element_name_2</metadata>
   *    <dynamic_element_name_2>value_2</dynamic_element_name_2>
   * <code>
   *
   * @param xmlUtils XML utilities object
   * @param document The request document
   * @param metadataName Metadata element name (provides actual field names)
   * @return Map containing actual field name/value pairs
   */
  public static KeyedList getDynamicFields(XmlUtils xmlUtils,
                                           Document document,
                                           String   metadataName) {

    Element   root        = document.getDocumentElement();
    NodeList  nameList    = xmlUtils.getList(root, metadataName);
    KeyedList keyedList   = new KeyedList();


    for (int i = 0; i < nameList.getLength(); i++) {
      String name   = XmlUtils.getText(nameList.item(i));
      String value  = ParseUtils.getText(xmlUtils, root, name);

      keyedList.put(name, value);
    }
    return keyedList;
  }

  /**
   * Get an ArrayList of all occurances of named text in the requested scope
   * @param xmlUtils XML utilities object
   * @param document Document (DOM) to inspect
   * @param textElementName Name of the text element
   * @return List of all associated text messages
   */
  public static ArrayList getElementTextList(XmlUtils xmlUtils,
                                             Document document,
                                             String   textElementName) {

    return getElementTextList(xmlUtils, document, textElementName, null);
  }

  /**
   * Get an ArrayList of all occurances of named text in the requested scope
   * @param xmlUtils XML utilities object
   * @param document Document (DOM) to inspect
   * @param textElementName Name of the text element
   * @param rootElementName Name of starting element within the response
   * document
   * @return List of all associated text messages
   */
  public static ArrayList getElementTextList(XmlUtils xmlUtils,
                                             Document document,
                                             String textElementName,
                                             String rootElementName) {
    Element   root;
    NodeList  nodeList;

    ArrayList textList;
    String    text;

    textList = new ArrayList();

    /*
     * Set search root (top of document or specified element)
     */
    root = document.getDocumentElement();

    if (rootElementName != null) {
      root = xmlUtils.getElement(root, rootElementName);
      verifyMandatoryElement(root, rootElementName);
    }
    /*
     * Add all matching document text
     */
    nodeList = xmlUtils.getList(root, textElementName);
    for (int i = 0; i < nodeList.getLength(); i++) {
      textList.add(XmlUtils.getText((Element) nodeList.item(i)));
    }

    return textList;
  }

  /**
   * Fetch the command name from the client request document - this is
   * the first element following to request root.
   * @param xmlUtils XML utilities object
   * @param document Client request
   * @return Client command name (null if none)
   */
   public static String getClientCommand(XmlUtils xmlUtils, Document document) {

    Element root    = document.getDocumentElement();
    Node    child   = root.getFirstChild();
    String  name    = null;

    if (child != null) {
      name = child.getLocalName();
    }
    return name;
  }

  /**
   * Add a setup request flag to the client request
   * @param xmlUtils XML utilities object
   * @param document Client request Document
   * @param command Command (element) name
   */
  public static Element addSetupRequest(XmlUtils xmlUtils,
                                        Document document, String command) {
    Element root;

    root = xmlUtils.getElement(document.getDocumentElement(), command);
    verifyMandatoryElement(root, command);

    return xmlUtils.createElement(root, AP_E_SETUP);
  }

  /**
   * Is this a setup request (is the flag present in the client request)?
   * @param xmlUtils XML utilities object
   * @param document Client request Document
   * @param command Command (element) name
   */
  public static boolean isSetupRequest(XmlUtils xmlUtils,
                                       Document document, String command) {
    Element root;

    root = xmlUtils.getElement(document.getDocumentElement(), command);
    verifyMandatoryElement(root, command);

    return (xmlUtils.getElement(root, AP_E_SETUP) != null);
  }

  /**
   * Does this status table cell contain a reference value?
   * @param xmlUtils XML utilities object
   * @param cellElement The cell
   * @return true if a reference is present
   */
  public static boolean isReferenceValue(XmlUtils xmlUtils,
                                         Element cellElement) {

    Element element = xmlUtils.getElement(cellElement,
                                          XmlStatusConstants.VALUE);
    if (element != null) {
      element = xmlUtils.getElement(element,
                                    XmlStatusConstants.REFERENCE_ELEM);
    }
    return (element != null);
  }

  /**
   * Verify required element is present
   */
  public static void verifyMandatoryElement(Element element, String name) {

    if (element == null) {
      String message  = "\"" + name + "\" element missing in document";

      throw new IllegalStateException(message);
    }
  }
}
