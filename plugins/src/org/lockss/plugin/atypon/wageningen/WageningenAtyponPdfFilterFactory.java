/*
 * $Id$
 */

/*

 Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.wageningen;

import java.util.regex.Pattern;
import org.lockss.plugin.atypon.BaseAtyponScrapingPdfFilterFactory;

/**
 * in addition to default scraping and date/metadata removal
 * This plugin also has to turn on removal of the vertical watermark on the 
 * left side of the page.
 * Example: hhttp://www.wageningenacademic.com/doi/pdf/10.3920/JCNS2014.0233
 */
public class WageningenAtyponPdfFilterFactory 
  extends BaseAtyponScrapingPdfFilterFactory {
  // watermark:
  // http://www.wageningenacademic.com/doi/pdf/10.3920/BM2012.0069 - Wednesday, May 13, 2015 11:31:47 AM - Stanford University Libraries IP Address:171.66.236.234
  public static final Pattern DOWNLOAD_PATTERN = Pattern.compile("^http://www\\.wageningenacademic\\.com.*IP Address:\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");

  @Override
  public boolean doRemoveDownloadStrip() {
    return true;    
  }
  /* and set the correct string to use for this publisher */
  @Override
  public Pattern getDownloadStripPattern() {
    return DOWNLOAD_PATTERN;
  }
  
}
