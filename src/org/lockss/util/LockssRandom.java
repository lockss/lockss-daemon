/*
 * $Id: LockssRandom.java,v 1.6 2008-11-02 21:15:39 tlipkis Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

/** Extension of java.util.Random that adds some missing methods. */
public class LockssRandom extends java.util.Random {

  public LockssRandom() {
    super();
  }

  public LockssRandom(long seed) {
    super(seed);
  }

  /** Return the next pseudorandom number with <code>bits</code> random
   * bits. */
  public long nextBits(int bits) {
    if (bits <= 32) {
      return next(bits) & (long)0xffffffffL;
    }
    return (((long)next(bits - 32)) << 32) | next(32) & (long)0xffffffffL;
  }

  /** Returns a pseudorandom, uniformly distributed int value between 0
   * (inclusive) and the specified value (exclusive), drawn from this
   * random number generator's sequence.  The algorithm is similar to that
   * given in the javadoc for {@link java.util.Random#nextInt(int)}.
   */
  public long nextLong(long n) {
    if (n<=0) {
      throw new IllegalArgumentException("n must be > 0");
    }
    long bits, val;
    do {
      bits = (nextLong() >>> 1);
      val = bits % n;
    } while(bits - val + (n-1) < 0);
    return val;
  }
}
