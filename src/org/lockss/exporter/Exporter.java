/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.exporter;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.commons.lang3.StringUtils;

import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.plugin.*;

/**
 * Export the contents and metadata from an AU
 */
public abstract class Exporter {

  private static final Logger log = Logger.getLogger(Exporter.class);

  static final String PREFIX = Configuration.PREFIX + "exporter.";

  /** Abort export after this many errors */
  public static final String PARAM_MAX_ERRORS = PREFIX + "maxErrors";
  public static final int DEFAULT_MAX_ERRORS = 5;

  protected static int maxErrors = DEFAULT_MAX_ERRORS;

  protected LockssDaemon daemon;
  protected ArchivalUnit au;
  protected File dir;
  protected String prefix;
  protected long maxSize = -1;
  protected int maxVersions = 1;
  protected boolean compress = false;
  protected boolean excludeDirNodes = false;
  protected FilenameTranslation xlate = FilenameTranslation.XLATE_NONE;
  protected List errors = new ArrayList();
  protected boolean isDiskFull = false;

  protected abstract void start() throws IOException;

  protected abstract void finish() throws IOException;

  protected abstract void writeCu(CachedUrl cu)
      throws IOException;

  protected Exporter(LockssDaemon daemon, ArchivalUnit au) {
    this.daemon = daemon;
    this.au = au;
  }
  
  /** Called by org.lockss.config.MiscConfig
   */
  public static void setConfig(Configuration config,
			       Configuration oldConfig,
			       Configuration.Differences diffs) {
    if (diffs.contains(PREFIX)) {
      maxErrors = config.getInt(PARAM_MAX_ERRORS, DEFAULT_MAX_ERRORS);
    }
  }

  public void setCompress(boolean val) {
    compress = val;
  }

  public boolean getCompress() {
    return compress;
  }

  public void setExcludeDirNodes(boolean val) {
    excludeDirNodes = val;
  }

  public boolean getExcludeDirNodes() {
    return excludeDirNodes;
  }

  public void setFilenameTranslation(FilenameTranslation val) {
    xlate = val;
  }

  public FilenameTranslation getFilenameTranslation() {
    return xlate;
  }

  public void setDir(File val) {
    dir = val;
  }

  public File getDir() {
    return dir;
  }

  public void setPrefix(String val) {
    prefix = val;
  }

  public String getPrefix() {
    return prefix;
  }

  public void setMaxSize(long val) {
    maxSize = val;
  }

  public long getMaxSize() {
    return maxSize;
  }

  public void setMaxVersions(int val) {
    maxVersions = val;
  }

  public int getMaxVersions() {
    return maxVersions;
  }

  public List getErrors() {
    return errors;
  }

  protected void checkArgs() {
    if (getDir() == null) {
      throw new IllegalArgumentException("Must supply output directory");
    }
    if (getPrefix() == null) {
      throw new IllegalArgumentException("Must supply file name/prefix");
    }
  }

  public void export() {
    log.debug("export(" + au.getName() + ")");
    log.debug("dir: " + dir + ", pref: " + prefix
	      + ", size: " + maxSize + ", ver: " + maxVersions
	      + (compress ? ", (C)" : ""));
    checkArgs();
    try {
      start();
    } catch (IOException e) {
      recordError("Error opening file", e);
      return;
    }      
    writeFiles();
    try {
      finish();
    } catch (IOException e) {
      if (!isDiskFull) {
	// If we already knew (and reported) disk full, also reporting it
	// as a close error is misleading.
	recordError("Error closing file", e);
      }
    }      
  }

  protected String xlateFilename(String url) {
    return xlate.xlate(url);
  }

  protected void recordError(String msg, Throwable t) {
    log.error(msg, t);
    errors.add(msg + ": " + t.toString());
  }

  protected void recordError(String msg) {
    log.error(msg);
    errors.add(msg);
  }

  protected String getSoftwareVersion() {
    String releaseName =
      BuildInfo.getBuildProperty(BuildInfo.BUILD_RELEASENAME);
    StringBuilder sb = new StringBuilder();
    sb.append("LOCKSS Daemon ");
    if (releaseName != null) {
      sb.append(releaseName);
    }
    return sb.toString();
  }

  protected String getHostIp() {
    try {
      IPAddr localHost = IPAddr.getLocalHost();
      return localHost.getHostAddress();
    } catch (UnknownHostException e) {
      log.error("getHostIp()", e);
      return "1.1.1.1";
    }
  }

  protected String getHostName() {
    String res = ConfigManager.getPlatformHostname();
    if (res == null) {
      try {
	InetAddress inet = InetAddress.getLocalHost();
	return inet.getHostName();
      } catch (UnknownHostException e) {
	log.warning("Can't get hostname", e);
	return "unknown";
      }
    }
    return res;
  }

  protected Properties filterResponseProps(Properties props) {
    Properties res = new Properties();
    for (Map.Entry ent : props.entrySet()) {
      String key = (String)ent.getKey();
      if (StringUtil.startsWithIgnoreCase(key, "x-lockss")
	  || StringUtil.startsWithIgnoreCase(key, "x_lockss")
	  || key.equalsIgnoreCase("org.lockss.version.number")) {
	continue;
      }
      // We've lost the original case - capitalize them the way most people
      // expect
      res.put(StringUtil.titleCase(key, '-'), (String)ent.getValue());
    }
    return res;
  }

  protected String getHttpResponseString(CachedUrl cu) {
    Properties cuProps = cu.getProperties();
    Properties filteredProps = filterResponseProps(cuProps);
    String hdrString = PropUtil.toHeaderString(filteredProps);
    StringBuilder sb = new StringBuilder(hdrString.length() + 30);
    String line1 = inferHttpResponseCode(cu, cuProps);
    sb.append(line1);
    sb.append(Constants.CRLF);
    sb.append(hdrString);
    sb.append(Constants.CRLF);
    return sb.toString();
  }

  String inferHttpResponseCode(CachedUrl cu, Properties cuProps) {
    if (cuProps.get("location") == null) {
      return "HTTP/1.1 200 OK";
    } else {
      return "HTTP/1.1 302 Found";
    }
  }

  // return the next CU with content
  private CachedUrl getNextCu(CuIterator iter) {
    return iter.hasNext() ? iter.next() : null;
  }

  /** Return true if, interpreting URLs as filenames, dirCu is a directory
   * containing fileCu.  Used to exclude directory content from output
   * files, so they can be unpacked by standard utilities (e.g., unzip).
   * Shouldn't be called with equal URLs, but return false in that case, as
   * we wouldn't want to exclude the URL */
  boolean isDirOf(CachedUrl dirCu, CachedUrl fileCu) {
    String dir = dirCu.getUrl();
    String file = fileCu.getUrl();
    if (!dir.endsWith("/")) {
      dir = dir + "/";
    }
    return file.startsWith(dir) && !file.equals(dir);
  }

  private void writeFiles() {
    PlatformUtil platutil = PlatformUtil.getInstance();
    CuIterator iter = AuUtil.getCuIterator(au);
    int errs = 0;
    CachedUrl curCu = null;
    CachedUrl nextCu = getNextCu(iter);
    while (nextCu != null) {
      curCu = nextCu;
      nextCu = getNextCu(iter);
      if (excludeDirNodes && nextCu != null && isDirOf(curCu, nextCu)) {
	continue;
      }
      CachedUrl[] cuVersions =
	curCu.getCuVersions(maxVersions > 0
			    ? maxVersions : Integer.MAX_VALUE);
      for (CachedUrl cu : cuVersions) {
	try {
	  log.debug2("Exporting " + cu.getUrl());
	  writeCu(cu);
	} catch (IOException e) {
	  if (platutil.isDiskFullError(e)) {
	    recordError("Disk full, can't write export file.");
	    isDiskFull = true;
	    return;
	  }
	} catch (Exception e) {
	  // XXX Would like to differentiate between errors opening or
	  // reading CU, which shouldn't cause abort, and errors writing
	  // to export file, which should.
	  recordError("Unable to copy " + cu.getUrl(), e);
	  if (errs++ >= maxErrors) {
	    recordError("Aborting after " + errs + " errors");
	    return;
	  }
	}
      }
    }
  }

  public interface Factory {
    public Exporter makeExporter(LockssDaemon daemon, ArchivalUnit au);
  }      

  static final String WINDOWS_FROM = "?<>:*|\\";
  static final String WINDOWS_TO =   "_______";
  static final String MAC_FROM = ":";
  static final String MAC_TO =   "_";

  /** Enum of filename translation types */
  public static enum FilenameTranslation {
    XLATE_NONE("None") {
      public String xlate(String s) {
	return s;
      }
    },
    XLATE_WINDOWS("Windows") {
      public String xlate(String s) {
	return StringUtils.replaceChars(s, WINDOWS_FROM, WINDOWS_TO);
      }
    },
    XLATE_MAC("MacOS") {
      public String xlate(String s) {
	return StringUtils.replaceChars(s, MAC_FROM, MAC_TO);
      }
    };

    private final String label;

    FilenameTranslation(String label) {
      this.label = label;
    }

    public String getLabel() {
      return label;
    }

    public abstract String xlate(String s);
  };


  /** Enum of Exporter types, and factories */
  public static enum Type implements Factory {
    ARC_RESOURCE("ARC (content only)") {
      public Exporter makeExporter(LockssDaemon daemon, ArchivalUnit au) {
	return new ArcExporter(daemon, au, false);
      }
    },
    ARC_RESPONSE("ARC (response and content)") {
      public Exporter makeExporter(LockssDaemon daemon, ArchivalUnit au) {
	return new ArcExporter(daemon, au, true);
      }
    },
    WARC_RESOURCE("WARC (content only)") {
      public Exporter makeExporter(LockssDaemon daemon, ArchivalUnit au) {
	return new WarcExporter(daemon, au, false);
      }
    },
    WARC_RESPONSE("WARC (response and content)") {
      public Exporter makeExporter(LockssDaemon daemon, ArchivalUnit au) {
	return new WarcExporter(daemon, au, true);
      }
    },
    ZIP("ZIP") {
      public Exporter makeExporter(LockssDaemon daemon, ArchivalUnit au) {
	return new ZipExporter(daemon, au);
      }
    };

    private final String label;

    Type(String label) {
      this.label = label;
    }

    public abstract Exporter makeExporter(LockssDaemon daemon,
					  ArchivalUnit au);

    public String getLabel() {
      return label;
    }
  };
}
