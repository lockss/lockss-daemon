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
import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.apache.xml.serialize.*;
import org.xml.sax.InputSource;

public class XmlDomBuilder {
  public final static String XML_VERSIONNAME = "version";

  final static String ENCODING = "UTF-8";

  private static Logger logger = Logger.getLogger("XmlDomBuilder");

  private String nsPrefix = "lockss"; // defaults
  private String nsUri = "http://lockss.stanford.edu/statusui";
  private String xmlVersion = "1";

  public XmlDomBuilder() { }

  public XmlDomBuilder(String prefix, String uri, String version) {
    nsPrefix = prefix;
    nsUri       = uri;
    xmlVersion  = version;
  }

  /**
   * Get a namespace aware document builder.
   * @return the DocumentBuilder
   * @throws XmlDomException
   */
  public static DocumentBuilder getDocumentBuilder() throws XmlDomException {
    try {
      DocumentBuilderFactory factory;

      factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);

      return factory.newDocumentBuilder();
    } catch (Exception e) {
      logger.debug2("Error getting DocumentBuilder.");
      throw new XmlDomException(e.toString());
    }
  }

  /**
   * Start a new Document.
   * @return the Document
   * @throws XmlDomException
   */
  public static Document createDocument() throws XmlDomException {
    try {
      return getDocumentBuilder().newDocument();
    } catch (Exception e) {
      logger.debug2("Error creating Document.");
      throw new XmlDomException(e.toString());
    }
  }

  /**
   * Establish the root element (in our namespace).
   * @param document the Document
   * @param name the root name
   * @return root Element
   */
  public Element createRoot(Document document, String name) {
    Element element;

    // Create the root element
    element = document.createElementNS(nsUri, makeTag(name));
    // Namespace declaration, version of this XML document
    element.setAttributeNS("http://www.w3.org/2000/xmlns/",
                           "xmlns:" + nsPrefix, nsUri);
    setAttribute(element, XML_VERSIONNAME, xmlVersion);

    // set the document root
    document.appendChild(element);
    return element;
  }

  /**
   * Add a new element (in our namespace) to the given parent
   * @param parent the parent Element
   * @param name the child name
   * @return new Element
   */
  public Element createElement(Element parent, String name) {
    Document document;
    Element element;

    document = parent.getOwnerDocument();
    element  = document.createElementNS(nsUri, makeTag(name));

    parent.appendChild(element);
    return element;
  }

  /**
   * Add Text object to an Element.
   * @param element the containing element
   * @param text the text to add
   */
  public static void addText(Element element, String text) {
    element.appendChild(element.getOwnerDocument().createTextNode(text));
  }

  /**
   * Get any text associated with this element.  Null if none available.
   * @param parent the node containing text
   * @return trimmed text
   */
  public static String getText(Node parent) {
    String text = null;
    if (parent != null) {
      parent.normalize();
      for (Node child = parent.getFirstChild(); child != null;
          child = child.getNextSibling()) {
        if (child.getNodeType() == Node.TEXT_NODE) {
          text = child.getNodeValue();
          break;
        }
      }
    }
    return text;
  }

  /**
   * Add encoded Text to an Element
  public static void addEncodedText(Element element, String text) {
    String b64 = Base64Utils.encode(text);
    element.appendChild(element.getOwnerDocument().createTextNode(b64));
  }

  /**
   * Get encoded text associated with this element
   * @return Decoded (and trimmed) text (null if no text is available)
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
*/

  /**
   * Copy an XML document, adding it as a child of the target document root
   * to the global response.
   * @param source Document to copy
   * @param target Document to contain copy
   */
  public static void copyDocument(Document source, Document target) {
    Node node = target.importNode(source.getDocumentElement(), true);
    target.getDocumentElement().appendChild(node);
  }

  /**
   * Get an Attribute (in our namespace) from an Element.  Returns a String
   * value, zero length if none found.
   * @param element the containing Element
   * @param name the attribute name
   * @return Attribute as a String
   */
  public String getAttribute(Element element, String name) {
    return element.getAttributeNS(nsUri, name);
  }

  /**
   * Set an Attribute (in our namespace) in an Element
   * @param element the containing Element
   * @param name the attribute name
   * @param value the attribute value
   */
  public void setAttribute(Element element, String name, String value) {
    element.setAttributeNS(nsUri, makeTag(name), value);
  }

  /**
   * Return a list of named Elements (in our namespace).
   * @param element the containing Element
   * @param name the tag name
   * @return NodeList of matching elements
   */
  public NodeList getElementList(Element element, String name) {
    return element.getElementsByTagNameNS(nsUri, name);
  }

  /**
   * Return the first named Element found (in our namespace).  Null if none.
   * @param element the containing Element
   * @param name the tag name
   * @return matching Element
   */
  public Element getElement(Element element, String name) {
    NodeList nodeList = getElementList(element, name);
    return (nodeList.getLength() == 0) ? null : (Element)nodeList.item(0);
  }

  /**
   * Remove this node from its parent.
   * @param node the node to remove
   * @return Node removed
   */
  public Node removeNode(Node node) {
    return node.getParentNode().removeChild(node);
  }

  /**
   * Parse XML text (from an input stream) into a Document.
   * @param xmlStream The XML text stream
   * @return DOM Document
   * @throws XmlDomException
   */
  public static Document parseXmlStream(InputStream xmlStream)
      throws XmlDomException {
    try {
      return getDocumentBuilder().parse(xmlStream);
    } catch (Exception e) {
      logger.debug2("Error parsing XML stream.");
      throw new XmlDomException(e.toString());
    }
  }

  /**
   * Parse XML text (from a Reader) into a Document.
   * @param xmlStream The XML reader
   * @return DOM Document
   * @throws XmlDomException
   */
  public static Document parseXmlStream(Reader xmlStream)
      throws XmlDomException {
    try {
      return getDocumentBuilder().parse(new InputSource(xmlStream));
    } catch (Exception e) {
      logger.debug2("Error parsing XML stream from reader.");
      throw new XmlDomException(e.toString());
    }
  }

  /**
   * Parse XML text (from a string) into a Document.
   * @param xml The XML text
   * @return DOM Document
   * @throws XmlDomException
   */
  public static Document parseXmlString(String xml) throws XmlDomException {
    return parseXmlStream(new ByteArrayInputStream(xml.getBytes()));
  }

  /**
   * Parse an XML file into a Document.
   * @param filename - The filename to parse
   * @return DOM Document
   * @throws XmlDomException
   */
  public static Document parseXmlFile(String filename) throws XmlDomException {
    try {
      return getDocumentBuilder().parse(filename);
    } catch (Exception e) {
      logger.debug2("Error parsing from file '"+filename+"'.");
      throw new XmlDomException(e.toString());
    }
  }

  /**
   * Get (and configure) an XML formatter.
   * @return A Xerces OutputFormat object
   */
  public static OutputFormat getFormatter() {
    OutputFormat outputFormat;

    outputFormat = new OutputFormat(Method.XML, ENCODING, true);
    outputFormat.setPreserveSpace(false); // true to preserve Text properly
    outputFormat.setIndent(4);

    return outputFormat;
  }

  /**
   * Write formatted XML text to supplied OutputStream.
   * @param document the Document to write
   * @param target stream to write to
   * @throws XmlDomException
   */
  public static void serialize(Document document, OutputStream target)
      throws XmlDomException {
    try {
      XMLSerializer xmlSerial = new XMLSerializer(target, getFormatter());
      xmlSerial.asDOMSerializer().serialize(document);
    } catch (Exception e) {
      logger.debug2("Error writing document to stream.");
      throw new XmlDomException(e.toString());
    }
  }

  /**
   * Write formatted XML text to supplied Writer.
   * @param document the Document to write
   * @param writer Writer the document is written to
   * @throws XmlDomException
   */
  public static void serialize(Document document, Writer writer)
      throws XmlDomException {
    try {
      XMLSerializer xmlSerial = new XMLSerializer(writer, getFormatter());
      xmlSerial.asDOMSerializer().serialize(document);
    } catch (Exception e) {
      logger.debug2("Error writing document to writer.");
      throw new XmlDomException(e.toString());
    }
  }

  /**
   * Write formatted XML text to specified file.
   * @param document the Document to write
   * @param filename Filename the document is written to
   * @throws XmlDomException
   */
  public static void serialize(Document document, String filename)
      throws XmlDomException {
    FileOutputStream stream = null;
    Writer writer = null;

    try {
      stream = new FileOutputStream(filename);
      writer = new OutputStreamWriter(stream, ENCODING);
      serialize(document, writer);
    } catch (Exception e) {
      logger.debug2("Error writing document to file '"+filename+"'.");
      throw new XmlDomException(e.toString());
    } finally {
      try { if (writer != null) writer.close(); } catch (Exception ignore) { }
      try { if (stream != null) stream.close(); } catch (Exception ignore) { }
    }
  }

  /**
   * Write formatted XML text to a String.'
   * @param document the Document to write
   * @return String containing the formatted document text
   * @throws XmlDomException
   */
  public static String serialize(Document document) throws XmlDomException {
    ByteArrayOutputStream stream = null;
    Writer writer = null;

    try {
      stream = new ByteArrayOutputStream();
      writer = new OutputStreamWriter(stream, ENCODING);
      serialize(document, writer);
      return stream.toString();
    } catch (Exception e) {
      logger.debug2("Error writing document to string.");
      throw new XmlDomException(e.toString());
    } finally {
      try { if (writer != null) writer.close(); } catch (Exception ignore) { }
      try { if (stream != null) stream.close(); } catch (Exception ignore) { }
    }
  }

  /**
   * Construct an element tag name in our namespace.
   * @param name the element name
   * @return the tag name
   */
  String makeTag(String name) {
    return nsPrefix + ":" + name;
  }

  public static class XmlDomException extends Exception {
    public XmlDomException(String msg) {
      super(msg);
    }
  }
}
