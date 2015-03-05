/*
 * $Id: UrlFetcher.java 39864 2015-02-18 09:10:24Z thib_gc $
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import org.lockss.test.LockssTestCase;

/**
 * @author Thib Guicherd-Callin
 * @since 1.67.5
 */
public class TestOneToOneNamespaceContext extends LockssTestCase {

  public static final String A = "a";
  public static final String A_URI = "http://a.example.com/auri";
  public static final String B = "b";
  public static final String B_URI = "http://b.example.com/buri";
  public static final String UNKNOWN = "unknown";
  public static final String UNKNOWN_URI = "http://unknown.example.com/unknownuri";
  public static final String CUSTOM_URI = "http://custom.example.com/customuri";
  
  public void testNoArgConstructor() throws Exception {
    // Should contain a mapping for XMLConstants.DEFAULT_NS_PREFIX
    NamespaceContext nsc = new OneToOneNamespaceContext();
    assertEquals(XMLConstants.NULL_NS_URI, nsc.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX));
    assertEquals(XMLConstants.DEFAULT_NS_PREFIX, nsc.getPrefix(XMLConstants.NULL_NS_URI));
    assertNull(nsc.getNamespaceURI(UNKNOWN));
    assertNull(nsc.getPrefix(UNKNOWN_URI));
  }
  
  public void testMapConstructor() throws Exception {
    Map<String, String> map = new HashMap<String, String>();
    map.put(A, A_URI);
    map.put(B, B_URI);
    
    // Should contain a mapping for 'a', 'b' and XMLConstants.DEFAULT_NS_PREFIX
    NamespaceContext nsc1 = new OneToOneNamespaceContext(map);
    assertEquals(XMLConstants.NULL_NS_URI, nsc1.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX));
    assertEquals(XMLConstants.DEFAULT_NS_PREFIX, nsc1.getPrefix(XMLConstants.NULL_NS_URI));
    assertEquals(A_URI, nsc1.getNamespaceURI(A));
    assertEquals(A, nsc1.getPrefix(A_URI));
    assertEquals(B_URI, nsc1.getNamespaceURI(B));
    assertEquals(B, nsc1.getPrefix(B_URI));
    assertNull(nsc1.getNamespaceURI(UNKNOWN));
    assertNull(nsc1.getPrefix(UNKNOWN_URI));
    
    map.put(XMLConstants.DEFAULT_NS_PREFIX, CUSTOM_URI);

    // Should contain a mapping for 'a', 'b' and a custome mapping for XMLConstants.DEFAULT_NS_PREFIX
    NamespaceContext nsc2 = new OneToOneNamespaceContext(map);
    assertEquals(CUSTOM_URI, nsc2.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX));
    assertEquals(XMLConstants.DEFAULT_NS_PREFIX, nsc2.getPrefix(CUSTOM_URI));
    assertEquals(A_URI, nsc2.getNamespaceURI(A));
    assertEquals(A, nsc2.getPrefix(A_URI));
    assertEquals(B_URI, nsc2.getNamespaceURI(B));
    assertEquals(B, nsc2.getPrefix(B_URI));
    assertNull(nsc2.getNamespaceURI(UNKNOWN));
    assertNull(nsc2.getPrefix(UNKNOWN_URI));
  }
  
  public void testChainedContruction() throws Exception {
    // Should contain a mapping for 'a', 'b' and XMLConstants.DEFAULT_NS_PREFIX
    NamespaceContext nsc = new OneToOneNamespaceContext().put(A, A_URI)
                                                         .put(B, B_URI);
    assertEquals(XMLConstants.NULL_NS_URI, nsc.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX));
    assertEquals(XMLConstants.DEFAULT_NS_PREFIX, nsc.getPrefix(XMLConstants.NULL_NS_URI));
    assertEquals(A_URI, nsc.getNamespaceURI(A));
    assertEquals(A, nsc.getPrefix(A_URI));
    assertEquals(B_URI, nsc.getNamespaceURI(B));
    assertEquals(B, nsc.getPrefix(B_URI));
    assertNull(nsc.getNamespaceURI(UNKNOWN));
    assertNull(nsc.getPrefix(UNKNOWN_URI));
  }
  
  public void testIterators() throws Exception {
    NamespaceContext nsc = new OneToOneNamespaceContext().put(A, A_URI)
                                                         .put(B, B_URI);
    for (String uri : Arrays.asList(XMLConstants.NULL_NS_URI, A_URI, B_URI)) {
      Iterator iter = nsc.getPrefixes(uri);
      assertTrue(iter.hasNext());
      assertEquals(nsc.getPrefix(uri), iter.next());
      assertFalse(iter.hasNext());
    }
  }
  
}
