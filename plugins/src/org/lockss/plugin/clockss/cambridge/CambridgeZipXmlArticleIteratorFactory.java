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

package org.lockss.plugin.clockss.cambridge;

import java.util.regex.Pattern;

import org.lockss.plugin.clockss.SourceZipXmlArticleIteratorFactory;
import org.lockss.util.Logger;

//
// Extend the basic SourceZipXmlArticleIteratorFactory to exclude the TOC xml files
//  we only want to "find" the article xml files
//
public class CambridgeZipXmlArticleIteratorFactory extends SourceZipXmlArticleIteratorFactory {

  protected static Logger log = Logger.getLogger(CambridgeZipXmlArticleIteratorFactory.class);
  
  // Be sure to exclude all nested archives in case supplemental data is provided this way
  // each article comes with two xml files
  // <article_number>h.xml
  // <article_number>w.xml
  // the "h" xml file is just the front matter, which is all we need. Ignore the "w" file.
  protected static final Pattern EXCLUDE_XML_PATTERN = 
      Pattern.compile(".*/[^/]+\\.zip!/([^/]+w\\.xml|.+\\.(zip|tar|gz|tgz|tar\\.gz))$", 
          Pattern.CASE_INSENSITIVE);
  
  // For Cambridge, we only want to iterate on the blahblah/12345h.xml files, not the 12345w.xmlfiles
  protected static final String ONLY_H_XML_TEMPLATE =
      "\"%s%d/.*\\.zip!/.*h\\.xml$\", base_url, year";

  @Override
  protected String getIncludePatternTemplate() {
    return ONLY_H_XML_TEMPLATE;
  }
  
}
