/*
 * $Id$
 */

/*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.frontiers;

import java.util.regex.Pattern;

import org.lockss.plugin.clockss.SourceZipXmlArticleIteratorFactory;
import org.lockss.util.Logger;

//
// Extend the basic SourceZipXmlArticleIteratorFactory to exclude DataSheetx.zip files
//  we only want to "find" the article xml files that live within issue zips
//
public class FrontiersZipXmlArticleIteratorFactory extends SourceZipXmlArticleIteratorFactory {

  protected static Logger log = Logger.getLogger(FrontiersZipXmlArticleIteratorFactory.class);
  
  // Exclude supplemental DataSheetX.zip files that may contain xml
  // Be sure to exclude all nested archives in case supplemental data is provided this way
  protected static final Pattern ExcludeDataSheetAndSubTreePattern = 
      Pattern.compile(".*/(DataSheet[^.]+\\.zip|[^/]+\\.zip!/.+\\.(zip|tar|gz|tgz|tar\\.gz)$)", 
          Pattern.CASE_INSENSITIVE);
  protected Pattern getExcludeSubTreePattern() {
    return ExcludeDataSheetAndSubTreePattern;
  }
}
