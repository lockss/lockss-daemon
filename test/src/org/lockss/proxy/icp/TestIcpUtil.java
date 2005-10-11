/*
 * $Id: TestIcpUtil.java,v 1.2 2005-10-11 05:51:04 tlipkis Exp $
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

import junit.framework.TestCase;

/**
 * <p>Tests the {@link IcpUtil} class.</p>
 * @author Thib Guicherd-Callin
 */
public class TestIcpUtil extends TestCase {

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
