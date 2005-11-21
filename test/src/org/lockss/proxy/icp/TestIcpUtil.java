/*
 * $Id: TestIcpUtil.java,v 1.3 2005-11-21 21:32:48 thib_gc Exp $
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

package org.lockss.proxy.icp;

import java.nio.ByteBuffer;

import org.lockss.test.LockssTestCase;

/**
 * <p>Tests the {@link IcpUtil} class.</p>
 * @author Thib Guicherd-Callin
 */
public class TestIcpUtil extends LockssTestCase {

  /**
   * <p>Tests the {@link IcpUtil#getIpFromBuffer(ByteBuffer, int)}
   * method.</p>
   * @throws Exception if something unexpected happens.
   */
  public void testGetIpFromBuffer() throws Exception {
    byte[] array = {
        (byte)0, (byte)0, (byte)0, (byte)0,
        (byte)12, (byte)34, (byte)56, (byte)78,
        (byte)0, (byte)0, (byte)0, (byte)0
    };
    ByteBuffer buffer = ByteBuffer.wrap(array);
    assertEquals(new byte[] {(byte)12, (byte)34, (byte)56, (byte)78},
                 IcpUtil.getIpFromBuffer(buffer, 4).getAddress());
  }

  /**
   * <p>Tests the {@link IcpUtil#getUrlFromBuffer(ByteBuffer, int)}
   * method.</p>
   * @throws Exception if something unexpected happens.
   */
  public void testGetUrlFromBuffer() throws Exception {
    byte[] array = {
        (byte)0, (byte)0, (byte)0, (byte)0,
        (byte)'h', (byte)'t', (byte)'t', (byte)'p', (byte)':',
        (byte)'/', (byte)'/', (byte)'w', (byte)'w', (byte)'w',
        (byte)'.', (byte)'l', (byte)'o', (byte)'c', (byte)'k',
        (byte)'s', (byte)'s', (byte)'.', (byte)'o', (byte)'r',
        (byte)'g', (byte)'/', (byte)0, (byte)0,
        (byte)0, (byte)0, (byte)0, (byte)0
    };
    ByteBuffer buffer = ByteBuffer.wrap(array);
    assertEquals("http://www.lockss.org/",
                 IcpUtil.getUrlFromBuffer(buffer, 4));
  }

  /**
   * <p>Tests the {@link IcpUtil#isValidOpcode} method.</p>
   */
  public void testValidOpcodes() {
    // Careful not to run into an infinite overflow loop!
    // for (byte op = Byte.MIN_VALUE ; op <= Byte.MAX_VALUE ; op++)
    // doesn't work, and
    // for (byte op = Byte.MIN_VALUE ; op < Byte.MAX_VALUE ; op++)
    // requires an extra test for Byte.MAX_VALUE.
    byte op = Byte.MIN_VALUE;
    do {
      assertEquals(
             ( 1 <= op && op <=  4)  // QUERY, HIT, MISS, ERR
          || (10 <= op && op <= 11)  // SECHO, DECHO
          || (21 <= op && op <= 23), // MISS_NOFETCH, DENIED, HIT_OBJ
          IcpUtil.isValidOpcode(op)
      );
      op++;
    } while (op > Byte.MIN_VALUE);
  }

}
