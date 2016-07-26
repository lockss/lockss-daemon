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

import java.util.regex.Pattern;

import org.lockss.plugin.clockss.SourceZipXmlArticleIteratorFactory;
import org.lockss.util.Logger;

//
// Cannot use the generic Zip XML article iterator because don't want the full
// text XML version of the articles, just the issue TOC XML files
// Delivery layout has changed since the sample. 
// 
// sample was a per-journal subdir, with issue subdirs under that,
// each of which had an issue XML file and then pdf and xml subdirs for the
// pdf and full-text XML versions of the articles.
// now they do a per-journal zip within the zip and no issue subdir, just
// one or more issue XML files. The content files are at the same level as the XML
// files.
// eg
//     clockss2016_7.zip!/acpq.zip/
//     clockss2016_7.zip!/acpq.zip/acpq_90-3.xml (yes, iterator should find)
//     clockss2016_7.zip!/acpq.zip/acpq_2016_0090_0003_0395_0413.pdf
//     clockss2016_7.zip!/acpq.zip/acpq_2016_0090_0003_0395_0413.pdf.xml (not metadata, full text xml)
// There could be more than one issue XML within the zip (eg jcathsoc_13-1.xml and jcathsoc_13-2.xml)
// along with all the corresponding PDF content
//
public class PhilDocZipXmlArticleIteratorFactory extends SourceZipXmlArticleIteratorFactory {

  private static final Logger log = Logger.getLogger(PhilDocZipXmlArticleIteratorFactory.class);

  // The iterator pattern supports both the original sample layout of for journal foo of 
  //   clockss.zip!/foo/foo_3-7/foo_3-7.xml 
  // as well as the newer, nested zip format of 
  //   clockss.zip!/foo.zip/foo_3-7.xml
  // note that the second zip doesn't have a "!" in our cache structure.
  // This pattern is dependent on the naming of the subdirs and TOC with a consistent 
  // journal id for the subdir, which may or may not be a zip (eg foo or foo.zip) 
  // and for the optional issue subdir (foo_3-2/)
  // before finding the issue toc which will start with the same id (foo_3-3.xml)

  // Now that there is the possibility that the article.pdf.xml file could be in the same
  // directory as the issue toc XML "file foo_#-#" add the requirement that there
  // not be a "." in the filename of the XML file. 
    
  //2016\/[^/]+\.zip!\/([^/]+)(\.zip)?\/(\1_[^/]+\/)?\1_[^/.]+\.xml$

  // exclusion of the "." in the TOC name will exclude the full text XML
  // even if they're in the same directory as the issue XML
  // no "bang" on the second zip in our cache structure.
  // capture group one will be the journal_id used as the subdir or zip name
  // expected that the issue XML and optional issue subdir will start with the 
  // same journal_id
  protected static final String ONLY_TOC_XML_PATTERN_TEMPLATE = 
      "\"%s%d/.*\\.zip!/([^/]+)(\\.zip)?/(\\1_[^/]+/)?\\1_[^/.]+\\.xml$\", base_url, year";
  //  "\"%s%d/.*\\.zip!/[^/.]+\\.zip/[^/.]+\\.xml$\", base_url, year";
  
  // Unlike the default, we need to nest two down (top delivery and each journal zip)
  // but exclude any archives below that
  protected static final Pattern DEEP_NESTED_ARCHIVE_PATTERN = 
      Pattern.compile(".*/[^/]+\\.zip!/[^/.]+\\.zip/.+\\.(zip|tar|gz|tgz|tar\\.gz)$", 
          Pattern.CASE_INSENSITIVE);

  @Override
  protected String getIncludePatternTemplate() {
    return ONLY_TOC_XML_PATTERN_TEMPLATE ;
  }
  

  // We need to allow descending one more layer, but no more below that
  protected Pattern getExcludeSubTreePattern() {
    return DEEP_NESTED_ARCHIVE_PATTERN;
  }
}
