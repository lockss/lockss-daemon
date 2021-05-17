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

package org.lockss.plugin.clockss.mdpi;

import java.util.regex.Pattern;

import org.lockss.plugin.clockss.SourceZipXmlArticleIteratorFactory;
import org.lockss.util.Logger;

//
// Extend the basic SourceZipXmlArticleIteratorFactory to exclude the TOC xml files
//  we only want to "find" the article xml files
//
public class MdpiZipXmlArticleIteratorFactory extends SourceZipXmlArticleIteratorFactory {

  protected static Logger log = Logger.getLogger(MdpiZipXmlArticleIteratorFactory.class);
  
  // Be sure to exclude all nested archives in case supplemental data is provided this way
  // also exclude __ToC_blah.xml files - but it is hard to do a positive statement to exclude this.
  // The files we *do* want are of the form....
  // zip: /water-07-10.zip
  // issue TOC xml: __ToC_water_07_10.xml
  // article xml: water-07-05731.xml
  // the xml may not start with a double "__"
  protected static final String NOT_TOC_XML_TEMPLATE =
      "\"%s\\d{4}(_\\d{2})?/.*\\.zip!/[^_][^_].*\\.xml$\", base_url";

  @Override
  protected String getIncludePatternTemplate() {
    return NOT_TOC_XML_TEMPLATE;
  }
  
}
