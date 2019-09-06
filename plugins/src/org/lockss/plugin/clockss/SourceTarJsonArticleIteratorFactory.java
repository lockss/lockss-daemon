/*
 * Copyright (c) 2019 Board of Trustees of Leland Stanford Jr. University,
 * all rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Stanford University shall not
 * be used in advertising or otherwise to promote the sale, use or other dealings
 * in this Software without prior written authorization from Stanford University.
 */

package org.lockss.plugin.clockss;

import java.util.regex.Pattern;

import org.lockss.util.Logger;

public class SourceTarJsonArticleIteratorFactory {

  // ROOT_TEMPLATE doesn't need to be defined as sub-tree is entire tree under base/year
  // pull out explicit use of "year" in pattern as it could be any directory (2018, 2018_A, etc)
  protected static final String ALL_ZIP_JSON_PATTERN_TEMPLATE =
      "\"%s[^/]+/.*\\.tar!/.*\\.json$\", base_url";
  // Be sure to exclude all nested archives in case supplemental data is provided this way
  protected static final Pattern SUB_NESTED_ARCHIVE_PATTERN =
      Pattern.compile(".*/[^/]+\\.tar!/.+\\.(zip|tar|gz|tgz|tar\\.gz)$",
          Pattern.CASE_INSENSITIVE);
  protected static Logger log = Logger.getLogger(SourceTarJsonArticleIteratorFactory.class);

  protected Pattern getExcludeSubTreePattern() {
    return SUB_NESTED_ARCHIVE_PATTERN;
  }

  protected String getIncludePatternTemplate() {
    return ALL_ZIP_JSON_PATTERN_TEMPLATE;
  }

  protected boolean getIsArchive() {
    return true;
  }

}
