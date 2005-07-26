/*
 * $Id: TestXStreamSerializer14.java,v 1.1 2005-07-26 17:28:35 thib_gc Exp $
 */

/*

Copyright (c) 2002-2005 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.StringReader;
import java.io.StringWriter;

import org.custommonkey.xmlunit.XMLTestCase;
import org.lockss.app.LockssApp;

/**
 * <p>Tests the functionality of the
 * {@link org.lockss.util.XStreamSerializer} class that requires at
 * least Java 1.4.</p>
 * @author Thib Guicherd-Callin
 */
public class TestXStreamSerializer14 extends XMLTestCase {

  /**
   * <p>Gives the post-deserialization mechanism a thorough test.</p>
   * <p>This test consists of a cyclic graph of objects of type D.
   * Type D inherits from type C which inherits from type B which
   * inherits from type A. Types C and A define post-deserialization
   * processing (but not D and B). The class hierarchy is such that
   * the test will fail unless all post-deserialization methods at all
   * levels get called.</p>
   * @throws Exception if an unexpected or unhandled problem arises.
   */
  public void testPostDeserialization_TortureTest()
      throws Exception {

    /* Local class */
    class A {
      private boolean explode = true;
      public void detonate() {
        if (explode) {
          fail("Failed post-deserialization torture test in class A.");
        }
      }
      protected void postUnmarshal(LockssApp lockssContext) {
        explode = false; // defuse bomb
      }
    }
    
    /* Local class */
    class B extends A {
      // Separate A and C's post-deserialization methods by a level
    }
    
    /* Local class */
    class C extends B {
      private boolean explode = true;
      public void detonate() {
        super.detonate();
        if (explode) {
          fail("Failed post-deserialization torture test in class C.");
        }
      }
      protected void postUnmarshal(LockssApp lockssContext) {
        super.postUnmarshal(lockssContext);
        explode = false; // defuse bomb
      }
    }
    
    /* Local class */
    class D extends C {
      private D first;
      private D second;
    }
    
    // Set up object graph
    D d1 = new D(); 
    D d2 = new D();
    D d3 = new D();
    D d4 = new D();
    D d5 = new D();
    d1.first = d2; d1.second = d4;
    d2.first = d3; d2.second = d5;
    d3.first = d1; d3.second = d2;
    d4.first = d2; d4.second = d5;
    d5.first = d2; d5.second = d3;
    
    // Set up needed objects
    ObjectSerializer serializer = new XStreamSerializer(null);
    ObjectSerializer deserializer = new XStreamSerializer(null);
    StringWriter writer = new StringWriter();
    StringReader reader;
    
    // Round trip
    serializer.serialize(writer, d1);
    reader = new StringReader(writer.toString());
    D d = (D)deserializer.deserialize(reader);
    
    // Tests
    d.detonate(); // aka d1
    d.first.detonate(); // aka d2
    d.first.first.detonate(); // aka d3
    d.second.detonate(); // aka d4
    d.second.second.detonate(); // aka d5
  }
  
}
