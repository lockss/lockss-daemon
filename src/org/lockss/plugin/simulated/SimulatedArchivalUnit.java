/*
 * $Id: SimulatedArchivalUnit.java,v 1.1 2002-10-23 23:43:05 aalto Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.simulated;

import gnu.regexp.RE;
import gnu.regexp.REException;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;

/**
 * This is ArchivalUnit of the simulated plugin, used for testing purposes.
 * It repeatably generates local content (via a file hierarchy),
 * with specific parameters obtained via Configuration.
 *
 * It emulates the fake URL 'www.simcontent.org'.
 *
 * @author  Emil Aalto
 * @version 0.0
 */

public class SimulatedArchivalUnit extends BaseArchivalUnit {
  public static final String SIMULATED_URL = "http://www.example.com";

  private String fileRoot; //root directory for the generated content
  private SimulatedContentGenerator scgen;

  public SimulatedArchivalUnit(String new_fileRoot) {
    super(new CrawlSpec(SIMULATED_URL, null));
    fileRoot = new_fileRoot;
    scgen = new SimulatedContentGenerator(fileRoot);
    if (!scgen.isContentTree()) {
      scgen.generateContentTree();
    }
  }

  public SimulatedArchivalUnit() {
    this("");
  }

  protected CachedUrlSet cachedUrlSetFactory(ArchivalUnit owner, CachedUrlSetSpec cuss) {
    return new SimulatedCachedUrlSet(owner, cuss);
  }

  protected CachedUrl cachedUrlFactory(CachedUrlSet owner, String url) {
    return new GenericFileCachedUrl(owner, url);
  }

  protected UrlCacher urlCacherFactory(CachedUrlSet owner, String url) {
    return new SimulatedUrlCacher(owner, url);
  }

  public CachedUrlSet getAUCachedUrlSet() {
    return new SimulatedCachedUrlSet(this,
               new RECachedUrlSetSpec(SIMULATED_URL));
  }

  // public methods
  public String getUrlRoot() { return fileRoot; }

  /**
   * resetContentTree() deletes and regenerates the simulated content,
   * restoring it to its starting state.
   */
  public void resetContentTree() {
    // clears and restores content tree to starting state
    if (scgen.isContentTree()) scgen.deleteContentTree();
    scgen.generateContentTree();
  }

  public void alterContentTree() {
    //XXX alters in a repeatable manner
  }

  /**
   * mapUrlToContentFileName()
   * This maps a given url to a content file location.
   *
   * @param url
   * @return fileName
   */
  public static String mapUrlToContentFileName(String url) {
    int lastSlashIdx = url.lastIndexOf("/");
    int lastPeriodIdx = url.lastIndexOf(".");

    String fileName = StringUtil.replaceString(url, SIMULATED_URL,
                                 SimulatedContentGenerator.ROOT_NAME);

    if (lastSlashIdx >= lastPeriodIdx) {
      if (url.charAt(url.length()-1) != '/') {
        fileName += "/";
      }
      fileName += "index.html";
    }
    return fileName;
  }

}
