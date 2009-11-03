/*
 * $Id: BaseConfigFile.java,v 1.4.30.1 2009-11-03 23:44:51 edwardsb1 Exp $
 */

/*

Copyright (c) 2001-2006 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

/**
 * Common functionality for a config file loadable from a URL or filename,
 * and parseable as either XML or props.
 */
public abstract class BaseConfigFile implements ConfigFile {
  protected static Logger log = Logger.getLogger("ConfigFile");

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
  protected int m_generation = 0;
  protected Map m_props;
  protected ConfigManager.KeyPredicate keyPred;

  /**
   * Create a ConfigFile for the URL
   */
  public BaseConfigFile(String url) {
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

  public Generation getGeneration() throws IOException {
    ensureLoaded();
    synchronized (this) {
      return new Generation(this, m_config, m_generation);
    }
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

  public void setConnectionPool(LockssUrlConnectionPool connPool) {
  }

  public void setProperty(String key, Object val) {
    if (m_props == null) {
      m_props = new HashMap();
    }
    m_props.put(key, val);
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
    } catch (FileNotFoundException ex) {
      log.debug2("File not found: " + m_fileUrl);
      m_IOException = ex;
      m_loadError = ex.toString();
      throw ex;
    } catch (IOException ex) {
      log.warning("Unexpected exception loading " + m_fileUrl + ": " + ex);
      m_IOException = ex;
      if (m_loadError == null ||
	  !StringUtil.equalStrings(ex.getMessage(), m_loadError)) {
	// Some subs set m_loadError to exception message.  Don't overwrite
	// those with message that includes java exception class
	m_loadError = ex.toString();
      }
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
      filterConfig(newConfig);
      // update stored configuration atomically
      newConfig.seal();
      m_config = newConfig;
      m_loadError = null;
      m_IOException = null;
      m_lastModified = calcNewLastModified();
      m_generation++;
      m_needsReload = false;
    } catch (IOException ex) {
      throw ex;
    } catch (Exception ex) {
      log.debug("Unexpected non-IO error loading configuration", ex);
      throw new IOException(ex.toString());
    }
  }

  public void setKeyPredicate(ConfigManager.KeyPredicate pred) {
    keyPred = pred;
  }

  protected void filterConfig(Configuration config) throws IOException {
    if (keyPred != null) {
      List<String> delKeys = null;
      for (String key : config.keySet()) {
	if (!keyPred.evaluate(key)) {
	  String msg = "Illegal config key: " + key + " = "
	    + StringUtils.abbreviate(config.get(key), 50) + " in " + m_fileUrl;
	  if (keyPred.failOnIllegalKey()) {
	    log.error(msg);
	    throw new IOException(msg);
	  } else {
	    log.warning(msg);
	    if (delKeys == null) {
	      delKeys = new ArrayList<String>();
	    }
	    delKeys.add(key);
	  }
	}
      }
      if (delKeys != null) {
	for (String key : delKeys) {
	  config.remove(key);
	}
      }	
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
