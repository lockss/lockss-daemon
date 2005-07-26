/*
 * $Id: TestXStreamSerializer.java,v 1.2 2005-07-26 17:28:36 thib_gc Exp $
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

import org.lockss.app.LockssApp;

/*
 * This source file contains more than one top-level definition.
 */

/**
 * <p>Tests the {@link org.lockss.util.XStreamSerializer} class.</p>
 * @author Thib Guicherd-Callin
 */
public class TestXStreamSerializer
    extends ObjectSerializerTest {
  
  /**
   * <p>Tests if post-deserialization works with one object containing
   * a map.</p>
   * <p>A much tougher test can be found in
   * {@link TestXStreamSerializer14#testPostDeserialization_TortureTest()}.</p>
   * @throws Exception if an unexpected or unhandled problem arises.
   * @see TestXStreamSerializer14#testPostDeserialization_TortureTest()
   */
  public void testPostDeserialization()
      throws Exception {
    
    // Set up needed objects
    ObjectSerializer serializer = makeObjectSerializer_ExtMapBean();
    ObjectSerializer deserializer = makeObjectSerializer_ExtMapBean();
    ExtMapBean original = new PostDeserializationMap();
    original.setMap(makeSample_TypedEntryMap().m_map);
    ExtMapBean clone;
    StringWriter writer = new StringWriter();
    StringReader reader;
    
    // Round trip
    serializer.serialize(writer, original);
    reader = new StringReader(writer.toString());
    clone = (ExtMapBean)deserializer.deserialize(reader);
    
    // Tests
    assertFalse("Original map contains the secret postdeserialization key.",
        original.getMap().containsKey("postdeserialization.key"));
    assertFalse("Original map contains the secret postdeserialization value.",
        original.getMap().containsValue("postdeserialization.value"));
    assertFalse("Original map and clone are equal.",
        original.getMap().equals(clone.getMap()));
    assertTrue("Clone map does not contain the secret postdeserialization key.",
        clone.getMap().containsKey("postdeserialization.key"));
    assertTrue("Clone map does not contain the secret postdeserialization value.",
        clone.getMap().containsValue("postdeserialization.value"));
    assertEquals("Clone map does not map the secret postdeserialization key to "
        + "the secret postdeserialization value.",
        "postdeserialization.value",
        clone.getMap().get("postdeserialization.key"));
  }

  protected ObjectSerializer makeObjectSerializer_ExtMapBean() {
    return new XStreamSerializer(null);
  }
  
}

/**
 * <p>This class adds post-deserialization processing to
 * {@link ExtMapBean} for the purposes of testing it.</p>
 * @author Thib Guicherd-Callin
 */
class PostDeserializationMap extends ExtMapBean {
  
  /**
   * <p>Leaves a post-deserialization trace by adding a key and a
   * value to this instance's map.</p>
   * <p>The key is {@link #KEY} and the value is {@link #VALUE}.</p>
   * @param lockssContext
   */
  protected void postUnmarshal(LockssApp lockssContext) {
    getMap().put(KEY, VALUE);
  }
  
  /**
   * <p>A secret post-deserialization key.</p>
   */
  private static final String KEY = "postdeserialization.key";
  
  /**
   * <p>A secret post-deserialization value associated with the key
   * {@link #KEY}.</p>
   */
  private static final String VALUE = "postdeserialization.value";
}
