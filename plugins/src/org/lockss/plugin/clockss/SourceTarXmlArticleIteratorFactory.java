/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.clockss;

import java.util.regex.Pattern;

import org.lockss.util.Logger;

//
// A generic article iterator for CLOCKSS source plugins that are zipped and
// want to iterate on files within the zipped file that end in .xml at some 
// level below the root directory. 
//
public class SourceTarXmlArticleIteratorFactory extends SourceXmlArticleIteratorFactory {

  protected static Logger log = Logger.getLogger(SourceTarXmlArticleIteratorFactory.class);
  
  // ROOT_TEMPLATE doesn't need to be defined as sub-tree is entire tree under base/year
    // pull out explicit use of "year" in pattern as it could be any directory (2018, 2018_A, etc)
  protected static final String ALL_ZIP_XML_PATTERN_TEMPLATE = 
      "\"%s[^/]+/.*\\.tar!/.*\\.xml$\", base_url";

  // Be sure to exclude all nested archives in case supplemental data is provided this way
  protected static final Pattern SUB_NESTED_ARCHIVE_PATTERN = 
      Pattern.compile(".*/[^/]+\\.tar!/.+\\.(zip|tar|gz|tgz|tar\\.gz)$", 
          Pattern.CASE_INSENSITIVE);

  protected Pattern getExcludeSubTreePattern() {
    return SUB_NESTED_ARCHIVE_PATTERN;
  }

  protected String getIncludePatternTemplate() {
    return ALL_ZIP_XML_PATTERN_TEMPLATE;
  }

  protected boolean getIsArchive() {
    return true;
  }
}
