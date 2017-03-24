/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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
