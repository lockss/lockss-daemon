/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
