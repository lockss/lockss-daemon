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

package org.lockss.plugin.atypon.futurescience;

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

    // On purpose get rid of "journal_id" from the patterns in 2022 since it no longer helpful,
    // but rather become a blocker from collecting articles without journal_id in url pattern
  private static final String JID_LIMITED_PATTERN_TEMPLATE_WITH_ABSTRACT = 
	      "\"^%sdoi/((abs|full|pdf|pdfplus)/)?[.0-9]+/\", base_url";
  private static final String JID_LIMITED_PATTERN_TEMPLATE = 
	      "\"^%sdoi/((full|pdf|pdfplus)/)?[.0-9]+/\", base_url";
  @Override
  protected String getPatternTemplate() {
	  return JID_LIMITED_PATTERN_TEMPLATE;
  }
  @Override
  protected String getPatternWithAbstractTemplate() {
	  return JID_LIMITED_PATTERN_TEMPLATE_WITH_ABSTRACT;
  }

  
}

