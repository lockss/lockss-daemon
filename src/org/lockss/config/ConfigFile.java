/*
 * $Id: ConfigFile.java,v 1.7 2005-02-16 19:39:53 smorabito Exp $
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
import java.net.*;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

/**
 * A simple wrapper class around the text representation of a
 * generic config (either plain text or XML)
 */

public abstract class ConfigFile {
  public static final int XML_FILE = 0;
  public static final int PROPERTIES_FILE = 1;

  protected int m_fileType;
  protected String m_lastModified;
  // FileConfigFile assumes the url doesn't change
  protected String m_fileUrl;
  protected String m_loadError = "Not yet loaded";
  protected IOException m_IOException;
  protected long m_lastAttempt;
  protected ConfigurationPropTreeImpl m_config;

  protected static Logger log = Logger.getLogger("ConfigFile");

  /**
   * Read the contents of a file or the results of a URL fetch into
   * memory.
   *
   * @throws IOException
   */
  public ConfigFile(String url)
      throws IOException {

    if (StringUtil.endsWithIgnoreCase(url, ".xml")) {
      m_fileType = ConfigFile.XML_FILE;
    } else {
      m_fileType = ConfigFile.PROPERTIES_FILE;
    }

    m_fileUrl = url;
  }

  public String getFileUrl() {
    return m_fileUrl;
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

  public Configuration getConfiguration() {
    return m_config;
  }

  /**
   * Subclasses must implement this method to load a Configuration
   * object from the supplied URL (file or remote).
   */
  protected abstract boolean reload()
      throws IOException;

  protected void setConfigFrom(InputStream in)
      throws ParserConfigurationException, SAXException, IOException {
    ConfigurationPropTreeImpl newConfig = new ConfigurationPropTreeImpl();
    
    // Load the configuration
    if (m_fileType == XML_FILE) {
      XmlPropertyLoader.load(newConfig.getPropertyTree(), in);
    } else {
      newConfig.getPropertyTree().load(in);
    }
    
    // update stored configuration atomically
    newConfig.seal();
    m_config = newConfig;
  }

  /**
   * Used for logging and testing and debugging.
   */
  public String toString() {
    return "{url=" + m_fileUrl + "; isLoaded=" + (m_config != null) +
      "; lastModified=" + m_lastModified + "}";
  }
}
