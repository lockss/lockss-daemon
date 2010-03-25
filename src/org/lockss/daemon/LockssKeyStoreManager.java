/*
 * $Id: LockssKeyStoreManager.java,v 1.5.2.1 2010-03-25 07:31:30 tlipkis Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;
import java.security.*;
import javax.net.ssl.*;

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.util.*;
import static org.lockss.daemon.LockssKeyStore.LocationType;

/** Central repository of loaded keystores
 */
public class LockssKeyStoreManager
  extends BaseLockssDaemonManager implements ConfigurableManager  {

  protected static Logger log = Logger.getLogger("LockssKeyStoreManager");

  static final String PREFIX = Configuration.PREFIX + "keyMgr.";

  /** Default type for newly created keystores.  */
  public static final String PARAM_DEFAULT_KEYSTORE_TYPE =
    PREFIX + "defaultKeyStoreType";
  public static final String DEFAULT_DEFAULT_KEYSTORE_TYPE = "JCEKS";

  /** Default keystore provider.  */
  public static final String PARAM_DEFAULT_KEYSTORE_PROVIDER =
    PREFIX + "defaultKeyStoreProvider";
  public static final String DEFAULT_DEFAULT_KEYSTORE_PROVIDER = null;

  /** Root of keystore definitions.  For each keystore, pick a unique
   * identifier and use it in place of &lt;id&gt; in the following */
  public static final String PARAM_KEYSTORE = PREFIX + "keystore";

  /** keystore name, used by clients to refer to it */
  public static final String KEYSTORE_PARAM_NAME = "name";
  /** keystore file.  Only one of file, resource or url should be set */
  public static final String KEYSTORE_PARAM_FILE = "file";
  /** keystore resource.  Only one of file, resource or url should be set */
  public static final String KEYSTORE_PARAM_RESOURCE = "resource";
  /** keystore url.  Only one of file, resource or url should be set */
  public static final String KEYSTORE_PARAM_URL = "url";
  /** keystore type */
  public static final String KEYSTORE_PARAM_TYPE = "type";
  /** keystore provider */
  public static final String KEYSTORE_PARAM_PROVIDER = "provider";
  /** keystore password */
  public static final String KEYSTORE_PARAM_PASSWORD = "password";
  /** private key password */
  public static final String KEYSTORE_PARAM_KEY_PASSWORD = "keyPassword";
  /** private key password file */
  public static final String KEYSTORE_PARAM_KEY_PASSWORD_FILE =
    "keyPasswordFile";
  /** If true, and the keystore doesn't exist, a keystore with a
   * self-signed certificate will be be created. */
  public static final String KEYSTORE_PARAM_CREATE = "create";

  protected String defaultKeyStoreType = DEFAULT_DEFAULT_KEYSTORE_TYPE;
  protected String defaultKeyStoreProvider = DEFAULT_DEFAULT_KEYSTORE_PROVIDER;

  // Pseudo params for param doc
  public static final String DOC_PREFIX = PARAM_KEYSTORE + ".<id>.";

  /** Name by which daemon component(s) refer to this keystore */
  public static final String PARAM_KEYSTORE_NAME =
    DOC_PREFIX + KEYSTORE_PARAM_NAME;
  /** Keystore filename.  Only one of file, resource or url should be set */
  public static final String PARAM_KEYSTORE_FILE =
    DOC_PREFIX + KEYSTORE_PARAM_FILE;
  /** Keystore resource.  Only one of file, resource or url should be set */
  public static final String PARAM_KEYSTORE_RESOURCE =
    DOC_PREFIX + KEYSTORE_PARAM_RESOURCE;
  /** Keystore url.  Only one of file, resource or url should be set */
  public static final String PARAM_KEYSTORE_URL =
    DOC_PREFIX + KEYSTORE_PARAM_URL;
  /** Keystore type (JKS, JCEKS, etc.) */
  public static final String PARAM_KEYSTORE_TYPE =
    DOC_PREFIX + KEYSTORE_PARAM_TYPE;
  /** Keystore provider (SunJCE, etc.) */
  public static final String PARAM_KEYSTORE_PROVIDER =
    DOC_PREFIX + KEYSTORE_PARAM_PROVIDER;
  /** Keystore password.  Default is machine's fqdn */
  public static final String PARAM_KEYSTORE_PASSWORD =
    DOC_PREFIX + KEYSTORE_PARAM_PASSWORD;
  /** Private key password */
  public static final String PARAM_KEYSTORE_KEY_PASSWORD =
    DOC_PREFIX + KEYSTORE_PARAM_KEY_PASSWORD;
  /** private key password file */
  public static final String PARAM_KEYSTORE_KEY_PASSWORD_FILE =
    DOC_PREFIX + KEYSTORE_PARAM_KEY_PASSWORD_FILE;
  /** If true, and the keystore doesn't exist, a keystore with a
   * self-signed certificate will be be created. */
  public static final String PARAM_KEYSTORE_CREATE =
    DOC_PREFIX + KEYSTORE_PARAM_CREATE;
  public static boolean DEFAULT_CREATE = false;


  protected Map<String,LockssKeyStore> keystoreMap =
    new HashMap<String,LockssKeyStore>();


  public void startService() {
    super.startService();
    loadKeyStores();
  }

  public synchronized void setConfig(Configuration config,
				     Configuration prevConfig,
				     Configuration.Differences changedKeys) {
    if (changedKeys.contains(PREFIX)) {
      defaultKeyStoreType = config.get(PARAM_DEFAULT_KEYSTORE_TYPE,
				       DEFAULT_DEFAULT_KEYSTORE_TYPE);
      defaultKeyStoreProvider = config.get(PARAM_DEFAULT_KEYSTORE_PROVIDER,
					   DEFAULT_DEFAULT_KEYSTORE_PROVIDER);
      if (changedKeys.contains(PARAM_KEYSTORE)) {
	configureKeyStores(config);
	// defer initial set of keystore loading until startService
	if (isInited()) {
	  // load any newly added keystores
	  loadKeyStores();
	}
      }
    }
  }

  /** Return the named LockssKeyStore or null */
  public LockssKeyStore getLockssKeyStore(String name) {
    return keystoreMap.get(name);
  }

  /** Convenience method to return the KeyManagerFactory from the named
   * LockssKeyStore, or null */
  public KeyManagerFactory getKeyManagerFactory(String name) {
    LockssKeyStore lk = getLockssKeyStore(name);
    if (lk != null) {
      return lk.getKeyManagerFactory();
    }
    return null;
  }

  /** Convenience method to return the TrustManagerFactory from the named
   * LockssKeyStore, or null */
  public TrustManagerFactory getTrustManagerFactory(String name) {
    LockssKeyStore lk = getLockssKeyStore(name);
    if (lk != null) {
      return lk.getTrustManagerFactory();
    }
    return null;
  }

  /** Create LockssKeystores from config subtree below {@link
   * #PARAM_KEYSTORE} */
  void configureKeyStores(Configuration config) {
    Configuration allKs = config.getConfigTree(PARAM_KEYSTORE);
    for (Iterator iter = allKs.nodeIterator(); iter.hasNext(); ) {
      String id = (String)iter.next();
      Configuration oneKs = allKs.getConfigTree(id);
      try {
	LockssKeyStore lk = createLockssKeyStore(oneKs);
	String name = lk.getName();
	if (name == null) {
	  log.error("KeyStore definition missing name: " + oneKs);
	  continue;
	}
	LockssKeyStore old = keystoreMap.get(name);
	if (lk.isSameLocation(old)) {
	  log.error("Duplicate keystore definition: " + oneKs);
	  log.error("Using original definition: "
		    + keystoreMap.get(name));
	  continue;
	}

	log.debug("Adding keystore " + lk.getName());
	keystoreMap.put(name, lk);

      } catch (Exception e) {
	log.error("Couldn't create keystore: " + oneKs, e);
      }
    }
  }

  /** Create LockssKeystore from a config subtree */
  LockssKeyStore createLockssKeyStore(Configuration config) {
    log.debug2("Creating LockssKeyStore from config: " + config);
    String name = config.get(KEYSTORE_PARAM_NAME);
    LockssKeyStore lk = new LockssKeyStore(name);

    String file = config.get(KEYSTORE_PARAM_FILE);
    String resource = config.get(KEYSTORE_PARAM_RESOURCE);
    String url = config.get(KEYSTORE_PARAM_URL);

    if (!StringUtil.isNullString(file)) {
      lk.setLocation(file, LocationType.File);
    } else if (!StringUtil.isNullString(resource)) {
      lk.setLocation(file, LocationType.Resource);
    } else if (!StringUtil.isNullString(url)) {
      lk.setLocation(file, LocationType.Url);
    }

    lk.setType(config.get(KEYSTORE_PARAM_TYPE, defaultKeyStoreType));
    lk.setProvider(config.get(KEYSTORE_PARAM_PROVIDER,
			      defaultKeyStoreProvider));
    lk.setPassword(config.get(KEYSTORE_PARAM_PASSWORD));
    lk.setKeyPassword(config.get(KEYSTORE_PARAM_KEY_PASSWORD));
    lk.setKeyPasswordFile(config.get(KEYSTORE_PARAM_KEY_PASSWORD_FILE));
    lk.setMayCreate(config.getBoolean(KEYSTORE_PARAM_CREATE, DEFAULT_CREATE));
    return lk;
  }

  void loadKeyStores() {
    List<LockssKeyStore> lst =
      new ArrayList<LockssKeyStore>(keystoreMap.values());
    for (LockssKeyStore lk : lst) {
      try {
	lk.load();
      } catch (Exception e) {
	log.error("Can't load keystore " + lk.getName(), e);
	keystoreMap.remove(lk.getName());
      }
    }
  }
}
