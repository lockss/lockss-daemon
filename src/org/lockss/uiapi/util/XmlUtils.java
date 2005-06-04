/*
 * $Id: XmlUtils.java,v 1.2 2005-06-04 19:21:32 tlipkis Exp $
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
import java.util.*;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.*;

import org.apache.xerces.dom.*;
import org.apache.xml.serialize.*;

import org.lockss.util.*;

public class XmlUtils extends XmlDomBuilder implements ApiParameters {

  /*
   * Constructors
   */
  private XmlUtils() { 
  }

  public XmlUtils(String prefix, String uri, String version) {
    super(prefix, uri, version);
  }
  
  /**
   * Get a namespace aware document builder
   * @return DocumentBuilder 
   * @throws XmlException
   */
  public static DocumentBuilder getDocumentBuilder() throws XmlException {
    try { 
      return XmlDomBuilder.getDocumentBuilder();
    } catch (Exception exception) {
      throw new XmlException(exception.toString());
    }
  }

  /**
   * Start a new Document
   * @return The Document
   * @throws XmlException
   */
  public static Document createDocument() throws XmlException {

    try {
      return XmlDomBuilder.createDocument();
    } catch (Exception exception) {
      throw new XmlException(exception.toString());
    }
  }
 
  /**
   * Add Text to an Element
   * @param element the containing element
   * @param text the text to add
   */
  public static void addText(Element element, String text) {
    XmlDomBuilder.addText(element, text); 
  }

  /**
   * Get any text associated with this element
   * @param parent Text location (text parent node)
   * @return Trimmed text (null if no text is available)
   */
  public static String getText(Node parent) {
    String text = XmlDomBuilder.getText(parent);

    return (text == null) ? null : text.trim();
  }

  /**
   * Add encoded Text to an Element
   * @param element the containing element
   * @param text the text to add
    */
  public static void addEncodedText(Element element, String text) {
    String b64 = Base64Utils.encode(text);
    
    element.appendChild(element.getOwnerDocument().createTextNode(b64));
  }

  /**
   * Get encoded text associated with this element
   * @param parent Text location (text parent node)
   * @return Decoded (and trimmed) text (null if no text is available)
   * @throws XmlException
   */
  public static String getEncodedText(Node parent) throws XmlException {
    
    String text = getText(parent);

    if (text != null) {
      try {
        text = Base64Utils.decodeToString(text);
      } catch (IOException exception) { 
        text = null;
      }
    }
    return text;
  }
  
  /**
   * Copy an XML document, adding it as a child of the target document root
   * to the global response
   * @param source Document to copy
   * @param target Recieving document
   */
  public static void copyDocument(Document source, Document target) { 
    XmlDomBuilder.copyDocument(source, target);
  }
 
  /**
   * Return a list of named Elements (in our namespace)
   * @param element Root element for search
   * @param name name to look up
   * @return NodeList of matching elements
   */
  public NodeList getList(Element element, String name) {
    return super.getElementList(element, name);
  }

  /**
   * Get Element from Nodelist
   * @param list NodeList
   * @param index Element index
   * @return w3c.dom.Element
   */
  public static Element getElementFromItem(NodeList list, int index) {
    return (Element) list.item(index);
  }
  
  /**
   * Parse XML text (from an input stream)
   * @param xmlStream The XML text stream
   * @return DOM Document 
   * @throws XmlException
   */
  public static Document parseXmlStream(InputStream xmlStream) 
                         throws XmlException {
    try {
      return XmlDomBuilder.parseXmlStream(xmlStream);
    } catch (Exception exception) {
      throw new XmlException(exception.toString());
    }
  }

  /**
   * Parse XML text (from a Reader)
   *
   * @param xmlStream The XML reader
   * @return DOM Document
   * @throws XmlException
   */
  public static Document parseXmlStream(Reader xmlStream) throws XmlException {
    try {
      return XmlDomBuilder.parseXmlStream(xmlStream);
    } catch (Exception exception) {
      throw new XmlException(exception.toString());
    }
  }

  /**
   * Parse XML text (from a string)
   *
   * @param xml The XML text
   * @return DOM Document
   * @throws XmlException
   */
  public static Document parseXmlString(String xml) throws XmlException {
    try {
      return XmlDomBuilder.parseXmlString(xml);
    } catch (Exception exception) {
      throw new XmlException(exception.toString());
    }
  }

  /**
   * Parse an XML file
   *
   * @param filename - The filename to parse
   * @return DOM Document
   * @throws XmlException
   */
  public static Document parseXmlFile(String filename) throws XmlException {
    try {
      return XmlDomBuilder.parseXmlFile(filename);
    } catch (Exception exception) {
      throw new XmlException(exception.toString());
    }
  }
      
  /**
   * Get (and configure) an XML formatter
   * @return A Xercies OutputFormat object
   */
  public static OutputFormat getFormatter() {
    return XmlDomBuilder.getFormatter();
  }
  
  /**
   * Write formatted XML text to supplied OutputStream
   * @param document XML DOM for rendering
   * @param stream Stream the document is written to
   * @throws XmlException
   */
  public static void serialize(Document document, OutputStream stream) 
                throws XmlException {
    try {
      XmlDomBuilder.serialize(document, stream); 
    } catch (Exception exception) {
      throw new XmlException(exception.toString());
    }
  }
    
  /**
   * Write formatted XML text to supplied Writer
   * @param document XML DOM for rendering
   * @param writer Writer the document is written to
   * @throws XmlException
   */
  public static void serialize(Document document, Writer writer) 
                throws XmlException {
    try {
      XmlDomBuilder.serialize(document, writer); 
    } catch (Exception exception) {
      throw new XmlException(exception.toString());
    }
  }
    
  /**
   * Write formatted XML text to specified file
   * @param document XML DOM for rendering
   * @param filename Filename the document is written to
   * @throws XmlException
   */
  public static void serialize(Document document, String filename) 
                throws XmlException {
    try {
      XmlDomBuilder.serialize(document, filename); 
    } catch (Exception exception) {
      throw new XmlException(exception.toString());
    }
  }
 
  /**
   * Write formatted XML text to a String
   * @param document XML DOM for rendering
   * @return String containing the formatted document text
   * @throws XmlException
   */
  public static String serialize(Document document) throws XmlException {
        
    try {
      return XmlDomBuilder.serialize(document); 
    } catch (Exception exception) {
      throw new XmlException(exception.toString());
    }
  }
}
