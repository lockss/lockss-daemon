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

import java.util.*;
import java.io.*;
import junit.framework.*;
import org.w3c.dom.*;
import org.lockss.test.*;

/**
 * This is the test class for org.lockss.util.XmlDoc
 */
public class TestXmlDoc extends LockssTestCase {
  public TestXmlDoc(String msg) {
    super(msg);
  }

  private File file;
  private XmlDoc doc;

  public void setUp() {
    try {
      super.setUp();
      file = new File(getTempDir().getAbsolutePath(), "test.xml");
      BufferedWriter fw = new BufferedWriter(new FileWriter(file));
      fw.write("<xml>");
      fw.write("<tag1 name=\"name1\">sometext</tag1>");
      fw.write("<tag1 name=\"name2\">othertext</tag1>");
      fw.write("</xml>");
      fw.close();
      doc = new XmlDoc(file.getAbsolutePath());
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  public void tearDown() throws Exception {
    super.tearDown();
    file.delete();
  }

  public void testGetNode() {
    Node node = doc.getTag("tag1");
    String txt = XmlDoc.getText(node);
    assertEquals(txt,"sometext");
  }

  public void testGetTagText() {
    assertEquals(doc.getTagText("tag1"),"sometext");
  }

  public void testGetNodeAttr() {
    Node node = doc.getTag("tag1");
    assertEquals("name1",XmlDoc.getNodeAttrText(node,"name"));
  }

  public void testGetAttrText() {
    assertEquals(doc.getAttrText("tag1","name"),"name1");
  }

  public void testNodeList() {
    NodeList list = doc.getNodeList("tag1");
    assertEquals(2,list.getLength());
  }

  public void testHasTag() {
    assertTrue(doc.hasTag("tag1"));
    assertFalse(doc.hasTag("tag101"));
  }

  public void testPutAttrFromNodeListIntoSet() {
    NodeList list = doc.getNodeList("tag1");
    Set set = new HashSet();
    XmlDoc.putAttrFromNodeListIntoSet(list,set,"name");
    assertTrue(set.contains("name1"));
    assertTrue(set.contains("name2"));
  }

  public static Test suite() {
    return new TestSuite(TestXmlDoc.class);
  }



}
