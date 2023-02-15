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

package org.lockss.plugin.clockss.nature;

import org.lockss.plugin.clockss.SourceZipXmlArticleIteratorFactory;
import org.lockss.util.Logger;

//
// Very slight variant on the std zip article iterator to ignore some 
// oddball ".xml" files that were delivered - limit to the known pattern
//
public class NpgZipXmlArticleIteratorFactory extends SourceZipXmlArticleIteratorFactory {

  private static final Logger log = Logger.getLogger(NpgZipXmlArticleIteratorFactory.class);
  
  // ROOT_TEMPLATE doesn't need to be defined as sub-tree is entire tree under base/year
  
  // For bonekey/nature, we only want to iterate on the bonekey####.xml that live at either
  // the top of the archive or under an "xml" or "xml_temp" directory.
  // Ignore the xml files that have boneke####_test or
  // bonekey20040128test.xml, bonekey201453_test.xml, and one in a deep set of directories...
  // This plugin will emit even if it doesn't have a matching pdf so need to exclude
  // extraneous XML files
  // drat - they redelivered two 'fixed' zips and these break the mold
  // bonekey_2001_bonekey2001032_xml_pdf/xml/bonekey2001032.xml
  // so allow this as well...
  protected static final String ONLY_BONEKEY_ARTICLE_XML_TEMPLATE =
      "\"%s%d/.*\\.zip!/(bonekey_[0-9]+_bonekey[0-9]+_xml_pdf/)?([^/]+/)?bonekey[0-9]+\\.xml$\", base_url, year";

  @Override
  protected String getIncludePatternTemplate() {
    return ONLY_BONEKEY_ARTICLE_XML_TEMPLATE ;
  }
}
