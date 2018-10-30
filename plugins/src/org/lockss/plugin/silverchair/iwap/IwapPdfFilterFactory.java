/*
 * $Id:$
 */

/*

 Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair.iwap;

import java.util.regex.Pattern;

import org.lockss.plugin.silverchair.BaseScPdfFilterFactory;
import org.lockss.util.Logger;


/**
 * A pdf filter removes the "Downloaded from... IP:.... on.... BT-ET section
 * 
 * These are located in content stream
 * and located on each page of the document
 * @author alexohlson
 *
 */
public class IwapPdfFilterFactory extends BaseScPdfFilterFactory {

  private static final Logger log = Logger.getLogger(IwapPdfFilterFactory.class);

  private static final String DOWNLOAD_REGEX_STRING = "^Downloaded from ";
  private static final Pattern DOWNLOAD_PATTERN = Pattern.compile(DOWNLOAD_REGEX_STRING);
  // It's all in one BT--ET, no need to find the rest
  
  @Override
  public Pattern getDownloadPattern() {
    return DOWNLOAD_PATTERN;
  }

  @Override
  public boolean doRemoveAllDocumentInfo() {
    return true;
  }

}
