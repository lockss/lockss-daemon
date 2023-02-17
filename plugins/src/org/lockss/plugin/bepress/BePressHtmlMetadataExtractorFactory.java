/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.bepress;

import java.io.*;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.extractor.FileMetadataExtractor.Emitter;
import org.lockss.plugin.*;

/**
 * One of the articles used to get the html source for this plugin is:
 * http://www.bepress.com/cppm/vol5/iss1/17/
 *
 */
public class BePressHtmlMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("BePressHtmlMetadataExtractorFactory");

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    return new BePressHtmlMetadataExtractor();
  }

  public static class BePressHtmlMetadataExtractor 
    implements FileMetadataExtractor {

    // Map BePress-specific HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("bepress_citation_doi", MetadataField.DC_FIELD_IDENTIFIER);
      tagMap.put("bepress_citation_doi", MetadataField.FIELD_DOI);
      tagMap.put("bepress_citation_date", MetadataField.DC_FIELD_DATE);
      tagMap.put("bepress_citation_date", MetadataField.FIELD_DATE);
      tagMap.put("bepress_citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("bepress_citation_issue", MetadataField.FIELD_ISSUE);
      tagMap.put("bepress_citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("bepress_citation_author", MetadataField.FIELD_AUTHOR);
      tagMap.put("bepress_citation_authors",
                 new MetadataField(MetadataField.FIELD_AUTHOR,
                                   MetadataField.splitAt(";")));
      tagMap.put("bepress_citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("bepress_citation_journal_title", MetadataField.FIELD_JOURNAL_TITLE);
    }

    /**
     * Use SimpleHtmlMetaTagMetadataExtractor to extract raw metadata, map
     * to cooked fields, then extract the ISSN by reading the file.
     */
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
      throws IOException {
      ArticleMetadata am = 
        new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(tagMap);

      // XXX Should this be conditional on
      //     !am.hasValidValue(MetadataField.FIELD_ISSN) ?

      // The ISSN is not in a meta tag but we can find it in the source
      // code. Here we need to some string manipulation and use some REGEX
      // to find the right index where the ISSN number is located.
      BufferedReader bReader = new BufferedReader(cu.openForReading());
      try {
        for (String line = bReader.readLine();
             line != null; line = bReader.readLine()) {
          line = line.trim();
          if (   StringUtil.startsWithIgnoreCase(line, "<div id=\"issn\">") 
              || StringUtil.startsWithIgnoreCase(line, "<p>ISSN:")) {
            log.debug2("Line: " + line);
            addISSN(line, am);
          }
        }
      } finally {
        IOUtil.safeClose(bReader);
      }
      emitter.emitMetadata(cu, am);
    }
		
    /**
     * Extract the ISSN from a line of HTML source code.
     * @param line The HTML source code that contains the ISSN and should have the following form:
     * <div id="issn"><p><!-- FILE: /main/production/doc/data/templates/www.bepress.com/proto_bpjournal/assets/issn.inc -->ISSN: 1934-2659 <!-- FILE:/main/production/doc/data/templates/www.bepress.com/proto_bpjournal/assets/footer.pregen (cont) --></p></div> 
     * @param ret The ArticleMetadat object used to put the ISSN in extracted metadata
     */
    protected void addISSN(String line, ArticleMetadata ret) {
      String issnFlag = "ISSN: ";
      int issnBegin = StringUtil.indexOfIgnoreCase(line, issnFlag);
      if (issnBegin <= 0) {
	log.debug(line + " : no " + issnFlag);
	return;
      }
      issnBegin += issnFlag.length();
      String issn = line.substring(issnBegin, issnBegin + 9);
      if (issn.length() < 9) {
	log.debug(line + " : too short");
	return;
      }			
      ret.put(MetadataField.FIELD_ISSN, issn);
    }
  }
}
