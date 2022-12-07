/*

Copyright (c) 2000-2018, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.util;

import java.util.Comparator;

/**
 * <p>
 * A {@link Comparator} that sorts strings with {@code '/'} sorting before any
 * other character, to match the traditional LOCKSS URL traversal order.
 * </p>
 * 
 * @since 1.5.0
 */
public class PreOrderComparator implements Comparator<String> {

  public static final Comparator<String> INSTANCE = new PreOrderComparator();
  
  @Override
  public int compare(String str1, String str2) {
    return preOrderCompareTo(str1, str2);
  }

  /** 
   * Comparison that matches the traversal order of CachedUrlSet iterators.
   * Differs from natural sort order in that '/' sorts before any other
   * char, because the tree traversal is pre-order.
   */
  public static int preOrderCompareTo(String str1, String str2) {
    int len1 = str1.length();
    int len2 = str2.length();
    int n = Math.min(len1, len2);

    for (int ix = 0 ; ix < n ; ++ix) {
      char c1 = str1.charAt(ix);
      char c2 = str2.charAt(ix);
      if (c1 != c2) {
        if (c1 == '/') {
          return -1;
        }
        if (c2 == '/') {
          return 1;
        }
        return c1 - c2;
      }
    }
    return len1 - len2;
  }

  /** 
   * Comparison that matches the traversal order of CachedUrlSet iterators.
   * Differs from natural sort order in that '/' sorts before any other
   * char, because the tree traversal is pre-order.  Null sorts after all
   * nun-null strings.
   */
  public static int preOrderCompareToNullHigh(String str1, String str2) {
    if (str1 == null) {
      return (str2 == null) ? 0 : 1;
    }
    if (str2 == null) {
      return -1;
    }
    return preOrderCompareTo(str1, str2);
  }

}
