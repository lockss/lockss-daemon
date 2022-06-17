/*
 * $Id$
 *

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.*;
import java.lang.reflect.*;
import java.util.zip.*;

import org.apache.commons.io.IOUtils;
import org.lockss.test.*;
import org.lockss.daemon.*;
import org.lockss.plugin.simulated.*;
import org.lockss.util.*;
import org.lockss.app.*;
import org.lockss.config.*;

/**
 * Utilities for manipulating plugins and their components in tests
 */

public class PluginTestUtil {
  static Logger log = Logger.getLogger("PluginTestUtil");
  static List aulist = new LinkedList();
  static final Pattern COMPRESSED_PATTERN = Pattern.compile(
      "\\.(zip|tar\\.gz|tgz)$",
      Pattern.CASE_INSENSITIVE
  );

  public static void registerArchivalUnit(Plugin plug, ArchivalUnit au) {
    PluginManager mgr = getPluginManager();
    log.debug3(mgr.toString());
    String plugid = au.getPluginId();
    log.debug("registerArchivalUnit plugin = " + plug +
	      "au = " + au);
    if (plug != mgr.getPlugin(plugid)) {
      try {
	PrivilegedAccessor.invokeMethod(mgr, "setPlugin",
					ListUtil.list(plugid, plug).toArray());
      } catch (Exception e) {
	log.error("Couldn't register AU", e);
	throw new RuntimeException(e.toString());
      }
    }
    PluginTestable tp = (PluginTestable)plug;
    tp.registerArchivalUnit(au);
    try {
      PrivilegedAccessor.invokeMethod(mgr, "putAuInMap",
				      ListUtil.list(au).toArray());
    } catch (InvocationTargetException e) {
      log.error("Couldn't register AU", e);
      log.error("Nested", e.getTargetException());
      throw new RuntimeException(e.toString());
    } catch (Exception e) {
      log.error("Couldn't register AU", e);
      throw new RuntimeException(e.toString());
    }
    aulist.add(au);
  }

  public static void registerArchivalUnit(ArchivalUnit au) {
    PluginManager mgr = getPluginManager();
    String plugid = au.getPluginId();
    Plugin plug = mgr.getPlugin(plugid);
    log.debug("registerArchivalUnit id = " + au.getAuId() +
	      ", pluginid = " + plugid + ", plugin = " + plug);
    if (plug == null) {
      MockPlugin mp = new MockPlugin();
      mp.setPluginId(plugid);
      plug = mp;
    }
    registerArchivalUnit(plug, au);
  }

  /*
  public static void registerArchivalUnit(ArchivalUnit au) {
    PluginManager mgr = getPluginManager();
    log.debug(mgr.toString());
    String plugid = au.getPluginId();
    Plugin plug = mgr.getPlugin(plugid);
    log.debug("registerArchivalUnit id = " + au.getAuId() +
	      ", pluginid = " + plugid + ", plugin = " + plug);
    if (plug == null) {
      MockPlugin mp = new MockPlugin();
      mp.setPluginId(plugid);
      plug = mp;
      try {
	PrivilegedAccessor.invokeMethod(mgr, "setPlugin",
					ListUtil.list(plugid, mp).toArray());
      } catch (Exception e) {
	log.error("Couldn't register AU", e);
	throw new RuntimeException(e.toString());
      }
    }
    PluginTestable tp = (PluginTestable)plug;
    tp.registerArchivalUnit(au);
    try {
      PrivilegedAccessor.invokeMethod(mgr, "putAuMap",
				      ListUtil.list(plug, au).toArray());
    } catch (Exception e) {
      log.error("Couldn't register AU", e);
      throw new RuntimeException(e.toString());
    }
    aulist.add(au);
  }
  */
  public static void unregisterArchivalUnit(ArchivalUnit au) {
    PluginManager mgr = getPluginManager();
    String plugid = au.getPluginId();
    Plugin plug = mgr.getPlugin(plugid);
    if (plug != null) {
      PluginTestable tp = (PluginTestable)plug;
      tp.unregisterArchivalUnit(au);
      aulist.remove(au);
    }
    mgr.stopAu(au, new AuEvent(AuEvent.Type.Delete, false));
  }

  public static void unregisterAllArchivalUnits() {
    log.debug("unregisterAllArchivalUnits()");
    List aus = new ArrayList(aulist);
    for (Iterator iter = aus.iterator(); iter.hasNext(); ) {
      unregisterArchivalUnit((ArchivalUnit)iter.next());
    }
  }

  public static Plugin findPlugin(String pluginName) {
    PluginManager pluginMgr = getPluginManager();
    String key = pluginMgr.pluginKeyFromName(pluginName);
    pluginMgr.ensurePluginLoaded(key);
    return pluginMgr.getPlugin(key);
  }

  public static Plugin findPlugin(Class pluginClass) {
    return findPlugin(pluginClass.getName());
  }

  public static ArchivalUnit createAu(Plugin plugin,
				      Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    return getPluginManager().createAu(plugin, auConfig,
                                       new AuEvent(AuEvent.Type.Create, false));
  }

  public static ArchivalUnit createAndStartAu(Plugin plugin,
					      Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    return startAu(createAu(plugin, auConfig));
  }

  static ArchivalUnit startAu(ArchivalUnit au) {
    LockssDaemon daemon = au.getPlugin().getDaemon();
    daemon.getHistoryRepository(au).startService();
    daemon.getLockssRepository(au).startService();
    daemon.getNodeManager(au).startService();
    return au;
  }

  public static ArchivalUnit createAu(String pluginName,
				      Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    return createAu(findPlugin(pluginName), auConfig);
  }

  public static ArchivalUnit createAndStartAu(String pluginName,
					      Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    return createAndStartAu(findPlugin(pluginName), auConfig);
  }

  public static ArchivalUnit createAu(Class pluginClass,
				      Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    return createAu(findPlugin(pluginClass.getName()), auConfig);
  }

  public static ArchivalUnit createAndStartAu(Class pluginClass,
					      Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    return createAndStartAu(findPlugin(pluginClass.getName()), auConfig);
  }

  public static SimulatedArchivalUnit createSimAu(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    return (SimulatedArchivalUnit)createAu(findPlugin(SimulatedPlugin.class),
					   auConfig);
  }

  static Configuration getAuConfig(TdbAu tau) {
    PluginManager mgr = getPluginManager();
    Plugin plugin = tau.getPlugin(mgr);
    TitleConfig tc = new TitleConfig(tau, plugin);
    return tc.getConfig();
  }

  public static ArchivalUnit createAu(TdbAu tau)
      throws ArchivalUnit.ConfigurationException {
    PluginManager mgr = getPluginManager();
    Plugin plugin = findPlugin(tau.getPluginId());
    return createAu(plugin, getAuConfig(tau));
  }

  public static ArchivalUnit createAu(Plugin plugin, TdbAu tau)
      throws ArchivalUnit.ConfigurationException {
    return createAu(plugin, getAuConfig(tau));
  }

  public static ArchivalUnit createAndStartAu(TdbAu tau)
      throws ArchivalUnit.ConfigurationException {
    return startAu(createAu(tau));
  }

  public static ArchivalUnit createAndStartAu(Plugin plugin, TdbAu tau)
      throws ArchivalUnit.ConfigurationException {
    return startAu(createAu(plugin, tau));
  }

  public static SimulatedArchivalUnit
    createAndStartSimAu(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    return createAndStartSimAu(SimulatedPlugin.class, auConfig);
  }

  public static SimulatedArchivalUnit
    createAndStartSimAu(Class pluginClass, Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    return (SimulatedArchivalUnit)createAndStartAu(pluginClass, auConfig);
  }

  public static void crawlSimAu(SimulatedArchivalUnit sau) {
    if (!sau.hasContentTree()) {
//       log.debug("Creating simulated content tree: " + sau.getParamMap());
      sau.generateContentTree();
    }
    log.debug("Crawling simulated content");
    NoCrawlEndActionsFollowLinkCrawler crawler =
      new NoCrawlEndActionsFollowLinkCrawler(sau, new MockAuState());
    //crawler.setCrawlManager(crawlMgr);
    crawler.doCrawl();
  }


  public static boolean copyAu(ArchivalUnit fromAu, ArchivalUnit toAu) {
    return copyAu(fromAu, toAu, null, null, null);
  }

  public static boolean copyAu(ArchivalUnit fromAu, ArchivalUnit toAu,
			       String ifMatch) {
    return copyAu(fromAu, toAu, ifMatch, null, null);
  }

  public static boolean copyAu(ArchivalUnit fromAu, ArchivalUnit toAu,
			       String ifMatch, String pat, String rep) {
    return copyCus(fromAu.getAuCachedUrlSet(), toAu, ifMatch, pat, rep);
  }

  public static boolean copyCus(CachedUrlSet fromCus, ArchivalUnit toAu) {
    return copyCus(fromCus, toAu, null, null, null);
  }

  public static boolean copyCus(CachedUrlSet fromCus, ArchivalUnit toAu,
				String ifMatch, String pat, String rep) {
    boolean res = true;
    ArchivalUnit fromAu = fromCus.getArchivalUnit();
    Pattern pattern = null;
    if (pat != null) {
      pattern = Pattern.compile(pat);
    }
    Pattern ifMatchPat = null;
    if (ifMatch != null) {
      ifMatchPat = Pattern.compile(ifMatch);
    }
    for (CachedUrl cu : fromCus.getCuIterable()) {
      try {
        String fromUrl = cu.getUrl();
        String toUrl = fromUrl;
        Matcher compMat = COMPRESSED_PATTERN.matcher(fromUrl);
        if (compMat.find()) {
          log.debug("encountered a compressed file, skipping");
          continue;
        }
        if (ifMatchPat != null) {
          Matcher mat = ifMatchPat.matcher(fromUrl);
          if (!mat.find()) {
            log.debug3("no match: " + fromUrl + ", " + ifMatchPat);
            continue;
          }
        }
        if (pattern != null) {
          Matcher mat = pattern.matcher(fromUrl);
          toUrl = mat.replaceAll(rep);
        }
        CIProperties props = cu.getProperties();
        if (props == null) {
          log.debug3("in copyCus() props was null for url: " + fromUrl);
        }
        UrlCacher uc = toAu.makeUrlCacher(
            new UrlData(cu.getUnfilteredInputStream(), props, toUrl));
        uc.storeContent();
        if (!toUrl.equals(fromUrl)) {
          log.info("Copied " + fromUrl + " to " + toUrl);
        } else {
          log.debug2("Copied " + fromUrl);
        }
      } catch (Exception e) {
        log.error("Couldn't copy " + cu.getUrl(), e);
        res = false;
      } finally {
        cu.release();
      }
    }
    return res;
  }

  /**
   * Overloaded method of copyCus which takes pattern-replacement set
   */
  public static boolean copyCus(CachedUrlSet fromCus, ArchivalUnit toAu,
                                String ifMatch, PatternReplacements prps) {
    return false;
  }

  /**
   * TODO: Take the plugin as param and check it for which types of files to see as archives
   * e.g. toAu.getArchiveFileTypes().getFromCu(cu)
   * if null
   *   copy cu as normal
   * elsif in map:
   *   switch :
   *      zip - use the copyzip
   *      tar - either figure it out or Throw an error
   *         com.ice.tar.TarInputStream
   *      tgz - ibid
   *        double wrap - unwrap gzip, then TarInputStream
   * else:
   *   Throw error
   */

  /**
   * Copies all zips in from one crawl into another without modification.
   *
   * @see PluginTestUtil#copyZip(CachedUrl, ArchivalUnit, PatternReplacements)
   */
  public static boolean copyAuZip(ArchivalUnit fromAu, ArchivalUnit toAu) {
    return copyZipCus(fromAu.getAuCachedUrlSet(), toAu, null);
  }

  /**
   * Works just like {@link PluginTestUtil#copyZipCus(CachedUrlSet, ArchivalUnit, List, List)}.
   * Just gets the cachedUrlSet in fromAU and Makes lists from pat & rep.
   *
   * @see PluginTestUtil#copyZipCus(CachedUrlSet, ArchivalUnit, List, List)
   */
  public static boolean copyAuZip(ArchivalUnit fromAu, ArchivalUnit toAu, String pat, String rep) throws Exception {
    return copyZipCus(fromAu.getAuCachedUrlSet(), toAu, pat, rep);
  }

  /**
   * Works just like {@link PluginTestUtil#copyZipCus(CachedUrlSet, ArchivalUnit, List, List)}.
   * Just gets the cachedUrlSet in fromAu.
   *
   * @see PluginTestUtil#copyZipCus(CachedUrlSet, ArchivalUnit, List, List)
   */
  public static boolean copyAuZip(ArchivalUnit fromAu, ArchivalUnit toAu, List<String> pats, List<String> reps)
      throws Exception {
    return copyZipCus(fromAu.getAuCachedUrlSet(), toAu, pats, reps);
  }

  /**
  * Works just like {@link PluginTestUtil#copyZipCus(CachedUrlSet, ArchivalUnit, List, List)}.
  * Just makes lists and adds pat and rep to them.
   *
  * @see PluginTestUtil#copyZipCus(CachedUrlSet, ArchivalUnit, List, List)
 */
  public static boolean copyZipCus(CachedUrlSet fromCus, ArchivalUnit toAu, String pat, String rep) throws Exception {
    List<String> pats = Collections.singletonList(pat);
    List<String> reps = Collections.singletonList(rep);
    return copyZipCus(fromCus, toAu, pats, reps);
  }

  /**
   * Utility to copy zip files from a simulated crawl to a mock archival unit.
   * if only fromAu and toAu are provided, all zips encountered are copied without modification.
   *
   * @param fromCus the CachedUrlSet which has been crawled
   * @param toAu the Archival Unit to copy content to
   * @param pats List of regexes to search in the zipped contents
   * @param reps List of replacements for each pattern
   * @return true, if all copies attempted succeeded, false otherwise
   */
  public static boolean copyZipCus(CachedUrlSet fromCus, ArchivalUnit toAu, List<String> pats, List<String> reps)
      throws Exception {
    return copyZipCus(fromCus, toAu,
        new PatternReplacements(pats, reps));
  }

  /**
   * Iterates over all CachedUrls in a simulated crawl and copies them to a mock Au if patterns and replacements
   * are identified in the simulated crawl from the Pattern-Replacements given.
   *
   * @see PluginTestUtil#copyZipCus(CachedUrlSet, ArchivalUnit, List, List)
   */
  private static boolean copyZipCus(CachedUrlSet fromCus, ArchivalUnit toAu, PatternReplacements patReps) {
    boolean res = true;
    for (CachedUrl cu : fromCus.getCuIterable()) {
      try {
        String fromUrl = cu.getUrl();
        // only copy the file if it is a zip.
        Matcher mat = COMPRESSED_PATTERN.matcher(fromUrl);
        if (mat.find()) {
          if (patReps!=null) {
            log.debug("entering zip file to copy contents");
            copyZip(cu, toAu, patReps);
          } else {
            log.debug("copying unmodified zip");
            CIProperties props = cu.getProperties();
            UrlCacher uc = toAu.makeUrlCacher(
                new UrlData(cu.getUnfilteredInputStream(), props, fromUrl));
            uc.storeContent();
          }
        }
      } catch (Exception e) {
        log.error("Couldn't copy " + cu.getUrl(), e);
        res = false;
      } finally {
        cu.release();
      }
    }
    return res;
  }

  /**
   * Opens a single CachedUrl zip file, iterates over its contents and copies the contents if they pass
   * given pattern(s) and replacements.
   * @param cu A CachedUrl of compressed content.
   * @param toAu The ArchivalUnit to copy the cu into.
   * @param patReps Pattern-Replacement pairs to selectively copy zipped content and rename it in the new zip.
   * @throws IOException When I/O zip files encounter any number of problems.
   */
  private static void copyZip(CachedUrl cu, ArchivalUnit toAu, PatternReplacements patReps)
    throws IOException {
    boolean doCache = false;
    String zipUrl = cu.getUrl() + "!/";
    String toUrl; String toFile; String zippedFile; String fromFile;
    ZipInputStream zis = null;
    ZipOutputStream zos;
    String outZip = null;
    try {
      InputStream cuis = new ReaderInputStream(cu.openForReading());
      zis = new ZipInputStream(cuis);
      zos = new ZipOutputStream(Files.newOutputStream(
          Paths.get(cu.getArchivalUnit().getProperties().getString("root") +
              "temp.zip")));
      zos.setMethod(ZipOutputStream.DEFLATED);
      zos.setLevel(Deflater.BEST_COMPRESSION);
      ZipEntry entry;

      while ((entry = zis.getNextEntry()) != null) {
        if (entry.isDirectory()) {
          continue;
        } else if (entry.getName().endsWith(".zip") ) {
          // TODO recurse through nested
          // zipCopy(cu, toAu, zipUrl + );
        }
        fromFile = entry.getName();
        zippedFile = zipUrl + fromFile;
        for (PatternReplacementPair prp : patReps.pairs) {
          Matcher mat = prp.pat.matcher(zippedFile);
          if (mat.find()) {
            toUrl = mat.replaceAll(prp.rep);
            if (!toUrl.equals(zippedFile)) {
              log.debug("Found a zipped file match: " + zippedFile + " -> " + toUrl);
              doCache = true;
              outZip = toUrl.split("!/")[0];
              toFile = toUrl.split("!/")[1];
              ZipEntry outEntry = new ZipEntry(toFile);
              zos.putNextEntry(outEntry);
              StreamUtil.copy(zis, zos);
              zos.closeEntry();
            }
            break;
          }
        }
      }
      zos.close();
      // any copied file triggers saving the new zip stream
      if (doCache) {
        FileInputStream is = new FileInputStream(new File(
            cu.getArchivalUnit().getProperties().getString("root"),
            "temp.zip"));
        // save all the copied zip entries to a new zip on the toAu
        CIProperties props = cu.getProperties();
        if (props == null) {
          log.debug3("in copyCus() props was null for url: " + cu.getUrl());
        }
        log.debug("Storing new cu: " + outZip);
        UrlCacher uc = toAu.makeUrlCacher(
            new UrlData(is, props, outZip));
        uc.storeContent();
        is.close();
      }

    } finally {
      IOUtil.safeClose(zis);
    }
  }


  public static List<String> urlsOf(final Iterable<CachedUrl> cus) {
    return new ArrayList<String>() {{
        for (CachedUrl cu : cus) {
          add(cu.getUrl());
        }
    }};
  }

  private static PluginManager getPluginManager() {
    return
      (PluginManager)LockssDaemon.getManager(LockssDaemon.PLUGIN_MANAGER);
  }

  public static PatternReplacementPair makePatRepPair(String pat, String rep) {
    return new PatternReplacementPair(pat , rep);
  }

  public static class PatternReplacementPair {
    public Pattern pat;
    public String rep;

    /**
     * Simple Container class for Regex pattern -> Replacement pairs.
     * @param pat String regex, gets compiled to a Pattern
     * @param rep Replacement string
     */
    PatternReplacementPair(String pat, String rep) {
      this.pat = Pattern.compile(pat, Pattern.CASE_INSENSITIVE);
      this.rep = rep;
    }
  }

  // TODO Remove this class
  private static class PatternReplacements {
    public PatternReplacementPair[] pairs;

    /**
     * Simple array class which stores Pattern-Replacement pairs.
     *
     * @param pats List of regex patterns in string format
     * @param reps List of corresponding replacement patterns.
     * @throws Exception if the inputs are not compliant
     */
    PatternReplacements(List<String> pats, List<String> reps) throws Exception {
      if (pats == null || reps == null) {
        throw new Exception("Patterns and Replacements cannot be null");
      }
      if (pats.size() != reps.size()) {
        throw new Exception("Patterns and Replacements do not match up. Must contain the same number of elements.");
      }
      if (pats.size() < 1) {
        throw new Exception("No patterns/replacements given. Must provide at least one pair.");
      }
      pairs = new PatternReplacementPair[pats.size()];
      for (int i=0; i<pats.size(); i++) {
        pairs[i] = new PatternReplacementPair(pats.get(i), reps.get(i));
      }
    }
  }

}
