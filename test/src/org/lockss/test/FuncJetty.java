/*
 * $Id: FuncJetty.java,v 1.3 2003-06-20 22:34:55 claire Exp $
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

package org.lockss.test;

import java.util.*;
import org.mortbay.tools.PropertyTree;

/**
 * Verifies behavior of PropertyTree name-substitution.
 */
public class FuncJetty extends LockssTestCase {
  /**
   * The PropertyTree should substitute for percent-surrounded variables in
   * values, but not in the key names.
   */
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
}
