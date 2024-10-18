/*

Copyright (c) 2000-2024 Board of Trustees of Leland Stanford Jr. University,
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
import java.security.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.lockss.app.LockssDaemon;

/**
 * Utility class for generating random Strings.
 * Replacement for the static methods in {@link
 * org.apache.commons.lang3.RandomStringUtils} which, beside being
 * deprecated, now now use SecureRandom, which causes unit test hangs
 * due to entropy starvation.  These methods will use SecureRandom
 *
 * Should be modified to use {@link org.lockss.daemon.RandomManager}
 * so secure randomness is used except during unit tests.
 */
public class RandomUtil {
  private static final Logger log = Logger.getLogger(RandomUtil.class);

  static Random rng;

  /** Lazily create the Random once RandomManager is likely to exist */
  static void lazyInit() {
    if (rng == null) {
      try {
        rng =
          LockssDaemon.getLockssDaemon().getRandomManager().getSecureRandom();
      } catch (IllegalArgumentException e) {
        log.critical("No RandomManager", e);
        rng = new Random();
      } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
        log.critical("RandomManager.getSecureRandom() threw", e);
        rng = new Random();
      }
    }
  }

  public static String randomAlphanumeric(int len) {
//     return RandomStringUtils.insecure().nextAlphanumeric(len);
    lazyInit();
    return RandomStringUtils.random(len, 0, 0, true, true, null, rng);
  }

  public static String randomAlphabetic(int len) {
//     return RandomStringUtils.insecure().nextAlphabetic(len);
    lazyInit();
    return RandomStringUtils.random(len, 0, 0, true, false, null, rng);
  }

}
