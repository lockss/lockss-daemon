/*
 * $Id: MemoryConfigFile.java,v 1.1 2006-04-05 22:32:03 tlipkis Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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
 * A ConfigFile wrapper for an in-memory Configuration
 */
public class MemoryConfigFile implements ConfigFile {

  private String url;
  private int fileType;
  private Generation gen;
  private String m_lastModified;
  // FileConfigFile assumes the url doesn't change
  private String m_loadError = "Not yet loaded";
  private IOException m_IOException;
  private long m_lastAttempt;
  private boolean m_needsReload = true;
  private boolean m_isPlatformFile = false;
  private ConfigurationPropTreeImpl m_config;
  private int m_generation = 0;

  /**
   * Create a ConfigFile for the URL
   */
//   public MemoryConfigFile(String url, Generation gen) {
//     this.url = url;
//     this.gen = gen;
//   }

  /**
   * Create a ConfigFile for the URL
   */
  public MemoryConfigFile(String url, Configuration config, int gen) {
    this.url = url;
//     this.gen = new ConfigFile.Generation(this, config, gen) ;
    this.gen = new MyGeneration(this, config, gen) ;
//     log.info("MemoryConfigFile: " + config);
  }

  public String getFileUrl() {
    return url;
  }

  public boolean isPlatformFile() {
    return false;
  }

  public int getFileType() {
    return ConfigFile.PROPERTIES_FILE;
  }

  public String getLastModified() {
    return "last";
  }

  public long getLastAttemptTime() {
    throw new UnsupportedOperationException();
  }

  public Generation getGeneration() throws IOException {
    return gen;
  }

  public String getLoadErrorMessage() {
    throw new UnsupportedOperationException();
  }

  public boolean isLoaded() {
    return true;
  }

  private void ensureLoaded() throws IOException {
  }

  protected boolean isCheckEachTime() {
    return true;
  }

  public void setNeedsReload() {
  }

  public Configuration getConfiguration() throws IOException {
    return gen.getConfig();
  }

  class MyGeneration extends ConfigFile.Generation {
    private int generation;
    public MyGeneration(ConfigFile cf, Configuration config, int generation) {
      super(cf, config, generation);
      this.generation = generation;
    }
    public int getGeneration() {
      return generation++;
    }
  }

}

