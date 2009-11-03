/*
 * $Id: RandomManager.java,v 1.1.4.2 2009-11-03 23:52:02 edwardsb1 Exp $
 *

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon;

import java.util.*;
import java.security.*;

import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.config.*;

/**
 * Supplies instances of SecureRandom to other parts of the daemon.
 * Centralizes algorithm and provider defaults, allows for unit tests to
 * set seed.
 */
public class RandomManager extends BaseLockssManager
  implements ConfigurableManager  {

  protected static Logger log = Logger.getLogger("RandomManager");

  public static final String PREFIX = Configuration.PREFIX + "random.";

  /** SecureRandom algorithm */
  public static String PARAM_SECURE_RANDOM_ALGORITHM =
    PREFIX + "secureAlgorithm";
  public static String DEFAULT_SECURE_RANDOM_ALGORITHM = "SHA1PRNG";

  /** SecureRandom provider */
  public static String PARAM_SECURE_RANDOM_PROVIDER =
    PREFIX + "secureProvider";
  public static String DEFAULT_SECURE_RANDOM_PROVIDER = "SUN";

  String alg = DEFAULT_SECURE_RANDOM_ALGORITHM;
  String prov = DEFAULT_SECURE_RANDOM_PROVIDER;

  public void setConfig(Configuration config, Configuration oldConfig,
			Configuration.Differences changedKeys) {
    if (changedKeys.contains(PREFIX)) {
      alg = config.get(PARAM_SECURE_RANDOM_ALGORITHM,
		       DEFAULT_SECURE_RANDOM_ALGORITHM);
      prov = config.get(PARAM_SECURE_RANDOM_PROVIDER,
			DEFAULT_SECURE_RANDOM_PROVIDER);
      log.info("alg: " + alg + ", prov: " + prov);
      log.info("isNullString(" + prov + "): " + StringUtil.isNullString(prov));
    }
  }

  public SecureRandom getSecureRandom()
      throws NoSuchAlgorithmException, NoSuchProviderException {

    SecureRandom rng = (StringUtil.isNullString(prov)
			? SecureRandom.getInstance(alg)
			: SecureRandom.getInstance(alg, prov));
    initRandom(rng);
    return rng;
  }

  protected void initRandom(SecureRandom rng) {
  }

}
