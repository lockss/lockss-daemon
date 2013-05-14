/*
 * $Id: TFileCache.java,v 1.4 2013-05-14 04:10:12 tlipkis Exp $
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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
import java.net.*;
import java.util.*;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.collections.map.ReferenceMap;
import de.schlichtherle.truezip.file.*;
import de.schlichtherle.truezip.fs.*;

import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;

/**
 * Manages a disk cache of temporary archive files being accessed by
 * TrueZip to support CachedUrl.Member
 */
public class TFileCache {

  // TODO:
  //
  // - Check whether it's safe to continue using a TFile whose backing file
  //   has been deleted.  If not, need locking protocol to prevent it.
  //
  // - UrlCacher.storeContent() should invalid any cached TFile
  //   corresponding to its previous content
  //
  // - Replace createTempFile();delete() with something better.  (File must
  //   not exist prior to input().)
  //


  static final Logger log = Logger.getLogger("TFileCache");

  private File tmpDir;
  private long maxSize;
  private long maxFiles;
  private long curSize;

  private Map<String,Entry> cmap = new HashMap<String,Entry>();

  // for stats & tests
  private int cacheHits = 0;
  private int cacheMisses = 0;

  /** Create a TFileCache to manage files in a directory.
   * @param dir the full path of the directory in which to store temp files.
   */
  public TFileCache(String dir) {
    this(new File(dir));
  }

  /** Create a TFileCache to manage files in a directory.
   * @param dir the directory in which to store temp files.
   */
  public TFileCache(File dir) {
    this.tmpDir = dir;
    if (!tmpDir.exists()) {
      if (!tmpDir.mkdirs()) {
	throw new RuntimeException("Couldn't create temp dir: " + tmpDir);
      }
      return;
    }
    if (!tmpDir.isDirectory()) {
      throw new RuntimeException("Temp dir is not a directory: " + tmpDir);
    }
    if (!tmpDir.canWrite()) {
      throw new RuntimeException("Temp dir is not writable: " + tmpDir);
    }
    FileUtil.emptyDir(tmpDir);
  }

  /** Set the target maximum amount of disk space to use, in bytes.  Will
   * be exceeded if necessary for larger archive files
   */
  public void setMaxSize(long bytes, int files) {
    this.maxSize = bytes;
    this.maxFiles = files;
  }

  /** Return a {@link de.schlichtherle.truezip.file.TFile} pointing to a
   * temp file with a copy of the CU's contents.  If not already present, a
   * new temp file will be created and the CU content copied into it.
   * @param cu the CU to open as a TFile.  Distinct CUs with the same URL
   * reference the same TFile.
   * @return a TFile or null if one cannot be created (e.g., because the cu
   * has no content, or isn't a known archive type)
   */
  public TFile getCachedTFile(CachedUrl cu) throws IOException {
    Entry ent = getCachedTFileEntry(cu);
    if (ent == null) {
      return null;
    }
    return ent.ctf;
  }    

  /** Return the {@link TFileCache.Entry} corresponding to the temp file
   * with a copy of the CU's contents.  If not already present, a new temp
   * file will be created and the CU content copied into it.
   * @param cu the CU to open as a TFile.  Distinct CUs with the same URL
   * reference the same TFile.
   * @return a TFileCache.Entry or null if one cannot be created (e.g.,
   * because the cu has no content, or isn't a known archive type)
   * @throws IOException if one occurs while creating and filling the temp
   * file
   */
  public Entry getCachedTFileEntry(CachedUrl cu) throws IOException {
    String key = getKey(cu);
    synchronized (cmap) {
      Entry ent = getEnt(key);
      if (ent != null) {
	return ent;
      }
      cacheMisses++;
      ent = createEnt(key, cu);

      if (ent == null) {
	return null;
      }
      ensureSpace(ent);
      fillTFile(ent, cu);
      if (ent.valid) {
	curSize += ent.size;
      } else {
	// TFile wasn't fully created, delete temp file and remove from map
	log.warning("Incompletely created TFile for: " + cu);
	flushEntry(ent);
	return null;
      }
      return ent;
    }
  }    

  /** Properties of an archive file CU that should be inherited by its
   * members. */
  static List<String> INHERIT_PROP_KEYS =
    ListUtil.list(CachedUrl.PROPERTY_FETCH_TIME,
		  CachedUrl.PROPERTY_LAST_MODIFIED,
		  "Date"
// 		  "Server",
// 		  "Expires"
		  );

  void fillTFile(Entry ent, CachedUrl cu) {
    TFile tf = ent.ctf;
    log.debug2("filling " + tf + " from " + cu);
    InputStream is = cu.getUnfilteredInputStream();
    try {
      tf.input(is);
      CIProperties cuProps = cu.getProperties();
      CIProperties arcProps = new CIProperties();
      for (String key : INHERIT_PROP_KEYS) {
	if (cuProps.containsKey(key)) {
	  arcProps.put(key, cuProps.get(key));
	}
	ent.arcCuProps = arcProps;
      }
      ent.valid = true;
    } catch (Exception e) {
      String msg = "Couldn't copy archive CU " + this + " to TFile " + tf;
      log.error(msg, e);
      throw new RuntimeException(msg, e);
    } finally {
      IOUtil.safeClose(is);
      AuUtil.safeRelease(cu);
    }
  }


  private String getKey(CachedUrl cu) {
    return cu.getArchivalUnit().getAuId() + "|" + cu.getUrl();
  }

  private Entry getEnt(String key) {
    Entry ent = cmap.get(key);
    if (ent != null) {
      cacheHits++;
      ent.used();
    }
    return ent;
  }

  private Entry createEnt(String key, CachedUrl cu)
      throws IOException {
    long size = cu.getContentSize();
    String ext = ArchiveFileTypes.getArchiveExtension(cu);
    if (ext == null) {
      log.error("No arc ext for " + cu);
      return null;
    }
    if (!ext.startsWith(".")) {
      ext = "." + ext;
    }
    log.debug3("createEnt("+cu+"), ext="+ext);

    Entry ent = newEntry(key, ext, size, cu.getUrl());
    ent.ctf = new TFile(FileUtil.createTempFile("ctmp", ext, tmpDir));
    ent.ctf.delete();
    cmap.put(key, ent);
    return ent;
  }

  Entry newEntry(String key, String ext, long size, String url) {
    return new Entry(size, key, url);
  }

  /**
   * Returns a snapshot of the values in the cache
   * @return a Set of {@link TFileCache.Entry}s
   */
  public Set<Entry> snapshot() {
    synchronized (cmap) {
      return new HashSet<Entry>(cmap.values());
    }
  }

  void deleteFile(Entry ent) throws IOException {
    // TFile.delete() works only if archive is empty.  Use
    // File.delete() instead
    File f = new File(ent.ctf.getPath());
    if (!f.delete()) {
      log.warning("Couldn't delete: " + f + (f.exists() ? " (exists)" : ""));
    }
  }

  String sizeToString(long bytes) {
    if (bytes < 10240) {
      return bytes + "B";
    }
    return StringUtil.sizeKBToString(bytes / 1024);
  }

  void ensureSpace(Entry newEnt) {
    synchronized (cmap) {
      long committed = curSize + newEnt.size;
      int curFiles = cmap.size();
      if (log.isDebug2()) {
	log.debug2("ensureSpace: " + sizeToString(committed)
		   + " <=? " + sizeToString(maxSize) +
		   ", " + curFiles + " <? " + maxFiles);
      }
      if (committed <= maxSize && curFiles < maxFiles) {
	return;
      }
      // Remove sort when switch to UnboundedLRUMap
      List<Entry> lst = new ArrayList<Entry>(cmap.values());
      Collections.sort(lst);
      for (Entry ent : lst) {
	if (ent.isValid()) {
	  if (log.isDebug2()) {
	    log.debug2("flushing " + sizeToString(ent.size) + " in " + ent.url
		       + ", " + ent.ctf.getName());
	  }
	  flushEntry(ent);
	  curFiles--;
	  committed -= ent.size;
	  if (committed <= maxSize && curFiles < maxFiles) {
	    break;
	  }
	}
      }
      if (committed > maxSize) {
	log.warning("TFile cache overcommitted: " + sizeToString(committed)
		    + ", max: " + sizeToString(maxSize));
      }
      curSize = committed - newEnt.size;
    }
  }

  public void flushEntry(Entry ent) {
    synchronized (cmap) {
      ent.invalidate();
      try {
	TFile.umount(ent.ctf);
      } catch (FsSyncException e) {
	log.warning("Error unmounting " + ent.ctf, e);
      }
      try {
	deleteFile(ent);
      } catch (Exception e) {
	log.warning("Error deleting " + ent.ctf, e);
      }	
      cmap.remove(ent.key);
    }
  }


  /**
   * Clears the cache
   */
  public void clear() {
    synchronized (cmap) {
      try {
	TFile.umount();
      } catch (FsSyncException e) {
	log.warning("Couldn't umount TFile", e);
      }
      for (Entry ent : cmap.values()) {
	if (ent.ctf != null) {
	  try {
	    deleteFile(ent);
	  } catch (Exception e) {
	    log.warning("Error deleting " + ent.ctf);
	  }	
	}
      }
      cmap.clear();
    }
  }

  // logging accessors

  public int getCacheHits() { return cacheHits; }
  public int getCacheMisses() { return cacheMisses; }

  public long getCurrentSize() {
    return curSize;
  }

  long globalCnt = 0;

  public class Entry implements Comparable<Entry> {
    TFile ctf;
    long size;
    long lastUse;
    long ctr;
    int refCnt = 0;
    boolean valid = false;
    String url;
    String key;
    CIProperties arcCuProps;

    Entry(long size, String key, String url) {
      this.size = size;
      this.key = key;
      this.url = url;
      used();
    }

    void used() {
      ctr = ++globalCnt;
      ++refCnt;
      lastUse = TimeBase.nowMs();
    }

    public TFile getTFile() {
      return ctf;
    }

    public CIProperties getArcCuProps() {
      return arcCuProps;
    }

    public void invalidate() {
      valid = false;
    }

    public boolean isValid() {
      return valid;
    }

    // for tests
    public boolean exists() {
      return new File(ctf.getPath()).exists();
    }

    public int compareTo(Entry o) {
      return (ctr < o.ctr) ? -1 : (ctr == o.ctr) ? 0 : 1;
    }

    public String toString() {
      return "[TFC.Ent" + (isValid() ? "" : " (inv)") + ": " + ctf + "]";
    }
  }

}
