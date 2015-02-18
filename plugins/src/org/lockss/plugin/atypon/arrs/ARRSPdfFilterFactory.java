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

package org.lockss.plugin.atypon.arrs;

import java.util.regex.Pattern;

import org.lockss.plugin.atypon.BaseAtyponScrapingPdfFilterFactory;

/**
 * ARRS - 
 * in addition to default scraping and date/metadata removal
 * This plugin also has to turn on removal of a "Downloaded by" strip
 * and the "This article cited by:" pages at the end of the document
 * EXAMPLE: http://www.ajronline.org/doi/pdfplus/10.2214/AJR.13.10940
 */
public class ARRSPdfFilterFactory extends BaseAtyponScrapingPdfFilterFactory {
  
  public static final Pattern ARRS_DOWNLOAD_PATTERN = Pattern.compile("^Downloaded from www\\.ajronline\\.org");

  @Override
  public boolean doRemoveCitedByPage() {
    return true;    
  }  
  @Override
  public boolean doRemoveDownloadStrip() {
    return true;    
  }
  /* and set the correct string to use for this publisher */
  @Override
  public Pattern getDownloadStripPattern() {
    return ARRS_DOWNLOAD_PATTERN;
  }

}
