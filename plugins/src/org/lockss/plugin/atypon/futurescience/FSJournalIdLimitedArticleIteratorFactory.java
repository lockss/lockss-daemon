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

package org.lockss.plugin.atypon.futurescience;

import java.util.Iterator;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.SubTreeArticleIteratorBuilder;
import org.lockss.plugin.atypon.BaseAtyponArticleIteratorFactory;
import org.lockss.util.Logger;

public class FSJournalIdLimitedArticleIteratorFactory extends BaseAtyponArticleIteratorFactory {

  private static final Logger log = Logger.getLogger(FSJournalIdLimitedArticleIteratorFactory.class);
  
  
  // 1. This is a bit risky but a check of all DOI's collected up until 2017 shows a consistent use of the
  //    journal id as the starting three letters - which might then have a dot, a hyphen or, in one case, no
  //      delimeter before continuing. True for both Future Science and Future Medicine journals 
  //      Examples: 10.4155/tde-2017-0041, 10.2217/3dp-2017-0012, 10.4155/tde.12.2, 10.2217/imt1611c1
  //        10.2217/FCA.13.23       
  // 2. In 2013, clockss massively overcrawled and the metadata collected didn't include the publication 
  //    level information (dc.*, not ris) so the same doi shows up across many journals. To get it associated
  //    with the correct journals - putting in this AI fix. Then we'll scrub and re-extract.
  // This must be a case-insensitive pattern template
  private static final String JID_LIMITED_PATTERN_TEMPLATE_WITH_ABSTRACT = 
	      "\"^%sdoi/((abs|full|pdf|pdfplus)/)?[.0-9]+/%s\", base_url, journal_id";
  private static final String JID_LIMITED_PATTERN_TEMPLATE = 
	      "\"^%sdoi/((full|pdf|pdfplus)/)?[.0-9]+/%s\", base_url, journal_id";
  @Override
  protected String getPatternTemplate() {
	  return JID_LIMITED_PATTERN_TEMPLATE;
  }
  @Override
  protected String getPatternWithAbstractTemplate() {
	  return JID_LIMITED_PATTERN_TEMPLATE_WITH_ABSTRACT;
  }

  
}

