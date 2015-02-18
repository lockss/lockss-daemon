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

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import java.io.*;
import java.util.*;

/**
 * Wraps an XML DOM document, shielding callers from the
 * complexity of the Xerces XML interfaces.
 * @author Tyrone Nicholas
 */

public class XmlDoc {

  /** Inner DOM Document */
  Document doc;

  /** Wrap a DOM object */
  public XmlDoc(Document doc) {
   this.doc = doc;
  }

  /** Load an XML file */
  public XmlDoc(String pathname) throws SAXException, IOException, ParserConfigurationException {
    DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = fac.newDocumentBuilder();
    File file = new File(pathname);
    this.doc = builder.parse(file);
  }

  /** Return the Node object of a certain tag, or null if none found.  If more
   * than one found, return the first
   */
  public Node getTag(String tag) {
    NodeList nlist = doc.getElementsByTagName(tag);
    if (nlist.getLength() == 0) {
      return null;
    }
    else {
      return nlist.item(0);
    }
  }

  /** Returns the text value of a node */
  public static String getText(Node node) {
    if (node == null) {
      return "";
    }
    else {
      return ( (Text) node.getFirstChild()).getData();
    }
  }

  /** Returns the text value of a node specified by string */
  public String getTagText(String tag) {
    return getText(getTag(tag));
  }


  /** Find the value of a given attribute of a node */
  public static String getNodeAttrText(Node node, String attribute) {
    Attr attr = (Attr) node.getAttributes().getNamedItem(attribute);
    if (attr == null) {
      return "";
    }
    else {
      return attr.getNodeValue();
    }
  }

  /** Find the value of a given attribute of a node specified by name */
  public String getAttrText(String tag, String attribute) {
    Node node = getTag(tag);
    if (node == null) {
      return "";
    }
    else {
      return getNodeAttrText(node, attribute);
    }
  }

  /** Return a list of all nodes of this tag */
  public NodeList getNodeList(String tag) {
   return doc.getElementsByTagName(tag);
  }

  /** Returns true if the tag is present in the document somewhere */
  public boolean hasTag(String tag) {
    return (doc.getElementsByTagName(tag).getLength() > 0);
  }

  public static void putAttrFromNodeListIntoSet(
      NodeList list, Set set, String attr) {
    for (int i = 0; i < list.getLength(); i++) {
      set.add(getNodeAttrText(list.item(i), attr));
    }
  }


}
