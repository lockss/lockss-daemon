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

package org.lockss.plugin.clockss.phildoc;

import org.lockss.plugin.clockss.SourceZipXmlArticleIteratorFactory;
import org.lockss.util.Logger;

//
// Cannot use the generic Zip XML article iterator because don't want the full
// text XML version of the articles, just the issue TOC XML files
//
public class PhilDocZipXmlArticleIteratorFactory extends SourceZipXmlArticleIteratorFactory {

  private static final Logger log = Logger.getLogger(PhilDocZipXmlArticleIteratorFactory.class);
    
  // We do not want to iterate on the full-text XML versions of the articles
  // just the issue TOC xml metadata
  //  YES: logos/logos_19-1/logos_19-1.xml 
  //   NO: logos/logos_19-1/xml/logos_2016_0019_0001_0005_0013.pdf.xml
  // identify by level under the zip file

  protected static final String ONLY_TOC_XML_PATTERN_TEMPLATE = 
      "\"%s%d/.*\\.zip!/[^/]+/[^/]+/[^/]+\\.xml$\", base_url, year";

  @Override
  protected String getIncludePatternTemplate() {
    return ONLY_TOC_XML_PATTERN_TEMPLATE ;
  }
}
