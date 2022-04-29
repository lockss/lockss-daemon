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

package org.lockss.plugin.atypon.endocrinesociety;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.*;

import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.atypon.BaseAtyponRisMetadataExtractorFactory.BaseAtyponRisMetadataExtractor;
import org.lockss.util.Logger;

/*
 * A subclass of the BaseAtypon version to handle weirdness in the Endocrine Books
 * RIS metadata. 
 * They provide an ISBN using the "SN" key but it isn't actually the isbn or eisbn
 *  So for now, don't use this value and instead pull the value from the DOI
 *  which seems to be of the form XXYZ.eisbn
 *  BOOKS ONLY - so just fall over if it is a journal article
 */
public class EndocrineRisMetadataExtractorFactory implements FileMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(EndocrineRisMetadataExtractorFactory.class);
  
  //(?:\/|%2F) is a non-capturing group for "/" or its normalized %2F version
  private static final Pattern URL_ISBN_PATTERN = Pattern.compile("action/downloadCitation\\?doi=[.0-9]+(?:\\/|%2F)[^.]+\\.([0-9X]{13})");

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {

    log.debug3("Inside Base Atypon Metadata extractor factory for RIS files");

    EndocrineRisMetadataExtractor endo_ris = new EndocrineRisMetadataExtractor();

    endo_ris.addRisTag("A1", MetadataField.FIELD_AUTHOR);
    // Do not use UR listed in the ris file! It will get set to full text CU by daemon
    return endo_ris;
  }

  public static class EndocrineRisMetadataExtractor
  extends BaseAtyponRisMetadataExtractor {


    /*
     * (non-Javadoc)
     * Endocrine seems to put bogus values in the SN field for books 
     * so pull a valid EISBN from the doi used in the url
     * otherwise the pre-emit check will fail...
     */
    @Override
    protected void postCookProcess(CachedUrl cu, ArticleMetadata am, String ris_type)  {

      // first do the regular stuff
      super.postCookProcess(cu,am,ris_type);
      // and then do a little extra
      if ( ris_type.contains("BOOK") || ris_type.contains("CHAP")) {
        String ris_url = cu.getUrl();
        log.debug3("book/chap ris and url is: " + ris_url);
        Matcher isbn_match = URL_ISBN_PATTERN.matcher(ris_url);
        if (isbn_match.find()) {
          String url_isbn = isbn_match.group(1);
          log.debug3("replacing ris isbn of: " + am.getRaw("SN") + " with " + url_isbn);
          // the isbn will still be wrong, but that's the way it goes...
          am.put(MetadataField.FIELD_EISBN,  url_isbn);
          // we have no way of removing this incorrect isbn, we'll have to leave it
        }
       // if not, there's nothing we can do but let it proceed and possibly not emit
      }
    }

  }

}
