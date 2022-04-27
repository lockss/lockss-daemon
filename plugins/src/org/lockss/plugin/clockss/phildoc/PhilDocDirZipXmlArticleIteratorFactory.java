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

package org.lockss.plugin.clockss.phildoc;

import org.lockss.plugin.clockss.SourceZipXmlArticleIteratorFactory;
import org.lockss.util.Logger;

import java.util.regex.Pattern;

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
public class PhilDocDirZipXmlArticleIteratorFactory extends SourceZipXmlArticleIteratorFactory {

  private static final Logger log = Logger.getLogger(PhilDocDirZipXmlArticleIteratorFactory.class);

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

  /*
  Note from Dec/2020

  The deliver has issue level xml and article level xml.
  The first one is issue level xml which contains all the metadata.
  The article level xml does not have much info.

  So we will get something.xml and exclude something.pdf.xml

  https://clockss-test.lockss.org/sourcefiles/phildoccenter-released/2020/PDC-backcontent-1.zip!/tpm_-1.zip/tpm_-1.xml
  https://clockss-test.lockss.org/sourcefiles/phildoccenter-released/2020/PDC-backcontent-1.zip!/tpm_-1.zip/tpm_1997_0001_0006_0007.pdf
  https://clockss-test.lockss.org/sourcefiles/phildoccenter-released/2020/PDC-backcontent-1.zip!/tpm_-1.zip/tpm_1997_0001_0006_0007.pdf.xml
  https://clockss-test.lockss.org/sourcefiles/phildoccenter-released/2020/PDC-backcontent-4.zip!/tptoday_7DD9805D8EDF5D93C1257A7F00655268.zip/tptoday_7DD9805D8EDF5D93C1257A7F00655268.xml
   */
  protected static final String ONLY_TOC_XML_PATTERN_TEMPLATE =
        "\"%s%s/.*\\.zip!/([^/]+)(\\.zip)?/(?!.*pdf).*\\.xml$\", base_url, directory";
  //    "\"%s%d/.*\\.zip!/([^/]+)(\\.zip)?/(\\1_[^/]+/)?\\1_[^/.]+\\.xml$\", base_url, year";
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
