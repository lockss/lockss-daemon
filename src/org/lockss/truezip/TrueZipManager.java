/*
 * $Id$
 *

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.truezip;

import java.io.*;
import java.util.*;
import java.security.*;

import de.schlichtherle.truezip.file.*;
import de.schlichtherle.truezip.fs.FsSyncException;

import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.config.*;

/** Sets global TrueZip config, manages a TFileCache instance
 */
public class TrueZipManager extends BaseLockssManager
  implements ConfigurableManager  {

  protected static Logger log = Logger.getLogger("TrueZipManager");

  public static final String PREFIX = Configuration.PREFIX + "truezip.";

  public static final String PARAM_CACHE_DIR = PREFIX + "cacheDir";
  public static final String DEFAULT_CACHE_DIR = ".";

  /** Target maximum size of TFile cache in MB.  Cache may grow larger if
   * necessary. */
  public static final String PARAM_CACHE_MAX_MB =
    PREFIX + "cacheMaxMb";
  public static final long DEFAULT_CACHE_MAX_MB = 100;

  /** Target maximum number of TFiles in TFiles cache.  Cache may grow
   * larger if necessary. */
  public static final String PARAM_CACHE_MAX_FILES =
    PREFIX + "cacheMaxFiles";
  public static final int DEFAULT_CACHE_MAX_FILES = 100;

  String cacheDir;
  TFileCache tfc;

  public void startService() {
    super.startService();
    TConfig config = TConfig.get();
    config.setLenient(false);
//     System.setProperty("de.schlichtherle.truezip.socket.spi.IOPoolService",
// 		       org.lockss.truezip.TempFilePool.class.getName());
  }

  public void stopService() {
    super.stopService();
    if (tfc != null) {
      tfc.clear();
    }
  }

  public void setConfig(Configuration config, Configuration oldConfig,
			Configuration.Differences changedKeys) {
    if (changedKeys.contains(PREFIX)) {
      if (cacheDir == null) {
	cacheDir = config.get(PARAM_CACHE_DIR, DEFAULT_CACHE_DIR);
	tfc = new TFileCache(cacheDir);
      }
      long maxMb = config.getLong(PARAM_CACHE_MAX_MB, DEFAULT_CACHE_MAX_MB);
      int maxFiles = config.getInt(PARAM_CACHE_MAX_FILES,
				   DEFAULT_CACHE_MAX_FILES);
      tfc.setMaxSize(maxMb * 1024 * 1024, maxFiles);
    }
  }

  public TFileCache getTFileCache() {
    return tfc;
  }

  public TFileCache.Entry getCachedTFileEntry(CachedUrl cu)
      throws IOException {
    return getTFileCache().getCachedTFileEntry(cu);
  }
  
  public TFile getCachedTFile(CachedUrl cu) throws IOException {
    return getTFileCache().getCachedTFile(cu);
  }

  /**
   * Marks an entry as flushable only after its Tfile is unmounted.
   * 
   * @param cu
   *          A CachedUrl with the CU used to locate the TFile in the cache.
   */
  public void setFlushAfterUnmountOnly(CachedUrl cu) {
    getTFileCache().setFlushAfterUnmountOnly(cu);
  }

  /**
   * Unmounts a TFile and removes it from the cache.
   * 
   * @param tf
   *          A TFile to be freed.
   * @param cu
   *          A CachedUrl with the CU used to locate the TFile in the cache, or
   *          <code>null</code> if the TFile is not in the cache.
   * @throws FsSyncException
   */
  public void freeTFile(TFile tf, CachedUrl cu) throws FsSyncException {
    getTFileCache().freeTFile(tf, cu);
  }

  /**
   * Marks a TFile as flushable.
   * 
   * @param tf
   *          A TFile to be marked as flushable.
   * @param cu
   *          A CachedUrl with the CU used to locate the TFile in the cache, or
   *          <code>null</code> if the TFile is not in the cache.
   * @throws FsSyncException
   */
  public void markArchiveAsFlushable(TFile tf, CachedUrl cu)
      throws FsSyncException {
    getTFileCache().markArchiveAsFlushable(tf, cu);
  }
}
