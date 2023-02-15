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
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.*;
import java.lang.reflect.*;
import java.util.zip.*;

import com.ice.tar.TarEntry;
import com.ice.tar.TarInputStream;
import com.ice.tar.TarOutputStream;
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

  /**
   * {@code ifMatch, patRepPairs} defaults to {null}.
   * {@code fromAu} defaults to {fromAu.getCachedUrlSet()}.
   *
   * @see PluginTestUtil#copyCus(CachedUrlSet, ArchivalUnit, String, List)
   */
  public static boolean copyAu(ArchivalUnit fromAu, ArchivalUnit toAu) throws MalformedURLException {
    return copyAu(fromAu, toAu, null, null, null);
  }

  /**
   * {@code patRepPairs} defaults to {null}.
   * {@code fromAu} defaults to {fromAu.getCachedUrlSet()}.
   *
   * @see PluginTestUtil#copyCus(CachedUrlSet, ArchivalUnit, String, List)
   */
  public static boolean copyAu(ArchivalUnit fromAu, ArchivalUnit toAu, String ifMatch) throws MalformedURLException {
    return copyAu(fromAu, toAu, ifMatch, null, null);
  }

  /**
   * {@code pat, rep} converted to {PatternReplacements}.
   * {@code fromAu} defaults to {fromAu.getCachedUrlSet()}.
   *
   * @see PluginTestUtil#copyCus(CachedUrlSet, ArchivalUnit, String, List)
   */
  public static boolean copyAu(ArchivalUnit fromAu, ArchivalUnit toAu,
			       String ifMatch, String pat, String rep) throws MalformedURLException {
    return copyCus(fromAu.getAuCachedUrlSet(), toAu, ifMatch, pat, rep);
  }

  /**
   * {@code fromAu} defaults to {fromAu.getCachedUrlSet()}.
   *
   * @see PluginTestUtil#copyCus(CachedUrlSet, ArchivalUnit, String, List)
   */
  public static boolean copyAu(ArchivalUnit fromAu,
                               ArchivalUnit toAu,
                               String ifMatch,
                               List<PatternReplacements> patRepPairs) throws MalformedURLException {
    return copyCus(fromAu.getAuCachedUrlSet(), toAu, ifMatch, patRepPairs);
  }

  /**
   * {@code ifMatch, patRepPairs} defaults to {null}.
   *
   * @see PluginTestUtil#copyCus(CachedUrlSet, ArchivalUnit, String, List)
   */
  public static boolean copyCus(CachedUrlSet fromCus, ArchivalUnit toAu) {
    return copyCus(fromCus, toAu, null, null, null);
  }

  /**
   * {@code pat, rep} converted to {PatternReplacements}.
   *
   * @param pat a regex used to match files in the simulated crawl
   * @param rep regex replacement pattern(s) to rename the original file.
   * @see PluginTestUtil#copyCus(CachedUrlSet, ArchivalUnit, String, List)
   */
  public static boolean copyCus(CachedUrlSet fromCus, ArchivalUnit toAu,
				String ifMatch, String pat, String rep) {
    List<PatternReplacements> patRepPairs;
    if (pat == null) {
      patRepPairs = null;
    } else {
      patRepPairs = Collections.singletonList(new PatternReplacements(pat, rep));
    }
    return copyCus(fromCus, toAu, ifMatch, patRepPairs);
  }

  /**
   * Utility to copy files from a simulated crawl to a mock archival unit.
   * For each file matched by ifMatch, the first Pattern matched will be copied for as many
   * replacements as are associated with it.
   * If only fromAu and toAu are provided, all files are copied without modification.
   *
   * @param fromCus the CachedUrlSet which has been crawled
   * @param toAu the Archival Unit to copy content to
   * @param ifMatch String, a regex to check on the url before pattern replacement transforming. e.g. .zip
   * @param patRepPairs A List of PatternReplacements to rename files from matched patterns to replacements.
   * @return true, if all copies attempted succeeded, false otherwise
   */
  public static boolean copyCus(CachedUrlSet fromCus, ArchivalUnit toAu,
                                String ifMatch, List<PatternReplacements> patRepPairs) {
    boolean res = true;
    Pattern ifMatchPat = null;
    if (ifMatch != null) {
      ifMatchPat = Pattern.compile(ifMatch);
    }
    ArchiveFileTypes aft = toAu.getArchiveFileTypes();
    for (CachedUrl cu : fromCus.getCuIterable()) {
      try {
        String fromUrl = cu.getUrl();
        String toUrl = fromUrl;
        String archiveType = (aft == null) ? null : aft.getFromCu(cu);
        if (ifMatchPat != null) {
          Matcher mat = ifMatchPat.matcher(fromUrl);
          if (!mat.find()) {
            if (archiveType == null) {
              // if we are dealing with an archive then it is best to just pass the pattern
              // into the copyArchive() and let the matches happen in the contents.
              log.debug3("no match: " + fromUrl + ", " + ifMatchPat);
              continue;
            }
          }
        }
        if (archiveType == null) {
          if (patRepPairs != null) {
            for (PatternReplacements prp : patRepPairs) {
              Matcher mat = prp.pat.matcher(fromUrl);
              if (mat.find()) {
                for (String rep : prp.rep) {
                  toUrl = mat.replaceAll(rep);
                  doCopyCu(cu, toAu, fromUrl, toUrl);
                }
                break;
              }
            }
          } else {
            doCopyCu(cu, toAu, fromUrl, toUrl);
          }
        } else {
          switch (archiveType) {
            case ".zip":
            case ".tar":
              copyArchive(cu, toAu, patRepPairs, archiveType, ifMatchPat);
              break;
            case ".tar.gz": // needs to be supported by the simcrawler in order to be implemented here.
            case ".tgz":
            default:
              throw new Exception("Unexpected Archive file type: '" + archiveType + "'");
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

  private static void doCopyCu(CachedUrl cu,
                               ArchivalUnit toAu,
                               String fromUrl,
                               String toUrl
  ) throws IOException {
    doCopyCu(cu.getUnfilteredInputStream(), cu.getProperties(), toAu, fromUrl, toUrl);
  }

  private static void doCopyCu(InputStream is,
                               CIProperties props,
                               ArchivalUnit toAu,
                               String fromUrl,
                               String toUrl
  ) throws IOException {
    if (props == null) {
      log.debug3("in copyCus() props was null for url: " + fromUrl);
    }
    UrlCacher uc = toAu.makeUrlCacher(
        new UrlData(is, props, toUrl));
    uc.storeContent();
    if (!toUrl.equals(fromUrl)) {
      log.debug2("Copied " + fromUrl + " to " + toUrl);
    } else {
      log.debug2("Copied " + fromUrl);
    }
  }

  /**
   * a wrapper method that calls the proper copy archive methods, allowing recursion in the archive files.
   * @param cu
   * @param toAu
   * @param patRepPairs
   * @param archiveType
   * @param ifMatchPat
   * @throws IOException Any error just gets thrown to as IO to copyCUs
   */
  private static void copyArchive(CachedUrl cu,
                                  ArchivalUnit toAu,
                                  List<PatternReplacements> patRepPairs,
                                  String archiveType,
                                  Pattern ifMatchPat)
      throws IOException {
    ArchiveMemberSpec ams = ArchiveMemberSpec.fromCu(cu, null);
    TempArchiveFile taf;
    try (InputStream is = cu.getUnfilteredInputStream()) {
      switch (archiveType) {
        case ".zip":
          try (ZipInputStream zis = new ZipInputStream(is)) {
            taf = copyZipIS(zis, cu, toAu, patRepPairs, archiveType, ifMatchPat, ams, 0);
          }
          break;
        case ".tar":
          try (TarInputStream tis = new TarInputStream(is)) {
            taf = copyTarIS(tis, cu, toAu, patRepPairs, archiveType, ifMatchPat, ams, 0);
          }
          break;
        case ".tar.gz": // needs to be supported by the simcrawler in order to be implemented here.
        case ".tgz":
        default:
          throw new IOException("Unexpected Archive file type: '" + archiveType + "'");
      }
      // we copy the temp archive file here because it doesnt work for the recursive case
      // only copy if there was a match found.
      if (taf.url != null) {
        copyArchiveFile(cu, toAu, ams, taf);
      }
    }
  }

  /**
   * Iterates over the contents of a zip stream and copies the contents if they pass
   * given pattern(s) and replacements.
   * <p>
   * Note: replacement(s) can rename the zip file, but all zip files should be the same in the replacement string(s)
   *
   * @return TempArchiveFile. if TempArchiveFile.url is null, the copy should not proceed.
   */
  private static TempArchiveFile copyZipIS(ZipInputStream zis,
                                           CachedUrl cu,
                                           ArchivalUnit toAu,
                                           List<PatternReplacements> patRepPairs,
                                           String archiveType,
                                           Pattern ifMatchPat,
                                           ArchiveMemberSpec ams,
                                           int tempFileId)
      throws IOException {

    TempArchiveFile taf = new TempArchiveFile(null,  "temp" + tempFileId + archiveType);
    ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(
        Paths.get(cu.getArchivalUnit().getProperties().getString("root") + taf.tempFile)));
    zos.setMethod(ZipOutputStream.DEFLATED);
    zos.setLevel(Deflater.BEST_COMPRESSION);
    ZipEntry entry;
    while ((entry = zis.getNextEntry()) != null) {
      ArchiveMemberSpec fromZipped = ArchiveMemberSpec.fromCu(cu, entry.getName());
      if (ifMatchPat != null) {
        Matcher mat = ifMatchPat.matcher(fromZipped.toUrl());
        if (!mat.find()) {
          continue;
        }
      }
      if (entry.isDirectory()) {
        continue;
      } else if (entry.getName().endsWith(archiveType) ) {
        // TODO test recurse through nested
        taf = copyZipIS(zis, cu, toAu, patRepPairs, archiveType, ifMatchPat, fromZipped, tempFileId++);
        // we need to write this temp file to the master temp zip file, not to the urlcacher
        // write the file here to zos doCopyZipEntry()
      } // TODO consider the case when a tar is inside a zip
      if (patRepPairs == null) {
        doCopyZipEntry(zos, zis, fromZipped.getName());
        taf.url = fromZipped.getName();
      } else {
        for (PatternReplacements prp : patRepPairs) {
          Matcher mat = prp.pat.matcher(fromZipped.toUrl());
          if (mat.find()) {
            for (String rep : prp.rep) {
              ArchiveMemberSpec toZipped = ArchiveMemberSpec.fromUrl(toAu, mat.replaceAll(rep));
              assert toZipped != null;
              if (!toZipped.toUrl().equals(fromZipped.toUrl())) {
                log.debug("Found a zipped file match: " + fromZipped.toUrl() + " -> " + toZipped.toUrl());
                doCopyZipEntry(zos, zis, toZipped.getName());
                taf.url = toZipped.getUrl();
              }
            }
            break;
          }
        }
      }
    }
    zos.close();
    return taf;
  }

  /**
   * Iterates over the contents of a tar stream and copies the contents if they pass
   * given pattern(s) and replacements.
   * <p>
   * Note: replacement(s) can rename the tar file, but all tar files should be the same in the replacement string(s)
   *
   * @return TempArchiveFile. if TempArchiveFile.url is null, the copy should not proceed.
   */
  private static TempArchiveFile copyTarIS(TarInputStream tis,
                                           CachedUrl cu,
                                           ArchivalUnit toAu,
                                           List<PatternReplacements> patRepPairs,
                                           String archiveType,
                                           Pattern ifMatchPat,
                                           ArchiveMemberSpec ams,
                                           int tempFileId) throws IOException {
    TempArchiveFile taf = new TempArchiveFile(null,  "temp" + tempFileId + archiveType);
    TarOutputStream tos = new TarOutputStream(Files.newOutputStream(
        Paths.get(cu.getArchivalUnit().getProperties().getString("root") + taf.tempFile)));
    TarEntry entry;
    while ((entry = tis.getNextEntry()) != null) {
      ArchiveMemberSpec fromTarred = ArchiveMemberSpec.fromCu(cu, entry.getName());
      if (ifMatchPat != null) {
        Matcher mat = ifMatchPat.matcher(fromTarred.toUrl());
        if (!mat.find()) {
          continue;
        }
      }
      if (entry.isDirectory()) {
        continue;
      } else if (entry.getName().endsWith(archiveType) ) {
        // TODO recurse through nested
        copyTarIS(tis, cu, toAu, patRepPairs, archiveType, ifMatchPat, fromTarred, tempFileId++);
      }
      if (patRepPairs == null) {
        doCopyTarEntry(tos, tis, entry);
        taf.url = fromTarred.getName();
      } else {
        for (PatternReplacements prp : patRepPairs) {
          Matcher mat = prp.pat.matcher(fromTarred.toUrl());
          if (mat.find()) {
            for (String rep : prp.rep) {
              ArchiveMemberSpec toTarred = ArchiveMemberSpec.fromUrl(toAu, mat.replaceAll(rep));
              assert toTarred != null;
              if (!toTarred.toUrl().equals(fromTarred.toUrl())) {
                log.debug("Found a tarred file match: " + fromTarred.toUrl() + " -> " + toTarred.toUrl());
                // rename the entry and copy it
                entry.setName(toTarred.getName());
                doCopyTarEntry(tos, tis, entry);
                taf.url = toTarred.getUrl();
              }
            }
            break;
          }
        }
      }
    }
    tos.close();
    return taf;
  }

  /**
   * Copy a zipped archive member from a ZipInputStream to a ZipOutputStream
   * @param zos The Zip file outputstream to copy the entry into
   * @param zis The Zip file inputstream to copy the entry from
   * @param toName the name of the new entry
   * @throws IOException if zos or zis encounter errors
   */
  private static void doCopyZipEntry(ZipOutputStream zos, ZipInputStream zis, String toName) throws IOException {
    ZipEntry outEntry = new ZipEntry(toName);
    zos.putNextEntry(outEntry);
    StreamUtil.copy(zis, zos);
    zos.closeEntry();
  }

  /**
   * Copy a tarred archive member from a TarInputStream to a TarOutputStream
   * @param tos The Tar file outputstream to copy the entry into
   * @param tis The Tar file inputstream to copy the entry from
   * @param entry the TarEntry to copy
   * @throws IOException if tos or tis encounters an error
   */
  private static void doCopyTarEntry(TarOutputStream tos, TarInputStream tis, TarEntry entry)
      throws IOException {
    byte[] buf = new byte[1024];
    tos.putNextEntry(entry);
    int bytesRead;
    while ((bytesRead = tis.read(buf)) != -1) {
      tos.write(buf, 0, bytesRead);
    }
    tos.closeEntry();
  }

  private static void copyArchiveFile(CachedUrl cu, ArchivalUnit toAu, ArchiveMemberSpec ams, TempArchiveFile taf) throws IOException {
    // open a temp file
    FileInputStream is = new FileInputStream(
        new File(cu.getArchivalUnit().getProperties().getString("root"), taf.tempFile));
    // save all the copied entries to a new archive on the toAu
    doCopyCu(is, cu.getProperties(), toAu, ams.getUrl(), taf.url);
    is.close();
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
  
  public static PatternReplacements makePatRep(String pat, String... rep) {
    return new PatternReplacements(pat , rep);
  }

  public static class PatternReplacements {
    public Pattern pat;
    public String[] rep;

    /**
     * Simple Container class for Regex pattern -> Replacement associations.
     * @param pat String regex, gets compiled to a Pattern
     * @param rep Replacement string
     */
    PatternReplacements(String pat, String... rep) {
      this.pat = Pattern.compile(pat, Pattern.CASE_INSENSITIVE);
      this.rep = rep;
    }
  }

  private static class TempArchiveFile {
    public String url;
    public String tempFile;

    TempArchiveFile(String url, String tempFile) {
      this.url = url;
      this.tempFile = tempFile;
    }
  }

}
