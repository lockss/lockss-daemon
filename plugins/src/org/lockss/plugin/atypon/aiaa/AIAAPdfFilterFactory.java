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

package org.lockss.plugin.atypon.aiaa;

import java.util.regex.Pattern;

import org.lockss.plugin.atypon.BaseAtyponScrapingPdfFilterFactory;

/*
 * The AIAA pdf files have the CreationDate and ModDate and the two ID numbers in the trailer
 * vary from collection to collection. Filter them out to avoid incorrect hash failures.
 * Because of varying BASEFONT values, must also extract text/images for hash comparison
 */
public class AIAAPdfFilterFactory extends BaseAtyponScrapingPdfFilterFactory {
  
  public static final Pattern AIAA_DOWNLOAD_PATTERN = Pattern.compile("^Downloaded by");

  /* 
   * Turn on removal of "This article cited by:" pages - the default string is correct
   */
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
    return AIAA_DOWNLOAD_PATTERN;
  }  
}
