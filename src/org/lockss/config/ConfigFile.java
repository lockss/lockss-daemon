/*
 * $Id: ConfigFile.java,v 1.8 2005-07-09 22:26:30 tlipkis Exp $
 */

/*

Copyright (c) 2001-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.config;

import java.io.*;
import org.lockss.util.*;

/**
 * Common functionality for a config file loadable from a URL or filename,
 * and parseable as either XML or props.
 */
public abstract class ConfigFile {
  protected static Logger log = Logger.getLogger("ConfigFile");

  public static final int XML_FILE = 0;
  public static final int PROPERTIES_FILE = 1;

  protected int m_fileType;
  protected String m_lastModified;
  // FileConfigFile assumes the url doesn't change
  protected String m_fileUrl;
  protected String m_loadError = "Not yet loaded";
  protected IOException m_IOException;
  protected long m_lastAttempt;
  protected boolean m_needsReload = true;
  protected boolean m_isPlatformFile = false;
  protected ConfigurationPropTreeImpl m_config;

  /**
   * Create a ConfigFile for the URL
   */
  public ConfigFile(String url) {
    if (StringUtil.endsWithIgnoreCase(url, ".xml")) {
      m_fileType = ConfigFile.XML_FILE;
    } else {
      m_fileType = ConfigFile.PROPERTIES_FILE;
    }
    m_fileUrl = url;
    m_isPlatformFile = StringUtil.endsWithIgnoreCase(m_fileUrl, "local.txt");
  }

  public String getFileUrl() {
    return m_fileUrl;
  }

  /** Return true if this file might contain platform values that are
   * needed in order to properly parse other config files.
   */
  public boolean isPlatformFile() {
    return m_isPlatformFile;
  }

  public int getFileType() {
    return m_fileType;
  }

  public String getLastModified() {
    return m_lastModified;
  }

  public long getLastAttemptTime() {
    return m_lastAttempt;
  }

  public String getLoadErrorMessage() {
    return m_loadError;
  }

  public boolean isLoaded() {
    return m_loadError == null;
  }

  private void ensureLoaded() throws IOException {
    if (m_needsReload || isCheckEachTime()) {
      reload();
    }
  }

  /** Return true if the file should be checked for modification each time
   * getConfiguration() is called.  If false, the file will only be checked
   * on the first call, and after calls to setNeedsReload().  Subclasses
   * use this to modify the default behavior.
   */
  protected boolean isCheckEachTime() {
    return true;
  }

  /**
   * Instruct the ConfigFile to check for modifications the next time it's
   * accessed
   */
  public void setNeedsReload() {
    m_needsReload = true;
  }

  /** Return the Configuration object built from this file
   */
  public Configuration getConfiguration() throws IOException {
    ensureLoaded();
    return m_config;
  }

  /**
   * Reload the contents if changed.
   */
  protected void reload() throws IOException {
    m_lastAttempt = TimeBase.nowMs();
    try {
      InputStream in = openInputStream();
      if (in != null) {
	try {
	  setConfigFrom(in);
	} finally {
	  IOUtil.safeClose(in);
	}
      }
    } catch (IOException ex) {
      log.warning("Unexpected exception trying to load " +
		  "config file (" + m_fileUrl + "): " + ex);
      m_IOException = ex;
      m_loadError = ex.toString();
      throw ex;
    }
  }

  protected void setConfigFrom(InputStream in) throws IOException {
    ConfigurationPropTreeImpl newConfig = new ConfigurationPropTreeImpl();
    
    try {
      // Load the configuration
      if (m_fileType == XML_FILE) {
	XmlPropertyLoader.load(newConfig.getPropertyTree(), in);
      } else {
	newConfig.getPropertyTree().load(in);
      }
      // update stored configuration atomically
      newConfig.seal();
      m_config = newConfig;
      m_loadError = null;
      m_IOException = null;
      m_lastModified = calcNewLastModified();
      m_needsReload = false;
      log.debug2("Storing this config's last-modified as: " +
		 m_lastModified);
    } catch (IOException ex) {
      throw ex;
    } catch (Exception ex) {
      log.debug("Unexpected non-IO error loading configuration", ex);
      throw new IOException(ex.toString());
    }
  }

  /**
   * Return an InputStream on the contents of the file, or null if the file
   * hasn't changed.
   */
  protected abstract InputStream openInputStream() throws IOException;

  /**
   * Return the new las-modified time
   */
  protected abstract String calcNewLastModified();

  /**
   * Used for logging and testing and debugging.
   */
  public String toString() {
    return "{url=" + m_fileUrl + "; isLoaded=" + (m_config != null) +
      "; lastModified=" + m_lastModified + "}";
  }
}
