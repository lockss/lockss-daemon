/*
 * $Id: ArcCrawler.java,v 1.3.10.1 2007-09-11 19:14:54 dshr Exp $
 */

/*

Copyright (c) 2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.crawler;

import java.util.*;
import java.io.*;
import org.archive.io.*;
import org.archive.io.arc.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.state.*;

/**
 * A crawler that extends NewContentCrawler to both ingest Internet
 * Archive ARC files,  and to behave as if it had ingested each
 * file in each ARC file directly from its original source.
 *
 * @author  David S. H. Rosenthal
 * @version 0.0
 */

public class ArcCrawler extends NewContentCrawler {

  private static Logger logger = Logger.getLogger("ArcCrawler");
  private String arcUrl = null;
  protected static final String PARAM_ARC_FILE_SUFFIX =
    Configuration.PREFIX + "ArcCrawler.suffix";
  private static final String DEFAULT_ARC_FILE_SUFFIX = "\\.arc\\.gz$";
  protected String arcFileExtension = DEFAULT_ARC_FILE_SUFFIX;
  protected static final String PARAM_EXPLODE_ARC_FILES =
    Configuration.PREFIX + "ArcCrawler.explodeArcFiles";
  private static final boolean DEFAULT_EXPLODE_ARC_FILES = true;
  protected boolean explodeArcFiles = DEFAULT_EXPLODE_ARC_FILES;
  private Configuration myConfig = null;
  

  public ArcCrawler(ArchivalUnit au, CrawlSpec crawlSpec, AuState aus) {
    super(au, crawlSpec, aus);
    exploderPattern = crawlSpec.getExploderPattern();
    if (exploderPattern == null) {
      exploderPattern = DEFAULT_ARC_FILE_SUFFIX;
    }
    crawlSpec.setExploderPattern(exploderPattern);
  }

  protected void setCrawlConfig(Configuration config) {
    super.setCrawlConfig(config);
    arcFileExtension = config.get(PARAM_ARC_FILE_SUFFIX,
				  DEFAULT_ARC_FILE_SUFFIX);
    explodeArcFiles = config.getBoolean(PARAM_EXPLODE_ARC_FILES,
					DEFAULT_EXPLODE_ARC_FILES);
    logger.info("Files ending " + arcFileExtension +
		(explodeArcFiles ? " will" : " won't") +
		" be exploded");
    explodeFiles = explodeArcFiles;
    storeArchive = true;
  }

  public int getType() {
    return Crawler.ARC;
  }

  public String getTypeString() {
    return "ARC";
  }

}
