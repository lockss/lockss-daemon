/*
 * $Id: TestPropertyTree.java,v 1.3 2006-04-05 22:56:02 tlipkis Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.test.LockssTestCase;

public class TestPropertyTree extends LockssTestCase {

  static final String TEST_FILE = "TestPropertyTree.txt";

  void checkContains(String str, String substr, String op) {
    assertTrue(substr + " wasn't found in " + str,
	       str.indexOf(substr) > 0);
  }

  void check(boolean val, String op) {
    assertTrue(op, val);
  }

  public void testPT() throws Exception {
    PropertyTree props = new PropertyTree();

    URL url = getClass().getResource(TEST_FILE);
    assertNotNull(TEST_FILE + " missing.", url);
    InputStream istr = UrlUtil.openInputStream(url.toString());

    props.load(istr);

    log.debug("tree="+props);

    assertEquals( "1", props.get("a"));
    assertEquals( "2", props.get("a.b"));
    assertEquals( "3", props.get("a.B"));
    assertEquals( "4", props.get("a.b.c"));
    assertEquals( "5", props.get("a.b.C"));
    assertEquals( "6", props.get("a.B.c"));
    assertEquals( "7", props.get("a.B.C"));
    assertEquals( null, props.get("a.*.C"));
    assertEquals( null, props.get("X.X.X"));


    assertEquals( "10", props.get("X"));
    assertEquals("11", props.get("X.y.z"));
    assertEquals("12", props.get("x.Y.z"));
    assertEquals("13", props.get("x.y.Z"));
    assertEquals("14", props.get("x.y.z"));
    assertEquals("15", props.get("X.Y"));

    assertEquals( "21", props.get("A.b.C.d.E"));
    assertEquals( "24", props.get("A.B.C.D.E"));

    PropertyTree sub = props.getTree("a.b");
    log.debug("getTree(a.b)="+sub);

    assertEquals( "4", sub.get("c"));
    assertEquals( "5", sub.get("C"));
    assertEquals( null, sub.get("*.C"));


    sub = props.getTree("a");
    log.debug("getTree(a)="+sub);

    assertEquals( "2", sub.get("b"));
    assertEquals( "3", sub.get("B"));
    assertEquals( "4", sub.get("b.c"));
    assertEquals( "5", sub.get("b.C"));
    assertEquals( "6", sub.get("B.c"));
    assertEquals( "7", sub.get("B.C"));
    assertEquals( null, sub.get("*.C"));
    assertEquals( null, sub.get("X.X"));
    assertEquals(null, sub.get("Y.z"));
    assertEquals(null, sub.get("y.Z"));
    assertEquals("11", sub.get("y.z"));
    assertEquals("15", sub.get("Y"));

    assertEquals( null, sub.get("b.C.d.E"));
    assertEquals( "22", sub.get("B.C.D.E"));


    props=new PropertyTree();
    String[] init =
      {//  0   1       2       3       4       5     6     7     8       9       10
	"*","*.b.c","a.*.c","a.b.*","*.b.*","a.*","*.b","*.B","a.b.c","a.*.b","a.*.B"
      };
    for (int i=0;i<init.length;i++)
      props.put(init[i],new Integer(i));

    sub=props.getTree("a.b");

    log.debug("getTree(a.b)="+sub);
    assertEquals("{b=9, *=3, c=8, B=10}".length(), sub.toString().length());
    checkContains(sub.toString(),"b=9","SubTree get");
    checkContains(sub.toString(),"*=3","SubTree get");
    checkContains(sub.toString(),"c=8","SubTree get");
    checkContains(sub.toString(),"B=10","SubTree get");

    sub=props.getTree("a");

    assertEquals(new Integer(9), sub.get("*.b"));

    sub=sub.getTree("b");
    assertEquals("{b=9, *=3, c=8, B=10}".length(), sub.toString().length());
    checkContains(sub.toString(),"b=9","SubTree");
    checkContains(sub.toString(),"*=3","SubTree");
    checkContains(sub.toString(),"c=8","SubTree");
    checkContains(sub.toString(),"B=10","SubTree");

//     Enumeration e=sub.getRealNodes();
//     check(e.hasMoreElements(),"getRealNodes");
//     assertEquals("*", e.nextElement());
//     check(e.hasMoreElements(),"getRealNodes");
//     assertEquals("c", e.nextElement());
//     check(!e.hasMoreElements(),"getRealNodes");


    Properties clone = (Properties)sub.clone();
    assertEquals("{b=9, *=3, c=8, B=10}".length(), sub.toString().length());
    checkContains(sub.toString(),"b=9","Clone");
    checkContains(sub.toString(),"*=3","Clone");
    checkContains(sub.toString(),"c=8","Clone");
    checkContains(sub.toString(),"B=10","Clone");

//     sub.put("C","C");
//     checkContains(props.toString(),"a.b.C=C","Subtree changed");
//     clone.put("C","X");
//     checkContains(props.toString(),"a.b.C=C","clone changed");
//     sub.put("*.B","B");
//     checkContains(props.toString(),"a.b.*.B=B","Subtree changed");
//     checkContains(props.toString(),"a.*.B=10","Subtree changed");

//     e=sub.elements();
//     String v=sub.toString();
//     while(e.hasMoreElements())
//       {
// 	String ev="="+e.nextElement();
// 	checkContains(v,ev,"Elements");
// 	v=v.substring(0,v.indexOf(ev))+v.substring(v.indexOf(ev)+ev.length());
//       }

    Vector nodes;
    nodes=enum2vector(props.getNodes(""));
    assertEquals(2, nodes.size());
    check(nodes.contains("a"),"Get root node");
    check(nodes.contains("*"),"Get root node");

    nodes=enum2vector(props.getNodes("a"));
    assertEquals(2, nodes.size());
    check(nodes.contains("b"),"Get a node");
    check(nodes.contains("*"),"Get a node");

    nodes=enum2vector(props.getNodes("*"));
    assertEquals(2, nodes.size());
    check(nodes.contains("b"),"Get wild node");
    check(nodes.contains("B"),"Get wild node");

    nodes=enum2vector(props.getNodes("a.*"));
    assertEquals(3, nodes.size());
    check(nodes.contains("b"),"Get node");
    check(nodes.contains("B"),"Get node");
    check(nodes.contains("c"),"Get node");

    // wild sub trees
    props=new PropertyTree();
    String[] init2 =
      {//  0   1     2     3       4       5       6
	"*","*.A","*.C","a.*.A","a.*.B","a.b.A","a.*"
      };
    for (int i=0;i<init2.length;i++)
      props.put(init2[i],new Integer(i));
    sub=props.getTree("a.*");
    String subs=sub.toString();
    log.debug(subs);

    checkContains(subs,"A=3","wild tree A=3");
    checkContains(subs,"B=4","wild tree B=4");

    sub.put("*.C",new Integer(7));
    sub.put("*",new Integer(8));
    checkContains(sub.toString(),"*.C=7","mod wild tree *.C=7");
    checkContains(sub.toString(),"*=8","mod wild tree *=8");

    String propss=props.toString();
    log.debug(propss);
//     checkContains(propss,"*.C=2","mod wild tree *.C=2");
//     checkContains(propss,"a.*.*.C=7","mod wild tree a.*.C=7");
//     checkContains(propss,"*=0","mod wild tree *=0");
//     checkContains(propss,"a.*.*=8","mod wild tree a.*=8");

  }

  public void testPropertyTreeWithPercents() {
    Properties props = new Properties();
    Enumeration propsEnum = System.getProperties().propertyNames();
    // get a random system variable
    String propName = (String)propsEnum.nextElement();
    String propValue = System.getProperty(propName);

    // add the property here to check against substitution within the tree
    props.setProperty(propName, "prop-value");
    props.setProperty("test", "value");
    props.setProperty("test-prop", "prop-%"+propName+"%");
    props.setProperty("prop-%"+propName+"%", "value2");
    props.setProperty("prop-%test%", "value-%test%");

    PropertyTree propTree = new PropertyTree(props);
    // normal property
    assertEquals("value", propTree.getProperty("test"));
    // substituted in value
    assertEquals("prop-"+propValue, propTree.getProperty("test-prop"));
    // not substituted in key
    assertEquals("value2", propTree.getProperty("prop-%"+propName+"%"));
    // uses local value correctly
    assertEquals("prop-value", propTree.getProperty(propName));
    // doesn't expand for local parameters
    assertEquals("value-%test%", propTree.getProperty("prop-%test%"));
  }

  private Vector enum2vector(Enumeration e) {
    Vector v = new Vector();
    while (e.hasMoreElements())
      v.addElement(e.nextElement());
    return v;
  }
}

