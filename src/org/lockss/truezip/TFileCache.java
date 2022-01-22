/*

Copyright (c) 2000-2022 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.schlichtherle.truezip.file.*;
import de.schlichtherle.truezip.fs.*;

import org.apache.commons.lang3.StringUtils;
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

  // Signature of regular zip
  private final byte[] ZIP_SIG = new byte[]{'P', 'K', 3, 4};
  // Signature of first or only part of a split zip
  private final byte[] SPLIT_ZIP_SIG = new byte[]{'P', 'K', 7, 8};

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
    this.tmpDir = dir; // can be relative (e.g. run_one_daemon)
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

  void handleSplitZipArchive(TFile tf, CachedUrl cu, InputStream zipIs,
                             int digits) throws IOException {
    String prefix = "splitzip.";

    // Get the CU's archival unit
    ArchivalUnit au = cu.getArchivalUnit();

    // Create new temporary directory for split zip files
    File splitZipsTmpDir = FileUtil.createTempDir("splitzips", "", tmpDir);

    copyParts(cu, digits, prefix, splitZipsTmpDir);

    // Copy zip file to temporary directory
    File zipFileDst = new File(splitZipsTmpDir, prefix + "zip");
    try (OutputStream output = new BufferedOutputStream(new FileOutputStream(zipFileDst))) {

      StreamUtil.copy(zipIs, output);
    }

    log.debug2("zipFileDst = " + zipFileDst);

    // Create a new temporary directory for the merged zip
    File mergedZipTmpDir = FileUtil.createTempDir("mergedzip", "", tmpDir);

    try {
      // Merged zip file name and path destination
      File mergedZip = new File(mergedZipTmpDir, "merged.zip");

      // Command list to cause zip to merged the split zip files
      List<String> command = new ArrayList<>(6);
      command.add("zip");
      command.add("-q");
      command.add("-s-");
      command.add(zipFileDst.getAbsolutePath()); // tmpDir i.e. splitZipsTmpDir i.e. zipFileDst can be relative
      command.add("-O");
      command.add(mergedZip.getAbsolutePath()); // tmpDir i.e. mergedZipTmpDir i.e. mergedZip can be relative

      // Invoke zip utility on split zip archive
      ProcessBuilder builder = new ProcessBuilder(command);
      builder.directory(splitZipsTmpDir); // this is why zipFileDst and mergedZip should be absolute
      builder.redirectErrorStream(true);
      Process process = builder.start();
      int exitCode = process.waitFor();

      // Remove no longer needed split zips temporary directory
      FileUtil.delTree(splitZipsTmpDir);

      if (exitCode != 0) {
        // Read and log zip output for error message (note: zip logs error messages to STDOUT)
        String zipError = StringUtil.fromInputStream(process.getInputStream());

        log.error(String.format("Zip exited with non-zero exit code %d:%n%s", exitCode, zipError));

        if (log.isDebug2()) {
          StringBuilder sb = new StringBuilder();
          sb.append("Details of the zip command:");
          for (int i = 0 ; i < command.size() ; ++i) {
            sb.append("\n");
            sb.append(i);
            sb.append(":\t");
            sb.append(command.get(i));
          }
          log.debug2(sb.toString());
        }
        
        throw new IOException(String.format("Zip exited with non-zero exit code %d", exitCode));
      }

      // Feed merged zip's input stream to TFile
      try (InputStream input = new BufferedInputStream(new FileInputStream(mergedZip))) {
        tf.input(input);
      }

    } catch (InterruptedException e) {
      log.warning("Zip process was interrupted");
      throw new IOException("Zip process was interrupted");

    } finally {
      // Clean-up temporary directories
      FileUtil.delTree(splitZipsTmpDir);
      FileUtil.delTree(mergedZipTmpDir);
    }
  }

  /** Copy split zip files to the temporary directory. */
  void copyParts(CachedUrl cu, int digits, String prefix, File splitZipsTmpDir)
      throws IOException {
    for (int i = 1; i < Integer.MAX_VALUE; i++) {

      // Check whether it's time to increase the number of digits
      if (i >= NumberUtil.intPow(10, digits)) {
        digits++;
      }

      CachedUrl splitZipCu = probeForSplitZip(cu, digits, i);

      if (splitZipCu != null) {
        String newUrl = splitZipCu.getUrl();
        String newExt =
          FileUtil.getExtension(new URL(newUrl).getPath()).toLowerCase();

        // Split zip destination file
        File splitZipFileDst = new File(splitZipsTmpDir, prefix + newExt);

        log.debug2("splitZipFileDst = " + splitZipFileDst);

        // Copy split zip file to temporary directory
        try (InputStream input = splitZipCu.getUnfilteredInputStream();
             OutputStream output =
             new BufferedOutputStream(new FileOutputStream(splitZipFileDst))) {
          StreamUtil.copy(input, output);
        }
      } else {
        // Assume we're done copying files if there's a break in the sequence;
        // allow any missing files to be detected by the zip utility
        break;
      }
    }
  }


  public static final Pattern ZIP_EXT_PATTERN =
      Pattern.compile(".*(\\.zip)(\\?.*|$)", Pattern.CASE_INSENSITIVE);

  public static String replaceZipExtension(String url, String prefix, int digits, long i) {
    String index = String.valueOf(i);
    String indexPadding = "";

    if (digits - index.length() > 0) {
      indexPadding = StringUtils.repeat("0", (int)digits - index.length());
    }

    Matcher m = ZIP_EXT_PATTERN.matcher(url);

    if (m.matches()) {
      return new StringBuilder(url)
          .replace(m.start(1), m.end(1), "." + prefix + indexPadding + index).toString();
    }

    // This should not happen because we've checked for the .zip extension elsewhere
    throw new ShouldNotHappenException();
  }

  // Maximum number of digits in split zip sequence (e.g. zXX has two digits
  private static final long MAX_DIGITS = 10;

  CachedUrl probeForSplitZip(CachedUrl cu, int digits, long i) {
    ArchivalUnit au = cu.getArchivalUnit();

    // List of split zip extension prefixes
    List<String> prefixes = ListUtil.list("z", "Z");

    for (String prefix : prefixes) {
      CachedUrl splitZipCu =
          au.makeCachedUrl(replaceZipExtension(cu.getUrl(), prefix, digits, i));

      if (splitZipCu != null && splitZipCu.hasContent()) {
        return splitZipCu;
      }
    }

    return null;
  }

  void fillTFile(Entry ent, CachedUrl cu) throws IOException {
    TFile tf = ent.ctf;
    log.debug2("filling " + tf + " from " + cu);

    // Get the CacheUrl file extension
    String url = cu.getUrl();
    String ext = UrlUtil.getFileExtension(url);

    boolean splitZipDetected = false;

    try (PushbackInputStream is =
         new PushbackInputStream(cu.getUnfilteredInputStream(), 4)) {
      if (ext.equalsIgnoreCase("zip")) {
        // This is a zip; check for split zip
        byte[] filesig = new byte[4];
        int siglen = is.read(filesig);
        if (siglen == 4 && Arrays.equals(SPLIT_ZIP_SIG, filesig)) {
          log.debug2("Detected single-part split zip: " + cu);
          // This is a single-part split zip.  "zip -s-" won't turn it into
          // a regular zip, so replace the magic number with that of a
          // regular zip
          is.unread(ZIP_SIG, 0, 4);
        } else {
          // Restore magic number
          is.unread(filesig, 0, siglen);
          // Scan for first split zip file (e.g., z1, z01, z001, etc)
          for (int digits = 1; digits <= MAX_DIGITS; digits++) {

            CachedUrl splitZipCu = probeForSplitZip(cu, digits, 1);

            if (splitZipCu != null) {
              // Yes: Process split zip archive
              try {
                splitZipDetected = true;
                log.debug2("Detected split zip: " + cu);
                handleSplitZipArchive(tf, cu, is, digits);
              } catch (IOException e) {
                log.error("Error processing split zip archive");
                throw e;
              }

              // Done - do not continue scanning for split zip files
              break;
            }
          }
        }
      }

      if (!splitZipDetected) {
        // No: Process CU normally
        try {
          tf.input(is);
        } catch (IOException e) {
          String msg = "Couldn't copy archive CU " + this + " to TFile " + tf;
          log.error(msg, e);
          throw e;
        } catch (Exception e) {
          String msg = "Couldn't copy archive CU " + this + " to TFile " + tf;
          log.error(msg, e);
          throw new IOException(msg, e);
        }
      }

      // Set properties
      CIProperties cuProps = cu.getProperties();
      CIProperties arcProps = new CIProperties();

      for (String key : INHERIT_PROP_KEYS) {
        if (cuProps.containsKey(key)) {
          arcProps.put(key, cuProps.get(key));
        }

        ent.arcCuProps = arcProps;
      }

      ent.valid = true;

    } finally {
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
	// Check whether this entry is flushable.
	if (ent.isValid()
	    && (!ent.flushAfterUnmountOnly || ent.freeingInstant >= 0)) {
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
	unmount(ent.ctf);
      } catch (Throwable t) {
	log.warning("Error unmounting " + ent.ctf, t);
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
	unmountAll();
      } catch (Throwable t) {
	log.warning("Error unmounting TFile", t);
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

  /**
   * Marks an entry as flushable only if its Tfile is unmounted.
   * 
   * @param cu
   *          A CachedUrl with the CU used to locate the TFile in the cache.
   */
  public void setFlushAfterUnmountOnly(CachedUrl cu) {
    Entry ent = getEnt(getKey(cu));
    if (ent != null) {
      ent.flushAfterUnmountOnly = true;
    } else {
      log.warning("Cannot find entry to set flushAfterUnmountOnly for cu = "
	  + cu);
    }
  }

  /**
   * Unmounts a TFile and removes it from the cache.
   * 
   * @param tf
   *          A TFile to be freed.
   * @param cu
   *          A CachedUrl with the CU used to locate the TFile in the cache, or
   *          <code>null</code> if the TFile is not in the cache.
   */
  public void freeTFile(TFile tf, CachedUrl cu) {
    // Unmount the archive.
    try {
	unmount(tf);
    } catch (Throwable t) {
	log.warning("Error unmounting " + tf, t);
    }

    // Nothing more to do if the TFile is not in the cache.
    if (cu == null) return;

    // Find the cache entry.
    Entry ent = getEnt(getKey(cu));
    if (ent == null) {
      log.warning("Cannot find entry to free for cu = " + cu);
      return;
    }

    // This TFile should have been marked to be flushed only after unmounting.
    if (!ent.flushAfterUnmountOnly) {
      log.warning("Entry for unmounted TFile = " + tf
	  + " was not marked flushAfterUnmountOnly - Fixed");
      ent.flushAfterUnmountOnly = true;
    }

    synchronized (cmap) {
      // Record the timestamp when this TFile was freed.
      ent.freeingInstant = TimeBase.nowMs();

      // Remove the file from the cache.
      ent.invalidate();

      try {
	deleteFile(ent);
      } catch (Exception e) {
	log.warning("Error deleting " + ent.ctf, e);
      }

      cmap.remove(ent.key);

      // Update cache size.
      curSize -= ent.size;
    }
  }

  /**
   * Marks a TFile as flushable.
   * 
   * @param tf
   *          A TFile to be marked as flushable.
   * @param cu
   *          A CachedUrl with the CU used to locate the TFile in the cache, or
   *          <code>null</code> if the TFile is not in the cache.
   */
  public void markArchiveAsFlushable(TFile tf, CachedUrl cu) {
    // Nothing more to do if the TFile is not in the cache.
    if (cu == null) return;

    // Find the cache entry.
    Entry ent = getEnt(getKey(cu));
    if (ent == null) {
      log.warning("Cannot find entry to mark as unmounted for cu = " + cu);
      return;
    }

    // This TFile should have been marked to be flushed only after unmounting.
    if (!ent.flushAfterUnmountOnly) {
      log.warning("Entry for unmounted TFile = " + tf
	  + " was not marked flushAfterUnmountOnly - Fixed");
      ent.flushAfterUnmountOnly = true;
    }

    // Record the timestamp when this TFile was marked.
    ent.freeingInstant = TimeBase.nowMs();
  }

  /**
   * Unmounts a TFile.
   * 
   * @param tf
   *          A TFile to be unmounted.
   * @throws FsSyncException
   */
  private void unmount(TFile tf) throws FsSyncException {
    TVFS.umount(tf);
  }

  /**
   * Unmounts all TFiles.
   * 
   * @throws FsSyncException
   */
  private void unmountAll() throws FsSyncException {
    TVFS.umount();
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
    boolean flushAfterUnmountOnly = false;
    long freeingInstant = -1L;

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
      // Check whether this entry needs to be unmounted to be flushed.
      if (flushAfterUnmountOnly) {
	// Yes: Check whether the other entry does not need to be unmounted to
	// be flushed.
	if (!o.flushAfterUnmountOnly) {
	  // Yes: If this entry is freed already, it goes first, otherwise,
	  // last.
	  return (freeingInstant >= 0) ? -1 : 1;
	}

	// If only one is already freed, it goes first.
	if (freeingInstant >= 0 && o.freeingInstant < 0) {
	  return -1;
	} else if (freeingInstant < 0 && o.freeingInstant >= 0) {
	  return 1;
	}

	// Both are already freed or none is: Sort by freeing order.
	return (freeingInstant < o.freeingInstant) ? -1
	    : (freeingInstant == o.freeingInstant) ? 0 : 1;
      } else {
	// No: Check whether the other entry needs to be freed to be flushed.
	if (o.flushAfterUnmountOnly) {
	  // Yes: If the other entry is freed already, it goes first, otherwise,
	  // last.
	  return (o.freeingInstant >= 0) ? 1 : -1;
	}

	// None of the entries need to be unmounted to be flushed: Sort by
	// creation order.
	return (ctr < o.ctr) ? -1 : (ctr == o.ctr) ? 0 : 1;
      }
    }

    public String toString() {
      return "[TFC.Ent" + (isValid() ? "" : " (inv)") + ": " + ctf + "]";
    }
  }

}

