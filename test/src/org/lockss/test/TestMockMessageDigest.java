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

package org.lockss.test;

import junit.framework.TestCase;

public class TestMockMessageDigest extends LockssTestCase {

  public TestMockMessageDigest(String msg){
    super(msg);
  }

  public void testUpdateNoBytes(){
    MockMessageDigest dig = new MockMessageDigest();
    assertEquals(-1, dig.getUpdatedByte());
  }

  public void testUpdateSingleByte(){
    MockMessageDigest dig = new MockMessageDigest();
    byte input = 4;
    dig.update(input);
    assertEquals(input, dig.getUpdatedByte());
  }

  public void testUpdatedSetOfBytes(){
    MockMessageDigest dig = new MockMessageDigest();
    dig.update((byte)1);
    dig.update((byte)2);
    dig.update((byte)3);
    dig.update((byte)4);

    assertEquals((byte)1, dig.getUpdatedByte());
    assertEquals((byte)2, dig.getUpdatedByte());
    assertEquals((byte)3, dig.getUpdatedByte());
    assertEquals((byte)4, dig.getUpdatedByte());
  }

  public void testUpdatedByteArray(){
    MockMessageDigest dig = new MockMessageDigest();
    byte[] input = {1, 2, 3, 4};
    dig.update(input);

    assertEquals((byte)1, dig.getUpdatedByte());
    assertEquals((byte)2, dig.getUpdatedByte());
    assertEquals((byte)3, dig.getUpdatedByte());
    assertEquals((byte)4, dig.getUpdatedByte());
  }

  public void testUpdatedByteArrayWithOffset(){
    MockMessageDigest dig = new MockMessageDigest();
    byte[] input = {1, 2, 3, 4, 5, 6, 7};
    dig.update(input, 1, 3);

    assertEquals((byte)2, dig.getUpdatedByte());
    assertEquals((byte)3, dig.getUpdatedByte());
    assertEquals((byte)4, dig.getUpdatedByte());
    assertEquals((byte)-1, dig.getUpdatedByte());
  }

  public void testGetUpdatedBytes(){
    MockMessageDigest dig = new MockMessageDigest();
    byte[] input = {1, 2, 3, 4};
    dig.update(input);
    byte[] output = new byte[4];

    assertEquals(output.length, dig.getUpdatedBytes(output));
    for (int i=0; i<4; i++){
      assertEquals(input[i], output[i]);
    }
  }

}
