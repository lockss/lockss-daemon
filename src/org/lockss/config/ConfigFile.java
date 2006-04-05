/*
 * $Id: ConfigFile.java,v 1.11 2006-04-05 22:29:12 tlipkis Exp $
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
import org.lockss.util.*;

/**
 * Common functionality for a config file loadable from a URL or filename,
 * and parseable as either XML or props.
 */
public interface ConfigFile {
  public static final int XML_FILE = 0;
  public static final int PROPERTIES_FILE = 1;

  public String getFileUrl();

  /** Return true if this file might contain platform values that are
   * needed in order to properly parse other config files.
   */
  public boolean isPlatformFile();

  public int getFileType();

  public String getLastModified();

  public long getLastAttemptTime();

  public Generation getGeneration() throws IOException;

  public String getLoadErrorMessage();

  public boolean isLoaded();

  /**
   * Instruct the ConfigFile to check for modifications the next time it's
   * accessed
   */
  public void setNeedsReload();

  /** Return the Configuration object built from this file
   */
  public Configuration getConfiguration() throws IOException;

  /** Represents a single generation (version) of the contents of a
   * ConfigFile, to make it easy to determine when the contents has
   * changed */
  public class Generation {
    private ConfigFile cf;
    private Configuration config;
    private int generation;
    public Generation(ConfigFile cf, Configuration config, int generation) {
      this.cf = cf;
      this.config = config;
      this.generation = generation;
    }
    public Configuration getConfig() {
      return config;
    }
    public int getGeneration() {
      return generation;
    }
    public String getUrl() {
      return cf.getFileUrl();
    }
  }

}
